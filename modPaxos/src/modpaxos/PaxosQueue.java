/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modpaxos;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author olivertownsend
 */
public class PaxosQueue {

    public static Vector<String[]> transactionQueue;
    public static boolean isProposing = false;

    public PaxosQueue() {
        transactionQueue = new Vector<String[]>();
    }
    
    public static void printQ() {
        for (int i = 0; i < transactionQueue.size(); ++i) {
            String [] val = transactionQueue.elementAt(i);
            System.out.println("Queue " + i + ": " + Arrays.toString(val));

        }
    }
    
    public static void queueWatcher() {

        // if there are items in the queue and if Paxos isn't currently proposing a value, then propose value
        if ((!transactionQueue.isEmpty()) && (!isProposing)) {
            if (!transactionQueue.isEmpty()) {
                String[] newTrans = transactionQueue.firstElement();
                System.out.println(Arrays.toString(newTrans));
                try {
                    ClientServer.paxosObject.prepareMsg(newTrans);
                    isProposing = true;
                } catch (Exception ex) {
                    System.out.println("queueWatcher:" + ex);
                }
            }
        }
    }

}
