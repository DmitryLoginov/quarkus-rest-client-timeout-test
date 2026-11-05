package dev.ldv.timeout;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/delayed")
@RegisterRestClient(configKey = "delayed-service")
public interface DelayedServiceClient {

    @GET
    String get();
}
