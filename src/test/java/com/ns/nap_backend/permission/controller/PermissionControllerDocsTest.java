package com.ns.nap_backend.permission.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ns.nap_backend.permission.service.PermissionService;
import com.ns.nap_backend.support.RestDocsConfig;
import com.ns.nap_backend.support.SecurityDocsTestConfig;
import com.ns.nap_backend.support.TestEntities;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(PermissionController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfig.class, SecurityDocsTestConfig.class})
class PermissionControllerDocsTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PermissionService permissionService;

  private static RequestPostProcessor jwtWith(String... authorities) {
    List<GrantedAuthority> granted =
        List.of(authorities).stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
    return jwt().authorities(granted);
  }

  @Test
  void getAllPermissions() throws Exception {
    given(permissionService.findAll())
        .willReturn(List.of(TestEntities.permission(1L, "user:read")));

    mockMvc
        .perform(get("/api/permissions").with(jwtWith("permission:read")))
        .andExpect(status().isOk())
        .andDo(document("permissions-get-all"));
  }

  @Test
  void getPermissionById() throws Exception {
    given(permissionService.findById(1L))
        .willReturn(Optional.of(TestEntities.permission(1L, "user:read")));

    mockMvc
        .perform(get("/api/permissions/{id}", 1L).with(jwtWith("permission:read")))
        .andExpect(status().isOk())
        .andDo(
            document(
                "permissions-get-by-id",
                pathParameters(parameterWithName("id").description("ID del permiso."))));
  }

  @Test
  void getPermissionByIdNotFound() throws Exception {
    given(permissionService.findById(99L)).willReturn(Optional.empty());

    mockMvc
        .perform(get("/api/permissions/{id}", 99L).with(jwtWith("permission:read")))
        .andExpect(status().isNotFound());
  }

  @Test
  void searchPermissionByName() throws Exception {
    given(permissionService.findByName("user:read"))
        .willReturn(Optional.of(TestEntities.permission(1L, "user:read")));

    mockMvc
        .perform(
            get("/api/permissions/search")
                .param("name", "user:read")
                .with(jwtWith("permission:read")))
        .andExpect(status().isOk())
        .andDo(
            document(
                "permissions-search",
                queryParameters(
                    parameterWithName("name").description("Nombre exacto del permiso."))));
  }
}
