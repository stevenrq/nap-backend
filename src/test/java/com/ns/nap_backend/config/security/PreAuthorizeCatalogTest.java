package com.ns.nap_backend.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test de guardia que evita el drift entre los {@code @PreAuthorize} de los controladores y el
 * {@link PermissionCatalog}. Escanea todas las authorities referenciadas con {@code
 * hasAuthority('...')} y verifica que cada una exista en el catálogo, atrapando typos y permisos
 * fantasma en tiempo de build (no en runtime, cuando un endpoint quedaría inaccesible).
 */
class PreAuthorizeCatalogTest {

  private static final String CONTROLLER_PACKAGE = "com.ns.nap_backend";
  private static final Pattern HAS_AUTHORITY =
      Pattern.compile("hasAuthority\\(\\s*'([^']+)'\\s*\\)");

  @Test
  void everyPreAuthorizeAuthorityExistsInCatalog() {
    Set<String> referenced = scanReferencedAuthorities();

    assertThat(referenced)
        .as("authorities usadas en @PreAuthorize")
        .isNotEmpty()
        .allSatisfy(
            authority ->
                assertThat(PermissionCatalog.names())
                    .as("permiso '%s' debe existir en PermissionCatalog", authority)
                    .contains(authority));
  }

  private Set<String> scanReferencedAuthorities() {
    Set<String> authorities = new LinkedHashSet<>();
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

    for (var beanDefinition : scanner.findCandidateComponents(CONTROLLER_PACKAGE)) {
      Class<?> controller =
          ClassUtils.resolveClassName(
              beanDefinition.getBeanClassName(), getClass().getClassLoader());
      collectFromAnnotation(controller.getAnnotation(PreAuthorize.class), authorities);
      for (Method method : controller.getDeclaredMethods()) {
        collectFromAnnotation(method.getAnnotation(PreAuthorize.class), authorities);
      }
    }
    return authorities;
  }

  private void collectFromAnnotation(PreAuthorize annotation, Set<String> sink) {
    if (annotation == null) {
      return;
    }
    Matcher matcher = HAS_AUTHORITY.matcher(annotation.value());
    while (matcher.find()) {
      sink.add(matcher.group(1));
    }
  }
}
