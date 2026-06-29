package com.ns.nap_backend.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros configurables de los tokens bajo el prefijo {@code security.jwt}.
 *
 * <ul>
 *   <li>{@code access-token.expiration-minutes}: vigencia del JWT de acceso.
 *   <li>{@code refresh-token.expiration-days}: vigencia del refresh token opaco.
 *   <li>{@code refresh-token.cookie.*}: atributos de la cookie HttpOnly que transporta el refresh
 *       token.
 * </ul>
 *
 * <p>El claim {@code security.jwt.user.generator} se sigue inyectando con {@code @Value} en {@link
 * com.ns.nap_backend.auth.service.JwtTokenGenerator} y no forma parte de este binding.
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(AccessToken accessToken, RefreshToken refreshToken) {

  public record AccessToken(long expirationMinutes) {}

  public record RefreshToken(long expirationDays, Cookie cookie) {

    public record Cookie(
        String name, String path, boolean httpOnly, boolean secure, String sameSite) {}
  }
}
