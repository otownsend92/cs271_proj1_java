/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientserver;

import java.util.Vector;

/**
 *
 * @author olivertownsend
 */
public class PaxosQueue {
    
    private Vector<Paxos.Value> transactionQueue;
	
	public PaxosQueue() {
		transactionQueue = new Vector<Paxos.Value>();
	}
	
	public boolean isEmpty() {
		return transactionQueue.isEmpty();
	}
	
	public void enqueue(Paxos.Value cmd) {
		transactionQueue.add(cmd);
	}
	
	public Paxos.Value dequeue() {
		Paxos.Value val = transactionQueue.get(0);
		transactionQueue.remove(0);
		return val;
	}
	
	public Paxos.Value peek() {
		return transactionQueue.get(0);
	}
	
	public void clear() {
		transactionQueue.removeAllElements();
	}
    
}
