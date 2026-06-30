package com.ns.nap_backend.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  private MockHttpServletRequest authRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/log-in");
    request.setServletPath("/api/auth/log-in");
    request.setRemoteAddr("10.0.0.1");
    return request;
  }

  @Test
  void allowsRequestsWithinLimit() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(2, Duration.ofMinutes(1));
    FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

    for (int i = 0; i < 2; i++) {
      filter.doFilter(authRequest(), new MockHttpServletResponse(), chain);
    }

    verify(chain, org.mockito.Mockito.times(2))
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void returns429WhenLimitExceeded() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(1, Duration.ofMinutes(1));
    FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

    filter.doFilter(authRequest(), new MockHttpServletResponse(), chain);

    MockHttpServletResponse blocked = new MockHttpServletResponse();
    filter.doFilter(authRequest(), blocked, chain);

    assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(blocked.getContentAsString()).contains("Rate limit exceeded");
    // La cadena solo se invocó para la primera petición (la segunda fue rechazada).
    verify(chain, org.mockito.Mockito.times(1))
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void doesNotLimitNonAuthPaths() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(1, Duration.ofMinutes(1));
    FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

    MockHttpServletRequest usersRequest = new MockHttpServletRequest("GET", "/api/users");
    usersRequest.setServletPath("/api/users");
    usersRequest.setRemoteAddr("10.0.0.2");

    // Aunque el cupo es 1, varias peticiones a un path ajeno nunca se limitan.
    for (int i = 0; i < 3; i++) {
      filter.doFilter(usersRequest, new MockHttpServletResponse(), chain);
    }

    verify(chain, org.mockito.Mockito.times(3))
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }
}
