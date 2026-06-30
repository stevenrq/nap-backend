package com.ns.nap_backend.permission.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ns.nap_backend.permission.entity.Permission;
import java.time.LocalDateTime;

/**
 * Representación de salida de un {@link Permission}. Evita exponer la entidad JPA directamente en
 * la capa web.
 */
@JsonPropertyOrder({"id", "name", "description", "createdAt", "updatedAt"})
public record PermissionResponse(
    Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {

  public static PermissionResponse fromPermission(Permission permission) {
    return new PermissionResponse(
        permission.getId(),
        permission.getName(),
        permission.getDescription(),
        permission.getCreatedAt(),
        permission.getUpdatedAt());
  }
}
