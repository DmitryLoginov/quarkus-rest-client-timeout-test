package dev.ldv.timeout;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;

@QuarkusTest
@QuarkusTestResource(value = DelayedServiceWireMockResource.class, restrictToAnnotatedClass = true)
class ReadTimeoutTest {

    @Inject
    @RestClient
    DelayedServiceClient client;

    @Test
    @Timeout(5)
    void throwsProcessingExceptionWithVertxTimeoutCause() {
        ProcessingException thrown = assertThrows(ProcessingException.class, client::get);

        System.out.println("=== READ TIMEOUT EXCEPTION CHAIN ===");
        thrown.printStackTrace(System.out);

        assertTrue(ExceptionChain.hasCauseOfType(thrown, "io.vertx.core.impl.NoStackTraceTimeoutException"),
                () -> "Expected a Vert.x timeout cause, got:\n" + ExceptionChain.describe(thrown));
        assertTrue(ExceptionChain.describe(thrown).toLowerCase(Locale.ROOT).contains("timeout"),
                () -> "Expected timeout text, got:\n" + ExceptionChain.describe(thrown));
        DelayedServiceWireMockResource.verifyRequestReceived();
    }
}
