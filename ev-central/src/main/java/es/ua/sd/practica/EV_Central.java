package es.ua.sd.practica;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;
import javax.swing.*;
import java.awt.*;

import es.ua.sd.practica.CommonConstants;
import es.ua.sd.practica.DatabaseManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.Set;

import static spark.Spark.*;


public class EV_Central {
	public static String brokerIP;
	public static int port;
	public static ArrayList<CP> cps = new ArrayList<>();
	public static Set<String> existingCPids = new HashSet<>();
	public static CentralMonitorGUI gui;
	public static Map<String, SecretKey> CPKeys = new ConcurrentHashMap<>();
	public static int API_AUTH;
	public static int API_PORT;
	public static String IP_DATABASE;
	private static DatabaseManager dbManager;
  
	public static void main(String[] args) {
		if (args.length < 5) 
		{
			System.err.println("Pase por argumentos el puerto del socket, la IP y puerto del broker, la IP de la base de datos, el puerto de la API de EV_w y el puerto de la API de autenticacion ");
			return;
		}
		gui = new CentralMonitorGUI(cps, CPKeys);
		port = Integer.parseInt(args[0]);
        brokerIP = args[1];
        API_PORT = Integer.parseInt(args[3]);
        API_AUTH = Integer.parseInt(args[4]);
        IP_DATABASE = args[2];
        
        String DB_NAME = "evcharging_db";
        String DB_USER = "evcharging";
        String DB_PASS = "practica2";

        dbManager = new DatabaseManager(IP_DATABASE, DB_NAME, DB_USER, DB_PASS);
        dbManager.createTable();
        
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
        
        Runnable CentralAuthenticator = new CentralAPI();
        new Thread(CentralAuthenticator).start();
        
        new Thread(() -> {
        	Producer r = new Producer(brokerIP, CommonConstants.CENTRAL_STATUS);
            while (true) {
                AddChargingPointFromDB();
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }).start();
        
        new Thread(() -> {
        	Producer r = new Producer(brokerIP, CommonConstants.CENTRAL_STATUS);
            while (true) {
                String keepAlive = "CENTRAL_STATUS#ALIVE";
                r.sendMessage(keepAlive);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }).start();
        
    }

	
	protected static String getSystemStatusAsJson() {
		String json = "[";
	    int count = 0;
	    
	    for(CP cp : cps) {
	        if (count > 0) {
	            json += ","; 
	        }

	        // UID
	        json += "{\"id\":\"" + cp.UID + "\",";
	        // LOCATION
	        json += "\"location\":\"" + cp.Location + "\",";
	        // STATUS
	        json += "\"status\":\"" + cp.State + "\",";
	        // TEMPERATURE
	        json += "\"temp\":\"" + cp.temperature + "\",";
	        // ALERT
	        json += "\"alert\":\"" + cp.alert + "\",";
	        // PRICE
	        json += "\"price\":\"" + cp.Price + "\"}"; 

	        count++;
	    }
	    
	    json += "]";
	    
	    return json;
    }
	
	public static void Serialize()
	{
		for (CP cp : cps)
		{
			dbManager.UpdateCPState(cp.UID, cp.State);
		}
	}
	
	protected static void refreshChargingPoints(CentralMonitorGUI gui) {
		Serialize();
		if (gui != null) {
	        gui.refreshChargingPoints(); 
	    } 
	}
	
	protected static void refreshLog()
	{
		if (gui != null)
		{
			gui.updateAuditLog();
		}
	}


	public static void AddChargingPointFromDB() { 
	    
	    Set<String> existingCPids = new HashSet<>(); // IDs de CPs que YA tenemos en memoria
	    Set<String> dbCPids = new HashSet<>();       // IDs de CPs LEÍDOS de la BD
	    
	    
	    List<String> cpsStrings = dbManager.GetAllCPS();
	    
	    for (String s : cpsStrings) {
	        String[] splitted = s.split("\\|");
	        String currentCPid = splitted[1];
	        dbCPids.add(currentCPid); 
	        if (!existingCPids.contains(currentCPid)) { 
	            if (!isCPinMemory(currentCPid)) { 
	                CP cp = new CP(splitted[1], splitted[3], splitted[2], "DESCONECTADO");
	                cps.add(cp);
	            }
	        }
	    }
	    
	    List<CP> cpsToRemove = new ArrayList<>();
	    for (CP cp : cps) {
	        if (!dbCPids.contains(cp.UID)) {
	            cpsToRemove.add(cp);
	        }
	    }
	    
	    cps.removeAll(cpsToRemove);
	    refreshChargingPoints(gui);
	}

	private static boolean isCPinMemory(String uid) {
	    for (CP cp : cps) {
	        if (cp.UID.equals(uid)) {
	            return true;
	        }
	    }
	    return false;
	}
    
}