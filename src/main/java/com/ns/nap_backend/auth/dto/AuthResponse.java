package com.ns.nap_backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Respuesta de autenticación: usuario, mensaje y access token. El {@code accessToken} se omite del
 * JSON cuando es nulo (por ejemplo, en el registro, que no emite tokens).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"username", "message", "accessToken"})
public record AuthResponse(String username, String message, String accessToken) {}
