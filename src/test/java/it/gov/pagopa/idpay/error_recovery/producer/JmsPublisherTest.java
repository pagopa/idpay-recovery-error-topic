package it.gov.pagopa.idpay.error_recovery.producer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class JmsPublisherTest {

    @Mock private JmsMessagingTemplate jmsMessagingTemplateMock;

    private JmsPublisher publisher;

    @BeforeEach
    void init(){
        publisher=new JmsPublisher(jmsMessagingTemplateMock);
    }

    @Test
    void test(){
        Message<String> message = new GenericMessage<>("PROVA", Map.of("H1", "V1".getBytes(StandardCharsets.UTF_8)));
        publisher.send(message);

        Mockito.verify(jmsMessagingTemplateMock).send(Mockito.argThat(a -> {
            Assertions.assertEquals("PROVA", new String((byte[])a.getPayload(), StandardCharsets.UTF_8));
            Assertions.assertEquals("V1", a.getHeaders().get("H1"));
            return true;
        }));
    }
}
