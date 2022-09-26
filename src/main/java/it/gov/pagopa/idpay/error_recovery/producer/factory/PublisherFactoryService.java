package it.gov.pagopa.idpay.error_recovery.producer.factory;

import it.gov.pagopa.idpay.error_recovery.producer.Publisher;

public interface PublisherFactoryService {
    Publisher createInstance(String srcServer, String srcTopic);
}
