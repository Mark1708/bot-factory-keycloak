package com.mark1708.properties;

public class UserClientProperties {

  private String baseUrl = "https://api.mark1708.ru/bot-factory-core/api/v1/keycloak/users";
  private String basicUsername = "super-keycloak";
  private String basicPassword = "x2ONnSWmncAldBR";

  public UserClientProperties() {
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getBasicUsername() {
    return basicUsername;
  }

  public String getBasicPassword() {
    return basicPassword;
  }
}
