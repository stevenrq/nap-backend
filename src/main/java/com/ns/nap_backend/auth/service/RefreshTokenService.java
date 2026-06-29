package com.ns.nap_backend.auth.service;

import com.ns.nap_backend.auth.dto.RotatedRefreshToken;
import com.ns.nap_backend.auth.entity.RefreshToken;
import com.ns.nap_backend.auth.repository.RefreshTokenRepository;
import com.ns.nap_backend.config.security.JwtProperties;
import com.ns.nap_backend.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gestiona el ciclo de vida de los refresh tokens opacos: emisión, validación con rotación y
 * revocación. El valor en claro (alta entropía) solo existe en memoria y en la cookie del cliente;
 * en BD se guarda únicamente su hash SHA-256.
 */
@Service
public class RefreshTokenService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenReuseHandler reuseHandler;
  private final long expirationDays;

  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository,
      RefreshTokenReuseHandler reuseHandler,
      JwtProperties jwtProperties) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.reuseHandler = reuseHandler;
    this.expirationDays = jwtProperties.refreshToken().expirationDays();
  }

  /** Emite y persiste un nuevo refresh token para el usuario, devolviendo el valor en claro. */
  @Transactional
  public String issue(User user) {
    String rawToken = generateRawToken();

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setTokenHash(hash(rawToken));
    refreshToken.setUser(user);
    Instant now = Instant.now();
    refreshToken.setCreatedAt(now);
    refreshToken.setExpiresAt(now.plus(expirationDays, ChronoUnit.DAYS));
    refreshToken.setRevoked(false);

    refreshTokenRepository.save(refreshToken);
    return rawToken;
  }

  /**
   * Valida el token recibido y lo rota: revoca el actual y emite uno nuevo. Si el token no existe
   * es inválido (401); si ya estaba revocado se asume reuso (posible robo) y se revocan todos los
   * del usuario; si expiró, 401.
   */
  @Transactional
  public RotatedRefreshToken validateAndRotate(String rawToken) {
    RefreshToken current = findValidOrThrow(rawToken);

    revoke(current);
    User user = current.getUser();
    String newRawToken = issue(user);

    return new RotatedRefreshToken(user, newRawToken);
  }

  /** Revoca el token recibido (logout). No falla si ya no existe o estaba revocado. */
  @Transactional
  public void revoke(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }
    refreshTokenRepository
        .findByTokenHash(hash(rawToken))
        .filter(token -> !token.isRevoked())
        .ifPresent(this::revoke);
  }

  private RefreshToken findValidOrThrow(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw unauthorized("Missing refresh token");
    }

    RefreshToken token =
        refreshTokenRepository
            .findByTokenHash(hash(rawToken))
            .orElseThrow(() -> unauthorized("Invalid refresh token"));

    if (token.isRevoked()) {
      // Reuso de un token ya rotado: posible robo. Se revocan todas las sesiones del usuario en una
      // transacción propia para que la revocación se confirme pese al rollback que provoca el 401.
      reuseHandler.revokeAllSessions(token.getUser());
      throw unauthorized("Refresh token reuse detected");
    }

    if (token.isExpired()) {
      throw unauthorized("Expired refresh token");
    }

    return token;
  }

  private void revoke(RefreshToken token) {
    token.setRevoked(true);
    token.setRevokedAt(Instant.now());
    refreshTokenRepository.save(token);
  }

  private static String generateRawToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return URL_ENCODER.encodeToString(bytes);
  }

  private static String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private static ResponseStatusException unauthorized(String reason) {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
  }
}
