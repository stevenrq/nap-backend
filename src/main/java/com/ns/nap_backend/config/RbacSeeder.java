package com.ns.nap_backend.config;

import com.ns.nap_backend.config.security.PermissionCatalog;
import com.ns.nap_backend.permission.entity.Permission;
import com.ns.nap_backend.permission.repository.PermissionRepository;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.role.repository.RoleRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconcilia la base de datos con el {@link PermissionCatalog} (única fuente de verdad) en cada
 * arranque, de forma idempotente: inserta los permisos que falten y mantiene su descripción al día.
 * Como el catálogo vive en código, esto funciona igual con {@code ddl-auto=create-drop} (dev) que
 * con una base de datos persistente (prod), sin depender de {@code data.sql} para los permisos.
 *
 * <p>Política para permisos "huérfanos" (presentes en la BD pero ya no en el catálogo): NO se
 * borran automáticamente, ya que podrían estar referenciados por roles; el caso debería resolverse
 * con una migración explícita. Aquí solo se garantiza que el catálogo esté completo.
 *
 * <p>También garantiza el rol bootstrap {@code ADMIN} (con todos los permisos del catálogo) y el
 * rol base {@code USER} (con {@code user:read}), de modo que siempre exista un superadmin operativo
 * y los nuevos permisos se incorporen a {@code ADMIN} al añadirse al catálogo. Las asignaciones de
 * roles a usuarios concretos siguen gestionándose como datos (panel / {@code data.sql} en dev).
 */
@Component
@Order(1)
public class RbacSeeder implements ApplicationRunner {

  private static final String ADMIN_ROLE = "ADMIN";
  private static final String USER_ROLE = "USER";

  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;

  public RbacSeeder(PermissionRepository permissionRepository, RoleRepository roleRepository) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
  }

  @Transactional
  @Override
  public void run(ApplicationArguments args) {
    Map<String, Permission> catalog = upsertCatalogPermissions();

    // ADMIN: unión con todos los permisos del catálogo (incorpora los nuevos sin quitar lo demás).
    Role admin = ensureRole(ADMIN_ROLE, "Rol administrador con todos los permisos del catálogo");
    if (admin.getPermissions().addAll(catalog.values())) {
      roleRepository.save(admin);
    }

    // USER: garantiza user:read.
    Role user = ensureRole(USER_ROLE, "Rol usuario con permiso de lectura de usuarios");
    Permission userRead = catalog.get(PermissionCatalog.USER_READ.getPermissionName());
    if (user.getPermissions().add(userRead)) {
      roleRepository.save(user);
    }
  }

  private Map<String, Permission> upsertCatalogPermissions() {
    Map<String, Permission> catalog = new HashMap<>();
    for (PermissionCatalog entry : PermissionCatalog.values()) {
      Permission permission =
          permissionRepository
              .findByName(entry.getPermissionName())
              .orElseGet(() -> new Permission(entry.getPermissionName(), entry.getDescription()));
      if (!entry.getDescription().equals(permission.getDescription())) {
        permission.setDescription(entry.getDescription());
      }
      catalog.put(entry.getPermissionName(), permissionRepository.save(permission));
    }
    return catalog;
  }

  private Role ensureRole(String name, String description) {
    return roleRepository
        .findByName(name)
        .orElseGet(
            () -> {
              Role role = new Role();
              role.setName(name);
              role.setDescription(description);
              return roleRepository.save(role);
            });
  }
}
