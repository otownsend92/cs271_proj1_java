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

    /*
    no idea how this works or if it works at all right now
    just put it in here for now
    */
    public void saveVectorToFile() throws IOException, ClassNotFoundException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(transactionLog);
        oos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Vector v2 = (Vector) ois.readObject();
        Enumeration e = transactionLog.elements();
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement());
        }
    }
    
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
    public static int updateBalance(String type, double amt) {
        if(type.equals("deposit")) {
            balance += amt;
            return 1; //successful deposit
        }
        else { // if withdraw need to check that balance does not go negative
            if(amt > balance) {
                // reject
                return 0; // FAILURE!
            } else balance -= amt;
            return -1; //successful withdraw
        }
    }
    
    /*
    Return current local account balance value.
    */
    public static double getBalance() {
        return balance;
    }
}
