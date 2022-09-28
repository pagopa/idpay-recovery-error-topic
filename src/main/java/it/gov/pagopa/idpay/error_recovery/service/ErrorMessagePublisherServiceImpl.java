package it.gov.pagopa.idpay.error_recovery.service;

import it.gov.pagopa.idpay.error_recovery.producer.Publisher;
import it.gov.pagopa.idpay.error_recovery.utils.Constants;
import it.gov.pagopa.idpay.error_recovery.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class ErrorMessagePublisherServiceImpl implements ErrorMessagePublisherService {

    private final long maxRetry;

    public ErrorMessagePublisherServiceImpl(@Value("${app.retry.max-retry}") long maxRetry) {
        this.maxRetry = maxRetry;
    }

    @Override
    public void publish(Headers headers, String key, String payload, Publisher publisher) {
        long retry = getNextRetry(headers);
        if (maxRetry <= 0 || retry <= maxRetry) {
            log.info("[ERROR_MESSAGE_HANDLER] Resubmitting message: {}; {}", Utils.toString(headers), payload);
            cleanHeaders(headers);
            headers.add(Constants.ERROR_MSG_HEADER_RETRY, (retry + "").getBytes(StandardCharsets.UTF_8));
            publisher.send(buildMessage(headers, key, payload));
        } else {
            log.info("[ERROR_MESSAGE_HANDLER] Max retry reached for message: {}; {}", Utils.toString(headers), payload);
        }
    }

    private static long getNextRetry(Headers headers) {
        String retryStr = Utils.readHeaderValue(headers, Constants.ERROR_MSG_HEADER_RETRY);
        if (retryStr != null) {
            try {
                return 1 + Long.parseLong(retryStr);
            } catch (Exception e) {
                log.info("[ERROR_MESSAGE_HANDLER] RETRY header not usable: {}", retryStr);
            }
        }
        return 1;
    }

    private static void cleanHeaders(Headers headers) {
        headers.remove(Constants.ERROR_MSG_HEADER_SRC_TYPE);
        headers.remove(Constants.ERROR_MSG_HEADER_SRC_SERVER);
        headers.remove(Constants.ERROR_MSG_HEADER_SRC_TOPIC);
        headers.remove(Constants.ERROR_MSG_HEADER_DESCRIPTION);
        headers.remove(Constants.ERROR_MSG_HEADER_RETRYABLE);
        headers.remove(Constants.ERROR_MSG_HEADER_STACKTRACE);
        headers.remove(Constants.ERROR_MSG_HEADER_RETRY);
    }

    private static Message<String> buildMessage(Headers headers, String key, String payload) {
        Map<String, Object> headersMap = StreamSupport.stream(headers.spliterator(), false)
                .collect(Collectors.toMap(Header::key, Header::value));
        if(key!=null){
            headersMap.put(KafkaHeaders.MESSAGE_KEY, key);
        }
        return MessageBuilder.createMessage(payload,
                new MessageHeaders(headersMap));
    }
}
