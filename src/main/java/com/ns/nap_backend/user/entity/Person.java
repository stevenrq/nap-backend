package com.ns.nap_backend.user.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "persons")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Person implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @NotNull
  @Column(name = "national_id", nullable = false, unique = true)
  private Long nationalId;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "first_name", nullable = false, length = 20)
  private String firstName;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "last_name", nullable = false, length = 20)
  private String lastName;

  @Email
  @Size(min = 16, max = 40)
  @NotBlank
  @Column(nullable = false, unique = true, length = 40)
  private String email;

  @NotNull
  @Column(name = "phone_number", nullable = false, unique = true)
  private Long phoneNumber;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "address_id", referencedColumnName = "id")
  private Address address;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);

    if (this instanceof User user) {
      user.prePersistUser();
    }
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
  }
}
