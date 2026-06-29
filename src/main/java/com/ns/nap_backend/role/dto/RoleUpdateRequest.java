package com.ns.nap_backend.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Petición para actualizar los datos básicos de un rol (no sus permisos). */
public record RoleUpdateRequest(
    @Size(min = 3, max = 20) @NotBlank String name, String description) {}
