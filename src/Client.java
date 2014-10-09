import java.io.*;
import java.net.*;
import java.nio.*;

/**
 * Comment
 */
public class Client {

	private static final int MAX_STAGE_B_FAILURES = 10;
	private static final int UDP_TIMEOUT = 500;
	private static final int LAST_THREE_SID = 166;
	private static final int DELAY_BETWEEN_STAGES = 500;
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
	

    public static void main(String arg[]) throws IOException, InterruptedException {
    	udpSocket = new DatagramSocket();
        address = InetAddress.getByName(URL);
        while (!run()) {
        	System.out.println("\nStage B failed " + MAX_STAGE_B_FAILURES + " times, starting over...");
        }
        System.out.println("secretA : " + secretA);
        System.out.println("secretB : " + secretB);
        System.out.println("secretC : " + secretC);
        System.out.println("secretD : " + secretD);
    }
    
    private static boolean run() throws InterruptedException, IOException {
    	doStageA();
    	Thread.sleep(DELAY_BETWEEN_STAGES);
    	try {
			doStageB();
		} catch (SomethingWrongWithStageBException e) {
			return false;
		}
		tcpSocket = new Socket(address, tcp_port);
    	Thread.sleep(DELAY_BETWEEN_STAGES);
    	doStageC();
    	Thread.sleep(DELAY_BETWEEN_STAGES);
    	doStageD();
    	return true;
    }
    
    private static void doStageD() throws IOException, InterruptedException {
		System.out.println("*** Commence STAGE D ***");
		DataOutputStream outStream = new DataOutputStream(tcpSocket.getOutputStream());
		InputStream inStream = tcpSocket.getInputStream();
		int payloadSizeD = len2;
		while (payloadSizeD % 4 != 0)
			payloadSizeD++;
		byte[] bytesD = new byte[payloadSizeD];
		for (int i = 0; i < len2; i++)
		    bytesD[i] = byteC;
		
		for (int i = 0; i < num2; i++) {
			System.out.println("Sending packet " + i);
		    try {
				byte[] payloadD = createPayload(secretC, 1, bytesD);
				//printBits(payloadD);
				sendTCPMessage(payloadD, outStream);
				System.out.println("Success");
		    }
		    catch (ConnectException e) {
				System.out.println("Connection refused, trying again");
				Thread.sleep(500);
				i--;
		    }
		    catch (SocketException e2) {
				System.out.println("Socket Exception, trying again");
				Thread.sleep(500);
				i--;
		    }
		}
		byte[] resultD = receiveTCPMessage(inStream);
		printBits(resultD);
		ByteBuffer bufferD = ByteBuffer.wrap(resultD);
		secretD = bufferD.getInt(12);
		//System.out.println("secretD : " + secretD);
    }
    
    private static void doStageC() throws IOException {
		System.out.println("*** Commence STAGE C ***");
		InputStream inStream = tcpSocket.getInputStream();
		byte[] resultC = receiveTCPMessage(inStream);
		ByteBuffer c2 = ByteBuffer.wrap(resultC);
		num2 = c2.getInt(12);
		len2 = c2.getInt(16);
		secretC = c2.getInt(20);
	    byteC = c2.get(24);
		c = (char) ((int) byteC);
        System.out.println("*** STAGE C Success ***");
		System.out.println("num2 : " + num2);
		System.out.println("len2 : " + len2);
		//System.out.println("secretC : " + secretC);
		System.out.println("byteC : " + byteC);
		System.out.println("c : " + c);
    }
    
    private static void doStageB() throws SomethingWrongWithStageBException, IOException, InterruptedException {
        System.out.println("*** Commence STAGE B ***");
        udpSocket.setSoTimeout(UDP_TIMEOUT);
        int payloadSizeB = len + 4;
        while (payloadSizeB % 4 != 0)
        	payloadSizeB++;
        for (int i = 0; i < num; i++) {
            System.out.print("packet " + i + " : ");
            ByteBuffer bufferB = ByteBuffer.allocate(payloadSizeB);
            bufferB.putInt(i);
        	byte[] payloadB = createPayload(secretA, 1, bufferB.array());
            int failureCount = 0;
            boolean success = false;
            while (!success) {
            	if (failureCount == MAX_STAGE_B_FAILURES)
            		throw new SomethingWrongWithStageBException();
	            try {
	                byte[] resultB = sendUDPMessage(payloadB, udp_port);
	                //ByteBuffer bufferResultB = ByteBuffer.wrap(resultB);
	                //System.out.println("Success, ack_packet_id " + bufferResultB.getInt(12));
	                success = true;
	            }
	            catch (SocketTimeoutException e) {
	                System.out.print("timeout ");
	                failureCount++;
	            }
            }
            System.out.println("yay");
        }
        byte[] resultB2 = receivePacket();
        ByteBuffer bufferB2 = ByteBuffer.wrap(resultB2);
        tcp_port = bufferB2.getInt(12);
        secretB = bufferB2.getInt(16);
        System.out.println("*** STAGE B Success ***");
        System.out.println("tcp_port : " + tcp_port);
        //System.out.println("secretB : " + secretB);
    }
    
    private static void doStageA() throws IOException, InterruptedException {
    	System.out.println("*** Commence STAGE A ***");
      	byte[] payloadA = createPayload(0, 1, getByteArrayFromString("hello world"));
        byte[] resultA = sendUDPMessage(payloadA, 12235);
        ByteBuffer bufferA = ByteBuffer.wrap(resultA);
        num = bufferA.getInt(12);
        len = bufferA.getInt(16);
        udp_port = bufferA.getInt(20);
        secretA = bufferA.getInt(24);
        System.out.println("*** STAGE A Success ***");
        System.out.println("num : " + num);
        System.out.println("len : " + len);
        System.out.println("udp_port : " + udp_port);
        //System.out.println("secretA : " + secretA);
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
        int size = s.length();
        while (size % 4 != 0)
            size++;
        ByteBuffer b = ByteBuffer.allocate(size * 2);
        for (char c : s.toCharArray())
            b.putChar(c);
        byte[] data = b.array();
        byte[] result = new byte[size];
        for (int i = 0; i < result.length; i++) {
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
    
    private static class SomethingWrongWithStageBException extends Exception {
    	
    }
}


























