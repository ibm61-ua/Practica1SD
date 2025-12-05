package es.ua.sd.practica;

import java.util.ArrayList;
import com.google.gson.Gson;

public class RegistroDeAuditoria {
	private static final Gson GSON = new Gson();
	public static ArrayList<String> log = new ArrayList<>();
	public static void NewLog(String message) {
		log.add(message);
	}
	
	public static String LogToJson()
	{
		return GSON.toJson(log);
	}
	
}
