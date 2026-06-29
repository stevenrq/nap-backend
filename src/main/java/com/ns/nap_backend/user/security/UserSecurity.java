package com.ns.nap_backend.user.security;

import com.ns.nap_backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Regla de autorización de dominio usada desde {@code @PreAuthorize} para permitir que un usuario
 * opere sobre su propia cuenta sin necesitar el permiso administrativo {@code user:update}.
 *
 * <p>El JWT solo lleva el {@code username} (claim {@code sub}), no el {@code id}, por lo que la
 * comprobación resuelve el usuario por id y compara su username con el del principal autenticado.
 */
@Component("userSecurity")
public class UserSecurity {

  private final UserRepository userRepository;

  public UserSecurity(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /** {@code true} si el usuario autenticado es el dueño de la cuenta {@code id}. */
  public boolean isSelf(Long id, Authentication authentication) {
    if (id == null || authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    return userRepository
        .findById(id)
        .map(user -> user.getUsername().equals(authentication.getName()))
        .orElse(false);
  }
}
