package it.gov.pagopa.idpay.error_recovery.producer.factory;

import com.azure.spring.cloud.autoconfigure.jms.ServiceBusJmsConnectionFactoryCustomizer;
import com.azure.spring.cloud.autoconfigure.jms.ServiceBusJmsConnectionFactoryUtils;
import com.azure.spring.cloud.autoconfigure.jms.properties.AzureServiceBusJmsProperties;
import it.gov.pagopa.idpay.error_recovery.config.HandledPublishersConfig;
import it.gov.pagopa.idpay.error_recovery.producer.JmsPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.jms.ConnectionFactory;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

@Service
public class JmsPublisherFactoryServiceImpl implements JmsPublisherFactoryService {

    private final HandledPublishersConfig handledPublishersConfig;
    private final JmsProperties jmsProperties;
    private final AzureServiceBusJmsProperties azureServiceBusJmsProperties;
    private final ObjectProvider<ServiceBusJmsConnectionFactoryCustomizer> factoryCustomizers;

    public JmsPublisherFactoryServiceImpl(HandledPublishersConfig handledPublishersConfig, JmsProperties jmsProperties, AzureServiceBusJmsProperties azureServiceBusJmsProperties, ObjectProvider<ServiceBusJmsConnectionFactoryCustomizer> factoryCustomizers) {
        this.handledPublishersConfig = handledPublishersConfig;
        this.jmsProperties = jmsProperties;
        this.azureServiceBusJmsProperties = azureServiceBusJmsProperties;
        this.factoryCustomizers = factoryCustomizers;
    }

    @Override
    public JmsPublisher createInstance(String srcServer, String srcTopic) {
        Map<String, String> producerProperties = handledPublishersConfig.getServiceBusPublisherProperties(srcServer, srcTopic);

        if (producerProperties != null) {
            AzureServiceBusJmsProperties properties = buildProperties(azureServiceBusJmsProperties, producerProperties);
            ConnectionFactory connectionFactory = ServiceBusJmsConnectionFactoryUtils.createConnectionFactory(jmsProperties, properties, factoryCustomizers);
            JmsMessagingTemplate jmsMessagingTemplate = new JmsMessagingTemplate(connectionFactory);
            jmsMessagingTemplate.setDefaultDestinationName(srcTopic);
            return new JmsPublisher(jmsMessagingTemplate);
        } else {
            return null;
        }
    }

    private AzureServiceBusJmsProperties buildProperties(AzureServiceBusJmsProperties defaultProps, Map<String, String> overrides) {
        AzureServiceBusJmsProperties out = new AzureServiceBusJmsProperties();
        out.setEnabled(getOrDefault(overrides, "enabled", Boolean::parseBoolean, defaultProps.isEnabled()));
        out.setConnectionString(overrides.getOrDefault("connection-string", defaultProps.getConnectionString()));
        out.setTopicClientId(overrides.getOrDefault("topic-clientId", defaultProps.getTopicClientId()));
        out.setIdleTimeout(getOrDefault(overrides, "idle-timeout", Duration::parse, defaultProps.getIdleTimeout()));
        out.setPricingTier(overrides.getOrDefault("pricing-tier", defaultProps.getPricingTier()));
        copyPrefetchProperties(out, defaultProps, overrides);
        copyPoolProperties(out, defaultProps, overrides);

        try {
            out.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalArgumentException("Something gone wrong while configuring ServiceBus properties", e);
        }
        return out;
    }

    private <T> T getOrDefault(Map<String, String> props, String key, Function<String, T> valueReader,T defaultValue){
        String value = props.get(key);
        if(value==null){
            return defaultValue;
        } else {
            return valueReader.apply(value);
        }
    }

    private void copyPrefetchProperties(AzureServiceBusJmsProperties out, AzureServiceBusJmsProperties defaultProps, Map<String, String> overrides) {
        out.getPrefetchPolicy().setAll(getOrDefault(overrides, "all", Integer::parseInt, defaultProps.getPrefetchPolicy().getAll()));
        out.getPrefetchPolicy().setDurableTopicPrefetch(getOrDefault(overrides, "durable-topic-prefetch", Integer::parseInt, defaultProps.getPrefetchPolicy().getDurableTopicPrefetch()));
        out.getPrefetchPolicy().setQueueBrowserPrefetch(getOrDefault(overrides, "queue-browser-prefetch", Integer::parseInt, defaultProps.getPrefetchPolicy().getQueueBrowserPrefetch()));
        out.getPrefetchPolicy().setQueuePrefetch(getOrDefault(overrides, "queue-prefetch", Integer::parseInt, defaultProps.getPrefetchPolicy().getQueuePrefetch()));
        out.getPrefetchPolicy().setTopicPrefetch(getOrDefault(overrides, "topic-prefetch", Integer::parseInt, defaultProps.getPrefetchPolicy().getTopicPrefetch()));
    }

    private void copyPoolProperties(AzureServiceBusJmsProperties out, AzureServiceBusJmsProperties defaultProps, Map<String, String> overrides) {
        out.getPool().setEnabled(getOrDefault(overrides, "enabled", Boolean::parseBoolean, defaultProps.getPool().isEnabled()));
        out.getPool().setBlockIfFull(getOrDefault(overrides, "block-if-full", Boolean::parseBoolean, defaultProps.getPool().isBlockIfFull()));
        out.getPool().setBlockIfFullTimeout(getOrDefault(overrides, "block-if-full-timeout", Duration::parse, defaultProps.getPool().getBlockIfFullTimeout()));
        out.getPool().setIdleTimeout(getOrDefault(overrides, "idle-timeout", Duration::parse, defaultProps.getPool().getIdleTimeout()));
        out.getPool().setMaxConnections(getOrDefault(overrides, "max-connections", Integer::parseInt, defaultProps.getPool().getMaxConnections()));
        out.getPool().setMaxSessionsPerConnection(getOrDefault(overrides, "max-sessions-per-connection", Integer::parseInt, defaultProps.getPool().getMaxSessionsPerConnection()));
        out.getPool().setTimeBetweenExpirationCheck(getOrDefault(overrides, "time-between-expiration-check", Duration::parse, defaultProps.getPool().getTimeBetweenExpirationCheck()));
        out.getPool().setUseAnonymousProducers(getOrDefault(overrides, "use-anonymous-producers", Boolean::parseBoolean, defaultProps.getPool().isUseAnonymousProducers()));
    }
}
