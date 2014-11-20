package clientserver;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.net.*;

public class ClientServer implements Runnable {
      
    String clientSentence;
    String capitalizedSentence;
    
    ServerSocket welcomeSocket = new ServerSocket(12000);    
    System.out.println("waiting for clients...");
    
    public static void main(String[] args) {
              
    }
    
}
