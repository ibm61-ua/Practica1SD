package es.ua.sd.practica;
import java.util.ArrayList;
import java.util.Map;
import java.time.Instant;
import java.time.Duration;

public class HeartbeatChecker extends EV_Central implements Runnable {

    private final Map<String, Instant> heartbeatMap; 
    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(5); 
    
    public HeartbeatChecker(Map<String, Instant> map) {
        this.heartbeatMap = map;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            
            Instant now = Instant.now();
            
            for (Map.Entry<String, Instant> entry : heartbeatMap.entrySet()) {
                String cpId = entry.getKey();
                Instant lastSeen = entry.getValue();
                
                Duration timeSinceLastContact = Duration.between(lastSeen, now);
                
                if (timeSinceLastContact.compareTo(TIMEOUT_DURATION) > 0) {
                	for(CP cp : cps)
                	{
                		if(cp.UID.equals(cpId) && !cp.State.equals("DESCONECTADO"))
                		{
                			cp.State = "DESCONECTADO";
                			javax.swing.SwingUtilities.invokeLater(() -> refreshChargingPoints(gui));
                		}
                	}
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
