package dev.ldv.timeout;

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
        ProcessingException thrown = invokeClientExpectingProcessingException();

        System.out.println("=== CONNECT TIMEOUT EXCEPTION CHAIN ===");
        thrown.printStackTrace(System.out);

        if (!ExceptionChain.hasCauseOfType(thrown, "io.netty.channel.ConnectTimeoutException")) {
            fail("The target did not produce a TCP connect timeout; the network may have rejected, accepted, "
                    + "or intercepted the connection. "
                    + "Rerun with -Dtimeout.blackhole.host=<dropping-address>. Observed:\n"
                    + ExceptionChain.describe(thrown));
        }
        assertTrue(ExceptionChain.describe(thrown).contains("connection timed out after 250 ms"),
                () -> "Expected the configured 250 ms connect timeout, got:\n" + ExceptionChain.describe(thrown));
    }

    private ProcessingException invokeClientExpectingProcessingException() {
        try {
            String response = client.get();
            return fail("The target accepted or intercepted the connection and returned a response instead of "
                    + "producing a TCP connect timeout. Rerun with "
                    + "-Dtimeout.blackhole.host=<dropping-address>. Response: " + response);
        } catch (ProcessingException exception) {
            return exception;
        } catch (RuntimeException exception) {
            return fail("The target did not produce the expected REST Client ProcessingException. Rerun with "
                    + "-Dtimeout.blackhole.host=<dropping-address>. Observed:\n"
                    + ExceptionChain.describe(exception), exception);
        }
    }
}
