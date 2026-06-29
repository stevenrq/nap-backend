package com.ns.nap_backend.role.service;

import com.ns.nap_backend.role.dto.RoleCreationRequest;
import com.ns.nap_backend.role.dto.RoleUpdateRequest;
import com.ns.nap_backend.role.entity.Role;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleService {

  Optional<Role> findById(Long id);

  Optional<Role> findByName(String name);

  List<Role> findAll();

  Role create(RoleCreationRequest request);

  Role update(Long id, RoleUpdateRequest request);

  Role replacePermissions(Long id, Set<String> permissionNames);

  void deleteById(Long id);
}
