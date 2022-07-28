package org.onosproject.netconfapi;

import org.onosproject.netconf.NetconfController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onosproject.rest.AbstractWebResource;
import org.onosproject.netconf.NetconfDevice;

import org.onosproject.net.DeviceId;

import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manage Callhome Device Configuration
 */
@Path("/")
public class CallhomeWebResources extends AbstractWebResource {

    @Context
    private UriInfo uriInfo;

    private static final String JSON_INVALID = "Invalid JSON input";
    private final Logger log = getLogger(getClass());
    private final ObjectNode root = mapper().createObjectNode();

    //private final CallhomeRestconfService crs = get(CallhomeRestconfService.class);
    private final NetconfController nfc = get(NetconfController.class);
    

    /**
     * Perform a operation onto specified device.
     *
     * @param device   device identifier
     * @param stream   operation JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel Callhome
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("data/{device}")
    public Response createData(@PathParam("device") String device, InputStream stream) {

        try {
            ObjectNode json = readTreeFromStream(mapper(), stream);
            log.info("Received data: {}", json);
            log.info("Target Device: {}", device);
            this.validateJson(json);

            NetconfDevice nfd = nfc.getDevicesMap().get(DeviceId.deviceId(device));

            log.info(json.path("content").asText());

            //return Response.status(OK).build();

            if (nfd != null) {
                CompletableFuture<String> operation = nfd.getSession().rpc(json.path("content").asText());

                return operation.handle((response, error) -> {
                    if (error != null) {
                        return Response.status(BAD_REQUEST).build();
                    }
                    else {
                        return Response.status(OK).build();
                    }
                });

            }
            else {
                return Response.status(NOT_FOUND).build();
            }

        } catch (IOException e) {
            log.error("Failed to parse JSON", e);
            return Response.status(BAD_REQUEST).build();
        }
    }



    private void validateJson(ObjectNode input) {
        JsonNode content = input.path("content");

        if (content == null || content.asText() == "") {
            throw new IllegalArgumentException(JSON_INVALID);
        }
    }
}
