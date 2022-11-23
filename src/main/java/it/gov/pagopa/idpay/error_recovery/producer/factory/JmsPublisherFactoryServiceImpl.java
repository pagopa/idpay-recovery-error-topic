package it.gov.pagopa.idpay.error_recovery.producer.factory;

import com.azure.spring.cloud.autoconfigure.jms.ServiceBusJmsConnectionFactory;
import com.azure.spring.cloud.autoconfigure.jms.properties.AzureServiceBusJmsProperties;
import it.gov.pagopa.idpay.error_recovery.config.HandledPublishersConfig;
import it.gov.pagopa.idpay.error_recovery.producer.JmsPublisher;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class JmsPublisherFactoryServiceImpl implements JmsPublisherFactoryService {

    private final HandledPublishersConfig handledPublishersConfig;
    private final AzureServiceBusJmsProperties defaultServiceBusJmsProperties;

    public JmsPublisherFactoryServiceImpl(HandledPublishersConfig handledPublishersConfig, AzureServiceBusJmsProperties defaultServiceBusJmsProperties) {
        this.handledPublishersConfig = handledPublishersConfig;
        this.defaultServiceBusJmsProperties = defaultServiceBusJmsProperties;
    }

    @Override
    public JmsPublisher createInstance(String srcServer, String srcTopic) {
        Map<String, String> producerProperties = handledPublishersConfig.getServiceBusPublisherProperties(srcServer, srcTopic);

        if (producerProperties != null) {
            AzureServiceBusJmsProperties properties = buildProperties(defaultServiceBusJmsProperties, producerProperties);
            ServiceBusJmsConnectionFactory connectionFactory = new ServiceBusJmsConnectionFactory(properties.getUsername(), properties.getPassword(), properties.getRemoteUrl());
            connectionFactory.setClientID(properties.getTopicClientId());
            JmsMessagingTemplate jmsMessagingTemplate = new JmsMessagingTemplate(connectionFactory);
            jmsMessagingTemplate.setDefaultDestinationName(srcTopic);
            return new JmsPublisher(jmsMessagingTemplate);
        } else {
            return null;
        }
    }

    private static AzureServiceBusJmsProperties buildProperties(AzureServiceBusJmsProperties defaultProps, Map<String, String> overrides) {
        AzureServiceBusJmsProperties properties = new AzureServiceBusJmsProperties();
        properties.setConnectionString(overrides.getOrDefault("connection-string", defaultProps.getConnectionString()));
        properties.setPricingTier(overrides.getOrDefault("pricing-tier", defaultProps.getPricingTier()));
        properties.setTopicClientId(overrides.getOrDefault("topic-client-id", defaultProps.getTopicClientId()));
        try {
            properties.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalArgumentException("Something gone wrong while configuring ServiceBus properties", e);
        }
        return properties;
    }

}
