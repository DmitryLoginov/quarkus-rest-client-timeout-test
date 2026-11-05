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

        if (!ExceptionChain.hasCauseNamed(thrown, "ConnectTimeoutException")) {
            fail("The network rejected the target immediately instead of dropping the TCP connection. "
                    + "Rerun with -Dtimeout.blackhole.host=<dropping-address>. Observed:\n"
                    + ExceptionChain.describe(thrown));
        }
        assertTrue(ExceptionChain.describe(thrown).contains("connection timed out after 250 ms"),
                () -> "Expected the configured 250 ms connect timeout, got:\n" + ExceptionChain.describe(thrown));
    }
}
