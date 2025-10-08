package es.ua.sd.practica;

public class Prueba {

	public static void main(String[] args) {
		String brokerIP = "192.168.1.24:9092"; 
        String topic = "mensaje"; 
		Producer producer = new Producer(brokerIP, topic);
		producer.sendMessage("mensaje");
		producer.close();
	}

}
