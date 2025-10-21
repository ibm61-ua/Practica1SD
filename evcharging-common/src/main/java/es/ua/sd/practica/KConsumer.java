package es.ua.sd.practica;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.util.function.Consumer; // Import the functional interface

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KConsumer implements Runnable {
	private final String brokerList;
	private final String topicName;
	private final String groupId;
	
	private final Consumer<String> messageHandler; 

	public KConsumer(String brokerList, String topicName, String groupId, Consumer<String> messageHandler) {
		this.brokerList = brokerList;
		this.topicName = topicName;
		this.groupId = groupId;
		this.messageHandler = messageHandler;
	}

	public void run() {
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
			consumer.subscribe(Collections.singletonList(topicName));
			while (true) {
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
				for (ConsumerRecord<String, String> record : records) {
					messageHandler.accept(record.value());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}