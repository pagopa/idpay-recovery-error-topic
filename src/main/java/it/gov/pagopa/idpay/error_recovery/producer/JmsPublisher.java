package it.gov.pagopa.idpay.error_recovery.producer;

import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.Message;

public class JmsPublisher implements Publisher {

    private final JmsMessagingTemplate jmsTemplate;

    public JmsPublisher(JmsMessagingTemplate jmsMessagingTemplate) {
        this.jmsTemplate = jmsMessagingTemplate;
    }

    @Override
    public void send(Message<String> message) {
        jmsTemplate.send(message);
    }
}
