package it.gov.pagopa.idpay.error_recovery.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

@Slf4j
public class KafkaPublisher implements Publisher{

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(Message<String> message) {
        kafkaTemplate.send(message)
                .thenAccept(r -> log.debug("[ERROR_MESSAGE_HANDLER] message successfully sent to {}", kafkaTemplate.getDefaultTopic()))
                .exceptionally(e -> {
                    log.error("[ERROR_MESSAGE_HANDLER] something gone wrong while sending message towards topic {}", kafkaTemplate.getDefaultTopic(), e);
                    return null;
                    }
                );
    }
}
