package es.ua.sd.practica;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;
import java.awt.*;

import es.ua.sd.practica.CommonConstants;
import es.ua.sd.practica.DatabaseManager;
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
		for (CP cp : cps)
		{
			DatabaseManager.UpdateCPState(cp.UID, cp.State);
		}
	}
	
	protected static void refreshChargingPoints(CentralMonitorGUI gui) {
		Serialize();
	    gui.clearAllChargingPoints(); 
	    AddCPToGui(gui); 
	}


	public static void AddChargingPointFromDB()
	{
		List<String> cpsStrings = DatabaseManager.GetAllCPS();
		for(String s : cpsStrings)
		{
			String[] splitted = s.split("\\|");
			CP cp = new CP(splitted[1], splitted[3], splitted[2], "DESCONECTADO");
	        cps.add(cp);
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