package es.ua.sd.practica;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;

class MainData {
    public double temp; 
}

class WeatherResponse {
    public MainData main;
}

public class EV_W {

    private static String API_CENTRAL; 
    private static String API_ALERT;
    private static String API_KEY;
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather"; 
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    
    private static List<CP> cps;
    private static final Map<String, Boolean> alertStatus = new HashMap<>();

    // NUEVO: Mapa para guardar las simulaciones (ConcurrentHashMap es thread-safe)
    // Clave: Ubicación Real -> Valor: Ubicación Simulada
    private static final Map<String, String> locationOverrides = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
    	
    	API_CENTRAL = args[0];
    	API_ALERT = args[1];
    	API_KEY = args[2];
        System.out.println("--- MÓDULO DE CLIMA INICIADO ---");
        System.out.println("   SIMULACIÓN: Escribe 'Origen Destino' para cambiar la ubicación.");
        System.out.println("   Ejemplo: 'Alicante Helsinki' (Para forzar frío)");
        System.out.println("   Escribe 'reset' para borrar simulaciones.");

        // 1. INICIAR HILO DE ESCUCHA DE CONSOLA
        startConsoleListener();

        // 2. BUCLE PRINCIPAL (CLIMA)
        while(true) {
            try {
                GetCentralAPI();
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error ciclo principal: " + e.getMessage());
            }
        }
    }

    // --- NUEVO MÉTODO: Hilo que escucha tu teclado ---
    private static void startConsoleListener() {
        Thread inputThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                
                if (line.equalsIgnoreCase("reset")) {
                    locationOverrides.clear();
                    System.out.println(">> Simulaciones borradas. Usando ubicaciones reales.");
                } else {
                    String[] parts = line.split("\\s+"); // Separar por espacios
                    if (parts.length == 2) {
                        String real = parts[0];
                        String simulada = parts[1];
                        locationOverrides.put(real, simulada);
                        System.out.println(">> SIMULACIÓN ACTIVA: Los CPs de '" + real + "' ahora usarán el clima de '" + simulada + "'");
                    } else {
                        System.out.println(">> Comando inválido. Usa: 'SitioReal SitioSimulado'");
                    }
                }
            }
        });
        inputThread.setDaemon(true); // El hilo morirá si el programa principal termina
        inputThread.start();
    }

    private static double GetTemp(String location) throws IOException {   
        Gson gson = new Gson();
        String urlCompleta = BASE_URL + "?q=" + location + "&units=metric" + "&appid=" + API_KEY;
        
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(urlCompleta).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Error obteniendo clima para " + location + ": " + response.code());
                return 20.0; 
            }

            String jsonResponse = response.body().string();
            WeatherResponse weatherData = gson.fromJson(jsonResponse, WeatherResponse.class);
            return weatherData.main.temp;
        }
    }

    public static void GetCentralAPI() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(API_CENTRAL).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Fallo conectando con Central: " + response.code());
            }
            String jsonResponse = response.body().string();
            ParseJSON(jsonResponse); 
            CheckWeather();       
        }
    }

    private static void CheckWeather() throws IOException {
        for(CP cp : cps) {
            if(cp.Location != null && !cp.Location.isEmpty()) {
                
                // --- LÓGICA DE SIMULACIÓN ---
                String ubicacionParaConsultar = cp.Location;
                String extraInfo = "";

                // Verificamos si hay una simulación activa para esta ciudad
                if (locationOverrides.containsKey(cp.Location)) {
                    ubicacionParaConsultar = locationOverrides.get(cp.Location);
                    extraInfo = " [SIMULANDO: " + ubicacionParaConsultar + "]";
                }
                // -----------------------------

                double temp = GetTemp(ubicacionParaConsultar);
                System.out.println("CP: " + cp.UID + " | Loc: " + cp.Location + extraInfo + " | Temp: " + temp + "ºC");

                boolean estaEnAlertaPrevia = alertStatus.getOrDefault(cp.UID, false);

                if(temp < 0) {
                    System.out.println("NUEVA ALERTA: " + cp.UID + " bajo cero.");
                    String msg = "ALERTA CLIMÁTICA: Temp crítica (" + temp + "ºC)" + extraInfo;
                    
                    enviarAlerta(cp.UID, cp.Location, temp, "HIGH", msg);
                    alertStatus.put(cp.UID, true);
                }

                else if (temp >= 0 && estaEnAlertaPrevia) {
                    System.out.println("RECUPERACIÓN: " + cp.UID + " temperatura normalizada.");
                    enviarAlerta(cp.UID, cp.Location, temp, "INFO", "RECUPERACIÓN: Temp normalizada (" + temp + "ºC)");
                    alertStatus.put(cp.UID, false);
                }
            }
        }
    }

    private static void enviarAlerta(String cpId, String location, double temp, String severity, String message) {
        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();

        Map<String, Object> alertaData = Map.of(
            "type", "WEATHER_ALERT",
            "cp_id", cpId,
            "message", message,
            "severity", severity 
        );

        String jsonBody = gson.toJson(alertaData);
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA);

        Request request = new Request.Builder()
                .url(API_ALERT)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Error al enviar notificación: " + response.code());
            }
        } catch (IOException e) {
            System.err.println("Fallo de conexión enviando notificación: " + e.getMessage());
        }
    }

    private static void ParseJSON(String jsonResponse) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<CP>>() {}.getType();
        cps = gson.fromJson(jsonResponse, listType);
    }
}