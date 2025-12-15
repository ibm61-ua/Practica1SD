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

import javax.crypto.SecretKey;

public class CentralLogic extends EV_Central {
	private final Map<String, Instant> lastHeartbeat = new ConcurrentHashMap<>();
	public void handleRequest(String message) {
		System.out.println("[DRIVER] " + message);
		String kwh = null;
		for(CP cp : cps)
		{
			if(cp.UID.equals(message.split("#")[1].split(";")[0]))
			{
				if(cp.State.equals("DESCONECTADO") || cp.State.equals("AVERIADO") || cp.State.equals("PARADO") || cp.State.equals("CARGANDO") )
				{
					Producer r = new Producer(super.brokerIP, CommonConstants.CONTROL);
					r.sendMessage("NOAVIABLE#" + message.split("#")[3]);
					r.close();
					return;
				}
				kwh = cp.Price;
				
				cp.driver = message.split("#")[3];
				cp.KWHRequested = Float.parseFloat(message.split("#")[2]);
				gui.refreshChargingPoints();
			}
		}
		LocalDateTime ahora = LocalDateTime.now();
		DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		String fechaHora = ahora.format(formato);
		String m = fechaHora + "   "  + message.split("#")[1].split(";")[0] + "   "  + message.split("#")[2] + "   "  + message.split("#")[3];
        gui.addOngoingMessage(m);
        
        Producer r = new Producer(super.brokerIP, CommonConstants.REQUEST_CP);
        
        String cpid = message.split("#")[1].split(";")[0];
        SecretKey key = CPKeys.get(cpid);
        String encryptedMessage = "";
		try {
			encryptedMessage = CryptoUtils.cifrar(message + "#" + kwh, key);
		} catch (Exception e) {
			e.printStackTrace();
		}
		r.sendMessage(cpid + "$" + encryptedMessage);
		r.close();
    }


    public void handleTelemetry(String message) {
    	System.out.println("[ENGINE] Mensaje encriptado: " + message);
    	
    	String cpid = message.split("\\$")[0];
		String encryptedMessage = message.split("\\$")[1];
		SecretKey key = CPKeys.get(cpid);
		String decryptedMessage = "";
		try {
			decryptedMessage = CryptoUtils.descifrar(encryptedMessage, key);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("[ENGINE] Mensaje desencriptado: " + decryptedMessage);
		
		if (decryptedMessage.contains("CHARGING"))
		{
	        String cpId = decryptedMessage.split("#")[1];
	        for (CP cp : cps) {
                if (cp.UID.equals(cpId)) {
                    
                    cp.State = "CARGANDO"; 
                    if (gui != null) {
                        gui.removeOngoingMessage(cpId);

                        refreshChargingPoints(gui);
                    }
                    
                    break; 
                }
            }
	        return;
		}
		
		if (decryptedMessage.contains("REJECT"))
		{
			Producer r = new Producer(super.brokerIP, CommonConstants.CONTROL);
	        r.sendMessage(decryptedMessage);
	        r.close();
	        String cpId = decryptedMessage.split("#")[1];
	        for (CP cp : cps) {
                if (cp.UID.equals(cpId)) {
                    
                    cp.State = "CONECTADO"; 
                    if (gui != null) {
                        gui.removeOngoingMessage(cpId);

                        refreshChargingPoints(gui);

                        RegistroDeAuditoria.NewLog(cpId, "Recarga", cpId + " rechazó la recarga de " + decryptedMessage.split("#")[2]);
                        gui.updateAuditLog();
                    }
                    
                    break; 
                }
            }
	        return;
		}
		
        if (decryptedMessage.contains("END")) {
            
            String cpId = decryptedMessage.split("#")[1]; // Extraemos el ID (ej. SEV10)

            for (CP cp : cps) {
                if (cp.UID.equals(cpId)) {
                    
                    cp.State = "CONECTADO"; 
                    
                    float costeFinal = Float.parseFloat(cp.Price) * cp.KWHRequested;
                    decryptedMessage += "#Precio: " + costeFinal + " euros.";

                    if (gui != null) {
                        gui.removeOngoingMessage(cpId);

                        refreshChargingPoints(gui);

                        RegistroDeAuditoria.NewLog(cpId, "Recarga" , "FIN DE CARGA: " + cpId + " | Coste: " + costeFinal + "€");
                        gui.updateAuditLog();
                    }
                    
                    break; 
                }
            }
        }

        Producer r = new Producer(super.brokerIP, CommonConstants.CONTROL);
        r.sendMessage(decryptedMessage);
        r.close();
    }
    
    public void handleCP(String message)
    {
    	String type = message.split("#")[0]; 
    	String cpUID = message.split("#")[1];
    	
    	
    	this.getLastHeartbeat().put(cpUID, Instant.now());
    	
    	if(type.equals("CONNECTION"))
    	{
    		for(CP cp : cps)
        	{
        		if(cp.UID.equals(cpUID) && !cp.State.equals("CONECTADO") && cp.autenticado)
        		{
        			cp.State = "CONECTADO";
        			RegistroDeAuditoria.NewLog(cpUID, "Charging Point" , cpUID + " esta ahora conectado.");
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
        			RegistroDeAuditoria.NewLog(cpUID, "Charging Point" , cpUID + " esta ahora conectado.");
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
        			RegistroDeAuditoria.NewLog(cpUID, "Charging Point" , cpUID + " esta averiado.");
        			javax.swing.SwingUtilities.invokeLater(() -> refreshChargingPoints(gui));
        		}
        	}
    	}
    	
    	refreshLog();
    	
    }

	public Map<String, Instant> getLastHeartbeat() {
		return lastHeartbeat;
	}
	
}
