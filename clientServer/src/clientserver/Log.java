package clientserver;

import static clientserver.ClientServer.welcomeSocket;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {

    public static Vector<String> transactionLog = new Vector(200);

    public static double balance;
    static String path = "./log.txt";
    static boolean append_to_file = true;

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
    public static void addToTransactionLog(Paxos.Value val, int i) {

//        Paxos.Value val = null;
//        val.amount = Double.parseDouble(message[2]);
//        val.type = message[1];
        String entry = val.type + " " + val.amount + " " + val.logPosition;
        transactionLog.add(i, entry);
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
        for (int i = 0; i < transactionLog.size(); ++i) {
            String val = transactionLog.elementAt(i);
            System.out.println("Log " + i + ": " + val);

        }
    }

    public static void sendLog(String serverId) throws IOException, ClassNotFoundException {

        int s = Integer.parseInt(serverId);
        Socket connectionSocket = new Socket(ClientServer.serverIPs[s], ClientServer.logPort);
        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

        OutputStream socketStream = connectionSocket.getOutputStream();
        ObjectOutput objectOutput = new ObjectOutputStream(socketStream);
        objectOutput.writeObject(transactionLog);

        connectionSocket.close();
    }

    public static void rebuildLog() throws IOException {

        append_to_file = false;
        // fix balance
        balance = 0;
        String block = "";
        for (int i = 0; i < transactionLog.size(); i++) {
            String trans = transactionLog.elementAt(i);
            String[] split = trans.split(" ");
            double amt = Double.parseDouble(split[1]);
            if (split[0].equals("deposit")) {
                //deposit
                balance += amt;

            } else {
                //withdraw
                balance -= amt;
            }
            if (i == transactionLog.size() - 1) {
                block += trans;
            } else {
                block += trans + "\n";
            }
        }
        //write to log at once
        writeToFile(block);
        append_to_file = true;
        ClientServer.ctrlc = 0;
    }
}
