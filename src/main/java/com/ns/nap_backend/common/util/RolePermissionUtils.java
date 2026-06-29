package com.ns.nap_backend.common.util;

import com.ns.nap_backend.permission.entity.Permission;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.role.exception.RoleNotFoundException;
import com.ns.nap_backend.role.exception.RoleRetrievalException;
import com.ns.nap_backend.role.repository.RoleRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * RolePermissionUtils es una clase de utilidad que proporciona métodos para manejar roles y
 * permisos de usuarios en la aplicación. Incluye métodos para obtener roles y permisos a partir de
 * un conjunto de roles, construir una lista de GrantedAuthority a partir de roles, y resolver
 * entidades {@link Role} a partir de sus nombres.
 */
public class RolePermissionUtils {
  private RolePermissionUtils() {}

  public static Set<String> getRolesAndPermissions(Set<Role> roles) {
    Objects.requireNonNull(roles, "El conjunto de roles no debe ser nulo");

    return Stream.concat(
            roles.stream().map(Role::getName),
            roles.stream().flatMap(role -> role.getPermissions().stream()).map(Permission::getName))
        .collect(Collectors.toSet());
  }

  /**
   * Construye una lista de {@link GrantedAuthority} a partir de un conjunto de roles. Los roles se
   * añaden con el prefijo "ROLE_" y los permisos se añaden sin prefijo, ya que el {@code
   * JwtAuthenticationConverter} los convierte directamente en {@code GrantedAuthority}.
   *
   * @param roles el conjunto de roles del usuario
   * @return una lista de {@code GrantedAuthority} que representa los roles y permisos del usuario
   */
  public static List<GrantedAuthority> buildAuthorities(Set<Role> roles) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    roles.forEach(
        role -> authorities.add(new SimpleGrantedAuthority("ROLE_".concat(role.getName()))));
    roles.stream()
        .flatMap(role -> role.getPermissions().stream())
        .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.getName())));
    return authorities;
  }

  /**
   * Resuelve las entidades {@link Role} correspondientes a un conjunto de nombres de rol. Pensado
   * para el flujo de actualización de usuario, donde la petición trae los roles como nombres y hay
   * que materializarlos contra la base de datos.
   *
   * <p>Se exige que {@code roleNames} no esté vacío: en una actualización, dejar a un usuario sin
   * roles explícitos podría desproteger una cuenta activa de forma accidental. Además, cada nombre
   * debe corresponder a un rol existente; un nombre desconocido se rechaza en lugar de ignorarse en
   * silencio, para que un cliente no pueda dejar al usuario con menos roles de los que pidió por un
   * simple typo.
   *
   * @param roleNames nombres de rol a resolver
   * @param roleRepository repositorio para buscar cada rol por nombre
   * @return las entidades {@link Role} correspondientes a esos nombres
   * @throws RoleRetrievalException si {@code roleNames} está vacío
   * @throws RoleNotFoundException si algún nombre no corresponde a un rol existente
   */
  public static Set<Role> getRolesByNames(Set<String> roleNames, RoleRepository roleRepository) {
    Objects.requireNonNull(roleNames, "El conjunto de nombres de rol no debe ser nulo");

    if (roleNames.isEmpty()) {
      throw new RoleRetrievalException(
          "User roles must not be empty", new NoSuchElementException());
    }

    Set<Role> roles = new HashSet<>();
    for (String name : roleNames) {
      roles.add(roleRepository.findByName(name).orElseThrow(() -> new RoleNotFoundException(name)));
    }
    return roles;
  }
}
