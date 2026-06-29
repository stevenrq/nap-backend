package com.ns.nap_backend.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ns.nap_backend.auth.service.TokenDenylistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class DenylistJwtValidatorTest {

  @Mock private TokenDenylistService tokenDenylistService;
  @Mock private Jwt jwt;

  @InjectMocks private DenylistJwtValidator validator;

  @Test
  void failsWhenJtiIsDenylisted() {
    given(jwt.getId()).willReturn("jti-1");
    given(tokenDenylistService.isDenylisted("jti-1")).willReturn(true);

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors()).anyMatch(error -> error.getErrorCode().equals("invalid_token"));
  }

  @Test
  void succeedsWhenJtiIsNotDenylisted() {
    given(jwt.getId()).willReturn("jti-2");
    given(tokenDenylistService.isDenylisted("jti-2")).willReturn(false);

    assertThat(validator.validate(jwt).hasErrors()).isFalse();
  }
}
