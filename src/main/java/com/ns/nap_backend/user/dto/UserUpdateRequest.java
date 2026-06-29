package com.ns.nap_backend.user.dto;

import com.ns.nap_backend.common.util.RolePermissionUtils;
import com.ns.nap_backend.common.validation.ValidationGroups;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.user.entity.Address;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserUpdateRequest {

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(nullable = false, length = 20)
  private String firstName;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "last_name", nullable = false, length = 20)
  private String lastName;

  private Address address;

  @NotNull
  @Column(name = "phone_number", nullable = false, unique = true, length = 10)
  private Long phoneNumber;

  @Email
  @Size(min = 16, max = 40)
  @NotBlank
  @Column(nullable = false, unique = true, length = 40)
  private String email;

  @Size(min = 5, max = 20)
  @NotBlank
  @Column(nullable = false, unique = true, length = 20)
  private String username;

  @Size(min = 5, max = 60, groups = ValidationGroups.Create.class)
  @NotBlank(groups = ValidationGroups.Create.class)
  @Column(nullable = false, length = 60)
  private String password;

  @Transient private boolean admin;

  private Set<Role> roles;

  public Set<String> getRolesAndPermissions() {
    return RolePermissionUtils.getRolesAndPermissions(this.roles);
  }
}
