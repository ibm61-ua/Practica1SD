package es.ua.sd.practica;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class MainData {
    public double temp; 
}
class WeatherResponse {
    public MainData main;
}

public class EV_W extends JFrame {

    private static String API_CENTRAL; 
    private static String API_ALERT;
    private static String API_KEY;
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather"; 
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private static List<CP> cps = new ArrayList<>();
    private static final Map<String, Boolean> alertStatus = new HashMap<>();
    private static final Map<String, String> locationOverrides = new ConcurrentHashMap<>();
    
    private JPanel mainListPanel;
    private JLabel statusLabel;
    private Map<String, CPWeatherPanel> panelMap = new HashMap<>();

    public EV_W(String apiCentral, String apiAlert, String apiKey) {
        super("Módulo de Clima - EV_w");
        API_CENTRAL = apiCentral;
        API_ALERT = apiAlert;
        API_KEY = apiKey;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 700);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(50, 50, 50));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel title = new JLabel("Módulo EV_Weather", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        
        statusLabel = new JLabel("Iniciando...", SwingConstants.CENTER);
        statusLabel.setForeground(Color.LIGHT_GRAY);

        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(statusLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        mainListPanel = new JPanel();
        mainListPanel.setLayout(new BoxLayout(mainListPanel, BoxLayout.Y_AXIS)); 
        
        JScrollPane scrollPane = new JScrollPane(mainListPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        startBackgroundWorker();

        setVisible(true);
    }


    private void updateUI() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("CPs conectados: " + cps.size() + " | Simulaciones activas: " + locationOverrides.size());

            List<String> currentIds = new ArrayList<>();
            for (CP cp : cps) currentIds.add(cp.UID);
            
            panelMap.keySet().removeIf(id -> {
                if (!currentIds.contains(id)) {
                    mainListPanel.remove(panelMap.get(id));
                    return true;
                }
                return false;
            });

            for (CP cp : cps) {
                if (panelMap.containsKey(cp.UID)) {
                    panelMap.get(cp.UID).updateData(cp);
                } else {
                    CPWeatherPanel newPanel = new CPWeatherPanel(cp);
                    panelMap.put(cp.UID, newPanel);
                    mainListPanel.add(newPanel);
                }
            }

            mainListPanel.revalidate();
            mainListPanel.repaint();
        });
    }

    class CPWeatherPanel extends JPanel {
        private String cpId;
        private JLabel lblName;
        private JLabel lblTemp;
        private JLabel lblLoc;
        private JTextField txtSimulate;
        private JButton btnSimulate;
        private JButton btnReset;

        public CPWeatherPanel(CP cp) {
            this.cpId = cp.UID;
            setLayout(new BorderLayout(5, 5));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createLineBorder(Color.GRAY, 1)
            ));
            setBackground(Color.WHITE);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

            JPanel infoPanel = new JPanel(new GridLayout(3, 1));
            infoPanel.setOpaque(false);
            lblName = new JLabel("CP: " + cp.UID);
            lblName.setFont(new Font("SansSerif", Font.BOLD, 14));
            lblLoc = new JLabel("" + cp.Location);
            lblTemp = new JLabel("-- °C");
            
            infoPanel.add(lblName);
            infoPanel.add(lblLoc);
            infoPanel.add(lblTemp);
            add(infoPanel, BorderLayout.CENTER);

            JPanel simPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            simPanel.setOpaque(false);
            simPanel.add(new JLabel("Simular Ubicación:"));
            
            txtSimulate = new JTextField(10);
            btnSimulate = new JButton("Aplicar");
            btnReset = new JButton("Reset");
            btnReset.setBackground(new Color(255, 200, 200));

            btnSimulate.addActionListener(e -> {
                String newLoc = txtSimulate.getText().trim();
                if (!newLoc.isEmpty()) {
                    locationOverrides.put(cpId, newLoc);
                    lblLoc.setText("📍 " + newLoc + " (Simulado)");
                    lblLoc.setForeground(Color.BLUE);
                }
            });

            btnReset.addActionListener(e -> {
                locationOverrides.remove(cpId);
                txtSimulate.setText("");
                lblLoc.setForeground(Color.BLACK);
            });

            simPanel.add(txtSimulate);
            simPanel.add(btnSimulate);
            simPanel.add(btnReset);
            add(simPanel, BorderLayout.SOUTH);
        }

        public void updateData(CP cp) {
        }
        
        public void setTemperatureDisplay(double temp, String realLocation) {
            lblTemp.setText(String.format("🌡%.1f °C", temp));
            
            if (temp < 0) setBackground(new Color(200, 230, 255)); 
            else if (temp > 30) setBackground(new Color(255, 220, 200)); 
            else setBackground(Color.WHITE);
            if (!locationOverrides.containsKey(cpId)) {
                lblLoc.setText(realLocation);
                lblLoc.setForeground(Color.BLACK);
            } else {
                lblLoc.setText(locationOverrides.get(cpId) + " (Simulado)");
                lblLoc.setForeground(new Color(0, 100, 200));
            }
        }
    }


    private void startBackgroundWorker() {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    GetCentralAPI(); 
                    updateUI(); 
                    CheckWeatherAndUpdate();

                    Thread.sleep(4000);
                } catch (Exception e) {
                    System.err.println("Error en ciclo: " + e.getMessage());
                    try { Thread.sleep(2000); } catch (Exception ex) {}
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void CheckWeatherAndUpdate() throws IOException {
        for (CP cp : cps) {
            if (cp.Location != null && !cp.Location.isEmpty()) {
                
                String locationQuery = cp.Location;
                String extraInfo = "";

                if (locationOverrides.containsKey(cp.UID)) {
                    locationQuery = locationOverrides.get(cp.UID);
                    extraInfo = " [SIM]";
                } else if (locationOverrides.containsKey(cp.Location)) {
                    locationQuery = locationOverrides.get(cp.Location);
                    extraInfo = " [SIM]";
                }

                double temp = GetTemp(locationQuery);
                
                if (panelMap.containsKey(cp.UID)) {
                    final double t = temp;
                    SwingUtilities.invokeLater(() -> 
                        panelMap.get(cp.UID).setTemperatureDisplay(t, cp.Location)
                    );
                }

                enviarNotificacion(cp.UID, cp.Location, temp, "UPDATE", "Clima: " + temp + "ºC");

                boolean estaEnAlerta = alertStatus.getOrDefault(cp.UID, false);

                if (temp < 0 && !estaEnAlerta) {
                    enviarNotificacion(cp.UID, cp.Location, temp, "HIGH", "ALERTA CLIMÁTICA: " + temp + "ºC" + extraInfo);
                    alertStatus.put(cp.UID, true);
                } else if (temp >= 0 && estaEnAlerta) {
                    enviarNotificacion(cp.UID, cp.Location, temp, "INFO", "RECUPERACIÓN: " + temp + "ºC");
                    alertStatus.put(cp.UID, false);
                }
            }
        }
    }


    private static double GetTemp(String location) throws IOException {   
        Gson gson = new Gson();
        String urlCompleta = BASE_URL + "?q=" + location + "&units=metric" + "&appid=" + API_KEY;
        
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(urlCompleta).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return 20.0;
            String jsonResponse = response.body().string();
            WeatherResponse weatherData = gson.fromJson(jsonResponse, WeatherResponse.class);
            return weatherData.main.temp;
        } catch (Exception e) { return 20.0; }
    }

    public static void GetCentralAPI() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(API_CENTRAL).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String jsonResponse = response.body().string();
                ParseJSON(jsonResponse);       
            }
        }
    }

    private static void enviarNotificacion(String cpId, String location, double temp, String severity, String message) {
        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();

        Map<String, Object> data = Map.of(
            "type", "WEATHER_ALERT",
            "cp_id", cpId,
            "message", message,
            "severity", severity,
            "temp", temp
        );

        RequestBody body = RequestBody.create(gson.toJson(data), JSON_MEDIA);
        Request request = new Request.Builder().url(API_ALERT).post(body).build();

        try (Response response = client.newCall(request).execute()) {
        } catch (IOException e) {
            System.err.println("Error enviando alerta: " + e.getMessage());
        }
    }

    private static void ParseJSON(String jsonResponse) {
        Gson gson = new Gson();
        java.lang.reflect.Type listType = (java.lang.reflect.Type) new TypeToken<List<CP>>() {}.getType();
        List<CP> fetchedCps = gson.fromJson(jsonResponse, listType);
        
        if (fetchedCps != null) {
            cps.clear();
            cps.addAll(fetchedCps);
        }
    }


    public static void main(String[] args) {
        if(args.length < 3) {
            System.out.println("Uso: java EV_W <URL_CENTRAL> <URL_ALERT> <API_KEY>");
            return;
        }
        SwingUtilities.invokeLater(() -> new EV_W(args[0], args[1], args[2]));
    }
}