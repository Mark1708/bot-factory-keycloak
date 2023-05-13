package com.mark1708.storage;

import com.mark1708.properties.UserClientProperties;
import java.util.List;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.utils.StringUtil;


public class UserDatabaseStorageProviderFactory implements UserStorageProviderFactory<UserDatabaseStorageProvider> {

  private final UserClientProperties userClientProperties = new UserClientProperties();

  public static final String PROVIDER_ID = "bot-factory-keycloak-provider";

  @Override
  public UserDatabaseStorageProvider create(KeycloakSession session, ComponentModel model) {
    return new UserDatabaseStorageProvider(session, model);
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public String getHelpText() {
    return "Bot Factory User Provider";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return ProviderConfigurationBuilder.create()
        .property(userClientProperties.getBaseUrl(), "Base URL", "Base URL of the API", ProviderConfigProperty.STRING_TYPE, "", null)
        .property(userClientProperties.getBasicUsername(), "BasicAuth Username", "Username for BasicAuth at the API", ProviderConfigProperty.STRING_TYPE, "", null)
        .property(userClientProperties.getBasicPassword(), "BasicAuth Password", "Password for BasicAuth at the API", ProviderConfigProperty.PASSWORD, "", null)
        .build();
  }

  @Override
  public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
    if (StringUtil.isBlank(config.get(userClientProperties.getBaseUrl()))
        || StringUtil.isBlank(config.get(userClientProperties.getBasicUsername()))
        || StringUtil.isBlank(config.get(userClientProperties.getBasicPassword()))) {
      throw new ComponentValidationException("Configuration not properly set, please verify.");
    }
  }
}
