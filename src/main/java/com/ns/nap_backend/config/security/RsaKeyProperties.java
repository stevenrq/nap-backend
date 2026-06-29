package com.ns.nap_backend.config.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Par de claves RSA usado para firmar y verificar los JWT. Spring Boot convierte automáticamente
 * los recursos PEM indicados en {@code app.rsa.public-key} y {@code app.rsa.private-key} (la
 * privada en PKCS8) a estos tipos mediante {@code RsaKeyConverters} al estar Spring Security en el
 * classpath.
 */
@ConfigurationProperties(prefix = "app.rsa")
public record RsaKeyProperties(RSAPublicKey publicKey, RSAPrivateKey privateKey) {}
