package clientserver;

// string cmd = "scp -r -i ~/.ssh/id_rsa ucsb_276c@"+address[i]+":/home/ucsb_276c/p2 ~/Dropbox/Current\\ Documents/cs276/

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class ClientServer implements Runnable {   
    
    // make new paxos object
    Paxos paxosObject = new Paxos();
    
    public static int serverId;
    public static double balance = 0.0;
    public static String[] serverIPs = {"123.123.123","123.123.123","123.123.123","123.123.123","123.123.123" };
    public static int[] serverPorts = {12000, 12001, 12002, 12003, 12004};
    
    String clientSentence, capitalizedSentence;
    Socket csocket;
    
    static Thread listenerThread;

    ClientServer(Socket csocket) {
        this.csocket = csocket;
    }

    public static void main(String[] args) throws Exception {

        System.out.println("~~~~~~~~~~~~~~~~~~~" + " CS271 Paxos "+ "~~~~~~~~~~~~~~~~~~~");
        // Check for log file
        String inputLine = null;
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        // this is assigning the local server id
        serverId = Integer.parseInt(args[0]);
        System.out.println("Starting server with Id: " + serverId);
        
        // Listener thread stuff
        listenerThread = new Thread() {
            public void run() {
                System.out.println("Entering listener thread...");
                ServerSocket welcomeSocket = null;
                try {
                    welcomeSocket = new ServerSocket(serverPorts[serverId]);    
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
        
        // Heartbeat thread stuff
        Thread heartBeatThread = new Thread() {
            public void run() {
                try {
                    System.out.println("Entering heartbeat thread...");
                    HeartBeat.pingAll(); // this should update the "numProc" int in HeartBeat.java
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }  
        };
        
        // Start the listener thread
        listenerThread.start();                        
        // Start the heartbeat thread
        heartBeatThread.start();
        
        // Start main thread for input
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
                System.out.println("Balance is: " + Log.getBalance());
            }
            
            else if(input[0].equals("fail")) {
                System.out.println("Failing...");
                fail();
            }
            
            else if(input[0].equals("unfail")) {
                System.out.println("Unfailing...");
                unfail();
            }
            
            // added simply for testing 
            else if(input[0].equals("send")) {
                // send message input[1] to server at port input[2]
                sendTo(input[1], input[2]);
            }

            else if(input[0].equals("quit")) {
                System.out.println("Quitting...");
                System.exit(0);
            }
        }       
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
                    
            //capitalizedSentence = clientSentence.toUpperCase() + '\n';
            //outToClient.writeBytes(capitalizedSentence);
            
            // Going to need to handle the received messages in here
            // This includes heartbeat 'pings'
            
            paxosObject.handleMsg(clientSentence);            
        }
        
        catch (IOException e) {
            System.out.println(e);
        }
    }
    
    public static void fail() {
        System.out.println("USER FAIL: Stopping the listener thread.");
        listenerThread.stop();
    }
    
    public static void unfail() {
        System.out.println("USER UNFAIL: Starting the listener thread again.");
        listenerThread.start();
        // get size from local log
        // poll others for largest size
        // if local is up to date, import data
        // else, get data from the most up to date process
        
        
    }
    
    // switch up ports and stuffs
    public static void sendTo(String m, String port) throws Exception {
        
        int p = Integer.parseInt(port);
        Socket clientSocket = new Socket("localhost", p); //serverPorts[leader]);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outToServer.writeBytes(m);
        //modifiedSentence = inFromServer.readLine();
        //System.out.println("FROM SERVER: " + modifiedSentence);
        clientSocket.close();
    }
    
    public static void sendToAll(String prepareMsg) throws Exception {
        
        for(int i = 0; i < 5; ++i) {

            int p = serverPorts[i];
            Socket clientSocket = new Socket("localhost", p); //serverPorts[leader]);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outToServer.writeBytes(prepareMsg);
            clientSocket.close();
        }
    }
}
