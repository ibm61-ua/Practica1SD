package es.ua.sd.practica;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CentralLogic extends EV_Central {
	private final Map<String, Instant> lastHeartbeat = new ConcurrentHashMap<>();
	public void handleRequest(String message) {
		//Si el driver pide actualizar los CPS
		// ejemplo: REQUEST#1|ALC|Alicante|0.35|DESCONECTADO#10.0#DV001
		if(message.equals("RELOAD"))
		{
			handleReload();
			return;
		}
		
		String kwh = null;
		for(CP cp : cps)
		{
			if(cp.UID.equals(message.split("#")[1].split("\\|")[1]))
			{
				if(cp.State.equals("DESCONECTADO") || cp.State.equals("AVERIADO") || cp.State.equals("PARADO") || cp.State.equals("CARGANDO") )
				{
					Producer r = new Producer(super.brokerIP, CommonConstants.TELEMETRY);
					r.sendMessage("NOAVIABLE#" + message.split("#")[3]);
					r.close();
					return;
				}
				kwh = cp.Price;
				cp.State = "CARGANDO";
				cp.driver = message.split("#")[3];
				cp.KWHRequested = Float.parseFloat(message.split("#")[2]);
				gui.refreshChargingPoints();
			}
		}
		LocalDateTime ahora = LocalDateTime.now();
		DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		String fechaHora = ahora.format(formato);
		String m = fechaHora + "   "  + message.split("#")[1].split("\\|")[1] + "   "  + message.split("#")[2] + "   "  + message.split("#")[3];
        gui.OnGoingPanel(m);
        
        Producer r = new Producer(super.brokerIP, CommonConstants.REQUEST_CP);
		r.sendMessage(message + "#" + kwh);
		r.close();
    }

    private void handleReload() {
    	String message = "RELOAD#";
    	
    	for (CP cp : cps)
    	{
    		message += cp.toString() + "|";
    	}
    	Producer r = new Producer(super.brokerIP, CommonConstants.CONTROL);
		r.sendMessage(message);
		r.close();
	}

	public void handleTelemetry(String message) {
		String telemetry = message;
    	if(message.contains("END"))
    	{
    		for(CP cp : cps)
    		{
    			if(cp.UID.equals(message.split("#")[1]))
    			{
    				cp.State = "CONECTADO";
    				Iterator<String> it = gui.rBuffer.iterator();
    				String part = message.split("#")[1];

    				while (it.hasNext()) {
    				    String s = it.next();
    				    if (s.contains(part)) {
    				        it.remove();
    				    }
    				}
    				gui.OnGoingPanel("");
    				gui.refreshChargingPoints();
    				telemetry += "#Precio: " + Float.parseFloat(cp.Price) * cp.KWHRequested + " euros.";
    			}
    		}
    		gui.deleteOnGoinMessage(message.split("#")[1]);
    	}
        Producer r = new Producer(super.brokerIP, CommonConstants.CONTROL);
		r.sendMessage(telemetry);
		r.close();
    }
    
    public void handleCP(String message)
    {
    	String type = message.split("#")[0]; //ejemplo mensaje CONNECTION#CP001 -> buscamos el CONNECTION
    	String cpUID = message.split("#")[1]; //ejemplo mensaje CONNECTION#CP001 -> buscamos el CP001
    	
    	
    	this.getLastHeartbeat().put(cpUID, Instant.now());
    	
    	if(type.equals("CONNECTION"))
    	{
    		for(CP cp : cps)
        	{
        		if(cp.UID.equals(cpUID) && !cp.State.equals("CONECTADO") && cp.autenticado)
        		{
        			cp.State = "CONECTADO";
        			javax.swing.SwingUtilities.invokeLater(() -> refreshChargingPoints(gui));
        		}
        	}
    	}
    	else if(type.equals("KEEPALIVE"))
    	{
    		for(CP cp : cps)
        	{
        		if(cp.UID.equals(cpUID) && cp.State.equals("DESCONECTADO") && cp.autenticado)
        		{
        			cp.State = "CONECTADO";
        			javax.swing.SwingUtilities.invokeLater(() -> refreshChargingPoints(gui));
        		}
        	}
    	}
    	else if(type.equals("ERROR"))
    	{
    		for(CP cp : cps)
        	{
        		if(cp.UID.equals(cpUID) && cp.autenticado)
        		{
        			cp.State = "AVERIADO";
        			javax.swing.SwingUtilities.invokeLater(() -> refreshChargingPoints(gui));
        		}
        	}
    	}
    	
    }

	public Map<String, Instant> getLastHeartbeat() {
		return lastHeartbeat;
	}
	
}
