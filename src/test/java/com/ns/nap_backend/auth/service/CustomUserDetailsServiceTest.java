package com.ns.nap_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ns.nap_backend.support.TestEntities;
import com.ns.nap_backend.user.entity.User;
import com.ns.nap_backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private CustomUserDetailsService service;

  @Test
  void loadsUserAndMapsAuthorities() {
    User user =
        TestEntities.user(
            1L,
            "johndoe",
            TestEntities.role(2L, "USER", TestEntities.permission(10L, "user:read")));
    given(userRepository.findByUsername("johndoe")).willReturn(Optional.of(user));

    UserDetails details = service.loadUserByUsername("johndoe");

    assertThat(details.getUsername()).isEqualTo("johndoe");
    assertThat(details.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_USER", "user:read");
  }

  @Test
  void throwsWhenUsernameNotFound() {
    given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
