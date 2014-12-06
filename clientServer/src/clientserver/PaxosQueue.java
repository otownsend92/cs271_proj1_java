/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientserver;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author olivertownsend
 */
public class PaxosQueue {
    
    public static Vector<String[]> transactionQueue;
    public boolean isProposing = false;
	
	public PaxosQueue() {
		transactionQueue = new Vector<String[]>();
	}
	
	public boolean isEmpty() {
		return transactionQueue.isEmpty();
	}
	
	public void enqueue(String[] cmd) {
		transactionQueue.add(cmd);
	}
	
	public String[] dequeue() {
		String[] val = transactionQueue.get(0);
		transactionQueue.remove(0);
		return val;
	}
	
	public String[] peek() {
		return transactionQueue.get(0);
	}
	
	public void clear() {
		transactionQueue.removeAllElements();
	}
        
        public void queueWatcher() {
            
            while(true) {
                
                // if there are items in the queue and if Paxos isn't currently proposing a value, then propose value
                if(!transactionQueue.isEmpty() && !isProposing){
                    String[] newTrans = transactionQueue.get(0);
                    try {
                        ClientServer.paxosObject.prepareMsg(newTrans);
                        isProposing = true;
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
            }
        }
    
}
