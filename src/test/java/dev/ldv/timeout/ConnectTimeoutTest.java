package dev.ldv.timeout;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;

@QuarkusTest
@TestProfile(ConnectTimeoutProfile.class)
class ConnectTimeoutTest {

    @Inject
    @RestClient
    DelayedServiceClient client;

    @Test
    @Timeout(5)
    void throwsProcessingExceptionWithNettyConnectTimeoutCause() {
        ProcessingException thrown = assertThrows(ProcessingException.class, client::get);

        System.out.println("=== CONNECT TIMEOUT EXCEPTION CHAIN ===");
        thrown.printStackTrace(System.out);

        if (!hasCauseNamed(thrown, "ConnectTimeoutException")) {
            fail("The network rejected the target immediately instead of dropping the TCP connection. "
                    + "Rerun with -Dtimeout.blackhole.host=<dropping-address>. Observed:\n"
                    + exceptionChain(thrown));
        }
        assertTrue(exceptionChain(thrown).contains("connection timed out after 250 ms"),
                () -> "Expected the configured 250 ms connect timeout, got:\n" + exceptionChain(thrown));
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
