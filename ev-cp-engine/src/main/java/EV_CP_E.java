
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import java.util.UUID;

import es.ua.sd.practica.CommonConstants;
import es.ua.sd.practica.CryptoUtils;
import es.ua.sd.practica.EVServerSocket;
import es.ua.sd.practica.KConsumer;
import es.ua.sd.practica.Producer;

public class EV_CP_E extends JFrame {

    private JButton btnEngineStatus; // Botón KO / Revivir
    private JPanel panelRequest;     // Panel que aparece al recibir petición
    private JLabel labelRequestInfo; // Info del conductor
    private JButton btnAccept;
    private JButton btnReject;
    private JTextArea textAreaLog;   // Consola de texto

    public static String broker;
    public static int port;
    public static String CP_ID = "Desconocido";
    public static SecretKey KEY;

    private EVServerSocket EVServer;
    private Thread socketThread;
    private Thread consumerThread;
    private Runnable consumerRequest;
    private boolean isEngineAlive = false;

    private String pendingDriverID = null;
    private String pendingRequestMessage = null;

    public EV_CP_E(String broker, int port) {
        super("Engine CP - Control Panel");
        EV_CP_E.broker = broker;
        EV_CP_E.port = port;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout(10, 10));

        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTop.setBorder(BorderFactory.createTitledBorder("Estado del Sistema"));

        btnEngineStatus = new JButton("INICIAR ENGINE");
        btnEngineStatus.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnEngineStatus.setBackground(new Color(220, 220, 220)); // Gris
        btnEngineStatus.addActionListener(e -> toggleEngineState());
        
        panelTop.add(btnEngineStatus);
        add(panelTop, BorderLayout.NORTH);

