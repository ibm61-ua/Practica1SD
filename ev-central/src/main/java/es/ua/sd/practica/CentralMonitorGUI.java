package es.ua.sd.practica;

import javax.swing.*;
import java.awt.*;

public class CentralMonitorGUI extends JFrame {
	private JPanel panelPrincipal;
    // Aquí podrías definir un JTable o varios JLabels para el estado de los CPs

    public CentralMonitorGUI() {
        // 1. Configuración de la Ventana (JFrame)
        super("EVCharging Network - Central Monitor");
        
        // Cierra la aplicación cuando se cierra la ventana
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setSize(800, 600); // Tamaño inicial
        
        // 2. Creación del Panel Principal
        panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new GridLayout(0, 4, 10, 10)); // Layout para mostrar 4 CPs por fila
        
        // 3. Añadir el panel al marco
        this.add(panelPrincipal);
        
        // 4. Hacer visible la ventana
        setVisible(true);
    }
    
    // Método para simular la adición de un CP
    public void addChargingPoint(String cpId) {
        // Crea un panel para el CP con su color de estado
        JPanel cpPanel = new JPanel();
        cpPanel.setBorder(BorderFactory.createTitledBorder(cpId));
        cpPanel.setBackground(Color.GRAY); // Estado inicial: Desconectado
        
        // Añade info específica del CP (ej. un JLabel con "Estado: Desconectado")
        cpPanel.add(new JLabel("ID: " + cpId));
        
        // Añade el CP al panel principal
        panelPrincipal.add(cpPanel);
        // Es necesario repintar el panel si añades componentes dinámicamente
        panelPrincipal.revalidate();
        panelPrincipal.repaint();
    }
}
