package dev.ldv.timeout;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public final class DelayedServiceWireMockResource implements QuarkusTestResourceLifecycleManager {

    private static WireMockServer wireMock;

    @Override
    public Map<String, String> start() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        wireMock.stubFor(get(urlEqualTo("/delayed"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("too late")
                        .withFixedDelay(1_000)));

        return Map.of("quarkus.rest-client.delayed-service.url", wireMock.baseUrl());
    }

    static void verifyRequestReceived() {
        wireMock.verify(1, getRequestedFor(urlEqualTo("/delayed")));
    }

    @Override
    public void stop() {
        if (wireMock != null) {
            wireMock.stop();
            wireMock = null;
        }
    }
}
