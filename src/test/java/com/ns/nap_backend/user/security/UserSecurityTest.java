package com.ns.nap_backend.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ns.nap_backend.support.TestEntities;
import com.ns.nap_backend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class UserSecurityTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserSecurity userSecurity;

  /** Token con authorities -> queda {@code isAuthenticated() == true}. */
  private static Authentication authenticatedAs(String username) {
    return new UsernamePasswordAuthenticationToken(username, "n/a", List.of());
  }

  @Test
  void isSelfTrueWhenAuthenticatedUserOwnsTheAccount() {
    given(userRepository.findById(51L)).willReturn(Optional.of(TestEntities.user(51L, "user01")));

    assertThat(userSecurity.isSelf(51L, authenticatedAs("user01"))).isTrue();
  }

  @Test
  void isSelfFalseWhenUsernameDoesNotMatch() {
    given(userRepository.findById(51L)).willReturn(Optional.of(TestEntities.user(51L, "user01")));

    assertThat(userSecurity.isSelf(51L, authenticatedAs("intruder"))).isFalse();
  }

  @Test
  void isSelfFalseWhenUserNotFound() {
    given(userRepository.findById(99L)).willReturn(Optional.empty());

    assertThat(userSecurity.isSelf(99L, authenticatedAs("user01"))).isFalse();
  }

  @Test
  void isSelfFalseForNullIdOrAuthentication() {
    assertThat(userSecurity.isSelf(null, authenticatedAs("user01"))).isFalse();
    assertThat(userSecurity.isSelf(51L, null)).isFalse();
  }

  @Test
  void isSelfFalseWhenNotAuthenticated() {
    var token = new UsernamePasswordAuthenticationToken("user01", "n/a");
    token.setAuthenticated(false);

    assertThat(userSecurity.isSelf(51L, token)).isFalse();
  }
}
