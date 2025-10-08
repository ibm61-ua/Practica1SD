package es.ua.sd.practica;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class Consumer {
    private final String brokerList;
    private final String topicName;
    private final String groupId;

    public Consumer(String brokerList, String topicName, String groupId) {
        this.brokerList = brokerList;
        this.topicName = topicName;
        this.groupId = groupId;
    }

    public void run() {
        // 1. Configuración de propiedades
        Properties props = new Properties();
        // Dirección del broker
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        // ID del grupo de consumidores (esencial para la coordinación)
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        // Clase para deserializar la clave
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // Clase para deserializar el valor
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // Opcional: Empezar a leer desde el principio si no hay offset previo
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); 
        
        // 2. Crear el Consumidor
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            
            // 3. Suscribirse al topic
            consumer.subscribe(Collections.singletonList(topicName));
            
            System.out.println("Consumidor escuchando en el topic: " + topicName);

            // 4. Bucle principal de lectura
            while (true) {
                ConsumerRecords<String, String> records = 
                    consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(record.value());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}