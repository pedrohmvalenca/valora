package br.com.senac.valora.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.valora.support.TestcontainersBaseTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Story 1.11 — Forçar troca de senha no primeiro acesso.
 *
 * <p>Cobertura (AC7 da story):
 * <ul>
 *   <li>Aluno novo (POST /students) nasce com {@code must_change_password=true}.</li>
 *   <li>Login com a senha provisória → {@code user.mustChangePassword=true}.</li>
 *   <li>{@code POST /auth/change-password} com current errada → 401/AUTH_001.</li>
 *   <li>{@code POST /auth/change-password} com new curta → 400/VAL_001.</li>
 *   <li>Troca OK → 204 + flag zerada no banco; próximo login → flag false.</li>
 *   <li>Admin seed (não-aluno) continua com {@code mustChangePassword=false}.</li>
 * </ul>
 *
 * <p>Cada teste cria um aluno novo com email único (UUID) para evitar
 * conflitos entre execuções do container reutilizado.
 */
@AutoConfigureMockMvc
class AuthChangePasswordIntegrationTest extends TestcontainersBaseTest {

    private static final String COURSE_ADS = "00000000-0000-0000-0000-000000c01ad5";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    // ============================================================
    // Admin seed (V1) — não-aluno, deve ficar com flag false
    // ============================================================
    @Test
    void loginAdminSeed_retornaMustChangePasswordFalse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@valora.local\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.mustChangePassword", is(false)));
    }

    // ============================================================
    // AC2 — POST /students seta must_change_password=true
    // ============================================================
    @Test
    void postStudents_marcaMustChangePasswordTrueNoBanco() throws Exception {
        String email = "novo-aluno-" + UUID.randomUUID() + "@valora.local";
        Cookie admin = loginAdmin();
        createStudent(admin, email);

        Boolean flag = jdbc.queryForObject(
                "SELECT must_change_password FROM users WHERE email = ?",
                Boolean.class, email);
        assertThat(flag).isTrue();
    }

    // ============================================================
    // AC3 — login com a provisória → mustChangePassword=true na response
    // ============================================================
    @Test
    void loginAlunoNovo_retornaMustChangePasswordTrue() throws Exception {
        String email = "novo-aluno-" + UUID.randomUUID() + "@valora.local";
        Cookie admin = loginAdmin();
        String provisional = createStudent(admin, email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + provisional + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.profile", is("STUDENT")))
                .andExpect(jsonPath("$.user.mustChangePassword", is(true)));
    }

    // ============================================================
    // AC4 — POST /auth/change-password com current errada → 401/AUTH_001
    // ============================================================
    @Test
    void changePassword_currentErrada_retorna401() throws Exception {
        String email = "novo-aluno-" + UUID.randomUUID() + "@valora.local";
        Cookie admin = loginAdmin();
        String provisional = createStudent(admin, email);
        Cookie student = loginStudent(email, provisional);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"esta-nao-eh\",\"newPassword\":\"nova-senha-forte\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")))
                .andExpect(jsonPath("$.message", is("Credenciais inválidas")));

        // Flag continua true — falha não pode zerar
        Boolean flag = jdbc.queryForObject(
                "SELECT must_change_password FROM users WHERE email = ?",
                Boolean.class, email);
        assertThat(flag).isTrue();
    }

    // ============================================================
    // AC4 — POST /auth/change-password com new < 8 chars → 400/VAL_001
    // ============================================================
    @Test
    void changePassword_newCurta_retorna400() throws Exception {
        String email = "novo-aluno-" + UUID.randomUUID() + "@valora.local";
        Cookie admin = loginAdmin();
        String provisional = createStudent(admin, email);
        Cookie student = loginStudent(email, provisional);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + provisional + "\",\"newPassword\":\"1234567\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VAL_001")));

        Boolean flag = jdbc.queryForObject(
                "SELECT must_change_password FROM users WHERE email = ?",
                Boolean.class, email);
        assertThat(flag).isTrue();
    }

    // ============================================================
    // Patch code review (P1) — new == current → 400/VAL_001
    // ============================================================
    @Test
    void changePassword_newIgualCurrent_retorna400() throws Exception {
        String email = "novo-aluno-" + UUID.randomUUID() + "@valora.local";
        Cookie admin = loginAdmin();
        String provisional = createStudent(admin, email);
        Cookie student = loginStudent(email, provisional);

        // Backend tem que rejeitar — caso contrário cliente API-savvy zera a flag
        // sem trocar a senha de verdade.
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + provisional
                                + "\",\"newPassword\":\"" + provisional + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VAL_001")));

        // Flag continua true — bypass não pode acontecer
        Boolean flag = jdbc.queryForObject(
                "SELECT must_change_password FROM users WHERE email = ?",
                Boolean.class, email);
        assertThat(flag).isTrue();
    }

    // ============================================================
    // AC4 — POST /auth/change-password sem auth → 401
    // ============================================================
    @Test
    void changePassword_semAuth_retorna401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"x\",\"newPassword\":\"nova-senha-forte\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_002")));
    }

    // ============================================================
    // AC4 + AC7 — troca OK → 204, flag zera, próximo login = false
    // ============================================================
    @Test
    void changePassword_sucesso_204EZeraFlagENaoForcaNoProximoLogin() throws Exception {
        String email = "novo-aluno-" + UUID.randomUUID() + "@valora.local";
        String newPassword = "Senha-Forte-123";
        Cookie admin = loginAdmin();
        String provisional = createStudent(admin, email);
        Cookie student = loginStudent(email, provisional);

        // Troca
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + provisional
                                + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Content-Type"));

        // Banco: flag zerada
        Boolean flag = jdbc.queryForObject(
                "SELECT must_change_password FROM users WHERE email = ?",
                Boolean.class, email);
        assertThat(flag).isFalse();

        // Próximo login com a senha NOVA → 200 + mustChangePassword=false
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + newPassword + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.mustChangePassword", is(false)));

        // Login com a senha ANTIGA (provisória) → 401 — hash foi substituído
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + provisional + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Cookie loginAdmin() throws Exception {
        return doLogin("admin@valora.local", "Admin@123");
    }

    private Cookie loginStudent(String email, String password) throws Exception {
        return doLogin(email, password);
    }

    private Cookie doLogin(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie c = res.getResponse().getCookie("AUTH_TOKEN");
        assertThat(c).isNotNull();
        return c;
    }

    /**
     * Cria aluno via POST /students vinculado ao seed ADS. Retorna a senha
     * provisória gerada pelo backend (campo {@code provisionalPassword} no
     * StudentDto da response — só vem quando o backend gerou a senha).
     */
    private String createStudent(Cookie admin, String email) throws Exception {
        // Usa um sufixo derivado do email (que contém um UUID v4 aleatório)
        // para garantir unicidade do registration_code entre execuções com
        // container reutilizado. Cabe em VARCHAR(50).
        String code = "M11-" + email.substring("novo-aluno-".length(), email.indexOf("@"));
        String body = "{"
                + "\"registrationCode\":\"" + code + "\","
                + "\"name\":\"Aluno Teste 1.11\","
                + "\"email\":\"" + email + "\","
                + "\"courseIds\":[\"" + COURSE_ADS + "\"]"
                + "}";

        MvcResult res = mockMvc.perform(post("/api/v1/students")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode dto = objectMapper.readTree(res.getResponse().getContentAsString());
        String provisional = dto.get("provisionalPassword").asText();
        assertThat(provisional).isNotBlank();
        return provisional;
    }
}
