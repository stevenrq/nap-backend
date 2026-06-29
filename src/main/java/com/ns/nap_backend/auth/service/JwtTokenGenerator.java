package com.ns.nap_backend.auth.service;

import com.ns.nap_backend.config.security.JwtProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Genera JWT firmados con la clave privada del backend. Se invoca desde {@link
 * AuthenticationService} tras autenticar credenciales o registrar un usuario. El {@code JwtConfig}
 * configura el {@code JwtAuthenticationConverter} para que Spring Security pueda validar la firma y
 * convertir el claim {@code "authorities"} en {@code GrantedAuthority} sin prefijo, de modo que
 * {@code hasAuthority("role:read")}, {@code hasAuthority("user:delete")}, etc. sigan funcionando
 * igual que con el {@code CustomUserDetailsService}.
 */
@Service
public class JwtTokenGenerator {

  private final JwtEncoder jwtEncoder;
  private final String issuer;
  private final long expirationMinutes;

  public JwtTokenGenerator(
      JwtEncoder jwtEncoder,
      @Value("${security.jwt.user.generator}") String issuer,
      JwtProperties jwtProperties) {
    this.jwtEncoder = jwtEncoder;
    this.issuer = issuer;
    this.expirationMinutes = jwtProperties.accessToken().expirationMinutes();
  }

  public String createToken(Authentication authentication) {
    Instant now = Instant.now();

    List<String> authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            // Excluye las "authentication factor" que añade Spring Security 7 (ej.
            // FACTOR_PASSWORD): el claim solo debe contener roles y permisos propios de la
            // aplicación.
            .filter(authority -> !authority.startsWith("FACTOR_"))
            .toList();

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(authentication.getName())
            .issuedAt(now)
            .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
            .notBefore(now)
            .id(UUID.randomUUID().toString())
            .claim("authorities", authorities)
            .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }
}
