package es.ua.sd.practica;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

class MainData {
    public double temp; 
}

class WeatherResponse {
    public MainData main;
}

public class EV_W {

	private static final String API_CENTRAL = "http://localhost:8081/api/status/all"; 
	private static final String API_KEY = "ae711f5c636ee01a6245e5dd94a6c66d";
	private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather"; 
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
		
		for(CP cp : cps)
		{
			if(cp.Location.length() != 0)
			{
				System.out.println(cp.Location + "->" + GetTemp(cp.Location));
			
			}
				
		}
	}

	private static void ParseJSON(String jsonResponse) {
		Gson gson = new Gson();
        Type listType = new TypeToken<List<CP>>() {}.getType();
        cps = gson.fromJson(jsonResponse, listType);
	}

}
