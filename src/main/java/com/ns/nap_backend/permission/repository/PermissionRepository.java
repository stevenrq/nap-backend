package com.ns.nap_backend.permission.repository;

import com.ns.nap_backend.permission.entity.Permission;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
  Optional<Permission> findByName(String name);

  Optional<Set<Permission>> findByNameIn(Set<String> names);
}
