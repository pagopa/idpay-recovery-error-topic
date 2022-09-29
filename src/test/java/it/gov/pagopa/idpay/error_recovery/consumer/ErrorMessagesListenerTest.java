package it.gov.pagopa.idpay.error_recovery.consumer;

import it.gov.pagopa.idpay.error_recovery.BaseIntegrationTest;
import it.gov.pagopa.idpay.error_recovery.producer.JmsPublisher;
import it.gov.pagopa.idpay.error_recovery.producer.KafkaPublisher;
import it.gov.pagopa.idpay.error_recovery.producer.factory.JmsPublisherFactoryService;
import it.gov.pagopa.idpay.error_recovery.producer.factory.KafkaPublisherFactoryService;
import it.gov.pagopa.idpay.error_recovery.utils.Constants;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "errorListener.idleInterval.minSeconds=7", // to configure in order to be > "Time spent to wait publishing kafka messages"
        "logging.level.it.gov.pagopa.idpay.error_recovery.service.ErrorMessageMediatorServiceImpl=WARN",
        "logging.level.it.gov.pagopa.idpay.error_recovery.service.ErrorMessagePublisherServiceImpl=WARN",
})
class ErrorMessagesListenerTest extends BaseIntegrationTest {

    @SpyBean
    private KafkaPublisherFactoryService kafkaPublisherFactoryService;
    @SpyBean
    private JmsPublisherFactoryService jmsPublisherFactoryService;

    private final Set<KafkaPublisher> kafkaPublisherSpies = Collections.synchronizedSet(new HashSet<>());
    private final Set<JmsPublisher> jmsPublisherMocks = Collections.synchronizedSet(new HashSet<>());

    private void configurePublisherSpies(){
        Mockito.doAnswer(a->{
            KafkaPublisher publisher = (KafkaPublisher)a.callRealMethod();
            if(publisher!=null) {
                KafkaPublisher spy = Mockito.spy(publisher);
                kafkaPublisherSpies.add(spy);
                return spy;
            } else {
                return null;
            }
        }).when(kafkaPublisherFactoryService).createInstance(Mockito.any(), Mockito.any());

        Mockito.doAnswer(a->{
            a.callRealMethod();
            JmsPublisher mock = Mockito.mock(JmsPublisher.class);
            jmsPublisherMocks.add(mock);
            return mock;
        }).when(jmsPublisherFactoryService).createInstance(Mockito.any(), Mockito.any());
    }

