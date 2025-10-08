package es.ua.sd.practica;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;
import java.awt.*;

import es.ua.sd.practica.CommonConstants;

public class EV_Central {
	public static void main(String[] args) {
        System.out.println("Iniciando EV_Central (Servidor de Control)...");
        SwingUtilities.invokeLater(() -> {
            CentralMonitorGUI gui = new CentralMonitorGUI();
            gui.addChargingPoint("CP001");
            gui.addChargingPoint("CP002");
            // ... Aquí arrancarías los servicios de Sockets y Kafka
        });
        
        String brokerIP = "192.168.1.24:9092"; 
        String topic = "mensaje"; 
        // Identificador del grupo (debe ser el mismo para consumidores que trabajan juntos)
        String groupId = "ev"; 
        new Consumer(brokerIP, topic, groupId).run();
    }
    
    /**
     * Inicia el ServerSocket y espera nuevas conexiones de EV_CP_M.
     * Utiliza un Thread Pool para manejar la concurrencia.
     */
}