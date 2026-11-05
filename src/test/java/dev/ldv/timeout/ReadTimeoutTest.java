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

        assertTrue(hasCauseNamed(thrown, "NoStackTraceTimeoutException"),
                () -> "Expected a Vert.x timeout cause, got:\n" + exceptionChain(thrown));
        assertTrue(exceptionChain(thrown).toLowerCase(Locale.ROOT).contains("timeout"),
                () -> "Expected timeout text, got:\n" + exceptionChain(thrown));
        DelayedServiceWireMockResource.verifyRequestReceived();
    }

    private static boolean hasCauseNamed(Throwable throwable, String simpleName) {
        return throwable != null
                && (throwable.getClass().getSimpleName().equals(simpleName)
                        || hasCauseNamed(throwable.getCause(), simpleName));
    }

    private static String exceptionChain(Throwable throwable) {
        StringBuilder result = new StringBuilder();
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (!result.isEmpty()) {
                result.append(System.lineSeparator()).append("caused by: ");
            }
            result.append(current.getClass().getName()).append(": ").append(current.getMessage());
        }
        return result.toString();
    }
}
