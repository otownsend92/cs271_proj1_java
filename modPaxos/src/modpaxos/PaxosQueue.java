/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modpaxos;

import java.util.Arrays;
import java.util.Vector;

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
            String[] val = transactionQueue.elementAt(i);
            System.out.println("Queue " + i + ": " + Arrays.toString(val));
        }
    }

    public static void queueWatcher() {

        // if there are items in the queue and if Paxos isn't currently proposing a value, then propose value
        if ((!transactionQueue.isEmpty()) && (!isProposing) && (HeartBeat.leaderId != ClientServer.serverId)) {
            if (!transactionQueue.isEmpty()) {
                String[] newTrans = transactionQueue.firstElement();
                System.out.println(Arrays.toString(newTrans));

                // bypass acks, always leader
                try {
                    String cohortProposal = "cohort " + newTrans[0] + " " + newTrans[1];
                    ClientServer.sendTo(cohortProposal, Integer.toString(HeartBeat.leaderId));
                    transactionQueue.remove(0);
//                    ClientServer.paxosObject.prepareMsg(newTrans);
                } catch (Exception ex) {
                    System.out.println("queueWatcher:" + ex);
                }
            }
            
        // if leader
        } else if ((!transactionQueue.isEmpty()) && (!isProposing) && (HeartBeat.leaderId == ClientServer.serverId)) {
            if (!transactionQueue.isEmpty()) {
                String trans[] = transactionQueue.firstElement();
                System.out.println("TRANS: "+Arrays.toString(trans));
                String winMsg = "accept "
                        + trans[0] + " "
                        + trans[1] + " "
                        + Log.currIndex;
                try {
                    isProposing = true;
                    ClientServer.sendToAll(winMsg);
                } catch (Exception ex) {
                    System.out.println("senttoall: "+ex);
                }
            }
        }
    }
}
