package it.gov.pagopa.idpay.error_recovery.producer.factory;

import com.azure.spring.cloud.autoconfigure.jms.ServiceBusJmsConnectionFactory;
import it.gov.pagopa.idpay.error_recovery.config.HandledPublishersConfig;
import it.gov.pagopa.idpay.error_recovery.producer.JmsPublisher;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class JmsPublisherFactoryServiceImpl implements JmsPublisherFactoryService {

    private final HandledPublishersConfig handledPublishersConfig;

    public JmsPublisherFactoryServiceImpl(HandledPublishersConfig handledPublishersConfig) {
        this.handledPublishersConfig = handledPublishersConfig;
    }

    @Override
    public JmsPublisher createInstance(String srcServer, String srcTopic) {
        Map<String, String> producerProperties = handledPublishersConfig.getServiceBusPublisherProperties(srcServer, srcTopic);

        if(producerProperties!=null){
            JmsMessagingTemplate jmsMessagingTemplate = new JmsMessagingTemplate(new ServiceBusJmsConnectionFactory(producerProperties.get("connection-string")));
            jmsMessagingTemplate.setDefaultDestinationName(srcTopic);
            return new JmsPublisher(jmsMessagingTemplate);
        } else {
            return null;
        }
    }
}
