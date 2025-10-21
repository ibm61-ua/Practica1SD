package es.ua.sd.practica;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class Producer {
    private final KafkaProducer<String, String> producer;
    private final String topicName;
    private final String broker_ip;

    public Producer(String brokerList, String topicName) {
        this.topicName = topicName;
        this.broker_ip = brokerList;
        
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker_ip);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        this.producer = new KafkaProducer<>(props);
    }

	public void sendMessage(String message) {
        ProducerRecord<String, String> record = 
            new ProducerRecord<>(topicName, message);

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                
            } else {
                exception.printStackTrace();
            }
        });
        
    }

    public void close() {
        if (producer != null) {
            producer.flush();
            producer.close();
        }
    } 
}