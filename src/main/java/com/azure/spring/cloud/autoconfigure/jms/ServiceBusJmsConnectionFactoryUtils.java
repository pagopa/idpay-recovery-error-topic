package com.azure.spring.cloud.autoconfigure.jms;

import com.azure.spring.cloud.autoconfigure.jms.properties.AzureServiceBusJmsProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jms.JmsProperties;

import javax.jms.ConnectionFactory;

public final class ServiceBusJmsConnectionFactoryUtils {
    private ServiceBusJmsConnectionFactoryUtils(){}

    private static final ServiceBusJmsConnectionFactoryConfiguration.SimpleConnectionFactoryConfiguration simpleConnectionFactoryConfiguration = new ServiceBusJmsConnectionFactoryConfiguration.SimpleConnectionFactoryConfiguration();
    private static final ServiceBusJmsConnectionFactoryConfiguration.SimpleConnectionFactoryConfiguration.CachingConnectionFactoryConfiguration cachingConnectionFactoryConfiguration = new ServiceBusJmsConnectionFactoryConfiguration.SimpleConnectionFactoryConfiguration.CachingConnectionFactoryConfiguration();
    private static final ServiceBusJmsConnectionFactoryConfiguration.PooledConnectionFactoryConfiguration pooledConnectionFactoryConfiguration = new ServiceBusJmsConnectionFactoryConfiguration.PooledConnectionFactoryConfiguration();

    public static ConnectionFactory createConnectionFactory(JmsProperties jmsProperties, AzureServiceBusJmsProperties properties, ObjectProvider<ServiceBusJmsConnectionFactoryCustomizer> factoryCustomizers) {
        if (!properties.getPool().isEnabled()) {
            if (!jmsProperties.getCache().isEnabled()) {
                return simpleConnectionFactoryConfiguration.jmsConnectionFactory(properties, factoryCustomizers);
            } else {
                return cachingConnectionFactoryConfiguration.jmsConnectionFactory(jmsProperties, properties, factoryCustomizers);
            }
        } else {
            return pooledConnectionFactoryConfiguration.jmsPoolConnectionFactory(properties, factoryCustomizers);
        }
    }
}
