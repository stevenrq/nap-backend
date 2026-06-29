package com.ns.nap_backend.role.exception;

/**
 * Excepción lanzada cuando se opera sobre un rol que no existe. El {@code ApiExceptionHandler} la
 * traduce a un 404 Not Found.
 */
public class RoleNotFoundException extends RuntimeException {

  public RoleNotFoundException(Long id) {
    super("Role not found: " + id);
  }

  public RoleNotFoundException(String name) {
    super("Role not found: " + name);
  }
}
