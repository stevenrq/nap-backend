package com.ns.nap_backend.config.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catálogo de permisos: única fuente de verdad. Cada valor corresponde a un punto de control real
 * en el código (una expresión {@code @PreAuthorize("hasAuthority('...')")}) y es propiedad de la
 * API. La tabla {@code permissions} es solo una proyección de este enum, reconciliada al arrancar
 * por {@code RbacSeeder}; el frontend la consulta en solo lectura para componer roles.
 *
 * <p>Añadir o quitar un permiso es un cambio de código aquí (más su {@code @PreAuthorize}
 * correspondiente). El test de guardia verifica que toda authority referenciada en los
 * controladores exista en este catálogo, evitando typos y permisos fantasma.
 */
public enum PermissionCatalog {
  ROLE_READ("role:read", "Leer roles"),
  ROLE_CREATE("role:create", "Crear roles"),
  ROLE_UPDATE("role:update", "Actualizar roles y sus permisos"),
  ROLE_DELETE("role:delete", "Eliminar roles"),
  PERMISSION_READ("permission:read", "Leer el catálogo de permisos"),
  USER_READ("user:read", "Leer usuarios"),
  USER_UPDATE("user:update", "Actualizar usuarios"),
  USER_DELETE("user:delete", "Eliminar usuarios");

  private final String permissionName;
  private final String description;

  PermissionCatalog(String permissionName, String description) {
    this.permissionName = permissionName;
    this.description = description;
  }

  public String getPermissionName() {
    return permissionName;
  }

  public String getDescription() {
    return description;
  }

  /** Nombres de todos los permisos del catálogo. */
  public static Set<String> names() {
    return Arrays.stream(values())
        .map(PermissionCatalog::getPermissionName)
        .collect(Collectors.toUnmodifiableSet());
  }
}
