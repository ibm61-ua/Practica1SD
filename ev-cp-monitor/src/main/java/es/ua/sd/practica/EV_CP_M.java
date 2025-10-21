package es.ua.sd.practica;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

public class EV_CP_M {
	public static String IP_Engine;
	public static int Port_Engine;
	public static String IP_Central;
	public static int Port_Central;
	public static String ID_CP;
	public static MonitorGUI gui;

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Introduce IP y puerto del EV_CP_E" + ", IP y puerto del EV_Central" + ", ID del CP");
			return;
		}
		DeserializeARGS(args);
		SwingUtilities.invokeLater(() -> {
			gui = new MonitorGUI(ID_CP);
			Runnable Connection =  new ConnectionToEngine(ID_CP, IP_Engine, Port_Engine);
			new Thread(Connection).start();
			Runnable KeepAlive = new MonitorKeepAlive(IP_Central, Port_Central, ID_CP, gui);
			new Thread(KeepAlive).start();
		});
		
	}
	
	
	
	public static void LostEngineConnection()
	{
		EVClient cpClient = new EVClient(IP_Central, Port_Central);
        
        if (cpClient.startConnection()) {
            
            String statusRequest = "ERROR#" + ID_CP;
            gui.NewMessage("[ENGINE] Enviando a CENTRAL: " + statusRequest);
            
            String response = cpClient.sendMessage(statusRequest); 
            
            if (response != null) {
            	gui.NewMessage("[CENTRAL] " + response);
            } else {
            	gui.NewMessage("[ERROR] no se recibió respuesta de CENTRAL");
            }
            
            cpClient.stopConnection();
        }
	}
	
	public static void EngineAlive()
	{
		EVClient cpClient = new EVClient(IP_Central, Port_Central);
        
        if (cpClient.startConnection()) {
            
            String statusRequest = "CONNECTION#" + ID_CP;
            gui.NewMessage("[MONITOR] Enviando a CENTRAL: " + statusRequest);
            
            String response = cpClient.sendMessage(statusRequest); 
            
            if (response != null) {
            	gui.NewMessage("[CENTRAL] " + response);
            } else {
            	gui.NewMessage("[ERROR] no se recibió respuesta de CENTRAL");
            }
            
            cpClient.stopConnection();
        }
	}

	private static void DeserializeARGS(String[] args) {
		String[] splitter;

		splitter = args[0].split(":");
		IP_Engine = splitter[0];
		Port_Engine = Integer.parseInt(splitter[1]);

		splitter = args[1].split(":");
		IP_Central = splitter[0];
		Port_Central = Integer.parseInt(splitter[1]);

		ID_CP = args[2];
	}

}
