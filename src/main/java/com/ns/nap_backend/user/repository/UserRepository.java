package com.ns.nap_backend.user.repository;

import com.ns.nap_backend.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  /**
   * Elimina las filas de la tabla de unión {@code users_roles} para un rol dado, desvinculándolo de
   * todos los usuarios. Se hace con un borrado directo (no cargando entidades {@code User}) para
   * evitar disparar la validación de Bean Validation en el flush del UPDATE y por eficiencia. Es el
   * paso previo necesario para borrar un rol, ya que {@code User} es el dueño de {@code
   * users_roles} (Hibernate no limpia esa tabla al borrar la entidad {@code Role}).
   */
  @Modifying
  @Query(value = "DELETE FROM users_roles WHERE role_id = :roleId", nativeQuery = true)
  void deleteRoleAssignments(@Param("roleId") Long roleId);
}
