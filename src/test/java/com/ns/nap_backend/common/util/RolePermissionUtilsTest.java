package com.ns.nap_backend.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.role.exception.RoleNotFoundException;
import com.ns.nap_backend.role.exception.RoleRetrievalException;
import com.ns.nap_backend.role.repository.RoleRepository;
import com.ns.nap_backend.support.TestEntities;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

@ExtendWith(MockitoExtension.class)
class RolePermissionUtilsTest {

  @Mock private RoleRepository roleRepository;

  @Test
  void getRolesByNamesResolvesEachName() {
    Role admin = TestEntities.role(1L, "ADMIN");
    given(roleRepository.findByName("ADMIN")).willReturn(Optional.of(admin));

    Set<Role> roles = RolePermissionUtils.getRolesByNames(Set.of("ADMIN"), roleRepository);

    assertThat(roles).containsExactly(admin);
  }

  @Test
  void getRolesByNamesThrowsWhenEmpty() {
    assertThatThrownBy(() -> RolePermissionUtils.getRolesByNames(Set.of(), roleRepository))
        .isInstanceOf(RoleRetrievalException.class)
        .hasMessageContaining("must not be empty");
  }

  @Test
  void getRolesByNamesThrowsWhenNameUnknown() {
    given(roleRepository.findByName("GHOST")).willReturn(Optional.empty());

    assertThatThrownBy(() -> RolePermissionUtils.getRolesByNames(Set.of("GHOST"), roleRepository))
        .isInstanceOf(RoleNotFoundException.class)
        .hasMessageContaining("GHOST");
  }

  @Test
  void getRolesByNamesThrowsOnNull() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> RolePermissionUtils.getRolesByNames(null, roleRepository));
  }

  @Test
  void getRolesAndPermissionsReturnsRoleAndPermissionNames() {
    Role admin =
        TestEntities.role(
            1L,
            "ADMIN",
            TestEntities.permission(10L, "user:read"),
            TestEntities.permission(11L, "user:delete"));

    Set<String> names = RolePermissionUtils.getRolesAndPermissions(Set.of(admin));

    assertThat(names).containsExactlyInAnyOrder("ADMIN", "user:read", "user:delete");
  }

  @Test
  void getRolesAndPermissionsThrowsOnNull() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> RolePermissionUtils.getRolesAndPermissions(null));
  }

  @Test
  void buildAuthoritiesPrefixesRolesAndKeepsPermissionsBare() {
    Role admin = TestEntities.role(1L, "ADMIN", TestEntities.permission(10L, "user:read"));

    var authorities =
        RolePermissionUtils.buildAuthorities(Set.of(admin)).stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

    assertThat(authorities)
        .containsExactlyInAnyOrder("ROLE_ADMIN", "user:read")
        .doesNotContain("ROLE_user:read");
  }
}
