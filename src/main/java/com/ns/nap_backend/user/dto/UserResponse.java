package com.ns.nap_backend.user.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ns.nap_backend.permission.entity.Permission;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.user.entity.Address;
import com.ns.nap_backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@JsonPropertyOrder({
  "id",
  "nationalId",
  "firstName",
  "lastName",
  "email",
  "phoneNumber",
  "address",
  "username",
  "enabled",
  "accountNonExpired",
  "accountNonLocked",
  "credentialsNonExpired",
  "roles",
  "permissions",
  "createdAt",
  "updatedAt"
})
public record UserResponse(
    Long id,
    Long nationalId,
    String firstName,
    String lastName,
    String email,
    Long phoneNumber,
    Address address,
    String username,
    boolean enabled,
    boolean accountNonExpired,
    boolean accountNonLocked,
    boolean credentialsNonExpired,
    Set<String> roles,
    Set<String> permissions,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static UserResponse fromUser(User user) {
    return new UserResponse(
        user.getId(),
        user.getNationalId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.getAddress(),
        user.getUsername(),
        user.isEnabled(),
        user.isAccountNonExpired(),
        user.isAccountNonLocked(),
        user.isCredentialsNonExpired(),
        user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
        user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getName)
            .collect(Collectors.toSet()),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
