package com.ns.nap_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ns.nap_backend.config.security.JwtProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class JwtTokenGeneratorTest {

  @Mock private JwtEncoder jwtEncoder;
  @Mock private Jwt jwt;

  private JwtTokenGenerator generator;

  @BeforeEach
  void setUp() {
    generator =
        new JwtTokenGenerator(
            jwtEncoder,
            "nap-backend-auth-server",
            new JwtProperties(
                new JwtProperties.AccessToken(15), new JwtProperties.RefreshToken(7, null)));
  }

  @Test
  void createTokenSetsSubjectAndFiltersFactorAuthorities() {
    var authentication =
        new UsernamePasswordAuthenticationToken(
            "johndoe",
            null,
            List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("user:read"),
                new SimpleGrantedAuthority("FACTOR_PASSWORD")));
    given(jwtEncoder.encode(any())).willReturn(jwt);
    given(jwt.getTokenValue()).willReturn("signed-jwt");

    String token = generator.createToken(authentication);

    assertThat(token).isEqualTo("signed-jwt");

    ArgumentCaptor<JwtEncoderParameters> captor =
        ArgumentCaptor.forClass(JwtEncoderParameters.class);
    org.mockito.Mockito.verify(jwtEncoder).encode(captor.capture());
    var claims = captor.getValue().getClaims();

    assertThat(claims.getSubject()).isEqualTo("johndoe");
    assertThat(claims.getClaims().get("iss")).isEqualTo("nap-backend-auth-server");
    List<String> authorities = claims.getClaim("authorities");
    assertThat(authorities)
        .containsExactlyInAnyOrder("ROLE_USER", "user:read")
        .doesNotContain("FACTOR_PASSWORD");
  }
}
