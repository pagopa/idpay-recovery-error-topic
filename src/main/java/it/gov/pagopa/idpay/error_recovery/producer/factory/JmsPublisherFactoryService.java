package it.gov.pagopa.idpay.error_recovery.producer.factory;

import it.gov.pagopa.idpay.error_recovery.producer.JmsPublisher;

public interface JmsPublisherFactoryService extends PublisherFactoryService {
    JmsPublisher createInstance(String srcServer, String srcTopic);
}
