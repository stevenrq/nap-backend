package com.ns.nap_backend.config.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.ns.nap_backend.auth.service.TokenDenylistService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Configuración de firma/validación de JWT con RSA asimétrico usando las librerías nativas de
 * Spring (Nimbus). El par de claves se inyecta vía {@link RsaKeyProperties} desde los PEM del
 * classpath; no se incrustan secretos en la configuración.
 */
@Configuration
@EnableConfigurationProperties({RsaKeyProperties.class, JwtProperties.class})
public class JwtConfig {

  private final RsaKeyProperties rsaKeys;

  public JwtConfig(RsaKeyProperties rsaKeys) {
    this.rsaKeys = rsaKeys;
  }

  /** Encoder para generar tokens firmados con la clave privada. */
  @Bean
  JwtEncoder jwtEncoder() {
    RSAKey rsaKey =
        new RSAKey.Builder(rsaKeys.publicKey()).privateKey(rsaKeys.privateKey()).build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
  }

  /**
   * Decoder que valida firma, {@code exp} y {@code nbf} con la clave pública y, además, rechaza los
   * access tokens revocados consultando la {@link TokenDenylistService} (logout inmediato).
   */
  @Bean
  JwtDecoder jwtDecoder(TokenDenylistService tokenDenylistService) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKeys.publicKey()).build();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefault(), new DenylistJwtValidator(tokenDenylistService)));
    return decoder;
  }

  /**
   * Convierte el claim {@code "authorities"} (colección/array JSON) en {@code GrantedAuthority} sin
   * prefijo, de modo que {@code hasAuthority("role:read")}, {@code hasAuthority("user:delete")},
   * etc. sigan funcionando igual que con el {@code CustomUserDetailsService}.
   */
  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("authorities");
    authoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }
}
