package com.ns.nap_backend.support;

import com.ns.nap_backend.permission.entity.Permission;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.user.entity.Address;
import com.ns.nap_backend.user.entity.User;
import java.util.HashSet;
import java.util.Set;

/**
 * Fábricas de entidades de prueba para reutilizar entre los tests unitarios y evitar duplicar el
 * cableado de {@code User}/{@code Role}/{@code Permission}.
 */
public final class TestEntities {

  private TestEntities() {}

  public static Permission permission(Long id, String name) {
    Permission permission = new Permission(name, name);
    permission.setId(id);
    return permission;
  }

  public static Role role(Long id, String name, Permission... permissions) {
    Role role = new Role();
    role.setId(id);
    role.setName(name);
    role.setPermissions(new HashSet<>(Set.of(permissions)));
    return role;
  }

  public static Address address(Long id, String street, String number, String city) {
    Address address = new Address();
    address.setId(id);
    address.setStreet(street);
    address.setNumber(number);
    address.setCity(city);
    return address;
  }

  public static User user(Long id, String username, Role... roles) {
    User user = new User();
    user.setId(id);
    user.setNationalId(1000000000L + (id == null ? 0 : id));
    user.setFirstName("Test");
    user.setLastName("User");
    user.setEmail(username + "@test.nap.com");
    user.setPhoneNumber(3000000000L + (id == null ? 0 : id));
    user.setUsername(username);
    user.setPassword("{bcrypt}hashed");
    user.setEnabled(true);
    user.setAccountNonExpired(true);
    user.setAccountNonLocked(true);
    user.setCredentialsNonExpired(true);
    user.setRoles(new HashSet<>(Set.of(roles)));
    return user;
  }
}
