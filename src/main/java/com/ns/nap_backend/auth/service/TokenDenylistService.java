package com.ns.nap_backend.auth.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Denylist en memoria de access tokens revocados, indexada por su claim {@code jti}. Permite
 * invalidar de inmediato un JWT (p. ej. en logout) pese a ser stateless: el {@code JwtDecoder} la
 * consulta en cada petición y rechaza los tokens presentes aquí.
 *
 * <p>Cada entrada solo se conserva hasta el {@code exp} del token; al expirar el JWT ya es inválido
 * por sí mismo, por lo que mantenerlo en la denylist no aporta nada. Es una implementación de un
 * único nodo; en un despliegue multi-instancia debería respaldarse por un store compartido (Redis).
 */
@Service
public class TokenDenylistService {

  private final Map<String, Instant> denylist = new ConcurrentHashMap<>();

  /** Revoca el token identificado por {@code jti} hasta su expiración. */
  public void denylist(String jti, Instant expiresAt) {
    if (jti == null || expiresAt == null || expiresAt.isBefore(Instant.now())) {
      return;
    }
    denylist.put(jti, expiresAt);
  }

  /** Indica si el {@code jti} está revocado; limpia la entrada de forma perezosa si ya expiró. */
  public boolean isDenylisted(String jti) {
    if (jti == null) {
      return false;
    }
    Instant expiresAt = denylist.get(jti);
    if (expiresAt == null) {
      return false;
    }
    if (expiresAt.isBefore(Instant.now())) {
      denylist.remove(jti, expiresAt);
      return false;
    }
    return true;
  }

  /** Elimina todas las entradas ya expiradas. Pensado para una purga periódica opcional. */
  public void purgeExpired() {
    Instant now = Instant.now();
    denylist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
  }
}
