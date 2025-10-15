package es.ua.sd.practica;

import javax.swing.SwingUtilities;

public class EV_Driver {
	public static String IP_BROKER;
	public static int Port_BROKER;
	
	public static String ID_Driver;
	
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Introduce IP y puerto del EV_CP_E" + ", ID del Driver");
			return;
		}
		
		DeserializeARGS(args);
		SwingUtilities.invokeLater(() -> {
            DriverGUI gui = new DriverGUI(ID_Driver);
            gui.CreateButtons("cpdatabase.txt");
        });
		
		Runnable consumerControl = new KConsumer(IP_BROKER + ":" + Port_BROKER, CommonConstants.CONTROL, "evdriver", EV_Driver::handleControl);
		new Thread(consumerControl).start();
		
		Producer p = new Producer(IP_BROKER + ":" + Port_BROKER, CommonConstants.REQUEST);
		p.sendMessage("hola quiero luz");
	}
	
	private static void DeserializeARGS(String[] args) {
		String[] splitter;

		splitter = args[0].split(":");
		IP_BROKER = splitter[0];
		Port_BROKER = Integer.parseInt(splitter[1]);

		ID_Driver = args[1];
	}
	
	public static void handleControl(String message) {
        System.out.println("Processing CONTROL: " + message);
        // ➡️ Logic: Handle final tickets or shutdown commands.
    }

}
