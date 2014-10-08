    import java.io.*;
import java.net.*;
import java.nio.*;

/**
 * Comment
 */
public class Client {

    static DatagramSocket socket;
    static Socket tcpSocket;
    static InetAddress address;

    public static void main(String arg[]) throws IOException, ClassNotFoundException, InterruptedException {
        socket = new DatagramSocket();
        socket.setSoTimeout(750);
        address = InetAddress.getByName("amlia.cs.washington.edu");
      	byte[] payload = createPayload(0, 1, getByteArrayFromString("hello world"));
        byte[] result = sendUDPMessage(payload, 12235);
        
        ByteBuffer b = ByteBuffer.wrap(result);
        //printBits(result);
        int num = b.getInt(12);
        int len = b.getInt(16);
        int port = b.getInt(20);
        int secret = b.getInt(24);
        System.out.println("The num is : " + num);
        System.out.println("The len is : " + len);
        System.out.println("The udp_port is : " + port);
        System.out.println("The secret is : " + secret);
        int size = len + 4;
        while (size % 4 != 0)
            size++;
        for (int i = 0; i < num; i++) {
            System.out.println("b1, id : " + i);
            b = ByteBuffer.allocate(size);
            b.putInt(i);
            try {
		payload = createPayload(secret, 1, b.array());
                result = sendUDPMessage(payload, port);
                System.out.println("Success");
            }
            catch (SocketTimeoutException e) {
                System.out.println("socket timed out");
                i--;
            }
        }
        result = receivePacket();
        ByteBuffer b2 = ByteBuffer.wrap(result);
        int portTCP = b2.getInt(12);
        int secretB = b2.getInt(16);
        System.out.println("portTCP : " + portTCP);
        System.out.println("secretB : " + secretB);

	tcpSocket = new Socket(address, portTCP);
	OutputStream outToServer = tcpSocket.getOutputStream();
	DataOutputStream outStream = new DataOutputStream(outToServer);
	InputStream inStream = tcpSocket.getInputStream();
	result = receiveTCPMessage(inStream);
	ByteBuffer c2 = ByteBuffer.wrap(result);
	int num2 = c2.getInt(12);
	int len2 = c2.getInt(16);
	int secretC = c2.getInt(20);
        byte byteC = c2.get(24);
	char c = (char) ((int) byteC);
	System.out.println("num2 : " + num2);
	System.out.println("len2 : " + len2);
	System.out.println("secretC : " + secretC);
	System.out.println("byteC : " + byteC);
	System.out.println("c : " + c);
        printBits(result);

	size = len2;
	while (size % 4 != 0)
	    size++;
	byte[] payloadC = new byte[size];
	for (int i = 0; i < len2; i++)
	    payloadC[i] = byteC;
	
	for (int i = 0; i < num2; i++) {
	    try {
		payload = createPayload(secretC, 1, payloadC);
		printBits(payload);
		sendTCPMessage(payload, outStream);
		System.out.println("Success");
	    }
	    catch (ConnectException e) {
		System.out.println("connection refused");
		Thread.sleep(500);
		i--;
	    }
	}
	result = receiveTCPMessage(inStream);
	printBits(result);
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
	byte[] digits = get2ByteArray(345);
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
        
        for (i = 0; i < buf.length; i++) {
            byte b = buf[i];
            
        }

	return buf;
    }
    
    private static void sendTCPMessage(byte[] buf, DataOutputStream outStream) throws IOException, InterruptedException {
	System.out.println("Sending packet..");
	outStream.write(buf, 0, buf.length);
	Thread.sleep(500);
    }

    private static byte[] receiveTCPMessage(InputStream inStream) throws IOException {
	byte[] result = new byte[64];
	System.out.println("Waiting for response..");
	inStream.read(result);
	return result;
    }

    /*    private static byte[] sendAndReceiveTCPMessage(byte[] buf) throws IOException, InterruptedException {
	OutputStream outToServer = tcpSocket.getOutputStream();
	DataOutputStream outStream = new DataOutputStream(outToServer);
	System.out.println("Sending packet..");
	outStream.write(buf, 0, buf.length);
	System.out.println("Success");
	byte[] result = new byte[64];
	System.out.println("Waiting for response..");
	tcpSocket.getInputStream().read(result);
	return result;
    } */

    private static byte[] sendUDPMessage(byte[] buf, int port) throws IOException, InterruptedException {
        
        DatagramPacket out = new DatagramPacket(buf, buf.length, address, port);
        byte[] result = new byte[64];
        DatagramPacket dp = new DatagramPacket(result, result.length);
        
        System.out.println("Sending packet..");
        socket.send(out);
        System.out.println("Packet sent, waiting for response..");
        socket.receive(dp);
        //String rcvd = "rcvd from " + dp.getAddress() + ", " + dp.getPort() + ": "
        //  + new String(dp.getData(), 0, dp.getLength());
          
        //System.out.println(rcvd);
        Thread.sleep(500);
        return result;
    }
    
    private static byte[] receivePacket() throws IOException {
        byte[] result = new byte[64];
        DatagramPacket dp = new DatagramPacket(result, result.length);
        socket.receive(dp);
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
}


























