package br.com.senac.valora.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.valora.support.TestcontainersBaseTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Testes de integração end-to-end de {@code /api/v1/auth/login} e {@code /me}.
 *
 * <p>Sobe contexto Spring Boot completo + Postgres em container; Flyway aplica
 * V1 e o seed Admin (admin@valora.local / Admin@123) é usado nos cenários.
 *
 * <p>Cobertura mínima conforme AC11:
 * <ul>
 *   <li>Login Admin seed → 200 + cookie correto + body correto</li>
 *   <li>Login com email inexistente → 401 + AUTH_001 + sem Set-Cookie</li>
 *   <li>Login com senha errada → 401 + AUTH_001</li>
 *   <li>{@code GET /me} com cookie válido → 200 + payload</li>
 *   <li>{@code GET /me} sem cookie → 401</li>
 * </ul>
 */
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends TestcontainersBaseTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void login_adminSeed_retorna200ComCookieEbody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@valora.local\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id", notNullValue()))
                .andExpect(jsonPath("$.user.name", is("Administrador VALORA (dev)")))
                .andExpect(jsonPath("$.user.profile", is("ADMINISTRATOR")))
                .andExpect(jsonPath("$.user.linkedCourses").isArray())
                // Body NÃO deve conter token
                .andExpect(jsonPath("$.user.token").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                // Set-Cookie com atributos corretos (validar via header text — MockMvc não parseia SameSite)
                .andExpect(header().string("Set-Cookie", containsString("AUTH_TOKEN=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=14400")))
                // Cookie acessível via API MockMvc
                .andExpect(cookie().exists("AUTH_TOKEN"))
                .andExpect(cookie().httpOnly("AUTH_TOKEN", true))
                .andExpect(cookie().secure("AUTH_TOKEN", false)) // profile=test → secure=false
                .andReturn();

        // Garantir que o cookie tem valor não vazio (token real)
        Cookie authCookie = result.getResponse().getCookie("AUTH_TOKEN");
        org.assertj.core.api.Assertions.assertThat(authCookie).isNotNull();
        org.assertj.core.api.Assertions.assertThat(authCookie.getValue()).isNotBlank();
    }

    @Test
    void login_emailInexistente_retorna401ComCodeAUTH001ESemSetCookie() throws Exception {
        // Shape novo do ErrorResponse (Story 1.4): timestamp + error + message + code + path
        // code AUTH_001 preservado; message migrou de "Invalid credentials" → "Credenciais inválidas"
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-existe@nada.com\",\"password\":\"qualquer-senha\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Credenciais inválidas")))
                .andExpect(jsonPath("$.code", is("AUTH_001")))
                .andExpect(jsonPath("$.path", is("/api/v1/auth/login")))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void login_senhaErrada_retorna401ComCodeAUTH001() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@valora.local\",\"password\":\"senha-errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Credenciais inválidas")))
                .andExpect(jsonPath("$.code", is("AUTH_001")))
                .andExpect(jsonPath("$.path", is("/api/v1/auth/login")))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void me_comCookieValido_retorna200ComPayload() throws Exception {
        // Faz login, extrai o cookie, usa em /me
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@valora.local\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie authCookie = login.getResponse().getCookie("AUTH_TOKEN");
        org.assertj.core.api.Assertions.assertThat(authCookie).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id", notNullValue()))
                .andExpect(jsonPath("$.user.name", is("Administrador VALORA (dev)")))
                .andExpect(jsonPath("$.user.profile", is("ADMINISTRATOR")))
                .andExpect(jsonPath("$.user.linkedCourses").isArray())
                // /me não emite novo Set-Cookie
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void me_semCookie_retorna401() throws Exception {
        // Após Story 1.4: JwtAuthenticationEntryPoint responde 401 com ErrorResponse JSON
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.code", is("AUTH_002")))
                .andExpect(jsonPath("$.message", is("Não autenticado")))
                .andExpect(jsonPath("$.path", is("/api/v1/auth/me")));
    }

    @Test
    void me_comCookieInvalido_retorna401() throws Exception {
        Cookie fake = new Cookie("AUTH_TOKEN", "isto-nao-eh-um-jwt-valido");
        mockMvc.perform(get("/api/v1/auth/me").cookie(fake))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.code", is("AUTH_002")));
    }
}
