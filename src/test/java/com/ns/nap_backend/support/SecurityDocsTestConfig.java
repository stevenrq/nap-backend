package com.ns.nap_backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadena de seguridad mínima para los tests {@code @WebMvcTest} que documentan la API. Replica las
 * reglas relevantes de producción ({@code /api/auth/**} público, resto autenticado, stateless, sin
 * CSRF) sin arrastrar la carga de claves PEM ni el {@code JwtDecoder} real. Necesaria porque, con
 * Spring Security en el classpath, el slice aplicaría la cadena por defecto (todo autenticado) y
 * bloquearía los endpoints públicos.
 *
 * <p>Incluye {@code @EnableMethodSecurity} para que los {@code @PreAuthorize} de los controladores
 * se apliquen también en los slices y puedan verificarse las reglas de autorización por permiso.
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityDocsTestConfig {

  @Bean
  SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers("/api/auth/**").permitAll().anyRequest().authenticated())
        .build();
  }
}
