package com.mark1708.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserData {

  private Long id;
  private String surname;
  private String name;
  private String username;
  private String email;
  private LocalDateTime registeredAt;
  private boolean enabled;
  private boolean emailVerified;
  private List<String> roles;
}
