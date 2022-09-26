package it.gov.pagopa.idpay.error_recovery.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
@PropertySource(value = "classpath:config/publishers.properties")
@ConfigurationProperties(prefix = "handled-publishers")
public class HandledPublishersConfig {
    @Setter
    private Map<String, String> kafka;
    @Setter
    private Map<String, String> servicebus;

    @Getter
    private Map<String, Map<String, String>> kafkaSrcKey2properties;
    @Getter
    private Map<String, Map<String, String>> servicebusSrcKey2properties;

    public static String buildSrcKey(String srcServer, String srcTopic) {
        return "%s@%s".formatted(srcServer, srcTopic);
    }

    @PostConstruct
    void init() {
        kafkaSrcKey2properties = buildSrcKey2Properties(kafka, sProps->sProps.get("bootstrap-servers"));
        servicebusSrcKey2properties = buildSrcKey2Properties(servicebus, sProps->extractServerFromServiceBusConnectionString(sProps.get("connection-string")));
    }

    private Map<String, Map<String, String>> buildSrcKey2Properties(Map<String, String> props, Function<Map<String, String>, String> serverProps2ServerHost) {
        Map<String, Map<String, String>> serverProperties = new HashMap<>();
        Map<String, Map<String, String>> destinationProperties = new HashMap<>();

        String tmpKeyServerName = "serverName";
        String tmpKeyServerHost = "serverHost";
        String tmpKeyDestinationName = "destinationName";
        props.forEach((key, value) -> {
            String[] propSplitted = key.split("\\.");

            String serverName = propSplitted[0];
            switch (propSplitted[1]) {
                case "properties" -> readProperty(serverProperties, serverName, propSplitted, 2, value);
                case "destination" -> {
                    String destinationName = propSplitted[2];
                    serverProperties.putIfAbsent(serverName, new HashMap<>());
                    Map<String, String> destinationProps = readProperty(destinationProperties, buildSrcKey(serverName, destinationName), propSplitted, 3, value);
                    destinationProps.put(tmpKeyServerName, serverName);
                    destinationProps.put(tmpKeyDestinationName, destinationName);
                }
                default -> throw new IllegalArgumentException("Invalid prop: %s".formatted(key));
            }
        });

        serverProperties.forEach((serverName, serverProps) -> {
                    String serverHost = serverProps2ServerHost.apply(serverProps);
                    if (!StringUtils.hasText(serverHost)) {
                        throw new IllegalStateException("server address not configured for server name: %s".formatted(serverName));
                    }
                    serverProps.put(tmpKeyServerHost, serverHost);
                }
        );

        return destinationProperties.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> {
                            String serverName = e.getValue().get(tmpKeyServerName);
                            String serverHost = serverProperties.get(serverName).get(tmpKeyServerHost);
                            String destination = e.getValue().get(tmpKeyDestinationName);
                            return buildSrcKey(serverHost, destination);
                        },
                        e -> {
                            String serverName = e.getValue().get(tmpKeyServerName);
                            Map<String,String> dProps = new HashMap<>(serverProperties.get(serverName));
                            dProps.putAll(e.getValue());
                            dProps.remove(tmpKeyServerName);
                            dProps.remove(tmpKeyServerHost);
                            dProps.remove(tmpKeyDestinationName);
                            return dProps;
                        }
                ));
    }

    private static Map<String, String> readProperty(Map<String, Map<String, String>> propertiesMap, String key, String[] propSplitted, int skip, String value) {
        return propertiesMap.compute(key, (sName, sProps) -> {
            if (sProps == null) {
                sProps = new HashMap<>();
            }
            String propKey = Arrays.stream(propSplitted).skip(skip).collect(Collectors.joining("."));
            sProps.put(propKey, value);
            return sProps;
        });
    }

    private final Pattern serviceBusEndpointPattern = Pattern.compile("Endpoint=sb://([^;]+)/?;");
    private String extractServerFromServiceBusConnectionString(String connectionString) {
        if(!StringUtils.hasText(connectionString)){
            return null;
        }
        final Matcher matcher = serviceBusEndpointPattern.matcher(connectionString);
        return matcher.find() ? matcher.group(1) : "ServiceBus";
    }
}
