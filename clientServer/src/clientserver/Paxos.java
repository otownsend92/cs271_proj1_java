package clientserver;

import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Paxos {

    public class Value {
        double amount = 0.0;
        String type = "blank";
        int logPosition = -1;
    }
    
    // keep track of number of acks - check when matches majority value
    double ackCount = 0;
    // generateNum is incremeneted by 1 each round - used f]to generate proposal numbers in conjunction with serverID
    int generateNum = 0;
    // value object that is created at  beginning of proposal 
    Value val = new Value();
    // set when a cohort accepts a proposed value
    Value acceptedVal = new Value();
    // what leader sends out in accept() message
    Value myVal = new Value();
    // counter for final accepts
    int numFinalA = 0;

    int minBallotNum = 0;
    int minBallotNumServerId = 0; //set this later
    boolean leader = false;
    boolean phase2 = false;

    /*
     LEADER'S PERSPECTIVE
     Receives 'msg' from ClientServer and uses it to generate a Value object val
     to be proposed to all other servers.
     */
    public void prepareMsg(String [] message) {
        
        generateNum++;
        //String[] message = msg.split(" ");
        val.type = message[0];
        val.amount = Double.parseDouble(message[1]);
        
//        System.out.println("PREPAREMSG logsize: " + Log.transactionLog.size());
        // Need to get postion from log
        val.logPosition = Log.transactionLog.size();
                
        String prepareMsg = "prepare " + generateNum + " " + ClientServer.serverId; 
        try {
//            System.out.println("SENDTOALL BEFORE START: " + prepareMsg);
            ClientServer.sendToAll(prepareMsg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        leader = true; //??
    }

    /*
     LEADER'S PERSPECTIVE
     Called when leader loses election. Will generate a new proposal number (prepareMsg)
     and use the same Value object val in a new round of prepares.
     */
    public void regeneratePrepare() throws Exception {
        String prepareMsg = "prepare " + generateNum + " " + ClientServer.serverId; 
        ClientServer.sendToAll(prepareMsg);

        generateNum++;
    }

    /*
     LISTENER'S PERSPECTIVE
     Is called when server receives messages in ClientServer from other servers.
     Will call various other handler methods based on message.
     */
    public void handleMsg(String msg) {
        
        String []message = msg.split(" ");

        if (message[0].equals("prepare")) {
            handlePrepare(message);
        } else if (message[0].equals("ack")) {
            handleAck(message);
        } else if (message[0].equals("accept")) {
            handleAccept(message);
        } else if (message[0].equals("finalaccept")) {
            handleFinalAccept(message);
        } 
    }

    /*
     COHORT'S PERSPECTIVE
     If receive a prepare message from some server, check if you haven't already
     agreed to higher ballot number. Regardless if you have or have not, reply
     with ack and most recent/highest ballot number seen so far with its value.
     If haven't accepted value
     */
    public void handlePrepare(String[] message) {
        int ballotNum = Integer.parseInt(message[1]);
        int ballotNumServerId = Integer.parseInt(message[2]);
        if ((ballotNum > minBallotNum) || ((ballotNum == minBallotNum) && (minBallotNumServerId > ballotNumServerId))) {
            minBallotNum = ballotNum;

            /*
             TODO:
             If have already accepted proposal - set reply Value value to this val,
             otherwise, set it to null so handleAck will know if some other erver has accepted
             a value or not.
             */
        }
        String reply
                = "ack "
                + minBallotNum + " "
                + minBallotNumServerId + " "
                + acceptedVal.type + " "
                + acceptedVal.amount + " "
                + acceptedVal.logPosition;
        try {
            ClientServer.sendTo(reply, Integer.toString(ballotNumServerId));  
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /*
     LEADER'S PERSPECTIVE and COHORT'S PERSPECTIVE
     (Maybe we should split this up into two methods?)
    
     If leader and if have received 'ack' from majority:
     if all vals in acks are null, good to go: myVal = initial proposed value
     else there is already an accepted value:
     set myVal to accepted, send accept(myVal), rerun Paxos with original proposed value
     */
    public void handleAck(String[] message) {

        if (leader) {
            // I think I'm the leader
            int receivedBalNum = Integer.parseInt(message[1]);
            int receivedBalNumServerId = Integer.parseInt(message[2]);
            // compare ballot number(receivedBalNum) and the server ID(receivedBalNumServerId
            if ((receivedBalNum > generateNum) || ((receivedBalNum == generateNum) && (receivedBalNumServerId > ClientServer.serverId))) {
                // Lost election
                System.out.println("Didn't get a vote");
                // Set myVal to the node val who won election
                // This is the value object
                myVal.type = message[3];
                myVal.amount = Double.parseDouble(message[4]);
                myVal.logPosition = Integer.parseInt(message[5]);

                String concedeMsg = "accept "
                        + receivedBalNum + " "
                        + receivedBalNumServerId + " "
                        + myVal.type + " "
                        + myVal.amount + " "
                        + myVal.logPosition;
                try {
                    // Accept the higher ballot
                    ClientServer.sendToAll(concedeMsg);
                    leader = false;
                    // Try to prepare another proposal
                    generateNum = receivedBalNum + 1;
                    regeneratePrepare();
                } catch (Exception ex) {
                    System.out.println(ex);
                }

            } else {
                if(phase2) {
                    // already sent out broadcast. do nothing
                } else {
                    // Won election   
                    System.out.println("Got a vote!");
                    ackCount++;
                    double majority = (double) ackCount / (HeartBeat.numProc);
                    System.out.println(majority);
                    if ( majority > 0.5) {
                        // Consensus
                        phase2 = true;
                        String winMsg = "accept "
                                + receivedBalNum + " "
                                + receivedBalNumServerId + " "
                                + val.type + " "
                                + val.amount + " "
                                + val.logPosition;
                        try {
                            // I won
                            System.out.println("We have a consensus, broadcasting out the accept");
                            ClientServer.sendToAll(winMsg);
                            leader = true; // ???????????? not sure
                        } catch (Exception ex) {
                            System.out.println(ex);
                        }                   
                        ackCount = 0;
                    }
                }
            }

        } else {
            // Not leader - do nothing
            System.out.println("I'm a cohort");
        }
    }

    /*
     COHORT'S PERSPECTIVE
    
     (Possibly set acceptedVal to default values/null so next election round
     knows that nothing has been accepted so far in new round)
     */
    public void handleAccept(String[] message) {

        int receivedBalNum = Integer.parseInt(message[1]);
        int receivedBalNumServerId = Integer.parseInt(message[2]);
        
        // compare ballot number(receivedBalNum) and the server ID(receivedBalNumServerId
        if ((receivedBalNum > generateNum) || ((receivedBalNum == generateNum) && (receivedBalNumServerId > ClientServer.serverId))) {
            acceptedVal.type = message[3];
            acceptedVal.amount = Double.parseDouble(message[4]);
            acceptedVal.logPosition = Integer.parseInt(message[5]);

            String cohortAcceptMsg = "finalaccept "
                    + receivedBalNum + " "
                    + receivedBalNumServerId + " "
                    + acceptedVal.type + " "
                    + acceptedVal.amount + " "
                    + acceptedVal.logPosition;

            try {
                ClientServer.sendToAll(cohortAcceptMsg);
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }

    }
    
    public void handleFinalAccept(String[] message) {
        // We're done if we get this step (if we get final accepts from ALL servers), save the final value
        numFinalA++;
        if(numFinalA == HeartBeat.numProc) {
            acceptedVal.type = message[3];
            acceptedVal.amount = Double.parseDouble(message[4]);
            acceptedVal.logPosition = Integer.parseInt(message[5]);

            Log.addToTransactionLog(acceptedVal);
            leader = false;

            if(acceptedVal == val) {
                PaxosQueue.isProposing = false;
                PaxosQueue.transactionQueue.remove(0);
            }
            // reset for next iteration
            numFinalA = 0;
            phase2 = false;
            System.out.println("Decided on: " + val.amount);
        }
    }
}
