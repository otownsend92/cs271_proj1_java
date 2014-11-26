package clientserver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Paxos {

    private class Value {

        double amount;
        String type;
        int logPosition = -1;
    }

    int ackCount = 0;
    int generateNum = 0;
    Value val;
    Value acceptedVal;
    Value myVal;

    int minBallotNum = 0;
    int minBallotNumServerId = 0; //set this later
    boolean leader = false;

    public void prepareMsg(String msg) {
        String[] message = msg.split(" ");
        val.type = message[0];
        val.amount = Double.parseDouble(message[1]);
        val.logPosition = 0; //need from log class

        String prepareMsg = "prepare " + generateNum + " server_id"; //change server_id
        ClientServer.sendToAll(prepareMsg);

        generateNum++;
    }

    public void regeneratePrepare() {
        String prepareMsg = "prepare " + generateNum + " server_id"; //change server_id
        ClientServer.sendToAll(prepareMsg);

        generateNum++;
    }

    public void handleMsg(String msg) {

        String[] message = msg.split(" ");

        if (message[0].equals("prepare")) {
            handlePrepare(message);
        } else if (message[0].equals("ack")) {
            handleAck(message);
        } else if (message[0].equals("accept")) {
            handleAccept(message);
        }
    }

    public void handlePrepare(String[] message) {
        int ballotNum = Integer.parseInt(message[1]);
        int ballotNumServerId = Integer.parseInt(message[2]);
        if ((ballotNum > minBallotNum) || ((ballotNum == minBallotNum) && (minBallotNumServerId > ballotNumServerId))) {
            minBallotNum = ballotNum;
        }
        String reply = 
                "ack " + 
                minBallotNum + " " + 
                minBallotNumServerId + " " + 
                acceptedVal.type + " " + 
                acceptedVal.amount + " " + 
                acceptedVal.logPosition;
        try {
            ClientServer.sendTo(reply, "blah port"); //fix  
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void handleAck(String[] message) {
        
        if (leader) {
            
            // I think I'm the leader
            int receivedBalNum = Integer.parseInt(message[1]);
            int receivedBalNumServerId = Integer.parseInt(message[2]);
            if ((receivedBalNum > generateNum) || ((receivedBalNum == generateNum) && (receivedBalNumServerId > "server_id"/*change*/))) {
                // Lost election
                
                // Set myVal to the node val who won election
                            
                myVal.type = message[3];
                myVal.amount = Double.parseDouble(message[4]);
                myVal.logPosition = Integer.parseInt(message[5]);
                
                String concedeMsg = "accept "; //add more to accept 
                
                // Try to prepare another proposal
                generateNum = receivedBalNum + 1;
                regeneratePrepare();

            } else {
                // Won election                                              
                ackCount++;
                if((double)ackCount/(double)numProc > 0.5) {
                    // Consensus
                    
                    ackCount = 0;
                }
            }

        } else {
            // Not leader

        }
    }

    public void handleAccept(String[] message) {

    }
}
