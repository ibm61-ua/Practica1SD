package es.ua.sd.practica;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

public class EV_CP_M {
	public static String IP_Engine;
	public static int Port_Engine;
	public static String IP_Central;
	public static int Port_Central;
	public static String ID_CP;

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Introduce IP y puerto del EV_CP_E" + ", IP y puerto del EV_Central" + ", ID del CP");
			return;
		}

		DeserializeARGS(args);

		SwingUtilities.invokeLater(() -> {
			MonitorGUI gui = new MonitorGUI(ID_CP);
			gui.Message("Tu puta madre");
		});

		Producer p = new Producer(IP_Central + ":" + Port_Central, CommonConstants.TELEMETRY);
		p.sendMessage("hola");
	}

	private static void DeserializeARGS(String[] args) {
		String[] splitter;

		splitter = args[0].split(":");
		IP_Engine = splitter[0];
		Port_Engine = Integer.parseInt(splitter[1]);

		splitter = args[1].split(":");
		IP_Central = splitter[0];
		Port_Central = Integer.parseInt(splitter[1]);

		ID_CP = args[2];
	}

}
