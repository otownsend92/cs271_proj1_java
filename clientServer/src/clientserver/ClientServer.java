package clientserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class ClientServer implements Runnable {   
    
    String clientSentence;
    String capitalizedSentence;
    Socket csocket;
    ClientServer(Socket csocket) {
        this.csocket = csocket;
    }

    public static void main(String[] args) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(12002);    
        System.out.println("waiting for clients...");
        while (true) {
            Socket connectionSocket = welcomeSocket.accept();
            System.out.println("Connected");
            new Thread(new ClientServer(connectionSocket)).start();
      }
    }
    
    public void run() {
        try {
            BufferedReader inFromClient =
            new BufferedReader(new InputStreamReader(csocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(csocket.getOutputStream());
            clientSentence = inFromClient.readLine();
            System.out.println("Received: " + clientSentence);
            capitalizedSentence = clientSentence.toUpperCase() + '\n';
            outToClient.writeBytes(capitalizedSentence);
        }
        
        catch (IOException e) {
            System.out.println(e);
        }
    }
}
