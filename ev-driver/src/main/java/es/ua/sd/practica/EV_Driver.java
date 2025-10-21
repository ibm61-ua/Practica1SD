package es.ua.sd.practica;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

public class EV_Driver {
	private static final int HEARTBEAT_INTERVAL_SECONDS = 3;
	public static String IP_BROKER;
	public static int Port_BROKER;
	
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
		Producer p = new Producer(IP_BROKER + ":" + Port_BROKER, CommonConstants.REQUEST);
		SwingUtilities.invokeLater(() -> {
           gui = new DriverGUI(ID_Driver, p);
	       gui.CreateButtons("cpdatabase.txt");
	       gui.Log("");
	   		
        });
		
	}
	
	private static void DeserializeARGS(String[] args) {
		String[] splitter;

		splitter = args[0].split(":");
		IP_BROKER = splitter[0];
		Port_BROKER = Integer.parseInt(splitter[1]);

		ID_Driver = args[1];
	}
	
	public static void handleControl(String message) {
		//mensaje ejemplo: CHARGING#ALC001#DV001#10.0#10.0
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
			gui.Log("[" + message.split("#")[0] + "] CP: " + message.split("#")[1]);
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

}
