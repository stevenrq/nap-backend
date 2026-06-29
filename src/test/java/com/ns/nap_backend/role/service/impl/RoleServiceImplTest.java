package com.ns.nap_backend.role.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import com.ns.nap_backend.permission.entity.Permission;
import com.ns.nap_backend.permission.exception.UnknownPermissionException;
import com.ns.nap_backend.permission.repository.PermissionRepository;
import com.ns.nap_backend.role.dto.RoleCreationRequest;
import com.ns.nap_backend.role.dto.RoleUpdateRequest;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.role.exception.RoleAlreadyExistsException;
import com.ns.nap_backend.role.exception.RoleNotFoundException;
import com.ns.nap_backend.role.repository.RoleRepository;
import com.ns.nap_backend.support.TestEntities;
import com.ns.nap_backend.user.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

  @Mock private RoleRepository roleRepository;
  @Mock private PermissionRepository permissionRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private RoleServiceImpl roleService;

  private void stubSave() {
    given(roleRepository.save(org.mockito.ArgumentMatchers.any(Role.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void createRejectsDuplicateName() {
    given(roleRepository.findByName("ADMIN"))
        .willReturn(Optional.of(TestEntities.role(1L, "ADMIN")));

    assertThatThrownBy(() -> roleService.create(new RoleCreationRequest("ADMIN", "desc", Set.of())))
        .isInstanceOf(RoleAlreadyExistsException.class);
  }

  @Test
  void createRejectsUnknownPermission() {
    given(roleRepository.findByName("EDITOR")).willReturn(Optional.empty());
    // El catálogo solo resuelve "role:read"; "ghost" no existe -> tamaños distintos.
    given(permissionRepository.findByNameIn(Set.of("role:read", "ghost")))
        .willReturn(Optional.of(Set.of(TestEntities.permission(1L, "role:read"))));

    assertThatThrownBy(
            () ->
                roleService.create(
                    new RoleCreationRequest("EDITOR", "desc", Set.of("role:read", "ghost"))))
        .isInstanceOf(UnknownPermissionException.class)
        .hasMessageContaining("ghost");
  }

  @Test
  void createPersistsRoleWithResolvedPermissions() {
    Permission read = TestEntities.permission(1L, "role:read");
    given(roleRepository.findByName("EDITOR")).willReturn(Optional.empty());
    given(permissionRepository.findByNameIn(Set.of("role:read")))
        .willReturn(Optional.of(Set.of(read)));
    stubSave();

    Role created =
        roleService.create(new RoleCreationRequest("EDITOR", "desc", Set.of("role:read")));

    assertThat(created.getName()).isEqualTo("EDITOR");
    assertThat(created.getPermissions()).containsExactly(read);
  }

  @Test
  void updateRejectsMissingRole() {
    given(roleRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roleService.update(99L, new RoleUpdateRequest("X", "d")))
        .isInstanceOf(RoleNotFoundException.class);
  }

  @Test
  void updateRejectsRenameToExistingName() {
    given(roleRepository.findById(2L)).willReturn(Optional.of(TestEntities.role(2L, "USER")));
    given(roleRepository.findByName("ADMIN"))
        .willReturn(Optional.of(TestEntities.role(1L, "ADMIN")));

    assertThatThrownBy(() -> roleService.update(2L, new RoleUpdateRequest("ADMIN", "d")))
        .isInstanceOf(RoleAlreadyExistsException.class);
  }

  @Test
  void updateChangesNameAndDescription() {
    given(roleRepository.findById(2L)).willReturn(Optional.of(TestEntities.role(2L, "USER")));
    stubSave();

    Role updated = roleService.update(2L, new RoleUpdateRequest("MEMBER", "nuevo"));

    assertThat(updated.getName()).isEqualTo("MEMBER");
    assertThat(updated.getDescription()).isEqualTo("nuevo");
  }

  @Test
  void replacePermissionsRejectsMissingRole() {
    given(roleRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roleService.replacePermissions(99L, Set.of("role:read")))
        .isInstanceOf(RoleNotFoundException.class);
  }

  @Test
  void replacePermissionsSetsResolvedPermissions() {
    Permission read = TestEntities.permission(1L, "role:read");
    given(roleRepository.findById(2L)).willReturn(Optional.of(TestEntities.role(2L, "USER")));
    given(permissionRepository.findByNameIn(Set.of("role:read")))
        .willReturn(Optional.of(Set.of(read)));
    stubSave();

    Role updated = roleService.replacePermissions(2L, Set.of("role:read"));

    assertThat(updated.getPermissions()).containsExactly(read);
  }

  @Test
  void deleteRejectsMissingRole() {
    given(roleRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roleService.deleteById(99L)).isInstanceOf(RoleNotFoundException.class);
  }

  @Test
  void deleteUnlinksUserAssignmentsBeforeDeletingRole() {
    Role role = TestEntities.role(2L, "USER");
    given(roleRepository.findById(2L)).willReturn(Optional.of(role));

    roleService.deleteById(2L);

    var ordered = inOrder(userRepository, roleRepository);
    ordered.verify(userRepository).deleteRoleAssignments(2L);
    ordered.verify(roleRepository).delete(role);
  }
}
