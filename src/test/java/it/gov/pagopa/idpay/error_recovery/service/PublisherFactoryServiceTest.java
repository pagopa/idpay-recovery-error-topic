package it.gov.pagopa.idpay.error_recovery.service;

import it.gov.pagopa.idpay.error_recovery.producer.JmsPublisher;
import it.gov.pagopa.idpay.error_recovery.producer.KafkaPublisher;
import it.gov.pagopa.idpay.error_recovery.producer.Publisher;
import it.gov.pagopa.idpay.error_recovery.producer.factory.JmsPublisherFactoryService;
import it.gov.pagopa.idpay.error_recovery.producer.factory.KafkaPublisherFactoryService;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
class PublisherFactoryServiceTest {

    @Mock private KafkaPublisherFactoryService kafkaPublisherFactoryServiceMock;
    @Mock private JmsPublisherFactoryService jmsPublisherFactoryServiceMock;

    private PublisherFactoryService publisherFactoryService;

    @BeforeEach
    void init(){
        publisherFactoryService = new PublisherFactoryServiceImpl(kafkaPublisherFactoryServiceMock, jmsPublisherFactoryServiceMock);
    }

    @Test
    void testKafkaPublisherRequest(){
        testPublishRequest("kafka", KafkaPublisher.class, kafkaPublisherFactoryServiceMock);
    }

    @Test
    void testJmsPublisherRequest(){
        testPublishRequest("servicebus", JmsPublisher.class, jmsPublisherFactoryServiceMock);
    }

    private void testPublishRequest(String srcType, Class<? extends Publisher> publisherClazz, it.gov.pagopa.idpay.error_recovery.producer.factory.PublisherFactoryService publisherFactoryServiceMock) {
        // Given
        Headers headers = buildHeaders(srcType);
        Publisher publisherMocked = Mockito.mock(publisherClazz);
        Mockito.when(publisherFactoryServiceMock.createInstance("srcServer", "srcTopic")).thenReturn(publisherMocked);

        // When
        Publisher result = this.publisherFactoryService.retrievePublisher(headers);

        // Then
        Assertions.assertSame(publisherMocked, result);
        Mockito.verify(publisherFactoryServiceMock).createInstance("srcServer", "srcTopic");

        Mockito.verifyNoMoreInteractions(jmsPublisherFactoryServiceMock, kafkaPublisherFactoryServiceMock);
    }

    private Headers buildHeaders(String srcType) {
        return new RecordHeaders(new Header[]{
           new RecordHeader("srcType", srcType.getBytes(StandardCharsets.UTF_8)),
           new RecordHeader("srcServer", "srcServer".getBytes(StandardCharsets.UTF_8)),
           new RecordHeader("srcTopic", "srcTopic".getBytes(StandardCharsets.UTF_8))
        });
    }

    @Test
    void testNotHandledSrcType() {
        // Given
        Headers headers = buildHeaders("DUMMY");

        // When
        testNoPublisherToExpect(headers);
    }

    @Test
    void testNoSrcHeaders() {
        // Given no srcServer and srcTopic
        Headers headers = new RecordHeaders();

        testNoPublisherToExpect(headers);

        // Given no srcServer
        headers = buildHeaders("kafka");
        headers.remove("srcServer");

        testNoPublisherToExpect(headers);

        // Given no srcTopic
        headers = buildHeaders("kafka");
        headers.remove("srcTopic");

        testNoPublisherToExpect(headers);
    }

    private void testNoPublisherToExpect(Headers headers) {
        // When
        Publisher result = this.publisherFactoryService.retrievePublisher(headers);

        // Then
        Assertions.assertNull(result);
        Mockito.verifyNoInteractions(jmsPublisherFactoryServiceMock, kafkaPublisherFactoryServiceMock);
    }
}
