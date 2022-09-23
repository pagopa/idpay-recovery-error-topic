package it.gov.pagopa.idpay.error_recovery.consumer;

import it.gov.pagopa.idpay.error_recovery.service.ErrorMessageMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.listener.BatchAcknowledgingConsumerAwareMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class ErrorMessagesListener implements BatchAcknowledgingConsumerAwareMessageListener<String, String> {

    private final Long pauseLength;

    private Consumer<?, ?> consumer;
    private final ErrorMessageMediatorService errorMessageMediatorService;

    private LocalDateTime paused=LocalDateTime.now();


    public ErrorMessagesListener(
            @Value("${errorListener.idleInterval.minSeconds}") Long pauseLength,
            ErrorMessageMediatorService errorMessageMediatorService) {
        this.pauseLength = pauseLength;
        this.errorMessageMediatorService = errorMessageMediatorService;
    }

    @Scheduled(cron = "${errorListener.idleInterval.scheduleCheck}")
    public void scheduledResume(){
        if( paused.until(LocalDateTime.now(), ChronoUnit.SECONDS)>= pauseLength && consumer != null){
            consumer.resume(consumer.assignment());
        }
    }

    @Override
    public void onMessage(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment, Consumer<?, ?> consumer) {
        if(pauseLength>0){
            paused = LocalDateTime.now();
            consumer.pause(consumer.assignment());
            this.consumer=consumer;
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
