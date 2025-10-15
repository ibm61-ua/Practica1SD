package es.ua.sd.practica;

public class CentralLogic extends EV_Central {
	public void handleRequest(String message) {
        System.out.println("Processing REQUEST: " + message);
        Producer p = new Producer(super.brokerIP, CommonConstants.CONTROL);
		p.sendMessage("aqui esta tu luz marica");
    }

    public void handleTelemetry(String message) {
        System.out.println("Processing TELEMETRY: " + message);
        // ➡️ Logic: Update the Central GUI (e.g., charge level, consumption).
    }
    

}
