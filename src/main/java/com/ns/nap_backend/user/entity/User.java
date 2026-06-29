package com.ns.nap_backend.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ns.nap_backend.common.util.RolePermissionUtils;
import com.ns.nap_backend.common.validation.ValidationGroups.Create;
import com.ns.nap_backend.role.entity.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
@PrimaryKeyJoinColumn(name = "person_id", referencedColumnName = "id")
public class User extends Person {

  @Serial private static final long serialVersionUID = 1L;

  @Size(min = 5, max = 20)
  @NotBlank
  @Column(nullable = false, unique = true, length = 20)
  private String username;

  @Size(min = 5, max = 60, groups = Create.class)
  @NotBlank(groups = Create.class)
  @Column(nullable = false, length = 60)
  private String password;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "account_non_expired", nullable = false)
  private boolean accountNonExpired;

  @Column(name = "account_non_locked", nullable = false)
  private boolean accountNonLocked;

  @Column(name = "credentials_non_expired", nullable = false)
  private boolean credentialsNonExpired;

  @Transient private boolean admin;

  @JsonIgnoreProperties({"handler", "hibernateLazyInitializer"})
  @EqualsAndHashCode.Exclude
  @ManyToMany(
      fetch = FetchType.EAGER,
      cascade = {
        CascadeType.DETACH,
        CascadeType.MERGE,
        CascadeType.PERSIST,
        CascadeType.REFRESH,
      })
  @JoinTable(
      name = "users_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  public void prePersistUser() {
    this.enabled = true;
    this.accountNonExpired = true;
    this.accountNonLocked = true;
    this.credentialsNonExpired = true;
  }

  public Set<String> getRolesAndPermissions() {
    return RolePermissionUtils.getRolesAndPermissions(this.roles);
  }
}
