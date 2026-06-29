package com.ns.nap_backend.role.exception;

/**
 * Excepción lanzada al intentar crear un rol cuyo nombre ya existe. El {@code ApiExceptionHandler}
 * la traduce a un 409 Conflict.
 */
public class RoleAlreadyExistsException extends RuntimeException {

  public RoleAlreadyExistsException(String name) {
    super("Role already exists: " + name);
  }
}
