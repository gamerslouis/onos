package org.onosproject.netconfapi;

import org.onosproject.netconf.NetconfController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onosproject.rest.AbstractWebResource;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfDevice;

import org.onosproject.net.DeviceId;

import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manage Netconf Device Configuration
 */
@Path("/")
public class CallhomeWebResources extends AbstractWebResource {

    private static final String JSON_INVALID = "Invalid JSON input";
    private final Logger log = getLogger(getClass());
    private final ObjectNode root = mapper().createObjectNode();

    private final NetconfController nfc = get(NetconfController.class);
    

    /**
     * Perform a operation onto specified device.
     *
     * @param device   device identifier
     * @param stream   operation JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid,
     * INTERNAL_SERVER_ERROR if rpc failed
     * @onos.rsModel NetconfApi
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("data/{device}")
    public void createData(@PathParam("device") String device, InputStream stream, @Suspended AsyncResponse response) {

        try {
            ObjectNode theJsonContent = readTreeFromStream(mapper(), stream);

            this.validateJson(theJsonContent);

            log.debug("Received data: {}", theJsonContent);
            log.debug("Target Device: {}", device);

            NetconfDevice nfd = nfc.getDevicesMap().get(DeviceId.deviceId(device));

            if (nfd != null) {
                CompletableFuture<String> operation = nfd.getSession().rpc(theJsonContent.path("content").asText());

                String result = operation.join();
            
                response.resume(Response.status(OK).entity(result).build());
            }
            else {
                log.error("Target Device: {} not found.", device);
                response.resume(Response.status(NOT_FOUND).entity("Target device not found.").build());
            }
        } catch (NetconfException e)  {
            log.error("Error at netconf session.");
            response.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build());
        } catch (IllegalArgumentException | IOException e) {
            log.error("Failed to parse JSON.");
            response.resume(Response.status(BAD_REQUEST).entity(e).build());
        }
    }

    private void validateJson(ObjectNode input) {
        JsonNode content = input.path("content");

        if (content == null || content.asText() == "") {
            throw new IllegalArgumentException(JSON_INVALID);
        }
    }
}
