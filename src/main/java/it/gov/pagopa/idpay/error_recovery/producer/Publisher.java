package it.gov.pagopa.idpay.error_recovery.producer;

import org.springframework.messaging.Message;

public interface Publisher {
    void send(Message<String> message);
}
