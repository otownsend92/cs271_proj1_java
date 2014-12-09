package modpaxos;

import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {

    public static Vector<String> transactionLog = new Vector(200);
    public static Vector<String> logToSend = new Vector(200);

    public static double balance;
    static String path = "./log.txt";
    static boolean append_to_file = true;
    public static int currIndex = 0;

    public static void writeToFile(String textLine) throws IOException {
        FileWriter write;
        write = new FileWriter(path, append_to_file);
        PrintWriter print_line = new PrintWriter(write);

        print_line.printf("%s" + "%n", textLine);
        print_line.close();
    }

    /*
     Add transaction to log with transaction type and amount
     */
    public static void addToTransactionLog(Paxos.Value val) {

//        Paxos.Value val = null;
//        val.amount = Double.parseDouble(message[2]);
//        val.type = message[1];
        String entry = val.type + " " + val.amount + " " + val.logPosition;
        String nullString1 = "";
        String nullString2 = "";
        transactionLog.add(nullString1);
        transactionLog.add(nullString2);
        transactionLog.add(val.logPosition, entry);
        currIndex++;
        updateBalance(val.type, val.amount);
        try {
            writeToFile(entry);
        } catch (IOException ex) {
            Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*
     Update local balance value.
     */
    public static void updateBalance(String type, double amt) {
        if (type.equals("deposit")) {
            balance += amt;
        } else {
            balance -= amt;
        }
    }

    /*
     Return current local account balance value.
     */
    public static double getBalance() {
        return balance;
    }

    public static void printLog() {
//        System.out.println("currIndex: " + currIndex);
        int j = 0;
        for (int i = 0; i < currIndex; ++i) {
            String val = transactionLog.elementAt(i);
            if (!val.equals("")) {
                String[] split = val.split(" ");
                System.out.println("Log " + j + ": " + split[0] + " " + split[1]);
                j++;
            }

        }
    }

    public static void sendLog(String serverId) throws IOException, ClassNotFoundException {

        int s = Integer.parseInt(serverId);
        Socket connectionSocket = new Socket(ClientServer.serverIPs[s], ClientServer.logPort);
        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

        OutputStream socketStream = connectionSocket.getOutputStream();
        ObjectOutput objectOutput = new ObjectOutputStream(socketStream);
        
        logToSend.removeAllElements();
        
        int j = 0;
        for (int i = 0; i < currIndex; ++i) {
            String val = transactionLog.elementAt(i);
            if (!val.equals("")) {
                logToSend.add(val);
            }

        }
                
        objectOutput.writeObject(logToSend);

        connectionSocket.close();
    }

    public static void rebuildLog() throws IOException {

        append_to_file = false;
        // fix balance
        balance = 0;
        currIndex = 0;
        String block = "";
//        System.out.println("TransLog size: " + transactionLog.size());
        for (int i = 0; i < transactionLog.size(); i++) {

            String trans = transactionLog.elementAt(i);
            if (!trans.equals("")) {
                
//                System.out.println("TRANS rebuild: " + trans);
                String[] split = trans.split(" ");
                double amt = Double.parseDouble(split[1]);
//                System.out.println("split: " + Arrays.toString(split));
                
                
                if (split[0].equals("deposit")) {
                    //deposit
//                    System.out.println("DEPOSITING: " + amt);
                    balance += amt;

                } else {
                    //withdraw
//                    System.out.println("WITHDRAWING: " + amt);
                    balance -= amt;
                }
                if (i == transactionLog.size() - 1) {
                    block += trans;
                } else {
                    block += trans + "\n";
                }
                
                currIndex++;
            }
        }
        //write to log at once
        writeToFile(block);
        append_to_file = true;
        ClientServer.ctrlc = 0;
        
        System.out.println("Finished updating.");
    }
}
