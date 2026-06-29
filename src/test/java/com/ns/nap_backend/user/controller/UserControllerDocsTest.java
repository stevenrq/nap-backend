package com.ns.nap_backend.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ns.nap_backend.support.RestDocsConfig;
import com.ns.nap_backend.support.SecurityDocsTestConfig;
import com.ns.nap_backend.support.TestEntities;
import com.ns.nap_backend.user.dto.UserUpdateRequest;
import com.ns.nap_backend.user.entity.User;
import com.ns.nap_backend.user.security.UserSecurity;
import com.ns.nap_backend.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(UserController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfig.class, SecurityDocsTestConfig.class})
class UserControllerDocsTest {

  private static final String VALID_BODY =
      """
      {
        "firstName": "Usuario2",
        "lastName": "Sistema",
        "phoneNumber": 3009876543,
        "email": "usuario@sistema.nap.com",
        "username": "user01",
        "roles": [ { "name": "USER" } ]
      }
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  @MockitoBean(name = "userSecurity")
  private UserSecurity userSecurity;

  private static RequestPostProcessor jwtWith(String... authorities) {
    List<GrantedAuthority> granted =
        List.of(authorities).stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
    return jwt().authorities(granted);
  }

  @Test
  void getAllUsers() throws Exception {
    given(userService.findAll())
        .willReturn(List.of(TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"))));

    mockMvc
        .perform(get("/api/users").with(jwtWith("user:read")))
        .andExpect(status().isOk())
        .andDo(document("users-get-all"));
  }

  @Test
  void getUserById() throws Exception {
    given(userService.findById(51L))
        .willReturn(Optional.of(TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"))));

    mockMvc
        .perform(get("/api/users/{id}", 51L).with(jwtWith("user:read")))
        .andExpect(status().isOk())
        .andDo(
            document(
                "users-get-by-id",
                pathParameters(parameterWithName("id").description("ID del usuario."))));
  }

  @Test
  void getUserByIdNotFound() throws Exception {
    given(userService.findById(99L)).willReturn(Optional.empty());

    mockMvc
        .perform(get("/api/users/{id}", 99L).with(jwtWith("user:read")))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateUserAsAdmin() throws Exception {
    User updated = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    given(userService.update(eq(51L), any(UserUpdateRequest.class)))
        .willReturn(Optional.of(updated));

    mockMvc
        .perform(
            put("/api/users/{id}", 51L)
                .with(jwtWith("user:update"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isOk())
        .andDo(
            document(
                "users-update",
                pathParameters(parameterWithName("id").description("ID del usuario a actualizar.")),
                relaxedRequestFields(
                    fieldWithPath("firstName").description("Nombre."),
                    fieldWithPath("email").description("Correo electrónico."),
                    fieldWithPath("username").description("Nombre de usuario."),
                    fieldWithPath("roles")
                        .description(
                            "Roles a asignar (solo aplican si el llamador tiene user:update).")),
                relaxedResponseFields(
                    fieldWithPath("id").description("ID del usuario."),
                    fieldWithPath("username").description("Nombre de usuario."),
                    fieldWithPath("roles").description("Nombres de rol efectivos."),
                    fieldWithPath("permissions").description("Permisos derivados de los roles."))));
  }

  @Test
  void updateUserAsSelfWithoutUpdateAuthority() throws Exception {
    User updated = TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"));
    given(userSecurity.isSelf(eq(51L), any())).willReturn(true);
    given(userService.update(eq(51L), any(UserUpdateRequest.class)))
        .willReturn(Optional.of(updated));

    mockMvc
        .perform(
            put("/api/users/{id}", 51L)
                .with(jwtWith()) // sin user:update; pasa por ser self
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void updateUserRejectedWhenNeitherSelfNorPrivileged() throws Exception {
    given(userSecurity.isSelf(eq(51L), any())).willReturn(false);

    mockMvc
        .perform(
            put("/api/users/{id}", 51L)
                .with(jwtWith("user:read"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateUserValidationError() throws Exception {
    String invalidBody = VALID_BODY.replace("\"Usuario2\"", "\"\"");

    mockMvc
        .perform(
            put("/api/users/{id}", 51L)
                .with(jwtWith("user:update"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteUser() throws Exception {
    given(userService.findById(51L))
        .willReturn(Optional.of(TestEntities.user(51L, "user01", TestEntities.role(2L, "USER"))));

    mockMvc
        .perform(delete("/api/users/{id}", 51L).with(jwtWith("user:delete")))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "users-delete",
                pathParameters(parameterWithName("id").description("ID del usuario a eliminar."))));
  }
}
