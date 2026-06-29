package com.ns.nap_backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ns.nap_backend.role.repository.RoleRepository;
import com.ns.nap_backend.support.TestEntities;
import com.ns.nap_backend.user.entity.User;
import com.ns.nap_backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserSeederTest {

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private AdminUserSeeder seeder;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(seeder, "adminUsername", "admin");
    ReflectionTestUtils.setField(seeder, "adminPassword", "secreto");
    ReflectionTestUtils.setField(seeder, "adminEmail", "admin@sistema.nap.com");
  }

  @Test
  void noCreaNadaSiElAdminYaExiste() {
    given(userRepository.findByUsername("admin"))
        .willReturn(Optional.of(TestEntities.user(1L, "admin")));

    seeder.run(null);

    verify(userRepository, never()).save(any());
  }

  @Test
  void creaElAdminCuandoNoExiste() {
    var adminRole = TestEntities.role(1L, "ADMIN");
    given(userRepository.findByUsername("admin")).willReturn(Optional.empty());
    given(roleRepository.findByName("ADMIN")).willReturn(Optional.of(adminRole));
    given(passwordEncoder.encode("secreto")).willReturn("$2a$hashed");
    given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

    seeder.run(null);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());

    User guardado = captor.getValue();
    assertThat(guardado.getUsername()).isEqualTo("admin");
    assertThat(guardado.getPassword()).isEqualTo("$2a$hashed");
    assertThat(guardado.getEmail()).isEqualTo("admin@sistema.nap.com");
    assertThat(guardado.getRoles()).contains(adminRole);
  }

  @Test
  void lanzaExcepcionSiElRolAdminNoExiste() {
    given(userRepository.findByUsername("admin")).willReturn(Optional.empty());
    given(roleRepository.findByName("ADMIN")).willReturn(Optional.empty());

    assertThatThrownBy(() -> seeder.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ADMIN");
  }
}
