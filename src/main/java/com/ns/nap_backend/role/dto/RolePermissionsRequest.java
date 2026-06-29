package com.ns.nap_backend.role.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * Petición para reemplazar el conjunto de permisos de un rol. {@code permissionNames} es
 * obligatorio (puede venir vacío para dejar el rol sin permisos) y debe referenciar permisos
 * existentes del catálogo.
 */
public record RolePermissionsRequest(@NotNull Set<String> permissionNames) {}
