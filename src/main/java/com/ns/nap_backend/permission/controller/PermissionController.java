package com.ns.nap_backend.permission.controller;

import com.ns.nap_backend.permission.dto.PermissionResponse;
import com.ns.nap_backend.permission.exception.PermissionNotFoundException;
import com.ns.nap_backend.permission.service.PermissionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * El catálogo de permisos es propiedad de la API (cada permiso corresponde a un punto de control
 * real en el código) y se siembra desde la propia API. Por eso solo se expone lectura: el panel
 * administrativo los consulta para componer roles, pero no puede crear/editar/borrar permisos.
 */
@RestController
@RequestMapping(path = "/api/permissions", version = "1.0")
public class PermissionController {
  private final PermissionService permissionService;

  public PermissionController(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('permission:read')")
  public ResponseEntity<List<PermissionResponse>> findAll() {
    return ResponseEntity.ok(
        permissionService.findAll().stream().map(PermissionResponse::fromPermission).toList());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('permission:read')")
  public ResponseEntity<PermissionResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(
        PermissionResponse.fromPermission(
            permissionService.findById(id).orElseThrow(() -> new PermissionNotFoundException(id))));
  }

  @GetMapping("/search")
  @PreAuthorize("hasAuthority('permission:read')")
  public ResponseEntity<PermissionResponse> findByName(@RequestParam String name) {
    return ResponseEntity.ok(
        PermissionResponse.fromPermission(
            permissionService
                .findByName(name)
                .orElseThrow(() -> new PermissionNotFoundException(name))));
  }
}
