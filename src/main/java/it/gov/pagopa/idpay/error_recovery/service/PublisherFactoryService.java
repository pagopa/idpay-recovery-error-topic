package it.gov.pagopa.idpay.error_recovery.service;

import it.gov.pagopa.idpay.error_recovery.producer.Publisher;
import org.apache.kafka.common.header.Headers;

public interface PublisherFactoryService {
    Publisher retrievePublisher(Headers headers);
}
