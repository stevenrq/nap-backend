package com.ns.nap_backend.config.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita por IP las peticiones a los endpoints de autenticación ({@code /api/auth/**}) para mitigar
 * ataques de fuerza bruta. Cada IP dispone de un cupo (token bucket) que se rellena de forma
 * continua; al agotarse se responde {@code 429 Too Many Requests}.
 *
 * <p>El estado es en memoria por instancia: suficiente para un despliegue de un solo nodo. Para
 * varios nodos conviene un backend distribuido (p. ej. Bucket4j sobre Redis).
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final String AUTH_PATH_PREFIX = "/api/auth";

  private final int capacity;
  private final Duration refillPeriod;
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  public RateLimitFilter() {
    this(10, Duration.ofMinutes(1));
  }

  public RateLimitFilter(int capacity, Duration refillPeriod) {
    this.capacity = capacity;
    this.refillPeriod = refillPeriod;
  }

  private Bucket newBucket() {
    Bandwidth limit =
        Bandwidth.builder().capacity(capacity).refillGreedy(capacity, refillPeriod).build();
    return Bucket.builder().addLimit(limit).build();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getServletPath().startsWith(AUTH_PATH_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Bucket bucket = buckets.computeIfAbsent(request.getRemoteAddr(), key -> newBucket());

    if (bucket.tryConsume(1)) {
      chain.doFilter(request, response);
    } else {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
    }
  }
}
