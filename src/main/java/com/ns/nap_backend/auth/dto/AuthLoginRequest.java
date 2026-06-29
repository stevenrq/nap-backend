package com.ns.nap_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Credenciales para el login (POST /auth/log-in). */
public record AuthLoginRequest(@NotBlank String username, @NotBlank String password) {}
