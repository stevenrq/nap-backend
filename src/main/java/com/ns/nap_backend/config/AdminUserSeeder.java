package com.ns.nap_backend.config;

import com.ns.nap_backend.role.repository.RoleRepository;
import com.ns.nap_backend.user.entity.Address;
import com.ns.nap_backend.user.entity.User;
import com.ns.nap_backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garantiza que exista un usuario administrador en la BD al arrancar. Es idempotente: si ya existe
 * un usuario con {@code admin.username}, no hace nada. Corre después de {@link RbacSeeder} (order
 * 2) para que el rol {@code ADMIN} esté disponible. Las credenciales se configuran vía env vars
 * {@code ADMIN_USERNAME}, {@code ADMIN_PASSWORD} y {@code ADMIN_EMAIL}; el resto de los campos de
 * {@code Person} son valores de sistema fijos.
 */
@Component
@Order(2)
public class AdminUserSeeder implements ApplicationRunner {

  private static final String ADMIN_ROLE = "ADMIN";
  private static final String SYSTEM_VALUE = "System";

  @Value("${admin.username:admin}")
  private String adminUsername;

  @Value("${admin.password:admin}")
  private String adminPassword;

  @Value("${admin.email:admin@system.nap.com}")
  private String adminEmail;

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  public AdminUserSeeder(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.findByUsername(adminUsername).isPresent()) {
      return;
    }

    var adminRole =
        roleRepository
            .findByName(ADMIN_ROLE)
            .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

    Address address = new Address();
    address.setStreet(SYSTEM_VALUE);
    address.setNumber("0");
    address.setCity(SYSTEM_VALUE);

    User admin = new User();
    admin.setNationalId(0L);
    admin.setFirstName("Admin");
    admin.setLastName(SYSTEM_VALUE);
    admin.setEmail(adminEmail);
    admin.setPhoneNumber(0L);
    admin.setAddress(address);
    admin.setUsername(adminUsername);
    admin.setPassword(passwordEncoder.encode(adminPassword));
    admin.getRoles().add(adminRole);

    userRepository.save(admin);
  }
}
