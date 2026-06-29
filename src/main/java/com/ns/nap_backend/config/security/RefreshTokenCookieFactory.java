package com.ns.nap_backend.config.security;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Construye la cookie HttpOnly que transporta el refresh token. Usa {@link ResponseCookie} (en
 * lugar de {@code jakarta.servlet.http.Cookie}) para poder fijar el atributo {@code SameSite}. Los
 * atributos se toman de {@code security.jwt.refresh-token.cookie.*}.
 */
@Component
public class RefreshTokenCookieFactory {

  private final JwtProperties.RefreshToken.Cookie cookie;
  private final Duration maxAge;

  public RefreshTokenCookieFactory(JwtProperties jwtProperties) {
    this.cookie = jwtProperties.refreshToken().cookie();
    this.maxAge = Duration.ofDays(jwtProperties.refreshToken().expirationDays());
  }

  public String cookieName() {
    return cookie.name();
  }

  /** Cookie con el refresh token y la vigencia configurada. */
  public ResponseCookie build(String rawToken) {
    return baseBuilder(rawToken, maxAge).build();
  }

  /** Cookie de borrado (maxAge=0) para el logout. */
  public ResponseCookie clear() {
    return baseBuilder("", Duration.ZERO).build();
  }

  private ResponseCookie.ResponseCookieBuilder baseBuilder(String value, Duration age) {
    return ResponseCookie.from(cookie.name(), value)
        .httpOnly(cookie.httpOnly())
        .secure(cookie.secure())
        .path(cookie.path())
        .sameSite(cookie.sameSite())
        .maxAge(age);
  }
}
