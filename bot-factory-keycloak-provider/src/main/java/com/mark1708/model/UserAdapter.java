package com.mark1708.model;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.LegacyUserCredentialManager;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapter;

public class UserAdapter extends AbstractUserAdapter.Streams {

  private final UserData user;

  public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model,
      UserData user) {
    super(session, realm, model);
    this.storageId = new StorageId(storageProviderModel.getId(), user.getUsername());
    this.user = user;
  }

  @Override
  public boolean isEnabled() {
    return user.isEnabled();
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }

  @Override
  public String getFirstName() {
    return user.getName();
  }

  @Override
  public String getLastName() {
    return user.getSurname();
  }

  @Override
  public String getEmail() {
    return user.getEmail();
  }

  @Override
  public boolean isEmailVerified() {
    return user.isEmailVerified();
  }

  @Override
  public boolean hasRole(RoleModel role) {
    return user.getRoles().stream().anyMatch(roleName -> role.getName().equals(roleName));
  }


  @Override
  public Long getCreatedTimestamp() {
    return user.getRegisteredAt()
        .atZone(ZoneId.of("Europe/Moscow"))
        .toInstant().toEpochMilli();
  }

  @Override
  public SubjectCredentialManager credentialManager() {
    return new LegacyUserCredentialManager(session, realm, this);
  }

  @Override
  public String getFirstAttribute(String name) {
    List<String> list = getAttributes().getOrDefault(name, List.of());
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public Map<String, List<String>> getAttributes() {
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.add(UserModel.USERNAME, getUsername());
    attributes.add(UserModel.EMAIL, getEmail());
    attributes.add(UserModel.FIRST_NAME, getFirstName());
    attributes.add(UserModel.LAST_NAME, getLastName());
    attributes.add(UserModel.ENABLED, String.valueOf(user.isEnabled()));
    attributes.add(UserModel.EMAIL_VERIFIED, String.valueOf(user.isEmailVerified()));
    attributes.add("dbId", String.valueOf(user.getId()));
    attributes.add("registeredAt", user.getRegisteredAt().toString());
    return attributes;
  }

  @Override
  public Stream<String> getAttributeStream(String name) {
    Map<String, List<String>> attributes = getAttributes();
    return (attributes.containsKey(name)) ? attributes.get(name).stream() : Stream.empty();
  }

  @Override
  protected Set<GroupModel> getGroupsInternal() {
    return Set.of();
  }

  @Override
  protected Set<RoleModel> getRoleMappingsInternal() {
    if (user.getRoles() != null) {
      return user.getRoles().stream().map(roleName -> new UserRoleModel(roleName, realm))
          .collect(Collectors.toSet());
    }
    return Set.of();
  }


}
