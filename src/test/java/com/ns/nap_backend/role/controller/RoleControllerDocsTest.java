package com.ns.nap_backend.role.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ns.nap_backend.permission.exception.UnknownPermissionException;
import com.ns.nap_backend.role.dto.RoleCreationRequest;
import com.ns.nap_backend.role.dto.RoleUpdateRequest;
import com.ns.nap_backend.role.exception.RoleNotFoundException;
import com.ns.nap_backend.role.service.RoleService;
import com.ns.nap_backend.support.RestDocsConfig;
import com.ns.nap_backend.support.SecurityDocsTestConfig;
import com.ns.nap_backend.support.TestEntities;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

@WebMvcTest(RoleController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfig.class, SecurityDocsTestConfig.class})
class RoleControllerDocsTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RoleService roleService;

  private static RequestPostProcessor jwtWith(String... authorities) {
    List<GrantedAuthority> granted =
        List.of(authorities).stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
    return jwt().authorities(granted);
  }

  @Test
  void getAllRoles() throws Exception {
    given(roleService.findAll()).willReturn(List.of(TestEntities.role(2L, "USER")));

    mockMvc
        .perform(get("/api/roles").with(jwtWith("role:read")))
        .andExpect(status().isOk())
        .andDo(document("roles-get-all"));
  }

  @Test
  void getRoleById() throws Exception {
    given(roleService.findById(2L)).willReturn(Optional.of(TestEntities.role(2L, "USER")));

    mockMvc
        .perform(get("/api/roles/{id}", 2L).with(jwtWith("role:read")))
        .andExpect(status().isOk())
        .andDo(
            document(
                "roles-get-by-id",
                pathParameters(parameterWithName("id").description("ID del rol."))));
  }

  @Test
  void getRoleByIdNotFound() throws Exception {
    given(roleService.findById(99L)).willReturn(Optional.empty());

    mockMvc
        .perform(get("/api/roles/{id}", 99L).with(jwtWith("role:read")))
        .andExpect(status().isNotFound());
  }

  @Test
  void searchRoleByName() throws Exception {
    given(roleService.findByName("USER")).willReturn(Optional.of(TestEntities.role(2L, "USER")));

    mockMvc
        .perform(get("/api/roles/search").param("name", "USER").with(jwtWith("role:read")))
        .andExpect(status().isOk())
        .andDo(
            document(
                "roles-search",
                queryParameters(parameterWithName("name").description("Nombre exacto del rol."))));
  }

  @Test
  void createRole() throws Exception {
    given(roleService.create(any(RoleCreationRequest.class)))
        .willReturn(TestEntities.role(3L, "EDITOR"));
    String body =
        """
        { "name": "EDITOR", "description": "Editor", "permissionNames": ["role:read"] }
        """;

    mockMvc
        .perform(
            post("/api/roles")
                .with(jwtWith("role:create"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "roles-create",
                relaxedRequestFields(
                    fieldWithPath("name").description("Nombre del rol."),
                    fieldWithPath("permissionNames")
                        .description("Permisos del catálogo a asignar.")),
                relaxedResponseFields(
                    fieldWithPath("id").description("ID generado."),
                    fieldWithPath("name").description("Nombre del rol."))));
  }

  @Test
  void createRoleWithUnknownPermissionReturnsBadRequest() throws Exception {
    given(roleService.create(any(RoleCreationRequest.class)))
        .willThrow(new UnknownPermissionException(Set.of("ghost")));
    String body =
        """
        { "name": "EDITOR", "description": "Editor", "permissionNames": ["ghost"] }
        """;

    mockMvc
        .perform(
            post("/api/roles")
                .with(jwtWith("role:create"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateRole() throws Exception {
    given(roleService.update(eq(2L), any(RoleUpdateRequest.class)))
        .willReturn(TestEntities.role(2L, "MEMBER"));
    String body =
        """
        { "name": "MEMBER", "description": "Renombrado" }
        """;

    mockMvc
        .perform(
            put("/api/roles/{id}", 2L)
                .with(jwtWith("role:update"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andDo(
            document(
                "roles-update",
                pathParameters(parameterWithName("id").description("ID del rol a actualizar."))));
  }

  @Test
  void replaceRolePermissions() throws Exception {
    given(roleService.replacePermissions(eq(2L), anySet()))
        .willReturn(TestEntities.role(2L, "USER"));
    String body =
        """
        { "permissionNames": ["role:read", "permission:read"] }
        """;

    mockMvc
        .perform(
            put("/api/roles/{id}/permissions", 2L)
                .with(jwtWith("role:update"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andDo(
            document(
                "roles-replace-permissions",
                pathParameters(parameterWithName("id").description("ID del rol."))));
  }

  @Test
  void replacePermissionsRoleNotFound() throws Exception {
    willThrow(new RoleNotFoundException(99L))
        .given(roleService)
        .replacePermissions(eq(99L), anySet());
    String body =
        """
        { "permissionNames": ["role:read"] }
        """;

    mockMvc
        .perform(
            put("/api/roles/{id}/permissions", 99L)
                .with(jwtWith("role:update"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteRole() throws Exception {
    mockMvc
        .perform(delete("/api/roles/{id}", 2L).with(jwtWith("role:delete")))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "roles-delete",
                pathParameters(parameterWithName("id").description("ID del rol a eliminar."))));
  }
}
