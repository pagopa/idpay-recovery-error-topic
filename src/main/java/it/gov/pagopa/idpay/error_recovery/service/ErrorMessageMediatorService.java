package it.gov.pagopa.idpay.error_recovery.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface ErrorMessageMediatorService {
    void accept(ConsumerRecord<String, String> message);
}
