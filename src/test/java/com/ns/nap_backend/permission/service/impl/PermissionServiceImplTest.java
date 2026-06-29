package com.ns.nap_backend.permission.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ns.nap_backend.permission.entity.Permission;
import com.ns.nap_backend.permission.repository.PermissionRepository;
import com.ns.nap_backend.support.TestEntities;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

  @Mock private PermissionRepository permissionRepository;

  @InjectMocks private PermissionServiceImpl permissionService;

  @Test
  void findByIdDelegatesToRepository() {
    Permission permission = TestEntities.permission(1L, "user:read");
    given(permissionRepository.findById(1L)).willReturn(Optional.of(permission));

    assertThat(permissionService.findById(1L)).contains(permission);
  }

  @Test
  void findByNameDelegatesToRepository() {
    Permission permission = TestEntities.permission(1L, "user:read");
    given(permissionRepository.findByName("user:read")).willReturn(Optional.of(permission));

    assertThat(permissionService.findByName("user:read")).contains(permission);
  }

  @Test
  void findAllDelegatesToRepository() {
    List<Permission> permissions = List.of(TestEntities.permission(1L, "user:read"));
    given(permissionRepository.findAll()).willReturn(permissions);

    assertThat(permissionService.findAll()).isEqualTo(permissions);
  }

  @Test
  void findByNameInDelegatesToRepository() {
    Set<Permission> permissions = Set.of(TestEntities.permission(1L, "user:read"));
    given(permissionRepository.findByNameIn(Set.of("user:read")))
        .willReturn(Optional.of(permissions));

    assertThat(permissionService.findByNameIn(Set.of("user:read"))).contains(permissions);
  }
}
