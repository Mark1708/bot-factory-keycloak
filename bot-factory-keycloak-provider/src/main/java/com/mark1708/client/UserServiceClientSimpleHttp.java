package com.mark1708.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mark1708.model.CredentialData;
import com.mark1708.model.UserData;
import com.mark1708.properties.UserClientProperties;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

@Slf4j
public class UserServiceClientSimpleHttp implements UserServiceClient {

  private final String baseUrl;
  private final String basicUsername;
  private final String basicPassword;
  private final ObjectMapper objectMapper;
  private final CloseableHttpClient httpClient;
  private final UserClientProperties userClientProperties;

  public UserServiceClientSimpleHttp(KeycloakSession session, ComponentModel model) {
    this.userClientProperties = new UserClientProperties();
    this.httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
    this.baseUrl = model.get(userClientProperties.getBaseUrl());
    this.basicUsername = model.get(userClientProperties.getBasicUsername());
    this.basicPassword = model.get(userClientProperties.getBasicPassword());
    this.objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Override
  @SneakyThrows
  public List<UserData> getAllSearchedUsers(String search, int offset, int limit) {
    log.info("Search users [{}, {}]", search, offset, limit);
    String url = String.format("%s/search", baseUrl);
    SimpleHttp simpleHttp = SimpleHttp
        .doGet(url, httpClient)
        .authBasic(basicUsername, basicPassword)
        .param("offset", String.valueOf(offset))
        .param("limit", String.valueOf(limit));

    if (search != null) {
      simpleHttp.param("search", search);
    }
    log.info("String: {}", simpleHttp.asString());
    String jsonText = simpleHttp.asString();

    List<UserData> userDataList = objectMapper
        .readValue(jsonText, new TypeReference<List<UserData>>() {});
    log.info("Objects: {}", userDataList);
    return userDataList;
  }

  @Override
  @SneakyThrows
  public Integer getUsersCount() {
    String url = String.format("%s/count", baseUrl);
    String count = SimpleHttp
        .doGet(url, httpClient)
        .authBasic(basicUsername, basicPassword)
        .asString();

    return Integer.valueOf(count);
  }

  @Override
  @SneakyThrows
  public UserData getUser(String query, String type) {
    log.info("Get user [{}, {}]", query, type);
    String url = String.format("%s/%s", baseUrl, query);
    SimpleHttp simpleHttp = SimpleHttp
        .doGet(url, httpClient)
        .authBasic(basicUsername, basicPassword);

    simpleHttp.param("type", type);

    SimpleHttp.Response response = simpleHttp.asResponse();

    if (response.getStatus() == 404) {
      throw new WebApplicationException(response.getStatus());
    }
    log.info("String: {}", simpleHttp.asString());
    String jsonText = simpleHttp.asString();

    UserData userData = objectMapper
        .readValue(jsonText, UserData.class);

    log.info("Object: {}", userData);
    return userData;
  }

  @Override
  @SneakyThrows
  public CredentialData getCredentialData(String id) {
    log.info("Get credential [{}]", id);
    String url = String.format("%s/%s/credentials", baseUrl, id);
    SimpleHttp.Response response = SimpleHttp
        .doGet(url, httpClient)
        .authBasic(basicUsername, basicPassword)
        .asResponse();

    if (response.getStatus() == 404) {
      throw new WebApplicationException(response.getStatus());
    }
    CredentialData credentialData = response.asJson(CredentialData.class);
    log.info("Object: {}", credentialData);
    return credentialData;
  }

  @Override
  @SneakyThrows
  public Response updateCredentialData(String id, CredentialData credentialData) {
    log.info("Update credential [{}]", id);
    String url = String.format("%s/%s/credentials", baseUrl, id);
    int status = SimpleHttp
        .doPut(url, httpClient)
        .authBasic(basicUsername, basicPassword)
        .json(credentialData)
        .asStatus();

    return Response.status(status).build();
  }
}