        textAreaLog = new JTextArea();
        textAreaLog.setEditable(false);
        textAreaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textAreaLog.setBackground(new Color(245, 245, 245));
        JScrollPane scrollLog = new JScrollPane(textAreaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Registro de Actividad (Log)"));
        add(scrollLog, BorderLayout.CENTER);

        panelRequest = new JPanel(new BorderLayout(10, 0));
        panelRequest.setBorder(BorderFactory.createTitledBorder(null, "Petición de Suministro Entrante", 
                                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, 
                                new Font("SansSerif", Font.BOLD, 14), Color.BLUE));
        panelRequest.setBackground(new Color(230, 240, 255));
        panelRequest.setVisible(false);

        labelRequestInfo = new JLabel("Esperando peticiones...");
        labelRequestInfo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        labelRequestInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel panelBotonesRequest = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnAccept = new JButton("✅ Aceptar Carga");
        btnAccept.setBackground(new Color(144, 238, 144));
        btnReject = new JButton("❌ Rechazar");
        btnReject.setBackground(new Color(255, 99, 71));

        btnAccept.addActionListener(e -> processRequestResponse(true));
        btnReject.addActionListener(e -> processRequestResponse(false));

        panelBotonesRequest.add(btnAccept);
        panelBotonesRequest.add(btnReject);
        panelBotonesRequest.setOpaque(false);

        panelRequest.add(labelRequestInfo, BorderLayout.CENTER);
        panelRequest.add(panelBotonesRequest, BorderLayout.SOUTH);
        add(panelRequest, BorderLayout.SOUTH);

        // INICIO AUTOMÁTICO DEL ENGINE
        log("Iniciando sistema...");
        toggleEngineState(); 

        setVisible(true);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            textAreaLog.append("> " + message + "\n");
            textAreaLog.setCaretPosition(textAreaLog.getDocument().getLength());
        });
    }

    private void toggleEngineState() {
        if (isEngineAlive) {
            log("Deteniendo Engine (KO)...");
            if (EVServer != null) EVServer.stop();
            if (socketThread != null) socketThread.interrupt();
            if (consumerThread != null) consumerThread.interrupt();
            
            isEngineAlive = false;
            btnEngineStatus.setText("REVIVIR ENGINE");
            btnEngineStatus.setBackground(new Color(255, 99, 71)); // Rojo
        } else {
            log("Iniciando Engine (VIVO)...");
            
            EVServer = new EVServerSocket(port, this::handleMonitor);
            socketThread = new Thread(EVServer);
            socketThread.start();

            consumerRequest = new KConsumer(broker, CommonConstants.REQUEST_CP, UUID.randomUUID().toString(), t -> {
                try {
                    handleDriverRequest(t);
                } catch (Exception e) {
                    log("Error en consumer: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            consumerThread = new Thread(consumerRequest);
            consumerThread.start();

            isEngineAlive = true;
            btnEngineStatus.setText("ENGINE OPERATIVO (KO)");
            btnEngineStatus.setBackground(new Color(144, 238, 144)); // Verde
        }
    }

    private void showRequestPrompt(String driverID, String kwh, String messageOriginal) {
        this.pendingDriverID = driverID;
        this.pendingRequestMessage = messageOriginal;

        SwingUtilities.invokeLater(() -> {
            labelRequestInfo.setText("<html>Conductor <b>" + driverID + "</b> solicita <b>" + kwh + " kWh</b>.<br>¿Aceptar suministro?</html>");
            panelRequest.setVisible(true);
            this.revalidate();
        });
        log("Petición recibida de " + driverID + " (" + kwh + " kWh). Esperando confirmación manual...");
    }

    private void processRequestResponse(boolean accepted) {
        panelRequest.setVisible(false); // Ocultar panel
        this.revalidate();

        if (pendingRequestMessage == null) return;

        final String message = pendingRequestMessage;
        final String driverID = pendingDriverID;
        pendingRequestMessage = null;

        new Thread(() -> { 
            try {
                Producer r = new Producer(broker, CommonConstants.TELEMETRY);
                
                if (!accepted) {
                    log("Rechazando petición de " + driverID);
                    String cryptoMessage = CryptoUtils.cifrar("REJECT#" + CP_ID + "#" + driverID, KEY);
                    r.sendMessage(CP_ID + "$" + cryptoMessage);
                } else {
                    // ACEPTAR
                    log("Aceptando petición de " + driverID + ". Iniciando carga...");
                    String cryptoMessage = CryptoUtils.cifrar("ACKREQUEST#" + CP_ID + "#" + driverID, KEY);
                    r.sendMessage(CP_ID + "$" + cryptoMessage);
                    
                    float kwrequested = Float.parseFloat(message.split("#")[2]);
                    float kws = Float.parseFloat(message.split("#")[4]) * 2.0f; 
                    float kwpassed = 0;

                    while(true) {
                        if(kwpassed + kws > kwrequested) {
                            kwpassed = kwrequested;
                            sendChargingUpdate(r, driverID, kwpassed, kwrequested, true); // Fin
                            break;
                        }
                        kwpassed += kws;
                        sendChargingUpdate(r, driverID, kwpassed, kwrequested, false); // Progreso
                        try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { break; } 
                    }
                    log("Carga finalizada para " + driverID);
                }
                r.close();
            } catch (Exception e) {
                log("Error procesando carga: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void sendChargingUpdate(Producer r, String driverID, float kwpassed, float kwrequested, boolean isEnd) throws Exception {
        String cryptoMessage;
        if (isEnd) {
            cryptoMessage = CryptoUtils.cifrar("END#" + CP_ID + "#" + driverID, KEY);
        } else {
            cryptoMessage = CryptoUtils.cifrar("CHARGING#" + CP_ID + "#" + driverID + "#" + kwpassed + "#" + kwrequested, KEY);
        }
        r.sendMessage(CP_ID + "$" + cryptoMessage);
        if(!isEnd) log("⚡ Cargando: " + String.format("%.2f", kwpassed) + " / " + kwrequested + " kWh");
    }


    void handleMonitor(String message) {
        log("[MONITOR SOCKET] Recibido: " + message);
        if(message.contains("CONNECTION")) {
            CP_ID = message.split("#")[1];
            log("ID del CP configurado: " + CP_ID);
            SwingUtilities.invokeLater(() -> setTitle("Engine CP - " + CP_ID));
        }
        if(message.contains("KEY")) {
            KEY = CryptoUtils.stringToKey(message.split("#")[1]);
            log("Clave de seguridad recibida y configurada.");
        }
    }

    void handleDriverRequest(String message) throws Exception {
        log("[KAFKA] " + message);
        try {
            String[] parts = message.split("\\$");
            if (parts.length < 2) {
                log("Mensaje Kafka con formato incorrecto, se ignora.");
                return;
            }
            String targetCP = parts[0];
            String encryptedMsg = parts[1];

            if(!CP_ID.equals(targetCP)) {
                return;
            }

            if (KEY == null) {
                log("ERROR: Intento de descifrar sin clave. ¿CP autenticado?");
                return;
            }
            String decrypted = CryptoUtils.descifrar(encryptedMsg, KEY);
            log("[DESCIFRADO] " + decrypted);

            String[] msgParts = decrypted.split("#");
            if (msgParts.length < 5) return;

            String driverID = msgParts[3];
            String kwhRequested = msgParts[2];

            showRequestPrompt(driverID, kwhRequested, decrypted);

        } catch (Exception e) {
            log("Error manejando petición: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java EV_CP_E_GUI <IP_BROKER> <PUERTO_SOCKET>");
            return;
        }
        String brokerIp = args[0];
        int portNum = Integer.parseInt(args[1]);
        
        SwingUtilities.invokeLater(() -> new EV_CP_E(brokerIp, portNum));
    }
}