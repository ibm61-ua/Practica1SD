package es.ua.sd.practica;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;
import java.awt.*;

import es.ua.sd.practica.CommonConstants;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class EV_Central {
	public static String brokerIP;
  
	public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CentralMonitorGUI gui = new CentralMonitorGUI();
            AddChargingPointFromDB(gui);
            OnGoingPanel(gui);
            MessagePanel(gui);
        });
        
        brokerIP = args[0]; 
        String topicRequest = CommonConstants.REQUEST; 
        String topicTelemetry = CommonConstants.TELEMETRY;
        String topicControl = CommonConstants.CONTROL;
        
        String groupId = "ev"; 
        CentralLogic centralLogic = new CentralLogic();
        Runnable consumerRequest = new KConsumer(brokerIP, topicRequest, groupId, centralLogic::handleRequest);
        Runnable consumerTelemetry = new KConsumer(brokerIP, topicTelemetry, groupId, centralLogic::handleTelemetry);

        
        new Thread(consumerTelemetry).start();
        new Thread(consumerRequest).start();
    }
	
	public static void AddChargingPointFromDB(CentralMonitorGUI gui)
	{
		try {
            File archivo = new File("cpdatabase.txt");
            Scanner s = new Scanner(archivo);

            while (s.hasNextLine()) {
                String linea = s.nextLine();
                gui.addChargingPoint(linea);
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
    
    /**
     * Inicia el ServerSocket y espera nuevas conexiones de EV_CP_M.
     * Utiliza un Thread Pool para manejar la concurrencia.
     */
}