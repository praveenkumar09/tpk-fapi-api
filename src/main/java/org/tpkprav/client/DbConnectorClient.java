package org.tpkprav.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.tpkprav.client.dto.StoreRequest;
import org.tpkprav.client.dto.StoreResponse;

@RegisterRestClient(configKey = "db-connector")
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DbConnectorClient {

    @POST
    @Path("/credentials")
    StoreResponse store(StoreRequest request);
}
