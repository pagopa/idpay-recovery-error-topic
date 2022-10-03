package it.gov.pagopa.idpay.error_recovery.producer.factory;

import it.gov.pagopa.idpay.error_recovery.config.HandledPublishersConfig;
import it.gov.pagopa.idpay.error_recovery.producer.KafkaPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KafkaPublisherFactoryServiceImpl implements KafkaPublisherFactoryService {

    private final ProducerFactory<String, String> producerFactory;
    private final HandledPublishersConfig handledPublishersConfig;

    public KafkaPublisherFactoryServiceImpl(ProducerFactory<String, String> producerFactory, HandledPublishersConfig handledPublishersConfig) {
        this.producerFactory = producerFactory;
        this.handledPublishersConfig = handledPublishersConfig;
    }

    @Override
    public KafkaPublisher createInstance(String srcServer, String srcTopic) {
        Map<String, Object> producerProperties = handledPublishersConfig.getKafkaPublisherProperties(srcServer, srcTopic);

        if(producerProperties!=null){
            KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory, producerProperties);
            kafkaTemplate.setDefaultTopic(srcTopic);
            return new KafkaPublisher(kafkaTemplate);
        } else {
            return null;
        }
    }
}
