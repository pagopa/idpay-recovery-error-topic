package it.gov.pagopa.idpay.error_recovery.producer;

import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class JmsPublisher implements Publisher {

    private final JmsMessagingTemplate jmsTemplate;

    public JmsPublisher(JmsMessagingTemplate jmsMessagingTemplate) {
        this.jmsTemplate = jmsMessagingTemplate;
    }

    @Override
    public void send(Message<String> message) {
        jmsTemplate.send(MessageBuilder.createMessage(message.getPayload().getBytes(StandardCharsets.UTF_8), convertHeaders(message)));
    }

    private static MessageHeaders convertHeaders(Message<String> message) {
        return new MessageHeaders(message.getHeaders().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        x -> (x.getValue() instanceof byte[] bytes) ? new String(bytes, StandardCharsets.UTF_8) : x.getValue())
                ));
    }
}
