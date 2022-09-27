package it.gov.pagopa.idpay.error_recovery.service;

import it.gov.pagopa.idpay.error_recovery.producer.Publisher;
import it.gov.pagopa.idpay.error_recovery.utils.Constants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@ExtendWith(MockitoExtension.class)
class ErrorMessagePublisherServiceTest {

    @Mock private Publisher publisherMock;

    private final ErrorMessagePublisherService errorMessagePublisherService = new ErrorMessagePublisherServiceImpl(10);

    private static ConsumerRecord<String, String> buildBaseMessage() {
        return new ConsumerRecord<>("prova", 0, 0L, null, "payload");
    }

    private ConsumerRecord<String, String> buildBaseMessageWithRetry(int retry) {
        ConsumerRecord<String, String> message = buildBaseMessage();
        message.headers().add(Constants.ERROR_MSG_HEADER_RETRY, (retry+"").getBytes(StandardCharsets.UTF_8));
        return message;
    }

    @Test
    void testNoRetry(){
        // Given
        ConsumerRecord<String, String> message = buildBaseMessage();

        message.headers().add(Constants.ERROR_MSG_HEADER_SRC_TYPE, "SRC_TYPE".getBytes(StandardCharsets.UTF_8));
        message.headers().add(Constants.ERROR_MSG_HEADER_SRC_SERVER, "SRC_SERVER".getBytes(StandardCharsets.UTF_8));
        message.headers().add(Constants.ERROR_MSG_HEADER_SRC_TOPIC, "SRC_TOPIC".getBytes(StandardCharsets.UTF_8));
        message.headers().add(Constants.ERROR_MSG_HEADER_DESCRIPTION, "DESCRIPTION".getBytes(StandardCharsets.UTF_8));
        message.headers().add(Constants.ERROR_MSG_HEADER_RETRYABLE, "RETRYABLE".getBytes(StandardCharsets.UTF_8));
        message.headers().add(Constants.ERROR_MSG_HEADER_STACKTRACE, "STACKTRACE".getBytes(StandardCharsets.UTF_8));

        // When
        errorMessagePublisherService.publish(message.headers(), message.key(), message.value(), publisherMock);

        // Then
        verifyPublishedMessage(message, 1, 1);
    }

    @Test
    void testRetryUnderMax(){
        // Given
        ConsumerRecord<String, String> message = buildBaseMessageWithRetry(5);
        message.headers().add("DUMMY", "DUMMY".getBytes(StandardCharsets.UTF_8));

        // When
        errorMessagePublisherService.publish(message.headers(), message.key(), message.value(), publisherMock);

        // Then
        verifyPublishedMessage(message, 2, 6);
    }

    private void verifyPublishedMessage(ConsumerRecord<String, String> message, int expectedHeaders, int expectedRetry) {
        Mockito.verify(publisherMock).send(Mockito.argThat(a->{
            Assertions.assertEquals(message.value(), a.getPayload());
            Assertions.assertEquals(expectedHeaders+2, a.getHeaders().size());
            Assertions.assertEquals(expectedRetry, Integer.parseInt(new String((byte[]) Objects.requireNonNull(a.getHeaders().get(Constants.ERROR_MSG_HEADER_RETRY)))));
            return true;
        }));
        Mockito.verifyNoMoreInteractions(publisherMock);
    }

    @Test
    void testRetryOverMax(){
        // Given
        ConsumerRecord<String, String> message = buildBaseMessageWithRetry(10);

        // When
        errorMessagePublisherService.publish(message.headers(), message.key(), message.value(), publisherMock);

        // Then
        Mockito.verifyNoInteractions(publisherMock);
    }
}
