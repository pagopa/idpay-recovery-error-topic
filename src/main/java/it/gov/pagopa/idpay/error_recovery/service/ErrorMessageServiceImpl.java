package it.gov.pagopa.idpay.error_recovery.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Service
public class ErrorMessageServiceImpl implements ErrorMessageService {
    @Override
    public void accept(ConsumerRecord<String, String> message) {
        System.out.println("TODO" + message.value());
    }
}
