package it.gov.pagopa.idpay.error_recovery.service;

import it.gov.pagopa.idpay.error_recovery.producer.Publisher;
import it.gov.pagopa.idpay.error_recovery.utils.Constants;
import it.gov.pagopa.idpay.error_recovery.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ErrorMessageMediatorServiceImpl implements ErrorMessageMediatorService {

    private final PublisherRetrieverService publisherRetrieverService;
    private final ErrorMessagePublisherService errorMessagePublisherService;

    public ErrorMessageMediatorServiceImpl(PublisherRetrieverService publisherRetrieverService, ErrorMessagePublisherService errorMessagePublisherService) {
        this.publisherRetrieverService = publisherRetrieverService;
        this.errorMessagePublisherService = errorMessagePublisherService;
    }

    @Override
    public void accept(ConsumerRecord<String, String> message) {
        Headers headers = message.headers();
        String payload = message.value();

        String retriable = Utils.readHeaderValue(headers, Constants.ERROR_MSG_HEADER_RETRYABLE);
        if(retriable != null && !"true".equalsIgnoreCase(retriable)){
            log.info("[ERROR_MESSAGE_HANDLER] message configured as not retriable: {}; {}", Utils.toString(headers), payload);
        } else {
            Publisher publisher = publisherRetrieverService.retrievePublisher(headers);

            if (publisher != null) {
                errorMessagePublisherService.publish(headers, payload, publisher);
            } else {
                log.info("[ERROR_MESSAGE_HANDLER] srcType/srcServer/srcTopic not configured! {}; {}", Utils.toString(headers), payload);
            }
        }
    }
}
