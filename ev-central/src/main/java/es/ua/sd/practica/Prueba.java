package es.ua.sd.practica;

public class Prueba {

	public static void main(String[] args) {
		String brokerIP = args[0]; 
        String topic = "mensaje"; 
		Producer producer = new Producer(brokerIP, topic);
		producer.sendMessage("mensaje");
		producer.close();
	}

}
