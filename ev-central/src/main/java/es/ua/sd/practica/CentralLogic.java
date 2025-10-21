package es.ua.sd.practica;

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
		String kwh = null;
		for(CP cp : cps)
		{
			if(cp.UID.equals(message.split("#")[1].split(";")[0]))
			{
				if(cp.State.equals("DESCONECTADO") || cp.State.equals("AVERIADO") || cp.State.equals("PARADO"))
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
		String m = fechaHora + "   "  + message.split("#")[1].split(";")[0] + "   "  + message.split("#")[2] + "   "  + message.split("#")[3];
        gui.OnGoingPanel(m);
        
        Producer r = new Producer(super.brokerIP, CommonConstants.REQUEST_CP);
		r.sendMessage(message + "#" + kwh);
		r.close();
    }

    public void handleTelemetry(String message) {
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
    			}
    		}
    		gui.deleteOnGoinMessage(message.split("#")[1]);
    	}
        Producer r = new Producer(super.brokerIP, CommonConstants.CONTROL);
		r.sendMessage(message);
		r.close();
    }
    
    public void handleCP(String message)
    {
    	String type = message.split("#")[0]; //ejemplo mensaje CONNECTION#CP001 -> buscamos el CONNECTION
    	String cpUID = message.split("#")[1]; //ejemplo mensaje CONNECTION#CP001 -> buscamos el CP001
    	
    	
    	this.getLastHeartbeat().put(cpUID, Instant.now());
    	
    	if(type.equals("CONNECTION"))
    	{
    		for(CP cp : super.cps)
        	{
        		if(cp.UID.equals(cpUID) && !cp.State.equals("CONECTADO"))
        		{
        			cp.State = "CONECTADO";
        			super.refreshChargingPoints(super.gui);
        		}
        	}
    	}
    	else if(type.equals("KEEPALIVE"))
    	{
    		for(CP cp : super.cps)
        	{
        		if(cp.UID.equals(cpUID) && cp.State.equals("DESCONECTADO"))
        		{
        			cp.State = "CONECTADO";
        			super.refreshChargingPoints(super.gui);
        		}
        	}
    	}
    	else if(type.equals("ERROR"))
    	{
    		for(CP cp : super.cps)
        	{
        		if(cp.UID.equals(cpUID))
        		{
        			cp.State = "AVERIADO";
        			super.refreshChargingPoints(super.gui);
        		}
        	}
    	}
    	
    }

	public Map<String, Instant> getLastHeartbeat() {
		return lastHeartbeat;
	}
    

}
