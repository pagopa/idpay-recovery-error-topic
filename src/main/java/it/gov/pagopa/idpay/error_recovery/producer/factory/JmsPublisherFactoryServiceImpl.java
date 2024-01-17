package it.gov.pagopa.idpay.error_recovery.producer.factory;

import com.azure.spring.cloud.autoconfigure.implementation.jms.properties.AzureServiceBusJmsProperties;
import com.azure.spring.cloud.autoconfigure.implementation.properties.core.authentication.TokenCredentialConfigurationProperties;
import com.azure.spring.cloud.core.implementation.connectionstring.ServiceBusConnectionString;
import com.azure.spring.jms.ServiceBusJmsConnectionFactory;
import it.gov.pagopa.idpay.error_recovery.config.HandledPublishersConfig;
import it.gov.pagopa.idpay.error_recovery.producer.JmsPublisher;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class JmsPublisherFactoryServiceImpl implements JmsPublisherFactoryService {

    private static final String AMQP_URI_FORMAT = "amqps://%s?amqp.idleTimeout=%d";
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
            ServiceBusConnectionString serviceBusConnectionString = new ServiceBusConnectionString(getConnectionString(defaultServiceBusJmsProperties, producerProperties));
            AzureServiceBusJmsProperties properties = buildProperties(serviceBusConnectionString, defaultServiceBusJmsProperties, producerProperties);
            ServiceBusJmsConnectionFactory connectionFactory = new ServiceBusJmsConnectionFactory(properties.getCredential().getUsername(), properties.getCredential().getPassword(), buildRemoteURI(serviceBusConnectionString, properties));
            connectionFactory.setClientID(properties.getTopicClientId());
            JmsMessagingTemplate jmsMessagingTemplate = new JmsMessagingTemplate(connectionFactory);
            jmsMessagingTemplate.setDefaultDestinationName(srcTopic);
            return new JmsPublisher(jmsMessagingTemplate);
        } else {
            return null;
        }
    }

    private static AzureServiceBusJmsProperties buildProperties(ServiceBusConnectionString serviceBusConnectionString, AzureServiceBusJmsProperties defaultProps, Map<String, String> overrides) {
        AzureServiceBusJmsProperties properties = new AzureServiceBusJmsProperties();
        properties.setConnectionString(getConnectionString(defaultProps, overrides));
        properties.setPricingTier(overrides.getOrDefault("pricing-tier", defaultProps.getPricingTier()));
        properties.setTopicClientId(overrides.getOrDefault("topic-client-id", defaultProps.getTopicClientId()));
        properties.setIdleTimeout(Duration.parse(overrides.getOrDefault("idle-timeout", defaultProps.getIdleTimeout().toString())));
        try {
            properties.afterPropertiesSet();

            properties.setCredential(new TokenCredentialConfigurationProperties());
            properties.getCredential().setUsername(serviceBusConnectionString.getSharedAccessKeyName());
            properties.getCredential().setPassword(serviceBusConnectionString.getSharedAccessKey());
        } catch (Exception e) {
            throw new IllegalArgumentException("Something gone wrong while configuring ServiceBus properties", e);
        }
        return properties;
    }

    private static String getConnectionString(AzureServiceBusJmsProperties defaultProps, Map<String, String> overrides) {
        return overrides.getOrDefault("connection-string", defaultProps.getConnectionString());
    }

    private static String buildRemoteURI(ServiceBusConnectionString serviceBusConnectionString, AzureServiceBusJmsProperties properties) {
        String host = serviceBusConnectionString.getEndpointUri().getHost();

        return String.format(AMQP_URI_FORMAT, host, properties.getIdleTimeout().toMillis());
    }

}
