package es.ua.sd.practica;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime; 
import java.time.format.DateTimeFormatter; 
import com.google.gson.Gson;


public class RegistroDeAuditoria {
	private static final Gson GSON = new Gson();
	public static ArrayList<String> log = new ArrayList<>();
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final int MAX_LOGS_WEB = 200;
    public static void NewLog(String IP, String event, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        
        String logMessage = "[" + timestamp + "] [" + IP + "] [" + event +  "] "+ message;
        
        log.add(logMessage);
    }
    
    public static String LogToJson() {
        int total = log.size();
        List<String> logsRecientes;

        if (total > MAX_LOGS_WEB) {
            logsRecientes = log.subList(total - MAX_LOGS_WEB, total);
        } else {
            logsRecientes = log;
        }

        return GSON.toJson(logsRecientes);
    }
	
}
