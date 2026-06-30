package com.ns.nap_backend.role.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ns.nap_backend.permission.entity.Permission;
import com.ns.nap_backend.role.entity.Role;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representación de salida de un {@link Role}. Evita exponer la entidad JPA directamente: los
 * permisos se proyectan como un conjunto de nombres en lugar de entidades anidadas.
 */
@JsonPropertyOrder({"id", "name", "description", "permissions", "createdAt", "updatedAt"})
public record RoleResponse(
    Long id,
    String name,
    String description,
    Set<String> permissions,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static RoleResponse fromRole(Role role) {
    return new RoleResponse(
        role.getId(),
        role.getName(),
        role.getDescription(),
        role.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet()),
        role.getCreatedAt(),
        role.getUpdatedAt());
  }
}
