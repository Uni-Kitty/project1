import java.io.*;
import java.net.*;

/**
 * Comment
 */
public class Client {

    public static void main(String arg[]) throws IOException, ClassNotFoundException {

        int portNum = 12235;

        Socket socket = new Socket("amlia.cs.washington.edu", portNum);

        // Integer Object to send to Server.
        Integer num = new Integer(50);

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        
        out.writeObject(num);
                
        String response = (String) in.readObject();

        System.out.println("Server message: " + response);
        
        //socket.close();
        
    }
}
