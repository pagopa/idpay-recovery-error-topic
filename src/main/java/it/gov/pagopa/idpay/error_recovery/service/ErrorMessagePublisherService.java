package it.gov.pagopa.idpay.error_recovery.service;

import it.gov.pagopa.idpay.error_recovery.producer.Publisher;
import org.apache.kafka.common.header.Headers;

public interface ErrorMessagePublisherService {
    void publish(Headers headers, String payload, Publisher publisher);
}
