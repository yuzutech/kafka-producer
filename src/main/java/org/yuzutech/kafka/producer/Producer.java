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
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class Producer {

    private static Logger log = LoggerFactory.getLogger(Producer.class);

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            log.error("Usage: java -jar kafka-producer-all.jar connector.properties");
            System.exit(1);
        }
        String connectorFile = args[0];
        assertPresent(connectorFile, "Usage: java -jar kafka-producer-all.jar connector.properties");

        Properties config = new Properties();
        File connectorProperties = loadConfig(connectorFile, config);

        int numRecords = getNumRecords(config);
        String topic = config.getProperty("producer.topic");
        String file = config.getProperty("producer.file");
        assertPresent(topic, "Configuration must include 'producer.topic' setting");
        assertPresent(file, "Configuration must include 'producer.file' setting");

        File dataFile;
        if (Paths.get(file).isAbsolute()) {
            dataFile = new File(file);
        } else {
            dataFile = new File(connectorProperties.getParentFile(), file);
        }
        log.debug("Reading file {}", dataFile);
        List<String> lines = Files.readAllLines(dataFile.toPath(), Charset.forName("UTF-8"));

        org.apache.kafka.clients.producer.Producer<String, String> producer = new KafkaProducer<>(config);
        produce(producer, topic, lines, numRecords);
    }

    private static int getNumRecords(Properties config) {
        String numRecordsValue = config.getProperty("producer.num.records");
        int numRecords;
        if (numRecordsValue != null && !numRecordsValue.isEmpty()) {
            numRecords = Integer.parseInt(numRecordsValue);
        } else {
            numRecords = 10;
        }
        return numRecords;
    }

    private static void produce(org.apache.kafka.clients.producer.Producer<String, String> producer, String topic, List<String> lines, int numRecords) {
        for (String line : lines) {
            log.debug("Sending {} record {} times", line, numRecords);
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

    private static File loadConfig(String connectorFile, Properties config) throws IOException {
        File connectorProperties = new File(connectorFile);
        config.load(new FileInputStream(connectorProperties));
        log.info("Configuration values: {}", config);
        return connectorProperties;
    }

    private static void assertPresent(String value, String message) {
        if (value == null || value.isEmpty()) {
            log.error(message);
            System.exit(1);
        }
    }
}
