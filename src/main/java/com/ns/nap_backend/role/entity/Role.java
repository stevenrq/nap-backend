package com.ns.nap_backend.role.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ns.nap_backend.permission.entity.Permission;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(nullable = false, unique = true, length = 20)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @EqualsAndHashCode.Exclude
  @JsonIgnoreProperties({"handler", "hibernateLazyInitializer"})
  @ManyToMany(
      fetch = FetchType.EAGER,
      cascade = {
        CascadeType.DETACH,
        CascadeType.MERGE,
        CascadeType.PERSIST,
        CascadeType.REFRESH,
      })
  @JoinTable(
      name = "roles_permissions",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id"))
  private Set<Permission> permissions = new HashSet<>();

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
  }

  public void addPermissions(Set<Permission> permissions) {
    this.getPermissions().addAll(permissions);
  }

  public void addPermission(Permission permission) {
    this.getPermissions().add(permission);
  }
}
