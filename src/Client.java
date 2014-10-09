import java.io.*;
import java.net.*;
import java.nio.*;

/**
 * CSE 461
 * John Wilson, Samuel Felker, Jake Nash
 * Project 1
 */
public class Client {

	// delay before retrying after socket exception or connection refused in phase D
	private static final int TCP_FAILURE_DELAY = 100;
	private static final int MAX_FAILURES = 10; // max failures for phase B or phase D
	private static final int UDP_TIMEOUT = 100;
	private static final int DELAY_BETWEEN_STAGES = 100;
	private static final int RETRY_DELAY = 2000; // delay between restarting entire run
	private static final int LAST_THREE_SID = 345;
	private static final String URL = "amlia.cs.washington.edu";
	private static DatagramSocket udpSocket;
	private static Socket tcpSocket;
	private static InetAddress address;
	private static int num;
	private static int len;
	private static int udp_port;
	private static int secretA;
	private static int tcp_port;
	private static int secretB;
	private static int num2;
	private static int len2;
	private static int secretC;
	private static byte byteC;
	private static char c;
	private static int secretD;
	
    /**
     *  Runs the program until success
     */
    public static void main(String arg[]) throws IOException, InterruptedException {
        while (!run()) {
        	Thread.sleep(RETRY_DELAY);
        }
        System.out.println("secretA : " + secretA);
        System.out.println("secretB : " + secretB);
        System.out.println("secretC : " + secretC);
        System.out.println("secretD : " + secretD);
    }
    
    /**
     *  Runs phases A-D
     *  @return true if successful, false if phase B or D get stuck in a failure loop
     */
    private static boolean run() throws InterruptedException, IOException {
    	udpSocket = new DatagramSocket();
        address = InetAddress.getByName(URL);
        udpSocket.setSoTimeout(UDP_TIMEOUT);
        String phase = "A";
    	try {
        	doStageA();
        	phase = "B";
        	Thread.sleep(DELAY_BETWEEN_STAGES);
			doStageB();
			phase = "C";
		    tcpSocket = new Socket(address, tcp_port);
        	Thread.sleep(DELAY_BETWEEN_STAGES);
        	doStageC();
        	phase = "D";
        	Thread.sleep(DELAY_BETWEEN_STAGES);
			doStageD();
		} catch (SomethingWrongException e) {
        	System.out.println("\nStage " + phase + " failed " + MAX_FAILURES + " times, starting over...");
			return false;
		}
    	return true;
    }
    
    private static void doStageA() throws IOException, InterruptedException, SomethingWrongException {
    	System.out.println("*** Commence STAGE A ***");
    	try {
          	byte[] payload = createPayload(0, 1, getByteArrayFromString("hello world"));
          	printBits(payload);
            byte[] result = sendUDPMessage(payload, 12235);
            ByteBuffer buffer = ByteBuffer.wrap(result);
            num = buffer.getInt(12);
            len = buffer.getInt(16);
            udp_port = buffer.getInt(20);
            secretA = buffer.getInt(24);
            System.out.println("*** STAGE A Success ***");
            System.out.println("num : " + num);
            System.out.println("len : " + len);
            System.out.println("udp_port : " + udp_port);
        }
        catch (SocketTimeoutException e) {
            throw new SomethingWrongException();
        }
    }
    
    private static void doStageB() throws SomethingWrongException, IOException, InterruptedException {
        System.out.println("*** Commence STAGE B ***");
        for (int i = 0; i < num; i++) {
            System.out.print("packet " + i + " : ");
            ByteBuffer buffer = ByteBuffer.allocate(len);
            buffer.putInt(i);
        	byte[] payload = createPayload(secretA, 1, buffer.array());
            int failureCount = 0;
            boolean success = false;
            while (!success) {
            	if (failureCount == MAX_FAILURES)
            		throw new SomethingWrongException();
	            try {
	                byte[] result = sendUDPMessage(payload, udp_port);
	                ByteBuffer resultBuffer = ByteBuffer.wrap(result);
	                System.out.println("success, ack_packet_id " + resultBuffer.getInt(12));
	                success = true;
	            }
	            catch (SocketTimeoutException e) {
	                System.out.print("timeout ");
	                failureCount++;
	            }
            }
            System.out.println("success");
        }
        byte[] finalPacket = receivePacket();
        ByteBuffer finalPacketBuffer = ByteBuffer.wrap(finalPacket);
        tcp_port = finalPacketBuffer.getInt(12);
        secretB = finalPacketBuffer.getInt(16);
        System.out.println("*** STAGE B Success ***");
        System.out.println("tcp_port : " + tcp_port);
    }
    
