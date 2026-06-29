package com.ns.nap_backend.role.exception;

/** Excepción personalizada lanzada cuando hay un error al recuperar los roles del usuario. */
public class RoleRetrievalException extends RuntimeException {

  public RoleRetrievalException(String message) {
    super(message);
  }

  public RoleRetrievalException(String message, Throwable cause) {
    super(message, cause);
  }
}
