package es.ua.sd.practica;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CentralMonitorGUI extends JFrame {
    
    private JPanel panelPrincipal;
    private JPanel panelInferior;  
    
    private JPanel auditPanel;    
    private JTextArea auditTextArea; 

    private JPanel ongoingPanel;   
    private JPanel ongoingContent; 

    private static ArrayList<CP> cps;
    public static Map<String, SecretKey> CPKeys = new ConcurrentHashMap<>();
    
    public static ArrayList<String> ongoingBuffer = new ArrayList<>();

    public CentralMonitorGUI(ArrayList<CP> cps, Map<String, SecretKey> cPKeys) {
        super("EVCharging Network - Central Monitor");
        CentralMonitorGUI.cps = cps;
        CPKeys = cPKeys;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800);
        setLayout(new BorderLayout(10, 10)); // Espaciado entre zonas

        panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new GridLayout(0, 4, 10, 10)); 
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollCPs = new JScrollPane(panelPrincipal);
        scrollCPs.setBorder(BorderFactory.createTitledBorder("Puntos de Carga (CPs)"));
        scrollCPs.setPreferredSize(new Dimension(1300, 400)); 
        
        this.add(scrollCPs, BorderLayout.NORTH);
        panelInferior = new JPanel();
        panelInferior.setLayout(new GridLayout(1, 2, 10, 0)); 
        panelInferior.setPreferredSize(new Dimension(1300, 350));
        panelInferior.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        auditPanel = new JPanel(new BorderLayout());
        TitledBorder bordeAudit = BorderFactory.createTitledBorder("Registro de Auditoría");
        bordeAudit.setTitleFont(new Font("SansSerif", Font.BOLD, 16));
        auditPanel.setBorder(bordeAudit);

        auditTextArea = new JTextArea();
        auditTextArea.setEditable(false);
        auditTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        auditTextArea.setBackground(new Color(30, 30, 30)); 
        auditTextArea.setForeground(Color.GREEN);
        
        JScrollPane scrollAudit = new JScrollPane(auditTextArea);
        scrollAudit.getVerticalScrollBar().addAdjustmentListener(e -> e.getAdjustable().setValue(e.getAdjustable().getMaximum()));
        
        auditPanel.add(scrollAudit, BorderLayout.CENTER);
        panelInferior.add(auditPanel); // Añadir a la izquierda

        ongoingPanel = new JPanel(new BorderLayout());
        TitledBorder bordeOngoing = BorderFactory.createTitledBorder("On-Going Driver Requests");
        bordeOngoing.setTitleFont(new Font("SansSerif", Font.BOLD, 16));
        ongoingPanel.setBorder(bordeOngoing);

        ongoingContent = new JPanel();
        ongoingContent.setLayout(new BoxLayout(ongoingContent, BoxLayout.Y_AXIS));
        
        JScrollPane scrollOngoing = new JScrollPane(ongoingContent);
        ongoingPanel.add(scrollOngoing, BorderLayout.CENTER);
        
        panelInferior.add(ongoingPanel);

        this.add(panelInferior, BorderLayout.CENTER);

        setVisible(true);
        
        refreshChargingPoints();
        updateAuditLog(); 
    }
    public void refreshChargingPoints() {
        panelPrincipal.removeAll();

        for (CP cp : cps) {
            addChargingPointWidget(cp);
        }

        panelPrincipal.revalidate();
        panelPrincipal.repaint();
    }
    
    private void addChargingPointWidget(CP cp) {
        JPanel cpPanel = new JPanel();
        cpPanel.setLayout(new BoxLayout(cpPanel, BoxLayout.Y_AXIS));
        cpPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        Color bgColor = Color.LIGHT_GRAY;
        if(cp.State.equals("DESCONECTADO")) bgColor = Color.LIGHT_GRAY;
        if(cp.State.equals("CONECTADO"))    bgColor = new Color(144, 238, 144); // Verde claro
        if(cp.State.equals("CARGANDO"))     bgColor = new Color(255, 255, 224); // Amarillo claro
        if(cp.State.equals("AVERIADO"))     bgColor = new Color(255, 99, 71);   // Rojo tomate
        if(cp.State.equals("PARADO"))       bgColor = new Color(255, 165, 0);   // Naranja

        cpPanel.setBackground(bgColor);

        Font fontBold = new Font("SansSerif", Font.BOLD, 14);
        Font fontPlain = new Font("SansSerif", Font.PLAIN, 12);

        JLabel title = new JLabel(cp.UID + " (" + cp.State + ")");
        title.setFont(fontBold);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        cpPanel.add(title);
        cpPanel.add(Box.createVerticalStrut(5));

        JLabel loc = new JLabel(cp.Location);
        loc.setAlignmentX(Component.CENTER_ALIGNMENT);
        cpPanel.add(loc);

        JLabel price = new JLabel(cp.Price + " €/kWh");
        price.setAlignmentX(Component.CENTER_ALIGNMENT);
        cpPanel.add(price);
        
        JLabel temp = new JLabel(cp.temperature + "°C");
        price.setAlignmentX(Component.CENTER_ALIGNMENT);
        cpPanel.add(temp);

        if(cp.State.equals("CARGANDO")) {
            cpPanel.add(new JSeparator());
            JLabel driver = new JLabel(cp.driver);
            driver.setAlignmentX(Component.CENTER_ALIGNMENT);
            cpPanel.add(driver);
            
            JLabel kwh = new JLabel(cp.KWHRequested + " kWh");
            kwh.setAlignmentX(Component.CENTER_ALIGNMENT);
            cpPanel.add(kwh);
            
            float cost = Float.parseFloat(cp.Price) * cp.KWHRequested;
            JLabel total = new JLabel(String.format("Total: %.2f €", cost));
            total.setFont(fontBold);
            total.setAlignmentX(Component.CENTER_ALIGNMENT);
            cpPanel.add(total);
        }

        cpPanel.add(Box.createVerticalStrut(10));
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);

        if(cp.State.equals("CONECTADO")) {
            JButton stopBtn = new JButton("STOP");
            stopBtn.setBackground(Color.RED);
            stopBtn.setForeground(Color.WHITE);
            stopBtn.addActionListener(e -> {
                cp.State = "PARADO";
                RegistroDeAuditoria.NewLog("Central", "Event", "MANUAL STOP: " + cp.UID + " detenido por operador.");
                refreshChargingPoints();
                updateAuditLog();
            });
            
            JButton revokeBtn = new JButton("Revocar clave");
            revokeBtn.setBackground(Color.RED);
            revokeBtn.setForeground(Color.WHITE);
            revokeBtn.addActionListener(e -> {
                revocarClaveManualmente(cp.UID);
            });
            btnPanel.add(stopBtn);
            btnPanel.add(revokeBtn);
        } 
        else if(cp.State.equals("PARADO")) {
            JButton startBtn = new JButton("START");
            startBtn.setBackground(Color.GREEN);
            startBtn.addActionListener(e -> {
                cp.State = "CONECTADO";
                RegistroDeAuditoria.NewLog("Central", "Event", "MANUAL START: " + cp.UID + " activado por operador.");
                refreshChargingPoints();
                updateAuditLog();
            });
            JButton revokeBtn = new JButton("Revocar clave");
            revokeBtn.setBackground(Color.RED);
            revokeBtn.setForeground(Color.WHITE);
            revokeBtn.addActionListener(e -> {
                revocarClaveManualmente(cp.UID);
            });
            btnPanel.add(startBtn);
            btnPanel.add(revokeBtn);
        }

        cpPanel.add(btnPanel);
        panelPrincipal.add(cpPanel);
    }
    
    public void revocarClaveManualmente(String cpId) {
        if (CPKeys.containsKey(cpId)) {
            CPKeys.remove(cpId);
        }

        for (CP cp : cps) {
            if (cp.UID.equals(cpId)) {
                cp.autenticado = false;
                cp.State = "DESCONECTADO";
                
                RegistroDeAuditoria.NewLog("Central", "Security", "Clave revocada manualmente para " + cpId);
                break;
            }
        }

        refreshChargingPoints(); 
        updateAuditLog();
    }


    
    public void updateAuditLog() {
        StringBuilder sb = new StringBuilder();
        for (String log : RegistroDeAuditoria.log) {
            sb.append(log).append("\n");
        }
        auditTextArea.setText(sb.toString());
    }

    public void addOngoingMessage(String message) {
        if (!ongoingBuffer.contains(message)) {
            ongoingBuffer.add(message);
            refreshOngoingPanel();
        }
    }

    public void removeOngoingMessage(String messagePattern) {
        ongoingBuffer.removeIf(s -> s.contains(messagePattern));
        refreshOngoingPanel();
    }

    private void refreshOngoingPanel() {
        ongoingContent.removeAll();
        
        for (String msg : ongoingBuffer) {
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT));
            item.setBackground(Color.WHITE);
            item.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            
            JLabel text = new JLabel(msg);
            text.setFont(new Font("SansSerif", Font.PLAIN, 14));
            
            item.add(text);
            ongoingContent.add(item);
        }
        
        ongoingContent.revalidate();
        ongoingContent.repaint();
    }

}