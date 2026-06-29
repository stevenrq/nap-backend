package com.ns.nap_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración web transversal. Define la estrategia de versionado de la API nativa de Spring
 * Framework 7.
 *
 * <p>La versión se transmite mediante el header {@code X-API-Version}. Al no ser obligatoria y
 * existir una versión por defecto ({@code 1.0}), los clientes que no envíen el header siguen
 * funcionando contra la versión actual.
 *
 * <p>Cada endpoint declara su versión con el atributo {@code version} de {@code @RequestMapping}
 * (normalmente a nivel de clase), por lo que no se incrusta en la ruta.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  /** Versión por defecto aplicada cuando el cliente no envía el header {@code X-API-Version}. */
  private static final String DEFAULT_VERSION = "1.0";

  @Override
  public void configureApiVersioning(ApiVersionConfigurer configurer) {
    configurer
        .useRequestHeader("X-API-Version")
        .setVersionRequired(false)
        .setDefaultVersion(DEFAULT_VERSION)
        .addSupportedVersions(DEFAULT_VERSION);
  }
}
