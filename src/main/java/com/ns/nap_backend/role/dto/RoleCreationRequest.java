package com.ns.nap_backend.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Petición para crear un rol. {@code permissionNames} debe referenciar permisos existentes del
 * catálogo (propiedad de la API); puede ser vacío/nulo para crear un rol sin permisos.
 */
public record RoleCreationRequest(
    @Size(min = 3, max = 20) @NotBlank String name,
    String description,
    Set<String> permissionNames) {}
