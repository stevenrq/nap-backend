package com.ns.nap_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ns.nap_backend.auth.dto.RotatedRefreshToken;
import com.ns.nap_backend.auth.entity.RefreshToken;
import com.ns.nap_backend.auth.repository.RefreshTokenRepository;
import com.ns.nap_backend.config.security.JwtProperties;
import com.ns.nap_backend.support.TestEntities;
import com.ns.nap_backend.user.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private RefreshTokenReuseHandler reuseHandler;

  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    JwtProperties jwtProperties =
        new JwtProperties(
            new JwtProperties.AccessToken(15), new JwtProperties.RefreshToken(7, null));
    refreshTokenService =
        new RefreshTokenService(refreshTokenRepository, reuseHandler, jwtProperties);
  }

  private static RefreshToken token(User user, boolean revoked, Instant expiresAt) {
    RefreshToken token = new RefreshToken();
    token.setUser(user);
    token.setTokenHash("hash");
    token.setRevoked(revoked);
    token.setCreatedAt(Instant.now());
    token.setExpiresAt(expiresAt);
    return token;
  }

  @Test
  void issuePersistsHashedTokenAndReturnsRawValue() {
    User user = TestEntities.user(1L, "johndoe");

    String raw = refreshTokenService.issue(user);

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());
    RefreshToken saved = captor.getValue();

    assertThat(raw).isNotBlank();
    assertThat(saved.getTokenHash()).isNotEqualTo(raw).hasSize(64); // SHA-256 hex
    assertThat(saved.isRevoked()).isFalse();
    assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    assertThat(saved.getUser()).isSameAs(user);
  }

  @Test
  void validateAndRotateRevokesCurrentAndIssuesNew() {
    User user = TestEntities.user(1L, "johndoe");
    RefreshToken current = token(user, false, Instant.now().plus(1, ChronoUnit.DAYS));
    given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(current));

    RotatedRefreshToken rotated = refreshTokenService.validateAndRotate("old-raw");

    assertThat(rotated.user()).isSameAs(user);
    assertThat(rotated.rawToken()).isNotBlank();
    assertThat(current.isRevoked()).isTrue();
    verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
  }

  @Test
  void validateAndRotateRejectsMissingToken() {
    assertThatThrownBy(() -> refreshTokenService.validateAndRotate("  "))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Missing refresh token");
    verifyNoInteractions(refreshTokenRepository);
  }

  @Test
  void validateAndRotateRejectsUnknownToken() {
    given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.empty());

    assertThatThrownBy(() -> refreshTokenService.validateAndRotate("ghost"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Invalid refresh token");
  }

  @Test
  void validateAndRotateDetectsReuseAndRevokesAllSessions() {
    User user = TestEntities.user(1L, "johndoe");
    RefreshToken revoked = token(user, true, Instant.now().plus(1, ChronoUnit.DAYS));
    given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(revoked));

    assertThatThrownBy(() -> refreshTokenService.validateAndRotate("reused"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("reuse detected");
    verify(reuseHandler).revokeAllSessions(user);
  }

  @Test
  void validateAndRotateRejectsExpiredToken() {
    User user = TestEntities.user(1L, "johndoe");
    RefreshToken expired = token(user, false, Instant.now().minus(1, ChronoUnit.DAYS));
    given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(expired));

    assertThatThrownBy(() -> refreshTokenService.validateAndRotate("expired"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Expired refresh token");
  }

  @Test
  void revokeIsNoOpForBlankToken() {
    refreshTokenService.revoke("   ");
    verifyNoInteractions(refreshTokenRepository);
  }

  @Test
  void revokeMarksActiveTokenAsRevoked() {
    User user = TestEntities.user(1L, "johndoe");
    RefreshToken active = token(user, false, Instant.now().plus(1, ChronoUnit.DAYS));
    given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(active));

    refreshTokenService.revoke("raw");

    assertThat(active.isRevoked()).isTrue();
    assertThat(active.getRevokedAt()).isNotNull();
    verify(refreshTokenRepository).save(active);
  }

  @Test
  void revokeIgnoresAlreadyRevokedToken() {
    User user = TestEntities.user(1L, "johndoe");
    RefreshToken alreadyRevoked = token(user, true, Instant.now().plus(1, ChronoUnit.DAYS));
    given(refreshTokenRepository.findByTokenHash(anyString()))
        .willReturn(Optional.of(alreadyRevoked));

    refreshTokenService.revoke("raw");

    verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
  }
}
