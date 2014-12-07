package clientserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

public class Log {

    public static ArrayList<Paxos.Value> transactionLog = new ArrayList();
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
    public static void addToTransactionLog(Paxos.Value val) {
        
//        Paxos.Value val = null;
//        val.amount = Double.parseDouble(message[2]);
//        val.type = message[1];
        val.logPosition = transactionLog.size();
        
        transactionLog.add(val.logPosition, val);
        updateBalance(val.type, val.amount);
    }
    
    /*
    Update local balance value.
    */
    public static void updateBalance(String type, double amt) {
        if(type.equals("deposit")) {
            balance += amt;
        }
        else { 
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
        for (int i = 0; i<transactionLog.size(); ++i) {
            Paxos.Value val = transactionLog.get(i);
            System.out.println("Log " + i + ": " + val.type + " " + val.amount);
            
        }
    }
}
