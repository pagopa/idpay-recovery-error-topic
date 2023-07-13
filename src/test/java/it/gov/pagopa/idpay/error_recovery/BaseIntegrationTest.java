package it.gov.pagopa.idpay.error_recovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@EnableAutoConfiguration()
@EmbeddedKafka(topics = {
        "${errorListener.topic}",
        "idpay-onboarding-outcome",
        "idpay-hpan-update",
        "idpay-rule-update",
        "idpay-transaction",
        "idpay-transaction-user-id-splitter",
}, controlledShutdown = true)
@TestPropertySource(
        properties = {
                //region kafka brokers
                "logging.level.org.apache.zookeeper=WARN",
                "logging.level.org.apache.kafka=WARN",
                "logging.level.kafka=WARN",
                "logging.level.state.change.logger=WARN",
                "spring.kafka.consumer.security.protocol=PLAINTEXT",
                "spring.kafka.producer.security.protocol=PLAINTEXT",
                "spring.cloud.stream.kafka.binder.zkNodes=${spring.embedded.zookeeper.connect}",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "handled-publishers.kafka.idpay-evh-ns-00.properties.bootstrap.servers=${spring.embedded.kafka.brokers}",
                "handled-publishers.kafka.idpay-evh-ns-01.properties.bootstrap.servers=${spring.embedded.kafka.brokers}",
                //endregion

                //region service bus
                // mocked replacing it using kafka
                "spring.cloud.azure.servicebus.connection-string=Endpoint=sb://ServiceBusEndpoint;SharedAccessKeyName=sharedAccessKeyName;SharedAccessKey=sharedAccessKey;EntityPath=entityPath",
                "spring.cloud.stream.binders.kafka-onboarding-request.type=kafka",
                "spring.cloud.stream.binders.kafka-onboarding-request.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.bindings.admissibilityProcessor-in-0.destination=idpay-onboarding-request",
                //endregion
        })
@AutoConfigureDataMongo
public abstract class BaseIntegrationTest {
    @Autowired
    protected EmbeddedKafkaBroker kafkaBroker;
    @Autowired
    protected KafkaTemplate<String, String> template;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    protected String kafkaBootstrapServers;
    @Value("${spring.cloud.stream.kafka.binder.zkNodes}")
    private String zkNodes;

    protected String serviceBusServers = "ServiceBusEndpoint";

    @Value("${errorListener.topic}")
    protected String topicErrors;
    @Value("${spring.kafka.consumer.group-id}")
    protected String groupIdtopicErrors;

    @Value("${errorListener.idleInterval.minSeconds}")
    protected long pauseLength;

    @Value("${app.retry.max-retry}")
    protected int maxRetries;

    protected final List<String> kafkaRecoveryTopics=List.of(
            "idpay-onboarding-outcome",
            "idpay-hpan-update",
            "idpay-rule-update",
            "idpay-transaction",
            "idpay-transaction-user-id-splitter"
    );

