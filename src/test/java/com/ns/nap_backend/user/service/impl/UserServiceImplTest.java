package com.ns.nap_backend.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.role.exception.RoleNotFoundException;
import com.ns.nap_backend.role.repository.RoleRepository;
import com.ns.nap_backend.support.TestEntities;
import com.ns.nap_backend.user.dto.UserUpdateRequest;
import com.ns.nap_backend.user.entity.Address;
import com.ns.nap_backend.user.entity.User;
import com.ns.nap_backend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserServiceImpl userService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateWith(String... authorities) {
    var granted = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("caller", null, granted));
  }

  private UserUpdateRequest baseRequest() {
    UserUpdateRequest request = new UserUpdateRequest();
    request.setFirstName("Nuevo");
    request.setLastName("Nombre");
    request.setPhoneNumber(3001112233L);
    request.setEmail("nuevo@correo.nap.com");
    request.setUsername("user01");
    return request;
  }

  private void stubSave() {
    given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void updateReturnsEmptyWhenUserNotFound() {
    given(userRepository.findById(99L)).willReturn(Optional.empty());

    assertThat(userService.update(99L, baseRequest())).isEmpty();
    verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void updateCopiesBasicFields() {
    authenticateWith("user:read");
    User existing = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    given(userRepository.findById(51L)).willReturn(Optional.of(existing));
    stubSave();

    User updated = userService.update(51L, baseRequest()).orElseThrow();

    assertThat(updated.getFirstName()).isEqualTo("Nuevo");
    assertThat(updated.getLastName()).isEqualTo("Nombre");
    assertThat(updated.getEmail()).isEqualTo("nuevo@correo.nap.com");
    assertThat(updated.getPhoneNumber()).isEqualTo(3001112233L);
  }

  @Test
  void adminCanChangeRoles() {
    authenticateWith("user:update");
    User existing = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    Role admin = TestEntities.role(1L, "ADMIN");
    given(userRepository.findById(51L)).willReturn(Optional.of(existing));
    given(roleRepository.findByName("ADMIN")).willReturn(Optional.of(admin));
    stubSave();

    UserUpdateRequest request = baseRequest();
    request.setRoles(Set.of(TestEntities.role(null, "ADMIN")));

    User updated = userService.update(51L, request).orElseThrow();

    assertThat(updated.getRoles()).extracting(Role::getName).containsExactly("ADMIN");
  }

  @Test
  void selfUpdatePreservesRolesAndCannotEscalate() {
    authenticateWith("user:read"); // no user:update -> es un self-update
    User existing = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    given(userRepository.findById(51L)).willReturn(Optional.of(existing));
    stubSave();

    UserUpdateRequest request = baseRequest();
    request.setRoles(Set.of(TestEntities.role(null, "ADMIN"))); // intento de auto-ascenso

    User updated = userService.update(51L, request).orElseThrow();

    assertThat(updated.getRoles()).extracting(Role::getName).containsExactly("USER");
    verify(roleRepository, never()).findByName(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void unknownRoleFromAdminPropagatesRoleNotFound() {
    authenticateWith("user:update");
    User existing = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    given(userRepository.findById(51L)).willReturn(Optional.of(existing));
    given(roleRepository.findByName("GHOST")).willReturn(Optional.empty());

    UserUpdateRequest request = baseRequest();
    request.setRoles(Set.of(TestEntities.role(null, "GHOST")));

    assertThatThrownBy(() -> userService.update(51L, request))
        .isInstanceOf(RoleNotFoundException.class);
  }

  @Test
  void passwordEncodedOnlyWhenProvided() {
    authenticateWith("user:read");
    User existing = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    existing.setPassword("OLD");
    given(userRepository.findById(51L)).willReturn(Optional.of(existing));
    given(passwordEncoder.encode("nuevaClave")).willReturn("ENC");
    stubSave();

    UserUpdateRequest request = baseRequest();
    request.setPassword("nuevaClave");

    User updated = userService.update(51L, request).orElseThrow();
    assertThat(updated.getPassword()).isEqualTo("ENC");
  }

  @Test
  void passwordUnchangedWhenAbsent() {
    authenticateWith("user:read");
    User existing = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    existing.setPassword("OLD");
    given(userRepository.findById(51L)).willReturn(Optional.of(existing));
    stubSave();

    User updated = userService.update(51L, baseRequest()).orElseThrow();

    assertThat(updated.getPassword()).isEqualTo("OLD");
    verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void addressUpdatedInPlaceWhenAlreadyPresent() {
    authenticateWith("user:read");
    User existing = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    Address current = TestEntities.address(7L, "Vieja", "1", "Cali");
    existing.setAddress(current);
    given(userRepository.findById(51L)).willReturn(Optional.of(existing));
    stubSave();

    UserUpdateRequest request = baseRequest();
    request.setAddress(TestEntities.address(null, "Calle Nueva", "789", "Medellin"));

    User updated = userService.update(51L, request).orElseThrow();

    assertThat(updated.getAddress()).isSameAs(current); // se reusa la entidad existente
    assertThat(updated.getAddress().getStreet()).isEqualTo("Calle Nueva");
    assertThat(updated.getAddress().getId()).isEqualTo(7L);
  }

  @Test
  void addressAttachedFreshWhenNonePresent() {
    authenticateWith("user:read");
    User existing = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    existing.setAddress(null);
    given(userRepository.findById(51L)).willReturn(Optional.of(existing));
    stubSave();

    UserUpdateRequest request = baseRequest();
    request.setAddress(TestEntities.address(99L, "Calle Nueva", "789", "Medellin"));

    User updated = userService.update(51L, request).orElseThrow();

    assertThat(updated.getAddress().getStreet()).isEqualTo("Calle Nueva");
    assertThat(updated.getAddress().getId()).isNull(); // se fuerza id=null para insertar
  }

  @Test
  void addressRemovedWhenRequestHasNone() {
    authenticateWith("user:read");
    User existing = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    existing.setAddress(TestEntities.address(7L, "Vieja", "1", "Cali"));
    given(userRepository.findById(51L)).willReturn(Optional.of(existing));
    stubSave();

    User updated = userService.update(51L, baseRequest()).orElseThrow();

    assertThat(updated.getAddress()).isNull();
  }
}
