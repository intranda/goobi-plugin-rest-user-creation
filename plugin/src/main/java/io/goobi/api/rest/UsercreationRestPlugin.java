package io.goobi.api.rest;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.auth0.jwt.interfaces.DecodedJWT;

import de.sub.goobi.helper.JwtHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
@javax.ws.rs.Path("/users")
public class UsercreationRestPlugin {

    /**
     * This sample method to allow the upload of JSON data via POST
     * 
     * Sample call:
     * curl --location --request POST 'http://localhost:8080/goobi/api/intern/sample' \
     *   --header 'Content-Type: application/json' \
     *   --data-raw '{
     *       "result": "success",
     *       "message": "This is a sample message",
     *       "processName": "Sample name",
     *       "processId": 45
     *   }'
     * 
     * @return XML as result
     */
    @javax.ws.rs.Path("/email/{token}")
    @GET
    @Produces("text/xml")
    public Response execute(@PathParam("token") String token) {
        try {
            DecodedJWT jwt =   JwtHelper.verifyTokenAndReturnClaims(token);

            String userId = jwt.getClaim("id").asString();
            String accountName = jwt.getClaim("user").asString();

            // your functionality comes here
            System.out.println(userId);
            System.out.println(accountName);

            // log user in
            // forward to institution creation screen

        } catch (Exception e) {
            log.error("An error occured while executing a REST call.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * A sample method to request information as json for a specific process. This is just a kickstart method without any real functionality
     * 
     * Sample call:
     * curl --location --request GET 'http://localhost:8080/goobi/api/intern/status/45'
     * 
     * @param processId identifier of a process
     * @return JSON object with status information
     */
    @javax.ws.rs.Path("status/{processId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SampleResponse getProcessStatusAsJson(@PathParam("processId") int processId) {
        SampleResponse sr = new SampleResponse();
        sr.setMessage("This is a sample message");
        sr.setProcessId(processId);
        sr.setProcessName("Sample name");
        sr.setResult("success");
        return sr;
    }
}
