package com.ns.nap_backend.auth.service;

import com.ns.nap_backend.auth.repository.RefreshTokenRepository;
import com.ns.nap_backend.user.entity.User;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revoca todas las sesiones de un usuario al detectar reuso de un refresh token. Se aísla en su
 * propio bean con {@link Propagation#REQUIRES_NEW} para que la revocación se confirme aunque la
 * transacción que la dispara haga rollback al lanzar el 401 (de lo contrario el rollback desharía
 * la revocación en cascada y la detección de reuso no tendría efecto).
 */
@Component
public class RefreshTokenReuseHandler {

  private final RefreshTokenRepository refreshTokenRepository;

  public RefreshTokenReuseHandler(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void revokeAllSessions(User user) {
    refreshTokenRepository.revokeAllByUser(user, Instant.now());
  }
}