    private static void doStageC() throws IOException {
		System.out.println("*** Commence STAGE C ***");
		InputStream inStream = tcpSocket.getInputStream();
		byte[] result = receiveTCPMessage(inStream);
		ByteBuffer buffer = ByteBuffer.wrap(result);
		num2 = buffer.getInt(12);
		len2 = buffer.getInt(16);
		secretC = buffer.getInt(20);
	    byteC = buffer.get(24);
		c = (char) ((int) byteC);
        System.out.println("*** STAGE C Success ***");
		System.out.println("num2 : " + num2);
		System.out.println("len2 : " + len2);
		System.out.println("byteC : " + byteC);
		System.out.println("c : " + c);
    }
    
    private static void doStageD() throws IOException, InterruptedException, SomethingWrongException {
		System.out.println("*** Commence STAGE D ***");
		DataOutputStream outStream = new DataOutputStream(tcpSocket.getOutputStream());
		InputStream inStream = tcpSocket.getInputStream();
		byte[] data = new byte[len2];
		for (int i = 0; i < len2; i++)
		    data[i] = byteC;
		for (int i = 0; i < num2; i++) {
			System.out.print("packet " + i + " : ");
			int failureCount = 0;
			boolean success = false;
			while (!success) {
			    if (failureCount == MAX_FAILURES)
			        throw new SomethingWrongException();
		        try {
				    byte[] payload = createPayload(secretC, 1, data);
				    sendTCPMessage(payload, outStream);
				    System.out.println("success");
				    success = true;
		        }
		        catch (ConnectException e) {
		            failureCount++;
				    System.out.print("connection refused ");
				    Thread.sleep(TCP_FAILURE_DELAY);
		        }
		        catch (SocketException e2) {
		            failureCount++;
		            e2.printStackTrace();
				    System.out.print("socket exception ");
				    Thread.sleep(TCP_FAILURE_DELAY);
		        }
		    }
		}
		byte[] result = receiveTCPMessage(inStream);
		ByteBuffer buffer = ByteBuffer.wrap(result);
		secretD = buffer.getInt(12);
		if (secretD == 0) {
		    System.out.println("secretD came back as 0 :(");
		    System.out.println("bits from response:");
		    printBits(result);
		    System.out.println("starting over...");
		    throw new SomethingWrongException();
		}
    }
    
    public static byte[] get2ByteArray(int value) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(value);
        byte[] buff = b.array();
        byte[] digits = new byte[2];
        digits[0] = buff[2];
        digits[1] = buff[3];
        return digits;
    }
    
    public static byte[] get4ByteArray(int value) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(value);
        byte[] buff = b.array();
        return buff;
    }
    
    public static byte[] getByteArrayFromString(String s) {
        int size = s.length() + 1;
        ByteBuffer b = ByteBuffer.allocate(size * 2);
        for (char c : s.toCharArray())
            b.putChar(c);
        byte[] data = b.array();
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = data[i * 2 + 1];
        }
        return result;
    }

    private static byte[] createPayload(int secretInt, int stepInt, byte[] payload) {
    	byte[] digits = get2ByteArray(LAST_THREE_SID);
        byte[] secret = get4ByteArray(secretInt);
        byte[] step = get2ByteArray(stepInt);
        byte[] payload_len = get4ByteArray(payload.length);
        int size = 12 + payload.length;
        while (size % 4 != 0)
            size++;
        byte[] buf = new byte[size];
        int i = 0;
        for (byte b : payload_len) {
            buf[i] = b;
            i++;
        }
        for (byte b : secret) {
            buf[i] = b;
            i++;
        }
        for (byte b : step) {
            buf[i] = b;
            i++;
        }
        for (byte b : digits) {
            buf[i] = b;
            i++;
        }
        for (byte b : payload) {
            buf[i] = b;
            i++;
        }
        return buf;
    }
    
    private static void sendTCPMessage(byte[] buf, DataOutputStream outStream) throws IOException, InterruptedException {
		outStream.write(buf, 0, buf.length);
    }

    private static byte[] receiveTCPMessage(InputStream inStream) throws IOException {
		byte[] result = new byte[64];
		inStream.read(result);
		return result;
    }

    private static byte[] sendUDPMessage(byte[] buf, int port) throws IOException, InterruptedException {
        DatagramPacket out = new DatagramPacket(buf, buf.length, address, port);
        byte[] result = new byte[64];
        DatagramPacket dp = new DatagramPacket(result, result.length);
        udpSocket.send(out);
        udpSocket.receive(dp);
        return result;
    }
    
    private static byte[] receivePacket() throws IOException {
        byte[] result = new byte[64];
        DatagramPacket dp = new DatagramPacket(result, result.length);
        udpSocket.receive(dp);
        return result;
    }
    
    private static void printBits(byte[] buf) {
        int i = 0;
        for (byte b : buf) {
            for (int j = 7; j >= 0; j--) {
                System.out.print( ( b >> j ) & 1);
            }
            i++;
            if (i % 4 == 0)
                System.out.println();
            else
                System.out.print(" ");
        }
    }
    
    private static class SomethingWrongException extends Exception {
    	
    }
}

























