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

/**
 * Testes de integração da segurança do Actuator (AC7 + resolve deferred-work
 * da Story 1.1):
 * <ul>
 *   <li>{@code /actuator/health} público (anônimo retorna 200)</li>
 *   <li>Demais endpoints actuator → ADMINISTRATOR-only</li>
 * </ul>
 */
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTest extends TestcontainersBaseTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    private Cookie cookieFor(UserProfile profile) {
        String token = jwtService.generateToken(UUID.randomUUID(), profile);
        return new Cookie("AUTH_TOKEN", token);
    }

    @Test
    void health_anonimo_retorna200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void info_anonimo_retorna401ComAuth002() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.code", is("AUTH_002")));
    }

    @Test
    void info_comCoordinator_retorna403ComAuth003() throws Exception {
        mockMvc.perform(get("/actuator/info").cookie(cookieFor(UserProfile.COORDINATOR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")))
                .andExpect(jsonPath("$.code", is("AUTH_003")));
    }

    @Test
    void info_comAdministrator_retorna200() throws Exception {
        mockMvc.perform(get("/actuator/info").cookie(cookieFor(UserProfile.ADMINISTRATOR)))
                .andExpect(status().isOk());
    }

    // ============================================================
    // P6 do code review 2026-05-03 — health.roles=ADMINISTRATOR
    // ============================================================

    @Test
    void health_comCoordinator_retorna200MasNaoMostraComponents() throws Exception {
        // when-authorized + roles=ADMINISTRATOR: Coordenador autenticado vê
        // status simplificado, NÃO o detalhe dos components (DB/SMTP/disk).
        mockMvc.perform(get("/actuator/health").cookie(cookieFor(UserProfile.COORDINATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void health_comAdministrator_mostraComponentsCompletos() throws Exception {
        // ADMINISTRATOR vê o objeto components completo (db, ping, etc.).
        mockMvc.perform(get("/actuator/health").cookie(cookieFor(UserProfile.ADMINISTRATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components", notNullValue()));
    }
}
