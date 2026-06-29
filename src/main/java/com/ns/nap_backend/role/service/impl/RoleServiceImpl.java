package com.ns.nap_backend.role.service.impl;

import com.ns.nap_backend.permission.entity.Permission;
import com.ns.nap_backend.permission.exception.UnknownPermissionException;
import com.ns.nap_backend.permission.repository.PermissionRepository;
import com.ns.nap_backend.role.dto.RoleCreationRequest;
import com.ns.nap_backend.role.dto.RoleUpdateRequest;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.role.exception.RoleAlreadyExistsException;
import com.ns.nap_backend.role.exception.RoleNotFoundException;
import com.ns.nap_backend.role.repository.RoleRepository;
import com.ns.nap_backend.role.service.RoleService;
import com.ns.nap_backend.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleServiceImpl implements RoleService {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final UserRepository userRepository;

  public RoleServiceImpl(
      RoleRepository roleRepository,
      PermissionRepository permissionRepository,
      UserRepository userRepository) {
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  @Override
  public Optional<Role> findById(Long id) {
    return roleRepository.findById(id);
  }

  @Transactional(readOnly = true)
  @Override
  public Optional<Role> findByName(String name) {
    return roleRepository.findByName(name);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Role> findAll() {
    return roleRepository.findAll();
  }

  @Transactional
  @Override
  public Role create(RoleCreationRequest request) {
    roleRepository
        .findByName(request.name())
        .ifPresent(
            existing -> {
              throw new RoleAlreadyExistsException(request.name());
            });

    Role role = new Role();
    role.setName(request.name());
    role.setDescription(request.description());
    role.setPermissions(resolvePermissions(request.permissionNames()));
    return roleRepository.save(role);
  }

  @Transactional
  @Override
  public Role update(Long id, RoleUpdateRequest request) {
    Role role = roleRepository.findById(id).orElseThrow(() -> new RoleNotFoundException(id));

    if (!role.getName().equals(request.name())) {
      roleRepository
          .findByName(request.name())
          .ifPresent(
              existing -> {
                throw new RoleAlreadyExistsException(request.name());
              });
    }

    role.setName(request.name());
    role.setDescription(request.description());
    return roleRepository.save(role);
  }

  @Transactional
  @Override
  public Role replacePermissions(Long id, Set<String> permissionNames) {
    Role role = roleRepository.findById(id).orElseThrow(() -> new RoleNotFoundException(id));
    role.setPermissions(resolvePermissions(permissionNames));
    return roleRepository.save(role);
  }

  @Transactional
  @Override
  public void deleteById(Long id) {
    Role role = roleRepository.findById(id).orElseThrow(() -> new RoleNotFoundException(id));

    // Desvincular el rol de todos los usuarios (tabla users_roles, propiedad de User) antes de
    // borrarlo, para evitar la violación de FK. Las filas de roles_permissions las limpia Hibernate
    // automáticamente al borrar el Role (que sí posee esa tabla de unión).
    // TODO: cuando se decida la invalidación activa de tokens, revocar aquí los refresh tokens de
    // los usuarios afectados (RefreshTokenRepository.revokeAllByUser) para que el cambio surta
    // efecto sin esperar a la expiración del access token.
    userRepository.deleteRoleAssignments(id);

    roleRepository.delete(role);
  }

  /**
   * Resuelve los nombres de permiso contra el catálogo. Rechaza cualquier nombre inexistente para
   * no permitir inventar permisos al componer un rol.
   */
  private Set<Permission> resolvePermissions(Set<String> permissionNames) {
    if (permissionNames == null || permissionNames.isEmpty()) {
      return new HashSet<>();
    }

    Set<Permission> found =
        permissionRepository.findByNameIn(permissionNames).orElseGet(HashSet::new);

    if (found.size() != permissionNames.size()) {
      Set<String> foundNames = found.stream().map(Permission::getName).collect(Collectors.toSet());
      Set<String> unknown =
          permissionNames.stream()
              .filter(name -> !foundNames.contains(name))
              .collect(Collectors.toSet());
      throw new UnknownPermissionException(unknown);
    }

    return found;
  }
}
