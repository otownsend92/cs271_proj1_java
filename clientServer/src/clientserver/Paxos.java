package clientserver;

import java.io.Serializable;

public class Paxos {

    public class Value {
        double amount = 0.0;
        String type;
        int logPosition = -1;
    }
    
    // keep track of number of acks - check when matches majority value
    double ackCount = 0;
    // generateNum is incremeneted by 1 each round - used f]to generate proposal numbers in conjunction with serverID
    int generateNum = 0;
    // value object that is created at  beginning of proposal 
    Value val;
    // set when a cohort accepts a proposed value
    Value acceptedVal;
    // what leader sends out in accept() message
    Value myVal;

    int minBallotNum = 0;
    int minBallotNumServerId = 0; //set this later
    boolean leader = false;

    /*
     LEADER'S PERSPECTIVE
     Receives 'msg' from ClientServer and uses it to generate a Value object val
     to be proposed to all other servers.
     */
    public void prepareMsg(String [] message) throws Exception {
        //String[] message = msg.split(" ");
        val.type = message[0];
        System.out.println("MSG " + val.type);
        val.amount = Double.parseDouble(message[1]);
        System.out.println("AMT " + val.amount);
        // Need to get postion from log
        val.logPosition = Log.transactionLog.size();
                
        String prepareMsg = "prepare " + generateNum + " server_id"; //change server_id
        System.out.println("SENDTOALL " + prepareMsg);
        ClientServer.sendToAll(prepareMsg);
        leader = true; //??
        generateNum++;
    } 

    /*
     LEADER'S PERSPECTIVE
     Called when leader loses election. Will generate a new proposal number (prepareMsg)
     and use the same Value object val in a new round of prepares.
     */
    public void regeneratePrepare() throws Exception {
        String prepareMsg = "prepare " + generateNum + " server_id"; //change server_id
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
            handleAccept(message);
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
            ClientServer.sendTo(reply, "blah port"); //fix  
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
                // Won election                                              
                ackCount++;
                if (((double) ackCount) / (HeartBeat.numProc) > 0.5) {
                    // Consensus
                    String winMsg = "accept "
                            + receivedBalNum + " "
                            + receivedBalNumServerId + " "
                            + myVal.type + " "
                            + myVal.amount + " "
                            + myVal.logPosition;
                    try {
                        // I won
                        ClientServer.sendToAll(winMsg);
                        leader = true; // ???????????? not sure
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }                   
                    ackCount = 0;
                }
            }

        } else {
            // Not leader - do nothing
            
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
        // We're done if we get this step, save the final value
        
        acceptedVal.type = message[3];
        acceptedVal.amount = Double.parseDouble(message[4]);
        acceptedVal.logPosition = Integer.parseInt(message[5]);
        
        Log.addToTransactionLog(acceptedVal);
        leader = false;
        
        if(acceptedVal == val) {
            PaxosQueue.isProposing = false;
            PaxosQueue.transactionQueue.remove(0);
        }
    }
}
