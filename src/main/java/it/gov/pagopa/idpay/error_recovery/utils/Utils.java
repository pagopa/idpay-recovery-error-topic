package it.gov.pagopa.idpay.error_recovery.utils;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class Utils {
    private Utils(){}

    /** To build a String with all the {@link Header}'s values */
    public static String toString(Headers headers) {
        if (headers != null) {
            return "Headers[%s]".formatted(
                    StreamSupport.stream(headers.spliterator(), false)
                            .map(h -> "%s=%s".formatted(h.key(), readHeaderValue(h)))
                            .collect(Collectors.joining(","))
            );
        } else {
            return "null";
        }
    }

    /** To read an {@link Header} value if exists from {@link Headers} */
    public static String readHeaderValue(Headers headers, String headerName) {
        Header h = headers.lastHeader(headerName);
        if (h != null) {
            return readHeaderValue(h);
        } else {
            return null;
        }
    }

    /** To read an {@link Header} value if exists */
    public static String readHeaderValue(Header h) {
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
