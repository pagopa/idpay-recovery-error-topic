package it.gov.pagopa.idpay.error_recovery.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

@Configuration
@PropertySource(value = "classpath:config/publishers.properties")
@ConfigurationProperties(prefix = "handled-publishers")
public class HandledPublishersConfig {
    @Setter
    private Map<String, String> kafka;
    @Setter
    private Map<String, String> servicebus;

    private Map<String, Map<String, String>> kafkaSrcKey2properties=new HashMap<>();
    private Map<String, Map<String, String>> servicebusSrcKey2properties=new HashMap<>();

    public static String buildSrcKey(String srcServer, String srcTopic) {
        return MessageFormat.format("{0}@{1}", srcServer, srcTopic);
    }

    @PostConstruct
    void init(){
        // TODO fill maps
    }
}
