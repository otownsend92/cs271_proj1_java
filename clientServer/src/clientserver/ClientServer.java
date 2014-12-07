package clientserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import static java.lang.Thread.sleep;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientServer implements Runnable {

    // make new paxos object
    public static Paxos paxosObject = new Paxos();
    public static PaxosQueue paxosQueueObj = new PaxosQueue();
    public static Log logObject = new Log();

    public static int ctrlc = 0;
    public static int isFail = 0;
    public static int serverId;
    public static int heardFrom = 0;
    public static double balance = 0.0;
    public static String[] serverIPs = {
        "54.174.167.183", // ssh -i /Users/wdai/Desktop/turtlebeards.pem ec2-user@54.174.167.183  
        "54.174.226.59", // ssh -i /Users/wdai/Desktop/turtlebeards.pem ec2-user@54.174.226.59 
        "54.86.223.159", // ssh -i /Users/wdai/Desktop/turtlebeards.pem ec2-user@54.86.223.159 
        "54.174.201.123", // ssh -i /Users/wdai/Desktop/turtlebeards.pem ec2-user@54.174.201.123 
        "54.174.164.18"}; //ssh -i /Users/wdai/Desktop/turtlebeards.pem ec2-user@54.174.164.18 
//    public static String[] serverIpPrivate = {
//        "ec2-54-174-167-183.compute-1.amazonaws.com",
//        "ec2-54-174-226-59.compute-1.amazonaws.com",
//        "ec2-54-86-223-159.compute-1.amazonaws.com",
//        "ec2-54-174-201-123.compute-1.amazonaws.com",
//        "ec2-54-174-164-18.compute-1.amazonaws.com"
//    };
    public static int[] serverPorts = {12000, 12001, 12002, 12003, 12004};
    public static int[] logSizes = {0, 0, 0, 0, 0};
    public static int logPort = 1210;

    static boolean listenerTrue = true;

    String clientSentence, capitalizedSentence;
    Socket csocket;
    static ServerSocket welcomeSocket, logSocket;
    static Thread listenerThread, logThread;

    ClientServer(Socket csocket) {
        this.csocket = csocket;
    }

    public static void main(String[] args) throws Exception {

        System.out.println("~~~~~~~~~~~~~~~~~~~" + " CS271 Paxos " + "~~~~~~~~~~~~~~~~~~~");
        String inputLine = null;
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        // this is assigning the local server id
        serverId = Integer.parseInt(args[0]);
        System.out.println("Starting server with Id: " + serverId);

        // Listener thread stuff
        listenerThread = new Thread() {
            public void run() {
                System.out.println("Waiting for clients...");
                try {
                    // init welcomeSocket ONLY once
                    welcomeSocket = new ServerSocket(serverPorts[serverId]);
                } catch (IOException ex) {
                    System.out.println("Welcome socket: " + ex);
                }
                while (true) {
                    if (!listenerTrue) {
                        try {
                            // don't listen
                            welcomeSocket.close();
                        } catch (IOException ex) {
                            System.out.println(ex);
                        }

                    } else {
                        try {
                            if (welcomeSocket.isClosed()) {
                                welcomeSocket = new ServerSocket(serverPorts[serverId]);
                            }
                            Socket connectionSocket = new Socket();
                            connectionSocket.setSoTimeout(100);
                            connectionSocket = welcomeSocket.accept();
//                            System.out.println("Connected.");

                            new Thread(new ClientServer(connectionSocket)).start();

                        } catch (IOException ex) {
                            System.out.println("Connection socket: " + ex);
                            ex.printStackTrace();
                        }
                    }
                }
            }
        };

        // log port thread stuff
        logThread = new Thread() {
            public void run() {
                try {
                    // init log ONLY once
                    logSocket = new ServerSocket(logPort);
                } catch (IOException ex) {
                    System.out.println("logSocket: " + ex);
                }
                while (true) {
                    if (!listenerTrue) {
                        try {
                            // don't listen
                            logSocket.close();
                        } catch (IOException ex) {
                            System.out.println(ex);
                        }

                    } else {
                        try {
                            Socket newSock = new Socket();
                            newSock.setSoTimeout(100);
                            newSock = logSocket.accept();
                            System.out.println("Connected to log port");
                            // now can receive data

                            InputStream socketStream = newSock.getInputStream();
                            ObjectInputStream objectInput = new ObjectInputStream(socketStream);
                            Vector<String> receivedLog = (Vector<String>) objectInput.readObject();
                            newSock.close();

                            Log.transactionLog = receivedLog;
                            System.out.println("This is the received catch-up log: " + Log.transactionLog);
                            Log.rebuildLog();

                        } catch (IOException ex) {
                            System.out.println("newSock socket: " + ex);
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(ClientServer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        };

        // Heartbeat thread stuff
        Thread heartBeatThread = new Thread() {
            public void run() {
                while (true) {
                    // runs forever in a loop, and waits for let's say, 3 sec before running again?
                    try {
//                        System.out.println("Entering heartbeat thread...");
                        HeartBeat.pingAll(); // this should update the "numProc" int in HeartBeat.java

                        // wait 3s
                        sleep(3000);
                        HeartBeat.countAliveServers();
                    } catch (IOException | InterruptedException ex) {
//                        System.out.println(ex);
                    } catch (Exception ex) {
                        Logger.getLogger(ClientServer.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }
        };

        // Queue thread stuff
        Thread queueWatchdogThread = new Thread() {
            public void run() {
                while (true) {
//                    System.out.println("Starting queuewatcher thread");
                    paxosQueueObj.queueWatcher();
                }
            }
        };

        // Start the listener thread
        listenerThread.start();
        // Start the heartbeat thread
        heartBeatThread.start();
        // Start the queue thread
        queueWatchdogThread.start();
        // Start the log thread
        logThread.start();
             
        // Check for log file
        File f = new File(Log.path);
        if(f.exists() && !f.isDirectory()) {
            sleep(3000);
            // if a log file is there
            ctrlc = 1;
            System.out.println("Rebuilding from CTRL C failure...");
            int size = Log.transactionLog.size();
            System.out.println("local size of log: " + size);
            String poll = "sizepoll " +serverId;
            sendPollToAll(poll);
        }
        
        // Start main thread for input
        while (true) {
            System.out.print("> ");
            try {
                inputLine = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String regex = "(?=\\()|(?<=\\)\\d)";
            String[] input = inputLine.split(regex);
//            System.out.println(Arrays.toString(input));

            if (input[0].equals("deposit")) {
                double amount;
                try {
                    input[1] = (input[1].substring(1, input[1].length() - 1));
                    System.out.println("Depositing: " + input[1]);
                    // Adding to queue
                    paxosQueueObj.transactionQueue.add(input);
                    for (int i = 0; i < paxosQueueObj.transactionQueue.size(); i++) {
                        String[] s = paxosQueueObj.transactionQueue.elementAt(i);
//                        System.out.println(Arrays.toString(s));
                    }
//                    paxosObject.prepareMsg(input);
                } catch (Exception e) {
                    System.out.println("Try deposit: " + e);
                    e.printStackTrace();
                }
            } else if (input[0].equals("withdraw")) {
                double amount;
                try {
                    input[1] = (input[1].substring(1, input[1].length() - 1));
                    if (logObject.balance < Double.parseDouble(input[1])) {
                        // Nonsufficient funds
                        System.out.println("Withdraw of: " + input[1] + " failed. Insufficient funds.");
                    } else {
                        // Adding to queue
                        System.out.println("Withdrawing: " + input[1]);
                        paxosQueueObj.transactionQueue.add(input);
                        for (int i = 0; i < paxosQueueObj.transactionQueue.size(); i++) {
                            String[] s = paxosQueueObj.transactionQueue.elementAt(i);
//                            System.out.println(Arrays.toString(s));
                        }
//                        paxosObject.prepareMsg(input);
                    }
                } catch (Exception e) {
                    System.out.println("Invalid command.");
                }
            } else if (input[0].equals("balance")) {
                System.out.println("Balance is: " + logObject.getBalance());
            } else if (input[0].equals("fail")) {
                System.out.println("Failing...");
                fail();
            } else if (input[0].equals("unfail")) {
                System.out.println("Unfailing...");
                unfail();
            } else if (input[0].equals("print")) {
                logObject.printLog();
            } else if (input[0].equals("heartbeat")) {
                System.out.println("LifeTable: " + Arrays.toString(HeartBeat.lifeTable));
            } else if (input[0].equals("printq")) {
                PaxosQueue.printQ();
            } // added simply for testing 
            else if (input[0].equals("send")) {
                // send message input[1] to server at port input[2]
                String server = input[2].substring(1, input[2].length() - 1);
                sendTo(input[1], server);
            } else if (input[0].equals("quit")) {
                System.out.println("Quitting...");
                System.exit(0);
            }
        }
    }

    public void run() {
        try {
            // Handler thread runnable
//            System.out.println("Spawning new handler thread...");
            String clientSentence;

            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
            clientSentence = inFromClient.readLine();

            if ((clientSentence != null) && (!clientSentence.isEmpty())) {
//                    System.out.println("Received: " + clientSentence);
                paxosObject.handleMsg(clientSentence);
            } else {
//                System.out.println("Thump");
            }
        } catch (IOException e) {
            System.out.println(e);
        } catch (Exception ex) {
            Logger.getLogger(ClientServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void fail() {
        System.out.println("USER FAIL: Stopping the listener thread.");
        listenerTrue = false;
    }

    public static void unfail() throws Exception {
        System.out.println("USER UNFAIL: Starting the listener thread again.");
        // begin listening again
        listenerTrue = true;
        // get size from local log
        int size = Log.transactionLog.size();
        System.out.println("local size of log: " + size);
        // poll others for largest size
        String pollMsg = "sizepoll " + serverId;
        sendPollToAll(pollMsg);
    }

    public static void sendTo(String m, String serverId) throws Exception {

        int server_id = Integer.parseInt(serverId);
        int p = serverPorts[server_id];
        String serverName = serverIPs[server_id];

//        System.out.println("Sending " + m + " to: " + serverName + " on port: " + p);
        Socket clientSocket = new Socket(serverName, p); //serverPorts[leader]);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outToServer.writeBytes(m);
        clientSocket.close();
//        System.out.println("Finished sending.");

    }

    public static void sendToAll(String prepareMsg) throws Exception {

        for (int i = 0; i < 5; ++i) {
            if (HeartBeat.lifeTable[i] == 1) {
//                System.out.println("Heartbeat at: " + i);
//                System.out.println("Sending to" + serverIPs[i] + ":" + serverPorts[i]);
                int p = serverPorts[i];
                Socket clientSocket = new Socket(serverIPs[i], p);
                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                outToServer.writeBytes(prepareMsg);
                clientSocket.close();
            }
        }
    }

    public static void sendPollToAll(String prepareMsg) throws Exception {

        for (int i = 0; i < 5; ++i) {
            if (HeartBeat.lifeTable[i] == 1) {
                if (i == serverId) {
                } else {
//                    System.out.println("Heartbeat at: " + i);
//                    System.out.println("Sending to" + serverIPs[i] + ":" + serverPorts[i]);
                    int p = serverPorts[i];
                    Socket clientSocket = new Socket(serverIPs[i], p);
                    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    outToServer.writeBytes(prepareMsg);
                    clientSocket.close();
                }
            }
        }
    }

    public static void requestLog() throws Exception {

        int chosenServer = serverId;
        System.out.println(Arrays.toString(logSizes));
        for (int i = 0; i < logSizes.length; i++) {
            if (logSizes[i] > logSizes[serverId]) { //Log.transactionLog.size()) {
                chosenServer = i;
            }
        }
        if (chosenServer != serverId) {
            System.out.println("Requesting from: " + chosenServer);
            // request log from other server
            String requestLog = "requestlog " + serverId;
            sendTo(requestLog, Integer.toString(chosenServer));
        } else {
            System.out.println("rebuilding from self");
            // else rebuild from yourself
        }

    }
}
