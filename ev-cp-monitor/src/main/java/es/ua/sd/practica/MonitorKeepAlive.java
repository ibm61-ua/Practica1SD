package es.ua.sd.practica;

import java.util.concurrent.TimeUnit;
import java.io.IOException;

public class MonitorKeepAlive implements Runnable {
    
    private final String ipCentral;
    private final int portCentral;
    private final String cpID;
    private final MonitorGUI gui;
    
    private static final long HEARTBEAT_INTERVAL_SECONDS = 1; 

    public MonitorKeepAlive(String ip, int port, String id, MonitorGUI gui) {
        this.ipCentral = ip;
        this.portCentral = port;
        this.cpID = id;
        this.gui = gui;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            
            EVClient cpClient = new EVClient(ipCentral, portCentral);
            
            if (cpClient.startConnection()) {
                
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        
                        String statusRequest = "KEEPALIVE#" + cpID;
                        cpClient.sendOnly(statusRequest);
                        
                        TimeUnit.SECONDS.sleep(HEARTBEAT_INTERVAL_SECONDS); 
                    }
                    
                } catch (InterruptedException e) {
                	System.out.println(e);
                    Thread.currentThread().interrupt();
                
                } 
                
                cpClient.stopConnection();

            } else {
                try {
                    TimeUnit.SECONDS.sleep(1); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}