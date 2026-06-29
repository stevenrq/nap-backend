package com.ns.nap_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ns.nap_backend.auth.dto.AuthCreateUserRequest;
import com.ns.nap_backend.auth.dto.AuthLoginRequest;
import com.ns.nap_backend.auth.dto.AuthResult;
import com.ns.nap_backend.auth.dto.RotatedRefreshToken;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.role.repository.RoleRepository;
import com.ns.nap_backend.support.TestEntities;
import com.ns.nap_backend.user.entity.User;
import com.ns.nap_backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock private AuthenticationManager authenticationManager;
  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtTokenGenerator jwtTokenGenerator;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private TokenDenylistService tokenDenylistService;

  @InjectMocks private AuthenticationService authenticationService;

  private AuthCreateUserRequest signUpRequest() {
    AuthCreateUserRequest request = new AuthCreateUserRequest();
    request.setNationalId(1003395547L);
    request.setFirstName("Steven");
    request.setLastName("Ricardo");
    request.setPhoneNumber(3148135687L);
    request.setEmail("steven@example.nap.com");
    request.setUsername("stevenrq8");
    request.setPassword("s3cr3tpass");
    return request;
  }

  @Test
  void signUpThrowsConflictWhenUsernameExists() {
    given(userRepository.findByUsername("stevenrq8"))
        .willReturn(Optional.of(TestEntities.user(1L, "stevenrq8")));

    assertThatThrownBy(() -> authenticationService.signUpUser(signUpRequest()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Username already exists");
  }

  @Test
  void signUpEncodesPasswordAssignsUserRoleAndSavesWithoutTokens() {
    Role userRole = TestEntities.role(2L, "USER");
    given(userRepository.findByUsername("stevenrq8")).willReturn(Optional.empty());
    given(roleRepository.findByName("USER")).willReturn(Optional.of(userRole));
    given(passwordEncoder.encode("s3cr3tpass")).willReturn("ENC");

    AuthResult result = authenticationService.signUpUser(signUpRequest());

    assertThat(result.username()).isEqualTo("stevenrq8");
    assertThat(result.accessToken()).isNull();
    assertThat(result.refreshToken()).isNull();

    var captor = org.mockito.ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getPassword()).isEqualTo("ENC");
    assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("USER");
  }

  @Test
  void loginAuthenticatesAndIssuesTokens() {
    User user = TestEntities.user(1L, "johndoe", TestEntities.role(2L, "USER"));
    Authentication authentication =
        new UsernamePasswordAuthenticationToken("johndoe", "s3cr3tpass");
    given(authenticationManager.authenticate(any())).willReturn(authentication);
    given(userRepository.findByUsername("johndoe")).willReturn(Optional.of(user));
    given(jwtTokenGenerator.createToken(authentication)).willReturn("access-jwt");
    given(refreshTokenService.issue(user)).willReturn("raw-refresh");

    AuthResult result =
        authenticationService.loginUser(new AuthLoginRequest("johndoe", "s3cr3tpass"));

    assertThat(result.accessToken()).isEqualTo("access-jwt");
    assertThat(result.refreshToken()).isEqualTo("raw-refresh");
  }

  @Test
  void loginThrowsUnauthorizedWhenUserMissingAfterAuth() {
    given(authenticationManager.authenticate(any()))
        .willReturn(new UsernamePasswordAuthenticationToken("ghost", "x"));
    given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

    assertThatThrownBy(() -> authenticationService.loginUser(new AuthLoginRequest("ghost", "x")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void refreshRotatesTokenAndMintsNewAccessToken() {
    User user = TestEntities.user(1L, "johndoe", TestEntities.role(2L, "USER"));
    given(refreshTokenService.validateAndRotate("old-raw"))
        .willReturn(new RotatedRefreshToken(user, "new-raw"));
    given(jwtTokenGenerator.createToken(any())).willReturn("new-access");

    AuthResult result = authenticationService.refresh("old-raw");

    assertThat(result.accessToken()).isEqualTo("new-access");
    assertThat(result.refreshToken()).isEqualTo("new-raw");
    assertThat(result.username()).isEqualTo("johndoe");
  }

  @Test
  void logoutRevokesRefreshTokenAndDenylistsAccessToken() {
    Instant exp = Instant.now().plusSeconds(900);

    authenticationService.logout("raw-refresh", "jti-123", exp);

    verify(refreshTokenService).revoke("raw-refresh");
    verify(tokenDenylistService).denylist("jti-123", exp);
  }
}
