package it.gov.pagopa.idpay.error_recovery.config;

import it.gov.pagopa.idpay.error_recovery.BaseIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class HandledPublishersConfigTest extends BaseIntegrationTest {

    @Autowired
    private HandledPublishersConfig config;

    @Test
    void testKafkaConfig() {
        Assertions.assertEquals(
                Map.of(
                        "localhost:9092@idpay-onboarding-outcome", Map.of(
                                "bootstrap-servers", "localhost:9092",
                                "sasl-jaas-config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"ONBOARDING_OUTCOME\";"),

                        "localhost:9093@idpay-hpan-update", Map.of(
                                "bootstrap-servers", "localhost:9093",
                                "sasl-jaas-config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"HPAN_UPDATE\";"),
                        "localhost:9093@idpay-rule-update", Map.of(
                                "bootstrap-servers", "localhost:9093",
                                "sasl-jaas-config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"RULE_UPDATE\";"),
                        "localhost:9093@idpay-transaction", Map.of(
                                "bootstrap-servers", "localhost:9093",
                                "sasl-jaas-config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"TRANSACTION\";"),
                        "localhost:9093@idpay-transaction-user-id-splitter", Map.of(
                                "bootstrap-servers", "localhost:9093",
                                "sasl-jaas-config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"TRANSACTION_USER_ID_SPLITTER\";")
                ),
                config.getKafkaSrcKey2properties()
        );
    }

    @Test
    void testServiceBusConfig() {
        Assertions.assertEquals(
                Map.of(
                        "ServiceBusEndpoint@idpay-onboarding-request", Map.of(
                                "connection-string", "Endpoint=sb://ServiceBusEndpoint;SharedAccessKeyName=sharedAccessKeyName;SharedAccessKey=sharedAccessKey;EntityPath=entityPath")
                ),
                config.getServicebusSrcKey2properties()
        );
    }
}
