package com.ns.nap_backend.permission.exception;

import java.util.Collection;

/**
 * Excepción lanzada cuando una petición referencia nombres de permiso que no existen en el catálogo
 * (propiedad de la API). El {@code ApiExceptionHandler} la traduce a un 400 Bad Request: no se
 * pueden inventar permisos por la puerta de atrás al componer un rol.
 */
public class UnknownPermissionException extends RuntimeException {

  public UnknownPermissionException(Collection<String> names) {
    super("Unknown permission(s): " + String.join(", ", names));
  }
}
