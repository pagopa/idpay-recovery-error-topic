package it.gov.pagopa.idpay.error_recovery.service;

import it.gov.pagopa.idpay.error_recovery.producer.Publisher;
import it.gov.pagopa.idpay.error_recovery.utils.Constants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

@ExtendWith({MockitoExtension.class})
class ErrorMessageMediatorServiceTest {
    @Mock private PublisherFactoryService publisherFactoryServiceMock;
    @Mock private ErrorMessagePublisherService errorMessagePublisherService;

    private ErrorMessageMediatorService errorMessageMediatorService;

    @BeforeEach
    void init(){
        errorMessageMediatorService=new ErrorMessageMediatorServiceImpl(publisherFactoryServiceMock, errorMessagePublisherService);
    }

    private static ConsumerRecord<String, String> buildBaseMessage() {
        return new ConsumerRecord<>("prova", 0, 0L, null, "payload");
    }

    @Test
    void testNotRetryableMessage(){
        // given
        ConsumerRecord<String, String> message = buildBaseMessage();
        message.headers().add(Constants.ERROR_MSG_HEADER_RETRYABLE, "false".getBytes(StandardCharsets.UTF_8));

        // when
        errorMessageMediatorService.accept(message);

        // Then
        Mockito.verifyNoInteractions(publisherFactoryServiceMock, errorMessagePublisherService);
    }

    @Test
    void testRetriableMessageNotConfigured(){
        // given
        ConsumerRecord<String, String> message = buildBaseMessage();
        message.headers().add(Constants.ERROR_MSG_HEADER_RETRYABLE, "true".getBytes(StandardCharsets.UTF_8));

        Mockito.when(publisherFactoryServiceMock.retrievePublisher(message.headers())).thenReturn(null);

        // when
        errorMessageMediatorService.accept(message);

        // Then
        Mockito.verify(publisherFactoryServiceMock).retrievePublisher(message.headers());

        Mockito.verifyNoMoreInteractions(publisherFactoryServiceMock);
        Mockito.verifyNoInteractions(errorMessagePublisherService);
    }

    @Test
    void testRetriableMessageHavingPublisherConfigured(){
        // given
        ConsumerRecord<String, String> message = buildBaseMessage();
        message.headers().add(Constants.ERROR_MSG_HEADER_RETRYABLE, "true".getBytes(StandardCharsets.UTF_8));

        Publisher publisher = Mockito.mock(Publisher.class);

        Mockito.when(publisherFactoryServiceMock.retrievePublisher(message.headers())).thenReturn(publisher);

        // when
        errorMessageMediatorService.accept(message);

        // Then
        Mockito.verify(publisherFactoryServiceMock).retrievePublisher(message.headers());
        Mockito.verify(errorMessagePublisherService).publish(message.headers(), message.key(), message.value(), publisher);

        Mockito.verifyNoMoreInteractions(publisherFactoryServiceMock, errorMessagePublisherService);
    }
}
