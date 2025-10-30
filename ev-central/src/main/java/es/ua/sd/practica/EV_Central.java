package es.ua.sd.practica;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;
import java.awt.*;

import es.ua.sd.practica.CommonConstants;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Scanner;

public class EV_Central {
	public static String brokerIP;
	public static int port;
	public static ArrayList<CP> cps = new ArrayList<>();
	public static CentralMonitorGUI gui;
	
  
	public static void main(String[] args) {
		if (args.length < 2) 
		{
			System.err.println("Pase por argumentos el puerto del socket y la IP y puerto del broker ");
			return;
		}
		AddChargingPointFromDB();
        SwingUtilities.invokeLater(() -> {
            gui = new CentralMonitorGUI(cps);
            AddCPToGui(gui);
            OnGoingPanel(gui);
            MessagePanel(gui);
        });
        
        port = Integer.parseInt(args[0	]);
        brokerIP = args[1];
        
        String topicRequest = CommonConstants.REQUEST; 
        String topicTelemetry = CommonConstants.TELEMETRY;
        
        String groupId = "ev"; 
        CentralLogic centralLogic = new CentralLogic();
        Runnable consumerRequest = new KConsumer(brokerIP, topicRequest, groupId, centralLogic::handleRequest);
        Runnable consumerTelemetry = new KConsumer(brokerIP, topicTelemetry, groupId, centralLogic::handleTelemetry);

        
        new Thread(consumerTelemetry).start();
        new Thread(consumerRequest).start();
        
        Runnable EVServer = new EVServerSocket(port, centralLogic::handleCP);
        new Thread(EVServer).start();
        
        Runnable checker = new HeartbeatChecker(centralLogic.getLastHeartbeat());
        new Thread(checker).start();
        
        new Thread(() -> {
        	Producer r = new Producer(brokerIP, CommonConstants.CENTRAL_STATUS);
            while (true) {
                String keepAlive = "CENTRAL_STATUS#ALIVE";
                r.sendMessage(keepAlive);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }).start();
    }
	
	
	private static void AddCPToGui(CentralMonitorGUI gui) {
		for(CP cp : cps)
		{
			gui.addChargingPoint(cp);
		}
		
	}
	
	public static void Serialize()
	{
		try (FileWriter fileWriter = new FileWriter("cpdatabase.txt", false); PrintWriter printWriter = new PrintWriter(fileWriter)) {

				for (CP cp : cps)
				{
					String str = cp.UID + ";" + cp.Price + ";" + cp.Location + ";" + cp.State;
		            printWriter.println(str);
				}
	        } catch (IOException e) {
	            System.err.println("Error al escribir en el archivo: " + e.getMessage());
	        }
	}
	
	protected static void refreshChargingPoints(CentralMonitorGUI gui) {
		Serialize();
	    gui.clearAllChargingPoints(); 
	    AddCPToGui(gui); 
	}


	public static void AddChargingPointFromDB()
	{
		try {
            File archivo = new File("cpdatabase.txt");
            Scanner s = new Scanner(archivo);

            while (s.hasNextLine()) {
                String linea = s.nextLine();
                String[] splited = linea.split(";");
                CP cp = new CP(splited[0], splited[1], splited[2], "DESCONECTADO");
                cps.add(cp);
            }

            s.close();
        } catch (FileNotFoundException e) {
            System.out.println("No se encontró el archivo.");
            e.printStackTrace();
        }
		
		
	}
	
	public static void OnGoingPanel(CentralMonitorGUI gui)
	{
		 gui.OnGoingPanel("");
	}
	
	public static void MessagePanel(CentralMonitorGUI gui)
	{
		 gui.MessagePanel("");
	}
    
}