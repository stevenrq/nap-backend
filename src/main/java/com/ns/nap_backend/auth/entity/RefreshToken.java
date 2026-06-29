package com.ns.nap_backend.auth.entity;

import com.ns.nap_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh token opaco persistido. Por seguridad solo se almacena el hash SHA-256 del valor que se
 * entrega al cliente (en una cookie HttpOnly), nunca el token en claro. Soporta rotación y
 * revocación: cada uso en {@code /api/auth/refresh} revoca el actual y emite uno nuevo.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  /** Hash SHA-256 (hex) del token en claro. */
  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  public boolean isActive() {
    return !revoked && !isExpired();
  }
}
