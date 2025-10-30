package es.ua.sd.practica;

import java.io.IOException;
import java.net.Socket;

public class ConnectionToEngine extends EV_CP_M implements Runnable {
    
    private final String ipEngine;
    private final String ID_CP;
    private final int portEngine;

    public ConnectionToEngine(String ID_CP, String ip, int port) {
    	this.ID_CP = ID_CP;
        this.ipEngine = ip;
        this.portEngine = port;
    }

    @Override
    public void run() {
        
        EVClient cpClient = new EVClient(ipEngine, portEngine);

        while (!Thread.currentThread().isInterrupted()) {
            
            boolean isConnected = cpClient.startConnection();
            
            if (isConnected) {
            	gui.NewMessage("[MONITOR] Conexión con Engine establecida.");
            	super.EngineAlive();
                try {
                    while (isConnected && !Thread.currentThread().isInterrupted()) {
                   
                        String statusRequest = "CONNECTION#" + ID_CP;
                        String response = cpClient.sendMessage(statusRequest); 
                        
                        if (response != null) {
                        	gui.NewMessage("[ENGINE] " + response);
                        } else {
                        	gui.NewMessage("[ERROR] Se perdió la conexión con el Engine");
                            break;
                        }
                        
                        Thread.sleep(5000); 
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
            } else {
            	gui.NewMessage("[MONITOR] Fallo al conectar con Engine. Reintentando en 10 segundos...");
                super.LostEngineConnection();
                try {
                    Thread.sleep(10000); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            cpClient.stopConnection(); 
        }
        
    }
}