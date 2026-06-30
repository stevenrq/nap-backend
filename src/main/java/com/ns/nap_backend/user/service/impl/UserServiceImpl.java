package com.ns.nap_backend.user.service.impl;

import com.ns.nap_backend.common.util.RolePermissionUtils;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.role.repository.RoleRepository;
import com.ns.nap_backend.user.dto.UserUpdateRequest;
import com.ns.nap_backend.user.entity.Address;
import com.ns.nap_backend.user.entity.User;
import com.ns.nap_backend.user.repository.UserRepository;
import com.ns.nap_backend.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

  private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  @Transactional
  public Optional<User> update(Long id, UserUpdateRequest userUpdateRequest) {
    return userRepository.findById(id).map(user -> applyUpdate(user, userUpdateRequest));
  }

  @Transactional
  public void deleteById(Long id) {
    userRepository.deleteById(id);
    log.info("User deleted (id={})", id);
  }

  private User applyUpdate(User user, UserUpdateRequest request) {
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setPhoneNumber(request.getPhoneNumber());
    user.setEmail(request.getEmail());
    user.setUsername(request.getUsername());

    applyAddress(user, request.getAddress());
    applyRoles(user, request);

    // El password es opcional en la actualización: solo se reescribe (y re-cifra) si viene
    // explícitamente en la petición; en caso contrario se conserva el actual.
    if (request.getPassword() != null && !request.getPassword().isBlank()) {
      user.setPassword(passwordEncoder.encode(request.getPassword()));
    }

    log.info("User updated (id={})", user.getId());
    return userRepository.save(user);
  }

  /**
   * Aplica los cambios de roles solo si el llamador tiene el permiso administrativo {@code
   * user:update}. En un self-update (un usuario editando su propia cuenta vía {@code
   * UserSecurity#isSelf}) los roles se conservan intactos, de modo que nadie pueda autoascenderse
   * incluyendo roles en su propia petición. Si no se envían roles, también se conservan los
   * actuales para no desproteger la cuenta.
   */
  private void applyRoles(User user, UserUpdateRequest request) {
    if (!callerCanManageRoles() || request.getRoles() == null || request.getRoles().isEmpty()) {
      return;
    }
    Set<String> roleNames =
        request.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    user.setRoles(RolePermissionUtils.getRolesByNames(roleNames, roleRepository));
  }

  private boolean callerCanManageRoles() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("user:update"::equals);
  }

  /**
   * Concilia la dirección del usuario con la de la petición: si ya tenía una, actualiza sus campos
   * in situ (aprovechando el {@code cascade} y {@code orphanRemoval} del mapeo); si no tenía,
   * adjunta la nueva como entidad fresca; y si la petición no trae dirección, la elimina.
   */
  private void applyAddress(User user, Address newAddress) {
    if (newAddress == null) {
      user.setAddress(null);
      return;
    }

    Address current = user.getAddress();
    if (current != null) {
      current.setStreet(newAddress.getStreet());
      current.setNumber(newAddress.getNumber());
      current.setCity(newAddress.getCity());
    } else {
      newAddress.setId(null);
      user.setAddress(newAddress);
    }
  }
}
