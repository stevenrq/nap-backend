package com.ns.nap_backend.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.cookies.CookieDocumentation.responseCookies;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ns.nap_backend.auth.dto.AuthResult;
import com.ns.nap_backend.auth.service.AuthenticationService;
import com.ns.nap_backend.config.security.RefreshTokenCookieFactory;
import com.ns.nap_backend.support.RestDocsConfig;
import com.ns.nap_backend.support.SecurityDocsTestConfig;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests de documentación (Spring REST Docs) para {@link AuthenticationController}. Cada test
 * ejecuta la petición real con MockMvc y genera los snippets bajo {@code
 * target/generated-snippets/auth-*}.
 */
@WebMvcTest(AuthenticationController.class)
@AutoConfigureRestDocs
@Import({RestDocsConfig.class, SecurityDocsTestConfig.class})
class AuthenticationControllerDocsTest {

  private static final String REFRESH_COOKIE = "refresh_token";
  private static final String SAMPLE_TOKEN = "eyJhbGciOiJSUzI1NiJ9.payload.signature";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthenticationService authenticationService;
  @MockitoBean private RefreshTokenCookieFactory cookieFactory;

  @BeforeEach
  void setUpCookieFactory() {
    given(cookieFactory.build(anyString()))
        .willReturn(
            ResponseCookie.from(REFRESH_COOKIE, "opaque-refresh-token")
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(7))
                .build());
    given(cookieFactory.clear())
        .willReturn(
            ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build());
  }

  @Test
  void signUp() throws Exception {
    given(authenticationService.signUpUser(any()))
        .willReturn(new AuthResult("johndoe", "Usuario registrado", null, null));

    String body =
        """
        {
          "nationalId": 1098765432,
          "firstName": "John",
          "lastName": "Doe",
          "phoneNumber": 3001234567,
          "email": "john.doe@example.com",
          "username": "johndoe",
          "password": "s3cr3tpass"
        }
        """;

    mockMvc
        .perform(post("/api/auth/sign-up").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "auth-sign-up",
                requestFields(
                    fieldWithPath("nationalId")
                        .description("Documento nacional de identidad (único)."),
                    fieldWithPath("firstName").description("Nombre (3-20 caracteres)."),
                    fieldWithPath("lastName").description("Apellido (3-20 caracteres)."),
                    fieldWithPath("phoneNumber").description("Teléfono de 10 dígitos (único)."),
                    fieldWithPath("email").description("Correo electrónico válido y único."),
                    fieldWithPath("username").description("Nombre de usuario (5-20 caracteres)."),
                    fieldWithPath("password").description("Contraseña (6-60 caracteres).")),
                responseFields(
                    fieldWithPath("username").description("Usuario registrado."),
                    fieldWithPath("message").description("Mensaje de confirmación."),
                    fieldWithPath("accessToken")
                        .description("Access token JWT. Ausente en el registro (no emite tokens).")
                        .type(JsonFieldType.STRING)
                        .optional())));
  }

  @Test
  void logIn() throws Exception {
    given(authenticationService.loginUser(any()))
        .willReturn(new AuthResult("johndoe", "Login correcto", SAMPLE_TOKEN, "raw-refresh"));

    String body =
        """
        {
          "username": "johndoe",
          "password": "s3cr3tpass"
        }
        """;

    mockMvc
        .perform(post("/api/auth/log-in").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andDo(
            document(
                "auth-login",
                requestFields(
                    fieldWithPath("username").description("Nombre de usuario."),
                    fieldWithPath("password").description("Contraseña.")),
                responseFields(
                    fieldWithPath("username").description("Usuario autenticado."),
                    fieldWithPath("message").description("Mensaje de resultado."),
                    fieldWithPath("accessToken").description("Access token JWT (Bearer).")),
                responseCookies(
                    cookieWithName(REFRESH_COOKIE)
                        .description("Refresh token opaco, cookie HttpOnly (path /api/auth)."))));
  }

  @Test
  void refresh() throws Exception {
    given(authenticationService.refresh(anyString()))
        .willReturn(new AuthResult("johndoe", "Token renovado", SAMPLE_TOKEN, "new-raw-refresh"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie(REFRESH_COOKIE, "raw-refresh")))
        .andExpect(status().isOk())
        .andDo(
            document(
                "auth-refresh",
                requestCookies(
                    cookieWithName(REFRESH_COOKIE)
                        .description("Refresh token vigente emitido en el login.")),
                responseFields(
                    fieldWithPath("username").description("Usuario."),
                    fieldWithPath("message").description("Mensaje de resultado."),
                    fieldWithPath("accessToken").description("Nuevo access token JWT.")),
                responseCookies(
                    cookieWithName(REFRESH_COOKIE)
                        .description(
                            "Refresh token rotado (la cookie anterior queda invalidada)."))));
  }

  @Test
  void logout() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/logout")
                .with(jwt())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + SAMPLE_TOKEN)
                .cookie(new jakarta.servlet.http.Cookie(REFRESH_COOKIE, "raw-refresh")))
        .andExpect(status().isNoContent())
        .andDo(
            document(
                "auth-logout",
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION)
                        .description("Access token Bearer del usuario a cerrar sesión.")),
                requestCookies(
                    cookieWithName(REFRESH_COOKIE)
                        .description("Refresh token a revocar.")
                        .optional())));
  }
}
