package com.ns.nap_backend.auth.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Datos para registrar un usuario (POST /auth/sign-up). Incluye {@code email} y {@code phoneNumber}
 * porque la entidad {@code Person} los exige. Los roles y permisos se crean vía cascade si no
 * existen, de modo que el registro es autosuficiente aun con {@code ddl-auto: create-drop}.
 */
@Getter
@Setter
@ToString
public class AuthCreateUserRequest {

  @NotNull
  @Column(name = "national_id", nullable = false, unique = true)
  Long nationalId;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(nullable = false, length = 20)
  private String firstName;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "last_name", nullable = false, length = 20)
  String lastName;

  @NotNull
  @Column(name = "phone_number", nullable = false, unique = true, length = 10)
  Long phoneNumber;

  @Email
  @Size(min = 16, max = 40)
  @NotBlank
  @Column(nullable = false, unique = true, length = 40)
  String email;

  @Size(min = 5, max = 20)
  @NotBlank
  String username;

  @Size(min = 5, max = 60)
  @NotBlank
  String password;
}
