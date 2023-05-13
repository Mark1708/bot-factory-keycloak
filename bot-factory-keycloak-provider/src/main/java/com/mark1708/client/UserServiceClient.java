package com.mark1708.client;

import com.mark1708.model.CredentialData;
import com.mark1708.model.UserData;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface UserServiceClient {

  @GET
  @Path("/search")
  List<UserData> getAllSearchedUsers(
      @QueryParam("search") String search,
      @QueryParam("offset") int offset,
      @QueryParam("limit") int limit
  );

  @GET
  @Path("/count")
  Integer getUsersCount();

  @GET
  @Path("/{query}")
  UserData getUser(
      @PathParam("query") String query,
      @QueryParam("type") String type
  );

  @GET
  @Path("/{id}/credentials")
  CredentialData getCredentialData(@PathParam("id") String id);

  @PUT
  @Path("/{id}/credentials")
  Response updateCredentialData(@PathParam("id") String id, CredentialData credentialData);
}
