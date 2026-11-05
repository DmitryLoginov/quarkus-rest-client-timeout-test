package dev.ldv.timeout;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/** Points the REST client at an address that is expected to drop TCP SYN packets. */
public final class ConnectTimeoutProfile implements QuarkusTestProfile {

    private static final String DEFAULT_BLACK_HOLE_HOST = "192.0.2.1";

    @Override
    public Map<String, String> getConfigOverrides() {
        String host = System.getProperty("timeout.blackhole.host", DEFAULT_BLACK_HOLE_HOST);

        return Map.of(
                "quarkus.rest-client.delayed-service.url", "http://" + host + ":81",
                "quarkus.rest-client.delayed-service.connect-timeout", "250",
                "quarkus.rest-client.delayed-service.read-timeout", "1500");
    }
}
