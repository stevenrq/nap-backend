package com.ns.nap_backend.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieFactoryTest {

  private final RefreshTokenCookieFactory factory =
      new RefreshTokenCookieFactory(
          new JwtProperties(
              new JwtProperties.AccessToken(15),
              new JwtProperties.RefreshToken(
                  7,
                  new JwtProperties.RefreshToken.Cookie(
                      "refresh_token", "/api/auth", true, false, "Lax"))));

  @Test
  void cookieNameComesFromProperties() {
    assertThat(factory.cookieName()).isEqualTo("refresh_token");
  }

  @Test
  void buildSetsValueAndConfiguredAttributes() {
    ResponseCookie cookie = factory.build("raw-token");

    assertThat(cookie.getName()).isEqualTo("refresh_token");
    assertThat(cookie.getValue()).isEqualTo("raw-token");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isFalse();
    assertThat(cookie.getPath()).isEqualTo("/api/auth");
    assertThat(cookie.getSameSite()).isEqualTo("Lax");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
  }

  @Test
  void clearProducesExpiredEmptyCookie() {
    ResponseCookie cookie = factory.clear();

    assertThat(cookie.getValue()).isEmpty();
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
  }
}
