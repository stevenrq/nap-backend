package com.ns.nap_backend.user.controller;

import com.ns.nap_backend.user.dto.UserResponse;
import com.ns.nap_backend.user.dto.UserUpdateRequest;
import com.ns.nap_backend.user.entity.User;
import com.ns.nap_backend.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/users", version = "1.0")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<List<UserResponse>> findAll() {
    List<UserResponse> users = userService.findAll().stream().map(UserResponse::fromUser).toList();
    return ResponseEntity.ok(users);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
    Optional<User> userOptional = userService.findById(id);
    if (userOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(UserResponse.fromUser(userOptional.orElseThrow()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('user:update') or @userSecurity.isSelf(#id, authentication)")
  public ResponseEntity<UserResponse> update(
      @PathVariable Long id, @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
    Optional<User> updated = userService.update(id, userUpdateRequest);
    if (updated.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(UserResponse.fromUser(updated.orElseThrow()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('user:delete')")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    if (!userService.findById(id).isPresent()) {
      return ResponseEntity.notFound().build();
    }
    userService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