    protected final List<String> serviceBusRecoveryTopics=List.of(
            "idpay-onboarding-request"
    );

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        unregisterMBean("kafka.*:*");
        unregisterMBean("org.springframework.*:*");
    }

    private static void unregisterMBean(String objectName) throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException {
        ObjectName mbeanName = new ObjectName(objectName);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        for (ObjectInstance mBean : mBeanServer.queryMBeans(mbeanName, null)) {
            mBeanServer.unregisterMBean(mBean.getObjectName());
        }
    }

    @PostConstruct
    public void logEmbeddedServerConfig() {
        System.out.printf("""
                        ************************
                        Embedded kafka: %s
                        ************************
                        """,
                "bootstrapServers: %s, zkNodes: %s".formatted(kafkaBootstrapServers, zkNodes));
    }

    protected Consumer<String, String> getEmbeddedKafkaConsumer(String topic, String groupId) {
        return getEmbeddedKafkaConsumer(topic, groupId, true);
    }

    protected Consumer<String, String> getEmbeddedKafkaConsumer(String topic, String groupId, boolean attachToBroker) {
        if (!kafkaBroker.getTopics().contains(topic)) {
            kafkaBroker.addTopics(topic);
        }

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "true", kafkaBroker);
        consumerProps.put("key.deserializer", StringDeserializer.class);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = cf.createConsumer();
        if(attachToBroker){
            kafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);
        }
        return consumer;
    }

    protected void readFromEmbeddedKafka(String topic, String groupId, java.util.function.Consumer<ConsumerRecord<String, String>> consumeMessage, Integer expectedMessagesCount, Duration timeout) {
        readFromEmbeddedKafka(getEmbeddedKafkaConsumer(topic, groupId), consumeMessage, true, expectedMessagesCount, timeout);
    }

    protected void readFromEmbeddedKafka(Consumer<String, String> consumer, java.util.function.Consumer<ConsumerRecord<String, String>> consumeMessage, boolean consumeFromBeginning, Integer expectedMessagesCount, Duration timeout) {
        if (consumeFromBeginning) {
            consumeFromBeginning(consumer);
        }
        int i = 0;
        while (i < expectedMessagesCount) {
            ConsumerRecords<String, String> published = consumer.poll(timeout);
            for (ConsumerRecord<String, String> stringStringConsumerRecord : published) {
                consumeMessage.accept(stringStringConsumerRecord);
                i++;
            }
        }
    }

    protected void consumeFromBeginning(Consumer<String, String> consumer) {
        consumer.seekToBeginning(consumer.assignment());
    }

    protected List<ConsumerRecord<String, String>> consumeMessages(String topic, int expectedNumber, long maxWaitingMs) {
        return consumeMessages(topic, "idpay-group-test-check", expectedNumber, maxWaitingMs);
    }

    protected List<ConsumerRecord<String, String>> consumeMessages(String topic, String groupId, int expectedNumber, long maxWaitingMs) {
        long startTime = System.currentTimeMillis();
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, groupId)) {

            List<ConsumerRecord<String, String>> payloadConsumed = new ArrayList<>(expectedNumber);
            while (payloadConsumed.size() < expectedNumber) {
                if (System.currentTimeMillis() - startTime > maxWaitingMs) {
                    Assertions.fail("timeout of %d ms expired. Read %d messages of %d".formatted(maxWaitingMs, payloadConsumed.size(), expectedNumber));
                }
                consumer.poll(Duration.ofMillis(7000)).iterator().forEachRemaining(payloadConsumed::add);
            }
            return payloadConsumed;
        }
    }

    protected void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, Object payload) {
        try {
            publishIntoEmbeddedKafka(topic, headers, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, String payload) {
        final RecordHeader retryHeader = new RecordHeader("RETRY", "1".getBytes());
        if (headers == null) {
            headers = new RecordHeaders(new RecordHeader[]{retryHeader});
        } else {
            headers = Stream.concat(
                            StreamSupport.stream(headers.spliterator(), false),
                            Stream.of(retryHeader))
                    .collect(Collectors.toList());
        }
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, null, key, payload, headers);
        template.send(record);
    }

    protected Map<TopicPartition, OffsetAndMetadata> getCommittedOffsets(String topic, String groupId){
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, groupId, false)) {
            return consumer.committed(consumer.partitionsFor(topic).stream().map(p-> new TopicPartition(topic, p.partition())).collect(Collectors.toSet()));
        }
    }

    protected Map<TopicPartition, OffsetAndMetadata> checkCommittedOffsets(String topic, String groupId, long expectedCommittedMessages){
        return checkCommittedOffsets(topic, groupId, expectedCommittedMessages, 10, 500);
    }

    // Cannot use directly Awaitlity cause the Callable condition is performed on separate thread, which will go into conflict with the consumer Kafka access
    protected Map<TopicPartition, OffsetAndMetadata> checkCommittedOffsets(String topic, String groupId, long expectedCommittedMessages, int maxAttempts, int millisAttemptDelay){
        RuntimeException lastException = null;
        if(maxAttempts<=0){
            maxAttempts=1;
        }

        for(;maxAttempts>0; maxAttempts--){
            try {
                final Map<TopicPartition, OffsetAndMetadata> commits = getCommittedOffsets(topic, groupId);
                Assertions.assertEquals(expectedCommittedMessages, commits.values().stream().mapToLong(OffsetAndMetadata::offset).sum());
                return commits;
            } catch (Throwable e){
                lastException = new RuntimeException(e);
                wait(millisAttemptDelay, TimeUnit.MILLISECONDS);
            }
        }
        throw lastException;
    }

    protected Map<TopicPartition, Long> getEndOffsets(String topic){
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, "idpay-group-test-check", false)) {
            return consumer.endOffsets(consumer.partitionsFor(topic).stream().map(p-> new TopicPartition(topic, p.partition())).toList());
        }
    }

    protected Map<TopicPartition, Long> checkPublishedOffsets(String topic, long expectedPublishedMessages){
        Map<TopicPartition, Long> endOffsets = getEndOffsets(topic);
        Assertions.assertEquals(expectedPublishedMessages, endOffsets.values().stream().mapToLong(x->x).sum());
        return endOffsets;
    }

    protected static void waitFor(Callable<Boolean> test, Supplier<String> buildTestFailureMessage, int maxAttempts, int millisAttemptDelay) {
        try {
            await()
                    .pollInterval(millisAttemptDelay, TimeUnit.MILLISECONDS)
                    .atMost((long) maxAttempts * millisAttemptDelay, TimeUnit.MILLISECONDS)
                    .timeout(((long)maxAttempts * millisAttemptDelay)+10, TimeUnit.MILLISECONDS)
                    .until(test);
        } catch (RuntimeException e) {
            Assertions.fail(buildTestFailureMessage.get(), e);
        }
    }

    protected static void wait(long timeout, TimeUnit timeoutUnit) {
        try{
            Awaitility.await().timeout(timeout, timeoutUnit).until(()->false);
        } catch (ConditionTimeoutException ex){
            // Do Nothing
        }
    }

}
