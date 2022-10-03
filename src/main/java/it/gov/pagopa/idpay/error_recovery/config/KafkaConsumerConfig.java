package it.gov.pagopa.idpay.error_recovery.config;

import it.gov.pagopa.idpay.error_recovery.consumer.ErrorMessagesListener;
import it.gov.pagopa.idpay.error_recovery.service.ErrorMessageMediatorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {


    private ErrorMessagesListener listener;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, String> container(
            @Value("${errorListener.topic}") String errorMessageTopic,
            @Value("${errorListener.idleInterval.minSeconds}") long pauseLength,
            ConcurrentKafkaListenerContainerFactory<String, String> factory,
            ErrorMessageMediatorService errorMessageMediatorService) {
        ConcurrentMessageListenerContainer<String, String> container = factory.createContainer(errorMessageTopic);

        listener = new ErrorMessagesListener(pauseLength, container, errorMessageMediatorService);

        container.getContainerProperties().setMessageListener(listener);
        return container;
    }

    @Scheduled(cron = "${errorListener.idleInterval.scheduleCheck}")
    public void scheduledResume(){
        if(listener!=null){
            listener.scheduledResume();
        }
    }
}
