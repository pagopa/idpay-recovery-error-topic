package it.gov.pagopa.idpay.error_recovery.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandledPublishersConfigTest {

    private HandledPublishersConfig config;

    @BeforeEach
    void setUp() throws Exception {
        config = new HandledPublishersConfig();

        // set defaultClientId (@Value field)
        setField(config, "defaultClientId", "test-client");

        // mock kafka properties
        config.setKafka(Map.of(
                "evh1.properties.bootstrap.servers", "localhost:9092",
                "evh1.destination.topicA", "someValue"
        ));

        // mock servicebus properties
        config.setServicebus(Map.of(
                "sb1.properties.connection-string", "Endpoint=sb://myservicebus.servicebus.windows.net/;SharedAccessKeyName=key;",
                "sb1.destination.queueA", "queueValue"
        ));

        // call @PostConstruct init()
        invokeMethod(config, "init");
    }

    @Test
    void shouldBuildKafkaPublisherProperties() {
        Map<String, Object> props =
                config.getKafkaPublisherProperties("localhost:9092", "topicA");

        assertNotNull(props);
        assertEquals("localhost:9092", props.get("bootstrap.servers"));
        assertEquals("test-client-topicA", props.get("client.id"));
    }

    @Test
    void shouldBuildServiceBusPublisherProperties() {
        Map<String, String> props =
                config.getServiceBusPublisherProperties("myservicebus.servicebus.windows.net/", "queueA");

        assertNotNull(props);
    }

    @Test
    void shouldExtractServerFromConnectionString() throws Exception {
        Method m = HandledPublishersConfig.class
                .getDeclaredMethod("extractServerFromServiceBusConnectionString", String.class);
        m.setAccessible(true);

        String result = (String) m.invoke(config,
                "Endpoint=sb://example.servicebus.windows.net/;SharedAccessKeyName=key;");

        assertEquals("example.servicebus.windows.net/", result);
    }

    @Test
    void shouldReturnNullServerWhenConnectionStringEmpty() throws Exception {
        Method m = HandledPublishersConfig.class
                .getDeclaredMethod("extractServerFromServiceBusConnectionString", String.class);
        m.setAccessible(true);

        String result = (String) m.invoke(config, "");

        assertNull(result);
    }

    // -------- helpers --------

    private static void setField(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void invokeMethod(Object target, String method) throws Exception {
        Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(target);
    }
}