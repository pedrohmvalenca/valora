package br.com.senac.valora.security;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.services.JwtService;
import br.com.senac.valora.support.TestcontainersBaseTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Testes de integração de DEFAULT DENY + hierarquia de roles em method level.
 *
 * <p>Sobe contexto Spring Boot completo + Postgres em container; Flyway
 * aplica V1; o {@code DummySecuredController} (profile=test) é registrado.
 *
 * <p>Cobre os ACs:
 * <ul>
 *   <li>AC1: endpoint sem {@code @PreAuthorize} → 403 (DEFAULT DENY herdada
 *       de {@code BaseSecuredController})</li>
 *   <li>AC2: hierarquia ADMINISTRATOR &gt; COORDINATOR &gt; STUDENT</li>
 *   <li>AC9: respostas 401/403 do filter chain seguem shape
 *       {@link br.com.senac.valora.exceptions.ErrorResponse}</li>
 * </ul>
 */
@AutoConfigureMockMvc
class MethodSecurityIntegrationTest extends TestcontainersBaseTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    /** Gera cookie {@code AUTH_TOKEN} com JWT válido para o profile dado. */
    private Cookie cookieFor(UserProfile profile) {
        String token = jwtService.generateToken(UUID.randomUUID(), profile);
        return new Cookie("AUTH_TOKEN", token);
    }

    // ============================================================
    // AC1 — DEFAULT DENY
    // ============================================================

    @Test
    void unrestricted_semCookie_retorna401ComErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/_test/secured/unrestricted"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.code", is("AUTH_002")))
                .andExpect(jsonPath("$.message", is("Não autenticado")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.path", is("/api/v1/_test/secured/unrestricted")));
    }

    @Test
    void unrestricted_comAdmin_retorna403PorDefaultDeny() throws Exception {
        // Endpoint sem @PreAuthorize herda denyAll() do BaseSecuredController
        // → todos os perfis recebem 403, mesmo Administrator.
        ResultActions r = mockMvc.perform(get("/api/v1/_test/secured/unrestricted")
                        .cookie(cookieFor(UserProfile.ADMINISTRATOR)));
        r.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")))
                .andExpect(jsonPath("$.code", is("AUTH_003")))
                .andExpect(jsonPath("$.message", is("Acesso negado")));
    }

    // ============================================================
    // AC2 — Hierarquia
    // ============================================================

    @Test
    void coordinatorOnly_comAdmin_retorna200_porHierarquia() throws Exception {
        mockMvc.perform(get("/api/v1/_test/secured/coordinator-only")
                        .cookie(cookieFor(UserProfile.ADMINISTRATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok", is("true")));
    }

    @Test
    void coordinatorOnly_comCoordinator_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/_test/secured/coordinator-only")
                        .cookie(cookieFor(UserProfile.COORDINATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok", is("true")));
    }

    @Test
    void coordinatorOnly_comStudent_retorna403() throws Exception {
        mockMvc.perform(get("/api/v1/_test/secured/coordinator-only")
                        .cookie(cookieFor(UserProfile.STUDENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")))
                .andExpect(jsonPath("$.code", is("AUTH_003")));
    }

    @Test
    void adminOnly_comCoordinator_retorna403_naoHerdaParaCima() throws Exception {
        // Hierarquia é unidirecional: COORDINATOR não vira ADMINISTRATOR.
        mockMvc.perform(get("/api/v1/_test/secured/admin-only")
                        .cookie(cookieFor(UserProfile.COORDINATOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentOrUp_aceitaTodosOsProfilesHierarquicamente() throws Exception {
        // Todos os 3 profiles devem passar — STUDENT direto, COORDINATOR e ADMINISTRATOR herdam.
        mockMvc.perform(get("/api/v1/_test/secured/student-or-up")
                        .cookie(cookieFor(UserProfile.STUDENT)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/_test/secured/student-or-up")
                        .cookie(cookieFor(UserProfile.COORDINATOR)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/_test/secured/student-or-up")
                        .cookie(cookieFor(UserProfile.ADMINISTRATOR)))
                .andExpect(status().isOk());
    }
}