    /**
     * It will send N/2 kafka recoverable messages, M servicebus messages, X messages which are expected to be not more published (retryable false, over maxRetry, topic/server not configured).
     * Thus, it will send N/2 kafka recoverable messages (this way we know that when we verify the recover of N kafka messages, we have read all the topic.
     * After have waited for at least 1 message execution, it will send 1 kafka recoverable message, whose republish is expected to be executed after consumer pause
     * <br />
     * Checks performed:
     * <ol>
     *     <li>{@link it.gov.pagopa.idpay.error_recovery.producer.Publisher} send invocation numbers (N+M)</li>
     *     <li>kafka messages really published onto destination topics (N)</li>
     *     <li>{@link it.gov.pagopa.idpay.error_recovery.producer.Publisher} send payload and headers</li>
     * </ol>
     * */
    @Test
    void test() {
        configurePublisherSpies();

        int kafkaMessage2recover = Math.max(10, kafkaRecoveryTopics.size()*2); // Configure even number
        int serviceBusMessage2recover = Math.max(2, serviceBusRecoveryTopics.size());
        int messageNotRetriable = 5;
        int messageMaxRetried = 5;
        int messageNotRecoverable = 1;

        List<Triple<Headers, String, String>> msgs = new ArrayList<>(IntStream.range(0, kafkaMessage2recover/2).mapToObj(this::buildKafkaMessage2Recover).toList());
        msgs.addAll(IntStream.range(msgs.size(), msgs.size() + serviceBusMessage2recover).mapToObj(this::buildServiceBusMessage2Recover).toList());
        msgs.addAll(IntStream.range(msgs.size(), msgs.size() + messageNotRetriable).mapToObj(this::buildMessageNotRetriable).toList());
        msgs.addAll(IntStream.range(msgs.size(), msgs.size() + messageMaxRetried).mapToObj(this::buildMessageMaxRetried).toList());
        msgs.addAll(IntStream.range(msgs.size(), msgs.size() + messageNotRecoverable).mapToObj(this::buildMessageNotRecoverable).toList());

        int lastHalfKafkaMessagesIndex = msgs.size();
        msgs.addAll(IntStream.range(lastHalfKafkaMessagesIndex, lastHalfKafkaMessagesIndex + kafkaMessage2recover/2).mapToObj(this::buildKafkaMessage2Recover).toList());

        long timePublishOnboardingStart = System.currentTimeMillis();
        msgs.forEach(m -> publishIntoEmbeddedKafka(topicErrors, m.getLeft(), m.getMiddle(), m.getRight()));
        long timePublishingOnboardingRequest = System.currentTimeMillis();

        waitPublisherInvocations(kafkaMessage2recover, serviceBusMessage2recover);
        long time2WaitSpiesPublishInvocation = System.currentTimeMillis() - timePublishingOnboardingRequest;

        // second launch, to be read after consumer pause
        IntStream.range(msgs.size(), msgs.size()+1).mapToObj(this::buildKafkaMessage2Recover).forEach(m->publishIntoEmbeddedKafka(topicErrors, m.getLeft(), m.getMiddle(), m.getRight()));
        long secondLaunchTime = System.currentTimeMillis();

        waitPublishedMessages(kafkaMessage2recover);
        long time2WaitKafkaRecoveredMessages = System.currentTimeMillis();

        Assertions.assertEquals(kafkaRecoveryTopics.size(), kafkaPublisherSpies.size());
        Assertions.assertEquals(serviceBusRecoveryTopics.size(), jmsPublisherMocks.size());

        checkPublishedMessages(kafkaMessage2recover, serviceBusMessage2recover, lastHalfKafkaMessagesIndex);
        long time2checkpayloads = System.currentTimeMillis() - time2WaitKafkaRecoveredMessages;

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d + %d + %d + %d) trx messages: %d millis
                        Time spent to wait publish invocations: %d millis
                        Time spent to wait publishing kafka messages: %d millis
                        Time spent to check payloads: %d millis
                        """,
                msgs.size(),
                kafkaMessage2recover,
                serviceBusMessage2recover,
                messageNotRetriable,
                messageMaxRetried,
                messageNotRecoverable,
                timePublishingOnboardingRequest - timePublishOnboardingStart,
                time2WaitSpiesPublishInvocation,
                time2WaitKafkaRecoveredMessages - timePublishingOnboardingRequest,
                time2checkpayloads
        );

        Awaitility.await().atLeast(pauseLength*1000-(System.currentTimeMillis()-secondLaunchTime)-900, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(()->countKafkaPublisherInvocations() == kafkaMessage2recover+1);
        long timeEnd = System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time occurred to process message send after first sleep (pause length %d seconds): %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                pauseLength,
                timeEnd - secondLaunchTime,
                timeEnd - timePublishOnboardingStart
        );

        checkCommittedOffsets(topicErrors, groupIdtopicErrors, msgs.size()+1);
    }

    //region messages build
    private Triple<Headers, String, String> buildKafkaMessage2Recover(int bias) {
        return buildMessage2Recover("kafka", kafkaBootstrapServers, kafkaRecoveryTopics, bias);
    }

    private Triple<Headers, String, String> buildServiceBusMessage2Recover(int bias) {
        return buildMessage2Recover("servicebus", serviceBusServers, serviceBusRecoveryTopics, bias);
    }

    private Triple<Headers, String, String> buildMessage2Recover(String srcType, String srcServer, List<String> destinations, int bias) {
        return Triple.of(
                buildHeaders(srcType, srcServer, destinations.get(bias % destinations.size()), bias % 2 == 0 ? true : null, bias%2==0? bias % maxRetries : null),
                bias % 2 == 0 ? bias + "" : null,
                bias + ""
        );
    }

    private Triple<Headers, String, String> buildMessageNotRetriable(int bias) {
        Triple<Headers, String, String> out = buildKafkaMessage2Recover(bias);
        out.getLeft().remove(Constants.ERROR_MSG_HEADER_RETRYABLE);
        out.getLeft().add(Constants.ERROR_MSG_HEADER_RETRYABLE, "false".getBytes(StandardCharsets.UTF_8));
        return out;
    }

    private Triple<Headers, String, String> buildMessageMaxRetried(int bias) {
        Triple<Headers, String, String> out = buildKafkaMessage2Recover(bias);
        out.getLeft().remove(Constants.ERROR_MSG_HEADER_RETRY);
        out.getLeft().add(Constants.ERROR_MSG_HEADER_RETRY, ((maxRetries + (bias % 2)) + "").getBytes(StandardCharsets.UTF_8));
        return out;
    }

    private Triple<Headers, String, String> buildMessageNotRecoverable(int bias) {
        Triple<Headers, String, String> out = buildKafkaMessage2Recover(bias);
        if(bias%2==0) {
            out.getLeft().remove(Constants.ERROR_MSG_HEADER_SRC_TOPIC);
            out.getLeft().add(Constants.ERROR_MSG_HEADER_SRC_TOPIC, ("NOT_RECOVERABLE_SERVER").getBytes(StandardCharsets.UTF_8));
        } else {
            out.getLeft().remove(Constants.ERROR_MSG_HEADER_SRC_SERVER);
            out.getLeft().add(Constants.ERROR_MSG_HEADER_SRC_SERVER, ("NOT_RECOVERABLE_SERVER").getBytes(StandardCharsets.UTF_8));
        }
        return out;
    }

    private Headers buildHeaders(String srcType, String srcServer, String srcTopic, Boolean retryable, Integer retry) {
        RecordHeaders headers = new RecordHeaders(new Header[]{
                new RecordHeader(Constants.ERROR_MSG_HEADER_SRC_TYPE, srcType.getBytes(StandardCharsets.UTF_8)),
                new RecordHeader(Constants.ERROR_MSG_HEADER_SRC_SERVER, srcServer.getBytes(StandardCharsets.UTF_8)),
                new RecordHeader(Constants.ERROR_MSG_HEADER_SRC_TOPIC, srcTopic.getBytes(StandardCharsets.UTF_8))
        });
        if (retryable != null) {
            headers.add(new RecordHeader(Constants.ERROR_MSG_HEADER_RETRYABLE, (retryable + "").getBytes(StandardCharsets.UTF_8)));
        }
        if (retry != null) {
            headers.add(new RecordHeader(Constants.ERROR_MSG_HEADER_RETRY, retry.toString().getBytes(StandardCharsets.UTF_8)));
        }
        return headers;
    }
    //endregion

    //region wait conditions
    private void waitPublishedMessages(int kafkaMessage2recover) {
        final long[] kafkaRecoveredPublishedMessages = new long[]{0};
        waitFor(() -> {
                    kafkaRecoveredPublishedMessages[0] = kafkaRecoveryTopics.stream().map(this::getEndOffsets).flatMapToLong(x -> x.values().stream().mapToLong(y -> y)).sum();
                    return kafkaRecoveredPublishedMessages[0] == kafkaMessage2recover;
                }, () -> "Recovered %d messages instead of %d".formatted(kafkaRecoveredPublishedMessages[0], kafkaMessage2recover),
                10, 1000);
    }

    private void waitPublisherInvocations(int kafkaMessage2recover, int serviceBusMessage2recover) {
        final long[] kafkaRecoveredMessages = new long[]{0};
        final long[] jmsRecoveredMessages = new long[]{0};
        waitFor(()->{
                    kafkaRecoveredMessages[0] = countKafkaPublisherInvocations();
                    jmsRecoveredMessages[0] = jmsPublisherMocks.stream().mapToLong(spy -> Mockito.mockingDetails(spy).getInvocations().size()).sum();
                    return kafkaRecoveredMessages[0]== kafkaMessage2recover && jmsRecoveredMessages[0]== serviceBusMessage2recover;
                }, () -> "Sending %d kafka messages instead of %d; Sending %d service bus messages instead of %d".formatted(kafkaRecoveredMessages[0], kafkaMessage2recover, jmsRecoveredMessages[0], serviceBusMessage2recover),
                10, 1000);
    }

    private long countKafkaPublisherInvocations() {
        return kafkaPublisherSpies.stream().mapToLong(spy -> Mockito.mockingDetails(spy).getInvocations().size()).sum();
    }
    //endregion

    //region messages recovered content checks
    private void checkPublishedMessages(int kafkaMessage2recover, int serviceBusMessage2recover, int lastHalfKafkaMessagesIndex) {
        kafkaPublisherSpies.forEach(spy -> Mockito.mockingDetails(spy).getInvocations().forEach(i->
                checkKafkaRecoveredMessage(i.getArgument(0), kafkaMessage2recover, lastHalfKafkaMessagesIndex))
        );
        jmsPublisherMocks.forEach(spy -> Mockito.mockingDetails(spy).getInvocations().forEach(i->
                checkServiceBusRecoveredMessage(i.getArgument(0), kafkaMessage2recover, serviceBusMessage2recover))
        );
    }

    private void checkKafkaRecoveredMessage(Message<String> recoveredMessage, int kafkaMessage2recover, int lastHalfKafkaMessagesIndex) {
        int bias = Integer.parseInt(recoveredMessage.getPayload());

        Assertions.assertTrue(bias >= 0 && (bias < kafkaMessage2recover/2 || bias >= lastHalfKafkaMessagesIndex), "Unexpected message handled as kafka: %d".formatted(bias));

        checkRecoveredMessage(recoveredMessage, bias);
    }

    private void checkServiceBusRecoveredMessage(Message<String> recoveredMessage, int kafkaMessage2recover, int servicebusMessage2recover) {
        int bias = Integer.parseInt(recoveredMessage.getPayload());

        int firstServiceBusMessageIndex = kafkaMessage2recover / 2;
        Assertions.assertTrue(bias >= firstServiceBusMessageIndex && bias < (firstServiceBusMessageIndex +servicebusMessage2recover), "Unexpected message handled as servicebus: %d".formatted(bias));

        checkRecoveredMessage(recoveredMessage, bias);
    }

    private void checkRecoveredMessage(Message<String> recoveredMessage, int bias){
        Assertions.assertEquals(bias+"", recoveredMessage.getPayload());
        int expectedRetry;

        if(bias % 2 == 0){
            Assertions.assertEquals(bias + "", recoveredMessage.getHeaders().get(KafkaHeaders.MESSAGE_KEY), "Read message (%d) without expected key".formatted(bias));
            expectedRetry=(bias%maxRetries)+1;
        } else {
            Assertions.assertNull(recoveredMessage.getHeaders().get(KafkaHeaders.MESSAGE_KEY), "Read message (%d) without expected key".formatted(bias));
            expectedRetry=1;
        }

        Assertions.assertEquals(expectedRetry+"", readHeader(recoveredMessage, Constants.ERROR_MSG_HEADER_RETRY), "Read message (%d) without expected retry header".formatted(bias));
    }

    private static String readHeader(Message<String> recoveredMessage, String headerName) {
        Object value = recoveredMessage.getHeaders().get(headerName);
        if(value!=null){
            return new String((byte[]) value);
        } else {
            return null;
        }
    }
    //endregion
}
