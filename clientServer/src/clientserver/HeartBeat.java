package clientserver;

// TO COPY: scp -r -i ~/Desktop/turtlebeards.pem ~/Dropbox/Current\ Documents/cs271/cs271_proj1_java/clientServer/dist/clientServer.jar ec2-user@54.174.167.183:/home/ec2-user/
// TO LOGIN: ssh -i /Users/wdai/Desktop/turtlebeards.pem ec2-user@54.174.167.183         

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

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
                socket.connect(new InetSocketAddress(serverIP, serverPort), 1000); //timeout 1000ms
                lifeTable[i] = 1;
                socket.close();
                alive++;                
            } catch (Exception ex) {
                // handle the timeout
                // System.out.println(ex);
                lifeTable[i] = 0;
            }            
        }
        numProc = alive;
//        System.out.println("LifeTable: " + Arrays.toString(HeartBeat.lifeTable));
    }
}
