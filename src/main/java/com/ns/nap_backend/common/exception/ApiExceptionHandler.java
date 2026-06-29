package com.ns.nap_backend.common.exception;

import com.ns.nap_backend.permission.exception.PermissionNotFoundException;
import com.ns.nap_backend.permission.exception.UnknownPermissionException;
import com.ns.nap_backend.role.exception.RoleAlreadyExistsException;
import com.ns.nap_backend.role.exception.RoleNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce a respuestas JSON ({@link ProblemDetail}, RFC 7807) las excepciones que ocurren dentro de
 * los controladores. Los errores 401/403 originados en los filtros de seguridad se manejan en
 * {@code JwtAuthenticationEntryPoint} y {@code JwtAccessDeniedHandler}. Las {@code
 * ResponseStatusException} (ej. el 409 de username duplicado) las gestiona la clase base.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * 401 cuando las credenciales del login son inválidas ({@code BadCredentialsException}, etc.).
   */
  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail handleAuthenticationException(AuthenticationException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    problemDetail.setTitle("Unauthorized");
    return problemDetail;
  }

  /** 404 cuando se opera sobre un permiso inexistente. */
  @ExceptionHandler(PermissionNotFoundException.class)
  ProblemDetail handlePermissionNotFound(PermissionNotFoundException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problemDetail.setTitle("Not Found");
    return problemDetail;
  }

  /** 404 cuando se opera sobre un rol inexistente. */
  @ExceptionHandler(RoleNotFoundException.class)
  ProblemDetail handleRoleNotFound(RoleNotFoundException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problemDetail.setTitle("Not Found");
    return problemDetail;
  }

  /** 409 cuando se intenta crear un rol con un nombre que ya existe. */
  @ExceptionHandler(RoleAlreadyExistsException.class)
  ProblemDetail handleRoleAlreadyExists(RoleAlreadyExistsException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problemDetail.setTitle("Conflict");
    return problemDetail;
  }

  /** 400 cuando un rol referencia permisos que no existen en el catálogo. */
  @ExceptionHandler(UnknownPermissionException.class)
  ProblemDetail handleUnknownPermission(UnknownPermissionException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setTitle("Bad Request");
    return problemDetail;
  }

  /** 400 con el detalle por campo cuando falla la validación del cuerpo de la petición. */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    problemDetail.setTitle("Bad Request");

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    problemDetail.setProperty("errors", errors);

    return handleExceptionInternal(ex, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
  }
}
