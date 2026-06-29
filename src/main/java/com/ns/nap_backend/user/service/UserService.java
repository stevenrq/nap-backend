package com.ns.nap_backend.user.service;

import com.ns.nap_backend.user.dto.UserUpdateRequest;
import com.ns.nap_backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

  List<User> findAll();

  Optional<User> findById(Long id);

  Optional<User> update(Long id, UserUpdateRequest userUpdateRequest);

  void deleteById(Long id);
}
