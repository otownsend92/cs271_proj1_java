package clientserver;

import static java.lang.Thread.sleep;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Paxos {

    public class Value {

        double amount = 0.0;
        String type = "blank";
        int logPosition = -1;
        int balNum = -1;
        int balNumServerId = -1;
    }

    public class Bucket {

        int numAccepts = 0;
        Paxos.Value v = new Paxos.Value();
    }

    // keep track of number of acks - check when matches majority value
    double ackCount = 0;
    // generateNum is incremeneted by 1 each round - used f]to generate proposal numbers in conjunction with serverID
    int generateNum = 0;
    // value object that is created at  beginning of proposal 
    Value val = new Value();
    // set when a cohort accepts a proposed value
    Value acceptedVal = new Value();
    // monitor highest val received that's not blank
    Value highestVal = new Value();
    // never initialize
    Value blankVal = new Value();
    // counter for final accepts

    public static Bucket[] ackBucketB = new Bucket[5];
    public static int[] finalAcceptBucket = {0, 0, 0, 0, 0};

    int numFinalA = 0;
    public static Vector<Value> ackedValues = new Vector();
    public static int[] ackedValBals;

    int minBallotNum = 0;
    int minBallotNumServerId = 0; //TESTING THIS? WHAT DO I SET THIS TO???
    boolean leader = false;
    boolean phase2 = false;
    boolean var = false;


    /*
     LEADER'S PERSPECTIVE
     Called when leader loses election. Will generate a new proposal number (prepareMsg)
     and use the same Value object val in a new round of prepares.
     */
//    public void regeneratePrepare() throws Exception {
//        String prepareMsg = "prepare " + generateNum + " " + ClientServer.serverId;
//        ClientServer.sendToAll(prepareMsg);
//
//        generateNum++;
//    }

    /*
     LISTENER'S PERSPECTIVE
     Is called when server receives messages in ClientServer from other servers.
     Will call various other handler methods based on message.
     */
    public void handleMsg(String msg) throws Exception {

        String[] message = msg.split(" ");

        if (message[0].equals("prepare")) {
            handlePrepare(message);
        } else if (message[0].equals("ack")) {
//            handleAck(message);
            handleAckNew(message);
        } else if (message[0].equals("accept")) {
            handleAccept(message);
        } else if (message[0].equals("finalaccept")) {
            handleFinalAccept(message);
        } else if (message[0].equals("sizepoll")) {
            handleSizeRequest(message);
        } else if (message[0].equals("requestlog")) {
            Log.sendLog(message[1]);
        } else if (message[0].equals("mysize")) {
            handleSizeResponse(message);
        }

    }

    /*
     LEADER'S PERSPECTIVE
     Receives 'msg' from ClientServer and uses it to generate a Value object val
     to be proposed to all other servers.
     */
    public void prepareMsg(String[] message) {

        generateNum++;
        leader = true;
        //String[] message = msg.split(" ");
        val.type = message[0];
        val.amount = Double.parseDouble(message[1]);
        val.logPosition = Log.transactionLog.size();
        val.balNum = generateNum;
        val.balNumServerId = ClientServer.serverId;

        String prepareMsg = "prepare " + val.balNum + " " + val.balNumServerId;
        try {
//            System.out.println("Sending prepareMsg");
            ClientServer.sendToAll(prepareMsg);
        } catch (Exception ex) {
            ex.printStackTrace();
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
        if ((ballotNum > minBallotNum) || ((ballotNum == minBallotNum) && (ballotNumServerId > minBallotNumServerId))) {
            minBallotNum = ballotNum;
            minBallotNumServerId = ballotNumServerId;
//            System.out.println("IFSTATEMENT");

        }

        String reply
                = "ack "
                + ballotNum + " "
                + ballotNumServerId + " "
                + acceptedVal.balNum + " "
                + acceptedVal.balNumServerId + " "
                + acceptedVal.type + " "
                + acceptedVal.amount + " "
                + acceptedVal.logPosition;
//        }

//        System.out.println("handlePrepare from server " + ClientServer.serverId + ": " + reply);
        try {
//            System.out.println("Sending ack");
            ClientServer.sendTo(reply, Integer.toString(ballotNumServerId));
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void handleAckNew(String[] message) {

        int receivedBalNum = Integer.parseInt(message[3]);
        int receivedBalNumServerId = Integer.parseInt(message[4]);

        int ourReceivedBalNum = Integer.parseInt(message[1]);
        int ourReceivedBalNumServerId = Integer.parseInt(message[2]);

        if ((ourReceivedBalNum == val.balNum) && (ourReceivedBalNumServerId == val.balNumServerId)) {
            Value receivedVal = new Value();
            receivedVal.type = message[5];
            receivedVal.amount = Double.parseDouble(message[6]);
            receivedVal.logPosition = Integer.parseInt(message[7]);
            receivedVal.balNum = receivedBalNum;
            receivedVal.balNumServerId = receivedBalNumServerId;

            ackCount++;
            double majority = (double) ackCount / (HeartBeat.numProc);

//            System.out.println("Received ack # " + ackCount + " from: " + receivedBalNum + " " + receivedBalNumServerId);

            // if someone has already accepted a value this round
            if (!receivedVal.type.equals("blank")) {

//                System.out.println("Not blank! Already accepted value");
                // if this received not-blank value is higher than previous not-blank value in this round, reset highest
                if ((receivedVal.balNum > highestVal.balNum) || ((receivedVal.balNum == highestVal.balNum) && (receivedVal.balNumServerId > highestVal.balNumServerId))) {
                    if (highestVal.amount == acceptedVal.amount) {
                        // old acks
                        resetHighestVal();

                    } else {

                        highestVal.type = receivedVal.type = message[5];
                        highestVal.amount = receivedVal.amount;
                        highestVal.logPosition = receivedVal.logPosition;
                        highestVal.balNum = receivedVal.balNum;
                        highestVal.balNumServerId = receivedVal.balNumServerId;
//                        System.out.println("Setting highestVal: " + highestVal.type + " " + highestVal.amount);
                    }

                }

            }

            if ((majority > 0.5) && (HeartBeat.numProc >= 3)) {
                if (phase2) {

                } else {
//                    System.out.println("majority: " + majority);
//                    System.out.println("Have reached majority");

                    // stop handling acks
                    phase2 = true;

                    // if there was an already accepted value, propose it
                    // lost this round
//                    System.out.println("highestVal: " + highestVal.type + " " + highestVal.amount);

                    if (!highestVal.type.equals("blank")) {   // !valsAreEqual(highestVal, blankVal)) {

//                        System.out.println("Server " + ClientServer.serverId + " lost, Sending concede.");
                        String concedeMsg = "accept "
                                + val.balNum + " "
                                + val.balNumServerId + " "
                                + highestVal.type + " "
                                + highestVal.amount + " "
                                + highestVal.logPosition;
                        ackCount = 0;
                        phase2 = true;
//                        resetHighestVal();
//                        System.out.println("1 Highest val now: " + highestVal.type + " " + highestVal.amount);

                        try {
                            // Accept the higher ballot
//                            System.out.println("Sending concede accept");
                            ClientServer.sendToAll(concedeMsg);
//                            ClientServer.paxosQueueObj.isProposing = false;
//                            resetHighestVal();
//                            sleep(500);
                            // Try to prepare another proposal
                            generateNum = receivedBalNum + 1;
//                            regeneratePrepare();
                        } catch (Exception ex) {
                            System.out.println(ex);
                        }
                    } else if (highestVal.type.equals("blank") && ClientServer.paxosQueueObj.isProposing) {  // won this round

//                        System.out.println("Server " + ClientServer.serverId + " won, Sending win msg.");
//                        System.out.println("Winning val: " + val.type + " " + val.amount + " " + val.logPosition);
                        String winMsg = "accept "
                                + val.balNum + " "
                                + val.balNumServerId + " "
                                + val.type + " "
                                + val.amount + " "
                                + val.logPosition;
//                        resetHighestVal();
//                        System.out.println("2 Highest val now: " + highestVal.type);
//                        ackCount = 0;
                        phase2 = true;

                        try {
                            // I won
//                            System.out.println("We have a consensus, broadcasting out the accept");
                            sleep(500);
//                            System.out.println("Sending winMsg: " + winMsg);
                            ClientServer.sendToAll(winMsg);
                        } catch (Exception ex) {
                            System.out.println(ex);
                        }
                    }

//                    resetHighestVal();
                }
            } else if (HeartBeat.numProc < 3) {
                System.out.println("Cannot reach majority, not enough servers.");
            }
        }

    }

    public boolean valsAreEqual(Value a, Value b) {

        return (a.type.equals(b.type)
                && a.amount == b.amount
                && a.balNum == b.balNum
                && a.balNumServerId == b.balNumServerId
                && a.logPosition == b.logPosition);
    }

    public void resetHighestVal() {
        highestVal.amount = blankVal.amount;
        highestVal.type = blankVal.type;
        highestVal.logPosition = blankVal.logPosition;
        highestVal.balNum = blankVal.balNum;
        highestVal.balNumServerId = blankVal.balNumServerId;
    }

    public void resetAcceptedVal() {
        acceptedVal.amount = blankVal.amount;
        acceptedVal.type = blankVal.type;
        acceptedVal.logPosition = blankVal.logPosition;
        acceptedVal.balNum = blankVal.balNum;
        acceptedVal.balNumServerId = blankVal.balNumServerId;
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
        if ((receivedBalNum > minBallotNum)
                || ((receivedBalNum == minBallotNum) && (receivedBalNumServerId >= minBallotNumServerId))) {
            acceptedVal.type = message[3];
            acceptedVal.amount = Double.parseDouble(message[4]);
            acceptedVal.logPosition = Integer.parseInt(message[5]);
            acceptedVal.balNum = receivedBalNum;
            acceptedVal.balNumServerId = receivedBalNumServerId;

//            System.out.println("handleAccept acceptedVal: " + acceptedVal.type + " " + acceptedVal.amount + " " + acceptedVal.logPosition);

            minBallotNum = receivedBalNum;
            minBallotNumServerId = receivedBalNumServerId;

            String cohortAcceptMsg = "finalaccept "
                    + receivedBalNum + " "
                    + receivedBalNumServerId + " "
                    + acceptedVal.type + " "
                    + acceptedVal.amount + " "
                    + acceptedVal.logPosition;

            try {
//                System.out.println("Broadcasting final accept: " + cohortAcceptMsg);
                ClientServer.sendToAll(cohortAcceptMsg);
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }

    }

    public void handleFinalAccept(String[] message) {
        // We're done if we get this step (if we get final accepts from ALL servers), save the final value
//        numFinalA++;
        int receivedBalNum = Integer.parseInt(message[1]);
        int receivedBalNumServerId = Integer.parseInt(message[2]);
        int serverIndex = receivedBalNumServerId;

//        System.out.println("handleFinalAccept message: " + Arrays.toString(message));
//        ackBucket[serverIndex].numAccepts++;
        finalAcceptBucket[serverIndex]++;

        if (finalAcceptBucket[serverIndex] == HeartBeat.numProc) {
            acceptedVal.type = message[3];
            acceptedVal.amount = Double.parseDouble(message[4]);
            acceptedVal.logPosition = Integer.parseInt(message[5]);

            Log.addToTransactionLog(acceptedVal, acceptedVal.logPosition);
//            System.out.println("Decided on: " + acceptedVal.amount);
            finalAcceptBucket[serverIndex] = 0;
            leader = false;

            if ((serverIndex == ClientServer.serverId) && (val.amount == acceptedVal.amount)) {
                ackCount = 0;
//                System.out.println("FH:LAHKL:SDL:ASJLKDAJLSKDJALS:DKLSD");
//                System.out.println("Accepting: " + acceptedVal.type + " " + acceptedVal.amount);
                PaxosQueue.printQ();
                ClientServer.paxosQueueObj.transactionQueue.removeElementAt(0);
                ClientServer.paxosQueueObj.isProposing = false;
                PaxosQueue.printQ();

            }
            // reset for next iteration
            phase2 = false;
            ackCount = 0;
            ClientServer.paxosQueueObj.isProposing = false;
            resetHighestVal();
            resetAcceptedVal();
            PaxosQueue.printQ();
            clearValues();
//            System.out.println("acceptedVal now: " + acceptedVal.type + " " + acceptedVal.amount);
//            System.out.println("========================== DONE WITH ROUND ========================== \n\n");
        }

        try {
            sleep(800);
        } catch (InterruptedException ex) {
            Logger.getLogger(Paxos.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void clearValues() {
        
        ackCount = 0;
        int generateNum = 0;
        
        val.amount = blankVal.amount;
        val.type = blankVal.type;
        val.logPosition = blankVal.logPosition;
        val.balNum = blankVal.balNum;
        val.balNumServerId = blankVal.balNumServerId;
        resetHighestVal();
        resetAcceptedVal();

        finalAcceptBucket[0] = 0;
        finalAcceptBucket[1] = 0;
        finalAcceptBucket[2] = 0;
        finalAcceptBucket[3] = 0;
        finalAcceptBucket[4] = 0;

        numFinalA = 0;

        minBallotNum = 0;
        minBallotNumServerId = 0; 
        leader = false;
        phase2 = false;
        var = false;
    }

    public static void handleSizeRequest(String[] request) throws Exception {
        // send size of local log back to server with attached ID
        int size = Log.transactionLog.size();
        String sizeResponse = "mysize " + size + " " + ClientServer.serverId;
        ClientServer.sendTo(sizeResponse, request[1]);
    }

    public static void handleSizeResponse(String[] response) throws Exception {

        // if local is up to date, import data
        int size = Integer.parseInt(response[1]);
        int server = Integer.parseInt(response[2]);

//        System.out.println("Server: " + server + " size is: " + size);
        ClientServer.logSizes[server] = size;

        ClientServer.heardFrom++;
//        System.out.println("heardfrom: " + ClientServer.heardFrom);
//        System.out.println("numproc: " + HeartBeat.numProc);
        if (ClientServer.heardFrom == HeartBeat.numProc - ClientServer.ctrlc) {
//            System.out.println("About to enter reqlog");
            ClientServer.requestLog();
            ClientServer.heardFrom = 0;
        }
        // else, get data from the most up to date process

        // also need to prevent user from sending messages?
    }

}
