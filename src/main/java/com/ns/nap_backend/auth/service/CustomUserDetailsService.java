package com.ns.nap_backend.auth.service;

import static com.ns.nap_backend.common.util.RolePermissionUtils.buildAuthorities;

import com.ns.nap_backend.user.entity.User;
import com.ns.nap_backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with username: " + username));

    List<GrantedAuthority> authorities = buildAuthorities(user.getRoles());

    return new org.springframework.security.core.userdetails.User(
        user.getUsername(),
        user.getPassword(),
        user.isEnabled(),
        user.isAccountNonExpired(),
        user.isCredentialsNonExpired(),
        user.isAccountNonLocked(),
        authorities);
  }
}
