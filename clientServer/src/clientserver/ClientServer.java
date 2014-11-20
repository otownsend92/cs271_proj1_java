package clientserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class ClientServer implements Runnable {   
    
    private static int serverID;
    private static int roundn;
    private static int procn;
    private static int leader = serverID;   // assigning self to leader
    private static double balance = 0.0;
    private static int[] serverPorts = {12000, 12001, 12002, 12003, 12004};
    
    String clientSentence;
    String capitalizedSentence;

    Socket csocket;
    
    
    ClientServer(Socket csocket) {
        this.csocket = csocket;
    }

    public static void main(String[] args) throws Exception {

        ServerSocket welcomeSocket = new ServerSocket(12002);    
        System.out.println("waiting for clients...");
        String inputLine = null;
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        
        while(true) {
            System.out.print("> ");
            try {
                inputLine = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String[] input = inputLine.split("\\s+");
            
            if(input[0].equals("deposit")) {
                System.out.println("Depositing: " + input[1]);
                balance += Double.parseDouble(input[1]);
            }
            
            else if(input[0].equals("withdraw")) {
                System.out.println("Withdrawing: " + input[1]);
                balance -= Double.parseDouble(input[1]);
            }
            
            else if(input[0].equals("balance")) {
                System.out.println("Balance is: " + balance);
            }
            
            else if(input[0].equals("quit")) {
                System.out.println("Quitting...");
                System.exit(0);
            }
        }
        
        // Start with a new thread to listen for things
        Thread listenerThread = new Thread() {
            public void run() {
                System.out.println("Entering listener thread...");
                ServerSocket welcomeSocket = null;
                try {
                    welcomeSocket = new ServerSocket(12002);    
                } catch (IOException ex) {
                    System.out.println(ex);
                }
                
                System.out.println("Waiting for clients...");
                while (true) {
                    Socket connectionSocket = null;
                    try {
                        connectionSocket = welcomeSocket.accept();
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                    System.out.println("Connected.");
                    new Thread(new ClientServer(connectionSocket)).start();
                }
            }  
        };
        listenerThread.start();

        // Start main for input
        
    }
    
    public void run() {
        try {
            System.out.println("Spawning new handler thread...");
            String clientSentence;
            String capitalizedSentence;
    
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
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
    
    
    public void sendToLeader(String m) throws Exception {
        
        Socket clientSocket = new Socket("localhost", serverPorts[leader]);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outToServer.writeBytes(m);
        //modifiedSentence = inFromServer.readLine();
        //System.out.println("FROM SERVER: " + modifiedSentence);
        clientSocket.close();
    }
    
    
}
