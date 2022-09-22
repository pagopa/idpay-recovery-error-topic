package it.gov.pagopa.idpay.error_recovery.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface ErrorMessageService {
    void accept(ConsumerRecord<String, String> message);
}
