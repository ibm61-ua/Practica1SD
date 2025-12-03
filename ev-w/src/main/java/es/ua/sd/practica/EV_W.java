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

class MainData {
    public double temp; 
}

class WeatherResponse {
    public MainData main;
}

public class EV_W {

	private static final String API_CENTRAL = "http://localhost:3000/api/status/all"; 
	private static final String API_KEY = "ae711f5c636ee01a6245e5dd94a6c66d";
	private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather"; 
	private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
	private static List<CP> cps;
	
	public static void main(String[] args) throws IOException {
		GetCentralAPI();
	}

	private static double GetTemp(String location) throws IOException {   
		Gson gson = new Gson();
	    String urlCompleta = BASE_URL + "?q=" + location + "&units=metric" + "&appid=" + API_KEY;
	    
		OkHttpClient client = new OkHttpClient();
		
        Request request = new Request.Builder().url(urlCompleta).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Código inesperado " + response);
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
                throw new IOException("Código inesperado " + response);
            }

            String jsonResponse = response.body().string();
            
            ParseJSON(jsonResponse);
            CheckWeather();
            
        }
    }

    private static void CheckWeather() throws IOException {
        for(CP cp : cps) {
            // Verificamos que tenga ubicación
            if(cp.Location != null && !cp.Location.isEmpty()) {
                
                double temp = GetTemp(cp.Location);
                System.out.println("CP: " + cp.UID + " | Loc: " + cp.Location + " | Temp: " + temp + "ºC");

                // CONDICIÓN: Si es menor a 0 grados
                if(temp < 0) {
                    System.out.println("❄️ ALERTA: Temperatura bajo cero detectada. Avisando a Central...");
                    enviarAlerta(cp.UID, cp.Location, temp);
                }
            }
        }
    }

    // --- NUEVO MÉTODO PARA ENVIAR EL POST ---
    private static void enviarAlerta(String cpId, String location, double temp) {
        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();

        // 1. Crear el objeto JSON de la alerta
        // Puedes crear una clase 'Alerta' o usar un Map simple
        Map<String, Object> alertaData = Map.of(
            "type", "WEATHER_ALERT",
            "cp_id", cpId,
            "message", "Temperatura crítica detectada: " + temp + "ºC en " + location,
            "severity", "HIGH"
        );

        String jsonBody = gson.toJson(alertaData);

        // 2. Crear el cuerpo de la petición
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA);

        // 3. Construir la petición POST
        Request request = new Request.Builder()
                .url(API_CENTRAL)
                .post(body) // POST para enviar datos
                .build();

        // 4. Ejecutar (usamos try-catch para no detener el bucle si falla)
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("✅ Alerta enviada correctamente a la Central.");
            } else {
                System.err.println("❌ Error al enviar alerta: " + response.code());
            }
        } catch (IOException e) {
            System.err.println("❌ Fallo de conexión enviando alerta: " + e.getMessage());
        }
    }

	private static void ParseJSON(String jsonResponse) {
		Gson gson = new Gson();
        Type listType = new TypeToken<List<CP>>() {}.getType();
        cps = gson.fromJson(jsonResponse, listType);
	}

}
