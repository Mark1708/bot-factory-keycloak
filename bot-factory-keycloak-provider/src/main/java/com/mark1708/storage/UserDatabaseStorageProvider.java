package com.mark1708.storage;

import com.mark1708.client.UserServiceClient;
import com.mark1708.client.UserServiceClientSimpleHttp;
import com.mark1708.model.CredentialData;
import com.mark1708.model.UserAdapter;
import com.mark1708.model.UserData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDatabaseStorageProvider implements
    UserStorageProvider, UserLookupProvider,
    UserQueryProvider, CredentialInputUpdater,
    CredentialInputValidator, UserRegistrationProvider {

  private static final Logger log = LoggerFactory.getLogger(UserDatabaseStorageProvider.class);
  private final KeycloakSession session;
  private final ComponentModel model;
  private final UserServiceClient client;

  protected Map<Long, UserModel> loadedUsers = new HashMap<>();

  public UserDatabaseStorageProvider(KeycloakSession session, ComponentModel model) {
    this.session = session;
    this.model = model;
    this.client = new UserServiceClientSimpleHttp(session, model);
  }

  @Override
  public void close() {
  }

  @Override
  public boolean supportsCredentialType(String credentialType) {
    return PasswordCredentialModel.TYPE.equals(credentialType);
  }

  @Override
  public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
    return supportsCredentialType(credentialType);
  }

  @Override
  public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
    if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
      return false;
    }

    CredentialData credentialData;
    try {
      String id = user.getFirstAttribute("dbId");
      log.info("External id: {}", id);
      credentialData = client.getCredentialData(id);
      log.info("Received credential data for userId {}: %{}", user.getId(), credentialData);
      if (credentialData == null) {
        return false;
      }
    } catch (WebApplicationException e) {
      log.error(String.format(
          "Request to verify credentials for userId %s failed with response status %d",
          user.getId(), e.getResponse().getStatus()), e);
      return false;
    }

    UserCredentialModel cred = (UserCredentialModel) input;

    PasswordCredentialModel passwordCredentialModel = credentialData.toPasswordCredentialModel();
    PasswordHashProvider passwordHashProvider = session
        .getProvider(PasswordHashProvider.class, CredentialData.algorithm);
    boolean isValid = passwordHashProvider.verify(cred.getChallengeResponse(),
        passwordCredentialModel);
    log.info("Password validation result: {}", isValid);
    return isValid;
  }

  @Override
  public boolean updateCredential(
      RealmModel realm,
      UserModel user,
      CredentialInput input
  ) {
    log.info("Try to update credentials type {} for user {}.", input.getType(), user.getId());
    if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
      return false;
    }

    UserCredentialModel cred = (UserCredentialModel) input;

    PasswordPolicy passwordPolicy = realm.getPasswordPolicy();
    PasswordHashProvider passwordHashProvider = session
        .getProvider(PasswordHashProvider.class, passwordPolicy.getHashAlgorithm());
    PasswordCredentialModel passwordCredentialModel = passwordHashProvider
            .encodedCredential(cred.getChallengeResponse(), passwordPolicy.getHashIterations());

    CredentialData credentialData = CredentialData
        .fromPasswordCredentialModel(passwordCredentialModel);

    log.info("Sending updateCredential request for userId {}", user.getId());
    log.trace("Payload for updateCredential request: {}", credentialData);
    try {
      String id = user.getFirstAttribute("dbId");
      log.info("External id: {}", id);
      Response updateResponse = client.updateCredentialData(id, credentialData);
      return updateResponse.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL);
    } catch (WebApplicationException e) {
      log.warn("Credential data update for user {} failed with response {}", user.getId(),
          e.getResponse().getStatus());
      return false;
    }
  }

  @Override
  public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
  }

  @Override
  public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
    return Stream.empty();
  }

  @Override
  public UserModel getUserById(RealmModel realm, String id) {
    log.info("getUserById: {}", id);
    String username = id.split(":")[2];
    try {
      UserData user = client.getUser(username, "username");
      UserModel adapter = new UserAdapter(session, realm, model, user);
      loadedUsers.put(user.getId(), adapter);
      return adapter;
    } catch (WebApplicationException e) {
      log.warn("User with id '{}' could not be found, response from server: {}", id,
          e.getResponse().getStatus());
      throw e;
    }
  }

  @Override
  public UserModel getUserByUsername(RealmModel realm, String username) {
    log.info("getUserByUsername: {}", username);
    try {
      UserData user = client.getUser(username, "username");
      UserModel adapter = new UserAdapter(session, realm, model, user);
      loadedUsers.put(user.getId(), adapter);
      return adapter;
    } catch (WebApplicationException e) {
      log.warn("User with username '{}' could not be found, response from server: {}",
          username, e.getResponse().getStatus());
      throw e;
    }
  }

  @Override
  public UserModel getUserByEmail(RealmModel realm, String email) {
    log.info("getUserByEmail: {}", email);
    try {
      UserData user = client.getUser(email, "email");
      UserModel adapter = new UserAdapter(session, realm, model, user);
      loadedUsers.put(user.getId(), adapter);
      return adapter;
    } catch (WebApplicationException e) {
      log.warn("User with email '{}' could not be found, response from server: {}", email,
          e.getResponse().getStatus());
      throw e;
    }
  }

  @Override
  public int getUsersCount(RealmModel realm) {
    return client.getUsersCount();
  }


  @Override
  public Stream<UserModel> searchForUserStream(
      RealmModel realm,
      String search,
      Integer firstResult,
      Integer maxResults
  ) {
    log.info("searchForUserStream, search={}, first={}, max={}", search, firstResult, maxResults);
    return toUserModelStream(
        client.getAllSearchedUsers(search, firstResult, maxResults), realm
    );
  }

  @Override
  public Stream<UserModel> searchForUserStream(
      RealmModel realm,
      Map<String, String> params,
      Integer firstResult,
      Integer maxResults
  ) {
    log.info("searchForUserStream, params={}, first={}, max={}", params, firstResult, maxResults);
    return toUserModelStream(
        client.getAllSearchedUsers(null, firstResult, maxResults), realm
    );
  }

  private Stream<UserModel> toUserModelStream(List<UserData> users, RealmModel realm) {
    log.info("Received {} users from provider", users.size());
    return users.stream().map(user -> new UserAdapter(session, realm, model, user));
  }

  @Override
  public Stream<UserModel> getGroupMembersStream(
      RealmModel realm,
      GroupModel group,
      Integer firstResult,
      Integer maxResults
  ) {
    return Stream.empty();
  }

  @Override
  public Stream<UserModel> searchForUserByUserAttributeStream(
      RealmModel realm,
      String attrName,
      String attrValue
  ) {
    return Stream.empty();
  }

  @Override
  public UserModel addUser(RealmModel realm, String username) {
    return null;
  }

  @Override
  public boolean removeUser(RealmModel realm, UserModel user) {
    return false;
  }
}
