package it.gov.pagopa.idpay.error_recovery.config;

import it.gov.pagopa.idpay.error_recovery.consumer.ErrorMessagesListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, String> container(
            @Value("${errorListener.topic}") String errorMessageTopic,
            ConcurrentKafkaListenerContainerFactory<String, String> factory,
            ErrorMessagesListener listener) {
        ConcurrentMessageListenerContainer<String, String> container = factory.createContainer(errorMessageTopic);
        container.getContainerProperties().setMessageListener(listener);
        return container;
    }
}
