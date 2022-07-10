package org.onosproject.netconf.callhome.restapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.glassfish.jersey.server.ChunkedOutput;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.restconf.api.MediaTypeRestconf;

import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
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
import java.util.concurrent.CompletableFuture;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.slf4j.LoggerFactory.getLogger;


@Path("/restnetconf")
public class RestAPIManager extends AbstractWebResource {

    private static final String JSON_INVALID = "Invalid JSON input";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(RestAPIManager.class);


    /**
     * Perform a operation to specified device.
     *
     * @param device   device identifier
     * @param stream   operation JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel Operation
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("data/{device}")
    public Response createData(@PathParam("device") String device, InputStream stream) {
        try {
            ObjectNode json = readTreeFromStream(stream);
            log.info("Received data: {}", json);
            this.validateJson();
            return Response.status(OK).build();
        } catch (IOException e) {
            log.error("Failed to parse JSON", e);
            return Response.status(BAD_REQUEST).build();
        }
    }



    private Boolean validateJson(ObjectNode input) {
        JsonNode content = intput.path("content");

        if (content == null || content.asText() == "") {
            throw new IllegalArgumentException(JSON_INVALID);
        }
    }
}
