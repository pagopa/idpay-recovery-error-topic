package it.gov.pagopa.idpay.error_recovery.config;

import it.gov.pagopa.idpay.error_recovery.BaseIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

@TestPropertySource(
        properties = {
                "handled-publishers.kafka.idpay-evh-ns-00.properties.bootstrap.servers=localhost:9092",
                "handled-publishers.kafka.idpay-evh-ns-01.properties.bootstrap.servers=localhost:9093",

                "handled-publishers.kafka.idpay-evh-ns-01.destination.idpay-transaction-user-id-splitter.client.id=OVERRIDDEN_CLIENT_ID"
        })
class HandledPublishersConfigTest extends BaseIntegrationTest {

    @Autowired
    private HandledPublishersConfig config;

    @Test
    void testKafkaConfig() {
        Assertions.assertEquals(Map.of(
                        "bootstrap.servers", "localhost:9092",
                        "client.id", "idpay-errors-recovery-idpay-onboarding-outcome",
                        "sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"ONBOARDING_OUTCOME\";"),
                config.getKafkaPublisherProperties("localhost:9092", "idpay-onboarding-outcome"));

        Assertions.assertEquals(Map.of(
                        "bootstrap.servers", "localhost:9093",
                        "client.id", "idpay-errors-recovery-idpay-hpan-update",
                        "sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"HPAN_UPDATE\";"),
                config.getKafkaPublisherProperties("localhost:9093", "idpay-hpan-update"));

        Assertions.assertEquals(Map.of(
                        "bootstrap.servers", "localhost:9093",
                        "client.id", "idpay-errors-recovery-idpay-rule-update",
                        "sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"RULE_UPDATE\";"),
                config.getKafkaPublisherProperties("localhost:9093", "idpay-rule-update"));

        Assertions.assertEquals(Map.of(
                        "bootstrap.servers", "localhost:9093",
                        "client.id", "idpay-errors-recovery-idpay-transaction",
                        "sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"TRANSACTION\";"),
                config.getKafkaPublisherProperties("localhost:9093", "idpay-transaction"));

        Assertions.assertEquals(Map.of(
                        "bootstrap.servers", "localhost:9093",
                        "client.id", "OVERRIDDEN_CLIENT_ID",
                        "sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"TRANSACTION_USER_ID_SPLITTER\";")
                , config.getKafkaPublisherProperties("localhost:9093", "idpay-transaction-user-id-splitter"));


        Assertions.assertNull(config.getKafkaPublisherProperties("DUMMY", "idpay-transaction-user-id-splitter"));
        Assertions.assertNull(config.getKafkaPublisherProperties("localhost:9093", "DUMMY"));
    }

    @Test
    void testServiceBusConfig() {
        Assertions.assertEquals(
                Map.of(
                        "connection-string", "Endpoint=sb://ServiceBusEndpoint;SharedAccessKeyName=sharedAccessKeyName;SharedAccessKey=sharedAccessKey;EntityPath=entityPath",
                        "topic-client-id", "idpay-errors-recovery-idpay-onboarding-request"),
                config.getServiceBusPublisherProperties("ServiceBusEndpoint", "idpay-onboarding-request"));

        Assertions.assertNull(config.getKafkaPublisherProperties("DUMMY", "idpay-onboarding-request"));
        Assertions.assertNull(config.getKafkaPublisherProperties("ServiceBusEndpoint", "DUMMY"));
    }
}
