package clientserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class HeartBeat {
    
    public static int[] lifeTable = {0,0,0,0,0}; //1 alive, 0 dead
    
    public void pingAll() {
        for(int i = 0; i < 5; ++i) {

            int p = ClientServer.serverPorts[i];
            Socket clientSocket; 
            try {
                String heartBeatMsg = "ping"; //maybe need to tack on server_id
                clientSocket = new Socket("localhost", p); //serverPorts[leader]);
                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                outToServer.writeBytes(heartBeatMsg);
                clientSocket.close();
            } catch (IOException ex) {
                System.out.println(ex);
            }            
        }
    }
    
    // only run when a message has been received
    public static void handlePing(String[] message) {
        int index = Integer.parseInt(message[1]); // array position 1 should be server_id
        lifeTable[index] = 1; // mark this as alive because we received a msg back
    }
      
    public void checkIfAlive() {
        for(int i = 0; i < 5; ++i) {
            if(lifeTable[i] == 1) {
                ;
            } else {
                ;
            }
        }
    }
}
