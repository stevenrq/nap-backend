package com.ns.nap_backend.auth.dto;

/**
 * Resultado interno de autenticar/renovar: el access token (JWT) va al cuerpo de la respuesta y el
 * refresh token en claro a la cookie HttpOnly. El controller traduce este objeto a {@code
 * AuthResponse} + {@code Set-Cookie}.
 */
public record AuthResult(
    String username, String message, String accessToken, String refreshToken) {}
