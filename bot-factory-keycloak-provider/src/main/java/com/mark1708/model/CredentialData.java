package com.mark1708.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.keycloak.common.util.Base64;
import org.keycloak.models.credential.PasswordCredentialModel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialData {

  private String password;
  private String salt;

  @JsonIgnore
  public static String algorithm = "pbkdf2-sha256";
  @JsonIgnore
  public static Integer iterations = 27500;
  @JsonIgnore
  public static String type = "password";

  @SneakyThrows
  public PasswordCredentialModel toPasswordCredentialModel() {
    return PasswordCredentialModel.createFromValues(
        algorithm, Base64.decode(this.getSalt()),
        iterations, this.getPassword()
    );
  }

  public static CredentialData fromPasswordCredentialModel(PasswordCredentialModel pcm) {
    return new CredentialData(
        pcm.getPasswordSecretData().getValue(),
        Base64.encodeBytes(pcm.getPasswordSecretData().getSalt())
    );
  }
}
