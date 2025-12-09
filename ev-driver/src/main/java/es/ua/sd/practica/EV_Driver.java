package es.ua.sd.practica;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

public class EV_Driver {
	private static final Duration HEARTBEAT_INTERVAL_SECONDS = Duration.ofSeconds(5);
	public static String IP_BROKER;
	public static int Port_BROKER;
	public static Instant lastHeartBeatCentral = Instant.now();;
	
	public static String ID_Driver;
	public static DriverGUI gui;
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Introduce IP y puerto del broker" + ", ID del Driver");
			return;
		}
		DeserializeARGS(args);

		Runnable consumerControl = new KConsumer(IP_BROKER + ":" + Port_BROKER, CommonConstants.CONTROL, UUID.randomUUID().toString(), EV_Driver::handleControl);
		new Thread(consumerControl).start();
		
		Runnable centralStatusControl = new KConsumer(IP_BROKER + ":" + Port_BROKER, CommonConstants.CENTRAL_STATUS, UUID.randomUUID().toString(), EV_Driver::handleCentralStatus);
		new Thread(centralStatusControl).start();
		
		Producer p = new Producer(IP_BROKER + ":" + Port_BROKER, CommonConstants.REQUEST);
		SwingUtilities.invokeLater(() -> {
           gui = new DriverGUI(ID_Driver, p, IP_BROKER);
	       gui.CreateButtons();
	       gui.Log("");
	   		
        });
		
		new Thread(() -> {
            while (true) {
            	Instant now = Instant.now();
            	Duration timeSinceLastContact = Duration.between(lastHeartBeatCentral, now);
                
                if (timeSinceLastContact.compareTo(HEARTBEAT_INTERVAL_SECONDS) > 0) {
                	gui.Central_Status = false;
                	gui.updateCentralStatus(false);
                }
            }
        }).start();
		
	}
	
	private static void DeserializeARGS(String[] args) {
		String[] splitter;

		splitter = args[0].split(":");
		IP_BROKER = splitter[0];
		Port_BROKER = Integer.parseInt(splitter[1]);

		ID_Driver = args[1];
	}
	
	public static void handleCentralStatus(String message) {
		lastHeartBeatCentral = Instant.now();
		gui.Central_Status = true;
		gui.updateCentralStatus(true);
	}
	
	public static void handleControl(String message) {
	    if (!message.contains(ID_Driver)) return;

	    String[] parts = message.split("#");
	    String tipo = parts[0];

	    if (tipo.equals("NOAVIABLE")) {
	        gui.Log("[ERROR] CP no disponible. Suministro cancelado.");
	        gui.Suministrando = false;
	        return;
	    }
	    
	    if (tipo.equals("REJECT")) {
	        gui.Log("[ERROR] CP ha cancelado el suministro");
	        gui.Suministrando = false;
	        return;
	    }

	    if (tipo.equals("END")) {
	        String infoExtra = (parts.length > 3) ? parts[3] : "";
	        
	        gui.Log("[FIN] CP: " + parts[1] + " Terminado. " + infoExtra);
	        gui.Suministrando = false;
	        return;
	    }

	    if (tipo.equals("CHARGING")) {
	        String logUpdate = String.format("[CARGANDO] CP: %s -> %s kWh / %s kWh", 
	                                         parts[1], // ID del CP
	                                         parts[3], // Actual
	                                         parts[4]  // Total
	        );
	        
	        gui.Log(logUpdate);
	    }
	}

}
