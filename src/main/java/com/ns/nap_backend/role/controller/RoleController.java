package com.ns.nap_backend.role.controller;

import com.ns.nap_backend.role.dto.RoleCreationRequest;
import com.ns.nap_backend.role.dto.RolePermissionsRequest;
import com.ns.nap_backend.role.dto.RoleUpdateRequest;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.role.exception.RoleNotFoundException;
import com.ns.nap_backend.role.service.RoleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/roles", version = "1.0")
public class RoleController {

  private final RoleService roleService;

  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('role:read')")
  public ResponseEntity<List<Role>> findAll() {
    return ResponseEntity.ok(roleService.findAll());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('role:read')")
  public ResponseEntity<Role> findById(@PathVariable Long id) {
    return ResponseEntity.ok(
        roleService.findById(id).orElseThrow(() -> new RoleNotFoundException(id)));
  }

  @GetMapping("/search")
  @PreAuthorize("hasAuthority('role:read')")
  public ResponseEntity<Role> findByName(@RequestParam String name) {
    return ResponseEntity.ok(
        roleService.findByName(name).orElseThrow(() -> new RoleNotFoundException(name)));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('role:create')")
  public ResponseEntity<Role> create(@Valid @RequestBody RoleCreationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('role:update')")
  public ResponseEntity<Role> update(
      @PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
    return ResponseEntity.ok(roleService.update(id, request));
  }

  @PutMapping("/{id}/permissions")
  @PreAuthorize("hasAuthority('role:update')")
  public ResponseEntity<Role> replacePermissions(
      @PathVariable Long id, @Valid @RequestBody RolePermissionsRequest request) {
    return ResponseEntity.ok(roleService.replacePermissions(id, request.permissionNames()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('role:delete')")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    roleService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
