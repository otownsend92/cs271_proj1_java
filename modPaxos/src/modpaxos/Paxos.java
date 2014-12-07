package modpaxos;

import static modpaxos.Log.transactionLog;
import java.io.IOException;
import java.io.Serializable;
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
    public static Vector<Value> ackedValues = new Vector();
    public static int[] ackedValBals;

    int minBallotNum = 0;
    int minBallotNumServerId = 0; //TESTING THIS? WHAT DO I SET THIS TO???
    boolean leader = false;
    boolean phase2 = false;
    boolean var = false;

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

//        System.out.println("PREPAREMSG logsize: " + Log.transactionLog.size());
        // Need to get postion from log
        val.logPosition = Log.transactionLog.size();

        String prepareMsg = "prepare " + generateNum + " " + ClientServer.serverId;
        try {
            System.out.println("Sending prepareMsg");
            ClientServer.sendToAll(prepareMsg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
    public void handleMsg(String msg) throws Exception {

        String[] message = msg.split(" ");

        if (message[0].equals("accept")) {
            handleAccept(message);
        } else if (message[0].equals("finalaccept")) {
            handleFinalAccept(message);
        } else if (message[0].equals("sizepoll")) {
            handleSizeRequest(message);
        } else if (message[0].equals("requestlog")) {
            Log.sendLog(message[1]);
        } else if (message[0].equals("mysize")) {
            handleSizeResponse(message);
        } else if (message[0].equals("cohort")) {
            handleCohort(message);
        }

    }

    /*
     COHORT'S PERSPECTIVE
    
     (Possibly set acceptedVal to default values/null so next election round
     knows that nothing has been accepted so far in new round)
     */
    public void handleAccept(String[] message) {

        acceptedVal.type = message[1];
        acceptedVal.amount = Double.parseDouble(message[2]);

        String cohortAcceptMsg = "finalaccept "
                + acceptedVal.type + " "
                + acceptedVal.amount;

        try {
            System.out.println("Broadcasting final accept");
            ClientServer.sendToAll(cohortAcceptMsg);
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    public void handleFinalAccept(String[] message) {
        // We're done if we get this step (if we get final accepts from ALL servers), save the final value
        numFinalA++;
        if (numFinalA == HeartBeat.numProc) {
            acceptedVal.type = message[1];
            acceptedVal.amount = Double.parseDouble(message[2]);

            Log.addToTransactionLog(acceptedVal);
            leader = false;

            if ((acceptedVal.type.equals(val.type))
                    && (acceptedVal.amount == val.amount)
                    ) {
                System.out.println("FH:LAHKL:SDL:ASJLKDAJLSKDJALS:DKLSD");
                PaxosQueue.printQ();
                ClientServer.paxosQueueObj.isProposing = false;
                if (HeartBeat.leaderId == ClientServer.serverId) {
                    ClientServer.paxosQueueObj.transactionQueue.removeElementAt(0);
                }
            }
            // reset for next iteration
            numFinalA = 0;
            phase2 = false;
            minBallotNum = 0;
            minBallotNumServerId = 0;
            System.out.println("Decided on: " + acceptedVal.amount);
        }
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

        System.out.println("Server: " + server + " size is: " + size);

        ClientServer.logSizes[server] = size;

        ClientServer.heardFrom++;
        System.out.println("heardfrom: " + ClientServer.heardFrom);
        System.out.println("numproc: " + HeartBeat.numProc);
        if (ClientServer.heardFrom == HeartBeat.numProc - ClientServer.ctrlc) {
            System.out.println("About to enter reqlog");
            ClientServer.requestLog();
            ClientServer.heardFrom = 0;
        }
        // else, get data from the most up to date process

        // also need to prevent user from sending messages?
    }

    public static void handleCohort(String[] message) {
        PaxosQueue.transactionQueue.add(message);
        System.out.println("Handle cohort");
    }

}
