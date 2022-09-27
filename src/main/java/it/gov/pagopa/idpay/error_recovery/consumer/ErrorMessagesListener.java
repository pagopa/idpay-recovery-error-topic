package it.gov.pagopa.idpay.error_recovery.consumer;

import it.gov.pagopa.idpay.error_recovery.service.ErrorMessageMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
public class ErrorMessagesListener implements BatchAcknowledgingMessageListener<String, String> {

    private final Long pauseLength;
    private final ConcurrentMessageListenerContainer<String, String> container;

    private final ErrorMessageMediatorService errorMessageMediatorService;

    private LocalDateTime paused=LocalDateTime.now();

    public ErrorMessagesListener(
            Long pauseLength,
            ConcurrentMessageListenerContainer<String, String> container,
            ErrorMessageMediatorService errorMessageMediatorService) {
        this.pauseLength = pauseLength;
        this.container = container;
        this.errorMessageMediatorService = errorMessageMediatorService;
    }

    public void scheduledResume(){
        if( paused.until(LocalDateTime.now(), ChronoUnit.SECONDS)>= pauseLength){
            container.resume();
        }
    }

    @Override
    public void onMessage(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        if(pauseLength>0){
            paused = LocalDateTime.now();
            container.pause();
        }

        for (ConsumerRecord<String, String> r : records) {
            try {
                errorMessageMediatorService.accept(r);
            } catch (Exception e) {
                log.error(String.format("Something gone wrong during the evaluation of the payload:%n%s", r.value()), e);
            }
        }

        if(acknowledgment!=null){
            acknowledgment.acknowledge();
        }
    }
}
