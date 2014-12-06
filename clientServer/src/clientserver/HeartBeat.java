package clientserver;

// TO COPY: scp -r -i ~/Desktop/turtlebeards.pem ~/Dropbox/Current\ Documents/cs271/cs271_proj1_java/clientServer/dist/clientServer.jar ec2-user@54.174.167.183:/home/ec2-user/
// TO LOGIN: ssh -i /Users/wdai/Desktop/turtlebeards.pem ec2-user@54.174.167.183         

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class HeartBeat {
    
    public static int[] lifeTable = {0,0,0,0,0}; //1 alive, 0 dead
    public static double numProc = -1;
    
    public static void pingAll() throws IOException { //returns # of processes alive
        int alive = 0;       
        for(int i = 0; i < 5; ++i) {
            int serverPort = ClientServer.serverPorts[i];
            String serverIP = ClientServer.serverIPs[i];
            
            Socket socket = new Socket();
            try {
                String heartBeatMsg = "ping"; //maybe need to tack on server_id                                
                socket.connect(new InetSocketAddress(serverIP, serverPort), 1000); //timeout 1000ms
                lifeTable[i] = 1;
                socket.close();
                alive++;                
            } catch (Exception ex) {
                // handle the timeout
                lifeTable[i] = 0;
            }            
        }
        numProc = alive;
    }
    
    // only run when a message has been received
    public static void handlePing(String[] message) {
        int index = Integer.parseInt(message[1]); // array position 1 should be server_id
        lifeTable[index] = 1; // mark this as alive because we received a msg back
    }
}
