package it.gov.pagopa.idpay.error_recovery.service;

import it.gov.pagopa.idpay.error_recovery.config.HandledPublishersConfig;
import it.gov.pagopa.idpay.error_recovery.producer.Publisher;
import it.gov.pagopa.idpay.error_recovery.utils.Constants;
import it.gov.pagopa.idpay.error_recovery.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PublisherRetrieverServiceImpl implements PublisherRetrieverService {

    private Map<String, Publisher> publishers=new HashMap<>();

    @Override
    public Publisher retrievePublisher(Headers headers) {
        String srcType = Optional.ofNullable(Utils.readHeaderValue(headers, Constants.ERROR_MSG_HEADER_SRC_TYPE)).orElse("kafka");
        String srcServer = Utils.readHeaderValue(headers, Constants.ERROR_MSG_HEADER_SRC_SERVER);
        String srcTopic = Utils.readHeaderValue(headers, Constants.ERROR_MSG_HEADER_SRC_TOPIC);

        if(StringUtils.hasText(srcServer) && StringUtils.hasText(srcTopic)){
            return publishers.computeIfAbsent(HandledPublishersConfig.buildSrcKey(srcServer, srcTopic), key -> {
                return null; // TODO build publisher
            });
        } else {
            return null;
        }
    }
}
