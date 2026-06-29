package com.ns.nap_backend.permission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Size(min = 3, max = 50)
  @NotBlank
  @Column(nullable = false, unique = true, length = 50)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
  }

  public Permission(String name, String description) {
    this.name = name;
    this.description = description;
  }
}
