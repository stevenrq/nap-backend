package com.ns.nap_backend.auth.dto;

import com.ns.nap_backend.user.entity.User;

/**
 * Resultado de rotar un refresh token: el usuario propietario (para reconstruir sus authorities y
 * firmar un nuevo access token) y el nuevo valor en claro que debe enviarse en la cookie.
 */
public record RotatedRefreshToken(User user, String rawToken) {}
