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
			System.err.println("Introduce IP y puerto del EV_CP_E" + ", ID del Driver");
			return;
		}
		DeserializeARGS(args);

		Runnable consumerControl = new KConsumer(IP_BROKER + ":" + Port_BROKER, CommonConstants.CONTROL, UUID.randomUUID().toString(), EV_Driver::handleControl);
		new Thread(consumerControl).start();
		
		Runnable centralStatusControl = new KConsumer(IP_BROKER + ":" + Port_BROKER, CommonConstants.CENTRAL_STATUS, UUID.randomUUID().toString(), EV_Driver::handleCentralStatus);
		new Thread(centralStatusControl).start();
		
		Producer p = new Producer(IP_BROKER + ":" + Port_BROKER, CommonConstants.REQUEST);
		SwingUtilities.invokeLater(() -> {
           gui = new DriverGUI(ID_Driver, p);
	       gui.CreateButtons("cpdatabase.txt");
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
		//mensaje ejemplo: CHARGING#ALC001#DV001#10.0#10.0
		if(message.contains("RELOAD"))
		{
			handleReload(message);
			return;
		}
		
		if(!message.contains(ID_Driver)) return;
		
		if(!message.split("#")[0].equals("END") && !message.split("#")[0].equals("CHARGING") && !message.split("#")[0].equals("NOAVIABLE")) return;
		
		if(message.split("#")[0].equals("NOAVIABLE"))
		{
			gui.Log("CP no disponible. Cancelado el suministro.");
			gui.Suministrando = false;
			return;
		}
		
		if(message.split("#")[0].equals("END"))
		{
			System.out.println(message);
			gui.Log("[" + message.split("#")[0] + "] CP: " + message.split("#")[1] + message.split("#")[3]);
			gui.Suministrando = false;
			return;
		}
		Iterator<String> it = gui.logBuffer.iterator();
		String part = message.split("#")[1];

		while (it.hasNext()) {
		    String s = it.next();
		    if (s.contains(part)) {
		        it.remove();
		    }
		}
        gui.Log("[" + message.split("#")[0] + "] CP: " + message.split("#")[1] + " -> " + message.split("#")[3] + "KwH/" + message.split("#")[4] + "KwH");
    }

	private static void handleReload(String message) {
	    try {
	        message = message.substring("RELOAD#".length());

	        String[] cpEntries = message.split("\\|");

	        try (BufferedWriter writer = new BufferedWriter(new FileWriter("cpdatabase.txt", false))) {
	            
	            for (String cpData : cpEntries) {
	                cpData = cpData.trim();
	                if (cpData.isEmpty()) continue;

	                writer.write(cpData + ";DESCONECTADO");
	                writer.newLine();
	            }
	        }
	        
	        gui.CreateButtons("cpdatabase.txt");

	    } catch (IOException e) {
	        System.err.println("Error al actualizar la base de datos: " + e.getMessage());
	    } catch (Exception e) {
	        System.err.println("Error procesando mensaje RELOAD: " + e.getMessage());
	    }
	}

}
