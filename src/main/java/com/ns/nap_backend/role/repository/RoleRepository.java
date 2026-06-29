package com.ns.nap_backend.role.repository;

import com.ns.nap_backend.permission.entity.Permission;
import com.ns.nap_backend.role.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(String name);

  List<Role> findAllByPermissionsContaining(Permission permission);
}
