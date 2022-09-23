package it.gov.pagopa.idpay.error_recovery.consumer;

import it.gov.pagopa.idpay.error_recovery.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class ErrorMessagesListenerTest extends BaseIntegrationTest {

    @Test
    void test(){
        publishIntoEmbeddedKafka(topicErrors, null, null, "PROVA");
        wait(1000L, TimeUnit.MILLISECONDS);
    }
}
