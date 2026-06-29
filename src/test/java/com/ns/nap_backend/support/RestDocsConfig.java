package com.ns.nap_backend.support;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import org.springframework.boot.restdocs.test.autoconfigure.RestDocsMockMvcConfigurationCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Aplica {@code prettyPrint()} por defecto a las peticiones y respuestas documentadas, de modo que
 * los snippets HTTP generados queden formateados sin repetir preprocesadores en cada test.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RestDocsConfig {

  @Bean
  RestDocsMockMvcConfigurationCustomizer restDocsCustomizer() {
    return configurer ->
        configurer
            .operationPreprocessors()
            .withRequestDefaults(prettyPrint())
            .withResponseDefaults(prettyPrint());
  }
}
