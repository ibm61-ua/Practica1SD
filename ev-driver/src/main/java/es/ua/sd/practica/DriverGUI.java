package es.ua.sd.practica;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class DriverGUI extends JFrame {
    
    private JPanel panelSuperiorCPs;
    private JPanel panelInferiorControl;
    private JPanel panelDerechoLog; 
    
    private JPanel containerCPs;
    private JTextArea textAreaLog;
    private JLabel labelCentralStatus;
    
    private String ip;
    
    private Producer producer;
    private String name;
    public boolean Suministrando = false;
    public static ArrayList<CP> cps = new ArrayList<>();
    public static boolean Central_Status = false;

    public DriverGUI(String name, Producer p, String ip) {
        super(name + " - Driver Client");
        this.producer = p;
        this.name = name;
        this.ip = ip;
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 700);
        setLayout(new BorderLayout(10, 10));

        containerCPs = new JPanel();
        containerCPs.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10)); 
        
        JScrollPane scrollCPs = new JScrollPane(containerCPs);
        scrollCPs.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK), 
            "Puntos de Recarga Disponibles (Base de Datos)",
            TitledBorder.LEFT, TitledBorder.TOP, 
            new Font("SansSerif", Font.BOLD, 14)
        ));
        scrollCPs.setPreferredSize(new Dimension(400, 400)); 
        
        this.add(scrollCPs, BorderLayout.CENTER);

        panelDerechoLog = new JPanel(new BorderLayout());
        panelDerechoLog.setBorder(BorderFactory.createTitledBorder("Registro de Actividad"));
        panelDerechoLog.setPreferredSize(new Dimension(350, 0)); 

        textAreaLog = new JTextArea();
        textAreaLog.setEditable(false);
        textAreaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textAreaLog.setBackground(new Color(240, 240, 240));
        
        JScrollPane scrollLog = new JScrollPane(textAreaLog);
        panelDerechoLog.add(scrollLog, BorderLayout.CENTER);
        
        this.add(panelDerechoLog, BorderLayout.EAST);

        panelInferiorControl = new JPanel(new BorderLayout());
        panelInferiorControl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelInferiorControl.setPreferredSize(new Dimension(0, 80));

        JPanel botonesControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        
        JButton btnScript = new JButton("▶ Ejecutar Script de Suministro");
        btnScript.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnScript.setBackground(new Color(200, 230, 255));
        
        JButton btnReload = new JButton("Actualizar Lista CPs (DB)");
        btnReload.setFont(new Font("SansSerif", Font.BOLD, 12));

        botonesControl.add(btnScript);
        botonesControl.add(btnReload);

        labelCentralStatus = new JLabel("Central: DESCONECTADA");
        labelCentralStatus.setFont(new Font("SansSerif", Font.BOLD, 16));
        labelCentralStatus.setForeground(Color.RED);
        labelCentralStatus.setHorizontalAlignment(JLabel.RIGHT);

        panelInferiorControl.add(botonesControl, BorderLayout.WEST);
        panelInferiorControl.add(labelCentralStatus, BorderLayout.EAST);

        this.add(panelInferiorControl, BorderLayout.SOUTH);

        btnScript.addActionListener(e -> ejecutarScript());
        
        btnReload.addActionListener(e -> {
            Log("Solicitando actualización de CPs a la base de datos...");
            CreateButtons(); 
        });
        setVisible(true);
    }


    public void updateCentralStatus(boolean status) {
        Central_Status = status;
        SwingUtilities.invokeLater(() -> {
            if (status) {
                labelCentralStatus.setText("Central: CONECTADA");
                labelCentralStatus.setForeground(new Color(0, 180, 0));
            } else {
                labelCentralStatus.setText("Central: DESCONECTADA");
                labelCentralStatus.setForeground(Color.RED);
            }
        });
    }

    private void ejecutarScript() {
        if (!Central_Status) {
            Log("ERROR: No se puede ejecutar script. Central desconectada.");
            return;
        }
        
        new Thread(() -> {
            try {
                File archivo = new File("request_script.txt");
                if(!archivo.exists()) {
                    SwingUtilities.invokeLater(() -> Log("ERROR: No se encuentra request_script.txt"));
                    return;
                }
                
                Scanner s = new Scanner(archivo);
                SwingUtilities.invokeLater(() -> Log("Iniciando script de suministro..."));
                

                while (s.hasNextLine()) {
                    if (Suministrando) {
                        Thread.sleep(1000); 
                        continue;
                    }
                    
                    String linea = s.nextLine();
                    String[] partes = linea.split("#");
                    if (partes.length < 3) continue;

                    String targetId = partes[1];
                    String kwh = partes[2];

                    for (CP cp : cps) {
                    	System.out.println(cp.UID);
                        if (cp.UID.equals(targetId)) {
                            Suministrando = true;
                            String request = "REQUEST#" + cp.UID + ";" + cp.Price + ";" + cp.Location + "#" + kwh + "#" + name + "#" + cp.Price;
                            
                            producer.sendMessage(request);
                            SwingUtilities.invokeLater(() -> Log("Script: Solicitando carga en " + cp.UID + " (" + kwh + " kWh)"));
                            break; 
                        }
                    }
                    Thread.sleep(4000);
                }
                s.close();
                SwingUtilities.invokeLater(() -> Log("Fin del script de suministro."));
                
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> Log("Error en script: " + ex.getMessage()));
            }
        }).start();
    }

    public void CreateButtons() {
        containerCPs.removeAll();
        
        new Thread(() -> {
            try {
                String DB_NAME = "evcharging_db";
                String DB_USER = "evcharging";
                String DB_PASS = "practica2";
                System.out.println(ip);
                DatabaseManager db = new DatabaseManager(ip, DB_NAME, DB_USER, DB_PASS);
                
                List<String> cpsString = db.GetAllCPS();
                
                SwingUtilities.invokeLater(() -> {
                    for (String s : cpsString) {
                        // Formato s: ID|Nombre|Ubicacion|Precio|Estado
                        String[] parts = s.split("\\|");
                        if(parts.length < 2) continue;
                        
                        String cpId = parts[1]; 
                        
                        JButton btn = new JButton("<html><center><b>" + cpId + "</b><br>Iniciar Carga</center></html>");
                        btn.setPreferredSize(new Dimension(120, 60));
                        btn.setBackground(new Color(230, 230, 230));
                        CP cp = new CP(cpId, parts[3], parts[2], parts[4]);
                        cps.add(cp);
                        CreateButtonAction(btn, s);
                        containerCPs.add(btn);
                    }
                    containerCPs.revalidate();
                    containerCPs.repaint();
                    Log("Lista de CPs actualizada desde BD (" + cpsString.size() + " encontrados).");
                });
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> Log("Error conectando a BD: " + e.getMessage()));
            }
        }).start();
    }
    
    private void CreateButtonAction(JButton button, String lineaCP) {
        button.addActionListener(e -> {
            if (!Central_Status) {
                Log("Error: Central desconectada.");
                return;
            }
            if (Suministrando) {
                Log("Aviso: Ya hay un suministro en curso.");
                return;
            }
            
            String[] parts = lineaCP.split("\\|"); 
            String cpInfo = parts[1] + ";" + parts[3] + ";" + parts[2];
            
            String mensajeKafka = "REQUEST#" + cpInfo + "#10.0#" + name;
            producer.sendMessage(mensajeKafka);
            
            Log("Manual: Solicitando carga en " + parts[1]);
            Suministrando = true;
        });
    }

    private ArrayList<String> activeLogs = new ArrayList<>();

    public void Log(String message) {
        if (message.contains("CP:")) {
            updateChargingLine(message);
        } else {
            SwingUtilities.invokeLater(() -> {
                activeLogs.add(message);
                refreshLogArea();
            });
        }
    }

    private void updateChargingLine(String newMessage) {
        SwingUtilities.invokeLater(() -> {
            String cpIdIdent = "";
            try {
                int start = newMessage.indexOf("CP:");
                int end = newMessage.indexOf("->");
                if (start != -1 && end != -1) {
                    cpIdIdent = newMessage.substring(start, end).trim(); // Ej: "CP: SEV1"
                }
            } catch (Exception e) { }

            boolean replaced = false;

            if (!cpIdIdent.isEmpty()) {
                for (int i = 0; i < activeLogs.size(); i++) {
                    if (activeLogs.get(i).contains(cpIdIdent)) {
                        activeLogs.set(i, newMessage); 
                        replaced = true;
                        break;
                    }
                }
            }

            if (!replaced) {
                activeLogs.add(newMessage);
            }
            refreshLogArea();
        });
    }

    private void refreshLogArea() {
        StringBuilder sb = new StringBuilder();
        for (String line : activeLogs) {
            sb.append(line).append("\n");
        }
        textAreaLog.setText(sb.toString());
    }
}