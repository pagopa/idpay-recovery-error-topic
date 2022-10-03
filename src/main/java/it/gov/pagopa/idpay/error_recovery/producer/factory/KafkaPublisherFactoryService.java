package it.gov.pagopa.idpay.error_recovery.producer.factory;

import it.gov.pagopa.idpay.error_recovery.producer.KafkaPublisher;

public interface KafkaPublisherFactoryService extends PublisherFactoryService {
    KafkaPublisher createInstance(String srcServer, String srcTopic);
}
