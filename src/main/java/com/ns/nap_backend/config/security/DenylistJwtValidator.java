package com.ns.nap_backend.config.security;

import com.ns.nap_backend.auth.service.TokenDenylistService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Validador que rechaza los JWT cuyo {@code jti} figura en la {@link TokenDenylistService}. Se
 * encadena con los validadores por defecto del {@code JwtDecoder}, de modo que un access token
 * revocado (p. ej. tras un logout) deja de ser válido de inmediato en cualquier endpoint protegido.
 */
public class DenylistJwtValidator implements OAuth2TokenValidator<Jwt> {

  private static final OAuth2Error REVOKED =
      new OAuth2Error("invalid_token", "The access token has been revoked", null);

  private final TokenDenylistService tokenDenylistService;

  public DenylistJwtValidator(TokenDenylistService tokenDenylistService) {
    this.tokenDenylistService = tokenDenylistService;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    if (tokenDenylistService.isDenylisted(token.getId())) {
      return OAuth2TokenValidatorResult.failure(REVOKED);
    }
    return OAuth2TokenValidatorResult.success();
  }
}
