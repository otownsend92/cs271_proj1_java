package clientserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

public class Log {

    public static Vector<Paxos.Value> transactionLog;
    public static double balance;
    String path = "./";
    boolean append_to_file = true;

    
    public void writeToFile(String textLine) throws IOException {
        
        FileWriter write = new FileWriter(path, append_to_file);
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
        
        transactionLog.addElement(val);
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
}
