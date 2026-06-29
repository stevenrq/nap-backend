package com.ns.nap_backend.auth.controller;

import com.ns.nap_backend.auth.dto.AuthCreateUserRequest;
import com.ns.nap_backend.auth.dto.AuthLoginRequest;
import com.ns.nap_backend.auth.dto.AuthResponse;
import com.ns.nap_backend.auth.dto.AuthResult;
import com.ns.nap_backend.auth.service.AuthenticationService;
import com.ns.nap_backend.config.security.RefreshTokenCookieFactory;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos de autenticación (registro, login, refresh y logout). */
@RestController
@RequestMapping(path = "api/auth", version = "1.0")
public class AuthenticationController {

  private final AuthenticationService authenticationService;
  private final RefreshTokenCookieFactory cookieFactory;

  public AuthenticationController(
      AuthenticationService authenticationService, RefreshTokenCookieFactory cookieFactory) {
    this.authenticationService = authenticationService;
    this.cookieFactory = cookieFactory;
  }

  @PostMapping("/sign-up")
  public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody AuthCreateUserRequest request) {
    AuthResult result = authenticationService.signUpUser(request);
    // El registro no emite tokens: respondemos solo con la confirmación, sin cookie ni accessToken.
    AuthResponse body = new AuthResponse(result.username(), result.message(), null);
    return ResponseEntity.status(HttpStatus.CREATED).body(body);
  }

  @PostMapping("/log-in")
  public ResponseEntity<AuthResponse> logIn(@Valid @RequestBody AuthLoginRequest request) {
    return toResponse(authenticationService.loginUser(request), HttpStatus.OK);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @CookieValue(name = "${security.jwt.refresh-token.cookie.name}", required = false)
          String refreshToken) {
    return toResponse(authenticationService.refresh(refreshToken), HttpStatus.OK);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = "${security.jwt.refresh-token.cookie.name}", required = false)
          String refreshToken,
      @AuthenticationPrincipal Jwt accessToken) {
    String accessTokenId = accessToken != null ? accessToken.getId() : null;
    Instant accessTokenExpiresAt = accessToken != null ? accessToken.getExpiresAt() : null;
    authenticationService.logout(refreshToken, accessTokenId, accessTokenExpiresAt);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
        .build();
  }

  /** Traduce el resultado interno a body {@code AuthResponse} + cookie HttpOnly con el refresh. */
  private ResponseEntity<AuthResponse> toResponse(AuthResult result, HttpStatus status) {
    ResponseCookie cookie = cookieFactory.build(result.refreshToken());
    AuthResponse body = new AuthResponse(result.username(), result.message(), result.accessToken());
    return ResponseEntity.status(status)
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(body);
  }
}
