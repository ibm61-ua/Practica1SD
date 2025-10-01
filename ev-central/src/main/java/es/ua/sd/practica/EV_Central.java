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
        // 1. INICIAR EL SERVIDOR DE SOCKETS (Para Registro/Autenticación de CPs)
        // Se ejecuta en un hilo separado para no bloquear el main.
        
        // 2. INICIAR LOS SERVICIOS DE KAFKA (Consumidores y Productores)
        
        // Consumidor 1: Recibe solicitudes de recarga del EV_Driver (ev_requests)
        // Aquí debes iniciar un hilo o servicio que escuche este tópico.
        // new Thread(new KafkaConsumerService(CommonConstants.TOPIC_REQUESTS)).start();
        
        // Consumidor 2: Recibe telemetría de consumo del EV_CP_E (ev_telemetry)
        // Aquí debes iniciar un hilo o servicio que escuche este tópico.
        // new Thread(new KafkaConsumerService(CommonConstants.TOPIC_TELEMETRY)).start();

        // 3. PANTALLA DE MONITORIZACIÓN Y CONTROL (Opcional en el main)
        // Puedes iniciar la interfaz de usuario/consola aquí para mostrar el estado de los CPs.
        // System.out.println("✅ Central operativa. Monitoreando consola para comandos de mantenimiento...");
    }
    
    /**
     * Inicia el ServerSocket y espera nuevas conexiones de EV_CP_M.
     * Utiliza un Thread Pool para manejar la concurrencia.
     */
}