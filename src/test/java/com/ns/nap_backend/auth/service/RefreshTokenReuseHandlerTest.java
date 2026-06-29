package com.ns.nap_backend.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.ns.nap_backend.auth.repository.RefreshTokenRepository;
import com.ns.nap_backend.support.TestEntities;
import com.ns.nap_backend.user.entity.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenReuseHandlerTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private RefreshTokenReuseHandler reuseHandler;

  @Test
  void revokeAllSessionsRevokesEveryTokenOfTheUser() {
    User user = TestEntities.user(1L, "johndoe");

    reuseHandler.revokeAllSessions(user);

    verify(refreshTokenRepository).revokeAllByUser(eq(user), any(Instant.class));
  }
}
