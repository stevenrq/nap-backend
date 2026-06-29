package com.ns.nap_backend.auth.repository;

import com.ns.nap_backend.auth.entity.RefreshToken;
import com.ns.nap_backend.user.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * Revoca todos los refresh tokens activos de un usuario. Se usa al detectar reuso de un token ya
   * rotado (posible robo) y para el logout global.
   */
  @Modifying
  @Query(
      "UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now "
          + "WHERE rt.user = :user AND rt.revoked = false")
  int revokeAllByUser(@Param("user") User user, @Param("now") Instant now);
}
