package com.ns.nap_backend.auth.service;

import static com.ns.nap_backend.common.util.RolePermissionUtils.buildAuthorities;

import com.ns.nap_backend.auth.dto.AuthCreateUserRequest;
import com.ns.nap_backend.auth.dto.AuthLoginRequest;
import com.ns.nap_backend.auth.dto.AuthResult;
import com.ns.nap_backend.auth.dto.RotatedRefreshToken;
import com.ns.nap_backend.role.entity.Role;
import com.ns.nap_backend.role.repository.RoleRepository;
import com.ns.nap_backend.user.entity.User;
import com.ns.nap_backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Orquesta el registro y el login, delegando la firma del JWT en {@link JwtTokenGenerator}. */
@Service
public class AuthenticationService {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

  /** Nombre del rol por defecto que se asigna a los usuarios registrados. */
  private static final String USER = "USER";

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenGenerator jwtTokenGenerator;
  private final RefreshTokenService refreshTokenService;
  private final TokenDenylistService tokenDenylistService;

  public AuthenticationService(
      AuthenticationManager authenticationManager,
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenGenerator jwtTokenGenerator,
      RefreshTokenService refreshTokenService,
      TokenDenylistService tokenDenylistService) {
    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenGenerator = jwtTokenGenerator;
    this.refreshTokenService = refreshTokenService;
    this.tokenDenylistService = tokenDenylistService;
  }

  /** Autentica credenciales existentes y devuelve un access token + refresh token. */
  @Transactional
  public AuthResult loginUser(AuthLoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    User user =
        userRepository
            .findByUsername(request.username())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    String accessToken = jwtTokenGenerator.createToken(authentication);
    String refreshToken = refreshTokenService.issue(user);
    log.info("User '{}' logged in successfully", user.getUsername());
    return new AuthResult(
        user.getUsername(), "User logged in successfully", accessToken, refreshToken);
  }

  /** Rota el refresh token recibido y emite un nuevo access token. */
  @Transactional
  public AuthResult refresh(String rawRefreshToken) {
    RotatedRefreshToken rotated = refreshTokenService.validateAndRotate(rawRefreshToken);
    User user = rotated.user();

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            user.getUsername(), null, buildAuthorities(user.getRoles()));
    String accessToken = jwtTokenGenerator.createToken(authentication);

    return new AuthResult(
        user.getUsername(), "Token refreshed successfully", accessToken, rotated.rawToken());
  }

  /**
   * Cierra la sesión: revoca el refresh token y añade el access token actual a la denylist para
   * invalidarlo de inmediato. {@code accessTokenId}/{@code accessTokenExpiresAt} pueden ser nulos
   * si la petición no incluyó un access token válido.
   */
  @Transactional
  public void logout(String rawRefreshToken, String accessTokenId, Instant accessTokenExpiresAt) {
    refreshTokenService.revoke(rawRefreshToken);
    tokenDenylistService.denylist(accessTokenId, accessTokenExpiresAt);
  }

  @Transactional
  public AuthResult signUpUser(AuthCreateUserRequest request) {
    if (userRepository.findByUsername(request.getUsername()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Username already exists: " + request.getUsername());
    }

    User user = new User();
    user.setNationalId(request.getNationalId());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setPhoneNumber(request.getPhoneNumber());
    user.setEmail(request.getEmail());
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    Set<Role> roles = new HashSet<>();
    roles.add(roleRepository.findByName(USER).orElseThrow());

    user.setRoles(roles);

    userRepository.save(user);
    log.info("New user registered: '{}'", user.getUsername());

    // El registro solo crea la cuenta: no emite access ni refresh token. El usuario debe iniciar
    // sesión después (POST /api/auth/log-in) para obtener sus tokens.
    return new AuthResult(user.getUsername(), "User created successfully", null, null);
  }
}
