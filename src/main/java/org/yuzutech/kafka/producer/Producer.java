package org.yuzutech.kafka.producer;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

public class Producer {

    private static Logger log = LoggerFactory.getLogger(Producer.class);

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            log.error("Usage: Producer connector.properties");
            System.exit(1);
        }
        String connectorFile = args[0];
        if (connectorFile == null || connectorFile.isEmpty()) {
            log.error("Usage: Producer connector.properties");
            System.exit(1);
        }
        Properties config = new Properties();
        File connectorProperties = new File(connectorFile);
        config.load(new FileInputStream(connectorProperties));
        org.apache.kafka.clients.producer.Producer<String, String> producer = new KafkaProducer<>(config);
        String topic = config.getProperty("producer.topic");
        if (topic == null || topic.isEmpty()) {
            log.error("Configuration must include 'producer.topic' setting");
            System.exit(1);
        }
        String file = config.getProperty("producer.file");
        if (file == null || file.isEmpty()) {
            log.error("Configuration must include 'producer.file' setting");
            System.exit(1);
        }
        // Currently only relative path are supported
        // TODO Handle absolute path
        File dataFile = new File(connectorProperties.getParentFile(), file);
        List<String> lines = Files.readAllLines(dataFile.toPath(), Charset.forName("UTF-8"));
        String numRecordsValue = config.getProperty("producer.num.records");
        int numRecords;
        if (numRecordsValue != null && !numRecordsValue.isEmpty()) {
            numRecords = Integer.parseInt(numRecordsValue);
        } else {
            numRecords = 10;
        }
        for (String line : lines) {
            for (int i = 0; i < numRecords; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, line);
                producer.send(record, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception exception) {
                        if (exception != null) {
                            log.warn("Something bad happened", exception);
                        }
                    }
                });
            }
        }
        producer.close();
    }
}
