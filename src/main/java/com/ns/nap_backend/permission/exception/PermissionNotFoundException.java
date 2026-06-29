package com.ns.nap_backend.permission.exception;

public class PermissionNotFoundException extends RuntimeException {

  public PermissionNotFoundException(Long id) {
    super("Permission not found: " + id);
  }

  public PermissionNotFoundException(String name) {
    super("Permission not found: " + name);
  }
}
