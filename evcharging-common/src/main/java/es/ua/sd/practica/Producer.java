package es.ua.sd.practica;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class Producer {
    private final KafkaProducer<String, String> producer;
    private final String topicName;
    private final String broker_ip;

    /**
     * Constructor: Inicializa la conexión con el broker de Kafka.
     * @param topicName El nombre del topic al que se enviarán mensajes.
     */
    public Producer(String brokerList, String topicName) {
        this.topicName = topicName;
        this.broker_ip = brokerList;
        
        // 1. Configuración de propiedades
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker_ip);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 2. Crear el Productor (manteniéndolo abierto para uso futuro)
        this.producer = new KafkaProducer<>(props);
        System.out.println("Productor inicializado. Conectado a: " + broker_ip);
    }

    /**
     * Método público para enviar un mensaje a demanda.
     * @param key La clave del mensaje (puede ser null).
     * @param message El contenido del mensaje (valor).
     */
	public void sendMessage(String message) {
        ProducerRecord<String, String> record = 
            new ProducerRecord<>(topicName, message);

        // 3. Enviar el mensaje de forma asíncrona
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                
            } else {
                System.err.println("❌ Error al enviar mensaje: " + exception.getMessage());
                exception.printStackTrace();
            }
        });
        
    }

    public void close() {
        if (producer != null) {
            producer.flush(); // Asegura que todos los mensajes pendientes se envíen
            producer.close();
            System.out.println("Productor cerrado.");
        }
    } 
}