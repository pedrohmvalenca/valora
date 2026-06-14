package br.com.senac.valora.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.valora.support.TestcontainersBaseTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Story 3.10 — Endpoints de status do vínculo student_course.
 *
 * <p>Usa o seed da V100:
 * <ul>
 *   <li>Alice ({@code 0a11ce}) → vínculos em ADS ({@code c01ad5}) e DJD ({@code c0d1d2})</li>
 *   <li>Diego ({@code d1e60}) → vínculo só em DJD</li>
 *   <li>Ana ({@code c00a01}) → Coord ADS</li>
 *   <li>Bia ({@code c00b02}) → Coord DJD</li>
 *   <li>Admin → admin@valora.local / Admin@123</li>
 * </ul>
 *
 * <p>{@code JdbcTemplate} reseta {@code student_course.status} antes de cada teste
 * para garantir idempotência entre execuções (o container é reutilizado).
 */
@AutoConfigureMockMvc
class StudentControllerStatusTest extends TestcontainersBaseTest {

    private static final String ALICE = "00000000-0000-0000-0000-0000000a11ce";
    private static final String DIEGO = "00000000-0000-0000-0000-0000000d1e60";
    private static final String COURSE_ADS = "00000000-0000-0000-0000-000000c01ad5";
    private static final String COURSE_DJD = "00000000-0000-0000-0000-000000c0d1d2";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void resetStatusToDefault() {
        jdbc.update("UPDATE student_course SET status = 'CURSANDO'");
    }

    // ============================================================
    // AC5.1 — migration aplicada + default CURSANDO
    // ============================================================
    @Test
    void migrationV104_aplica_eVinculosSeedFicamCursando() {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM student_course", Integer.class);
        Integer cursando = jdbc.queryForObject(
                "SELECT COUNT(*) FROM student_course WHERE status = 'CURSANDO'", Integer.class);
        assertThat(total).isGreaterThan(0);
        assertThat(cursando).isEqualTo(total);
    }

    // ============================================================
    // AC5.2 — GET como admin retorna todos os vínculos com status
    // ============================================================
    @Test
    void getCourses_admin_retornaTodosVinculosComStatus() throws Exception {
        Cookie admin = loginAdmin();
        mockMvc.perform(get("/api/v1/students/" + ALICE + "/courses").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].status",
                        containsInAnyOrder("CURSANDO", "CURSANDO")))
                .andExpect(jsonPath("$[*].courseCode",
                        containsInAnyOrder("ADS-2026", "DJD-2026")));
    }

    // ============================================================
    // AC5.3 — GET como coord retorna apenas vínculos do escopo dele
    // ============================================================
    @Test
    void getCourses_coordAna_retornaApenasVinculosDeAds() throws Exception {
        Cookie ana = loginAna();
        mockMvc.perform(get("/api/v1/students/" + ALICE + "/courses").cookie(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseCode", is("ADS-2026")))
                .andExpect(jsonPath("$[0].status", is("CURSANDO")));
    }

    // ============================================================
    // AC5.4 — PATCH para CONCLUIDO retorna 200 + atualiza no banco
    // ============================================================
    @Test
    void patchStatus_admin_atualizaParaConcluido() throws Exception {
        Cookie admin = loginAdmin();
        mockMvc.perform(patch("/api/v1/students/" + ALICE + "/courses/" + COURSE_ADS + "/status")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONCLUIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONCLUIDO")))
                .andExpect(jsonPath("$.courseCode", is("ADS-2026")));

        String dbStatus = jdbc.queryForObject(
                "SELECT status FROM student_course WHERE student_id = ?::uuid AND course_id = ?::uuid",
                String.class, ALICE, COURSE_ADS);
        assertThat(dbStatus).isEqualTo("CONCLUIDO");
    }

    // ============================================================
    // AC5.5 — PATCH com valor inválido retorna 400 (via @Pattern)
    // ============================================================
    @Test
    void patchStatus_valorInvalido_retorna400() throws Exception {
        Cookie admin = loginAdmin();
        mockMvc.perform(patch("/api/v1/students/" + ALICE + "/courses/" + COURSE_ADS + "/status")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVENTADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VAL_001")));
    }

    // ============================================================
    // AC5.6 — PATCH como coord em curso fora do escopo retorna 409
    // ============================================================
    @Test
    void patchStatus_coordEmCursoForaDoEscopo_retorna409() throws Exception {
        // Ana é Coord ADS; mexer em vínculo Alice×DJD deve dar 409.
        Cookie ana = loginAna();
        mockMvc.perform(patch("/api/v1/students/" + ALICE + "/courses/" + COURSE_DJD + "/status")
                        .cookie(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONCLUIDO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("BIZ_006")));
    }

    // ============================================================
    // Bônus: PATCH idempotente (mesmo status atual) retorna 200 sem mexer no banco
    // ============================================================
    @Test
    void patchStatus_mesmoStatusAtual_eIdempotente() throws Exception {
        Cookie admin = loginAdmin();
        // Alice/ADS já está CURSANDO (reset no @BeforeEach)
        mockMvc.perform(patch("/api/v1/students/" + ALICE + "/courses/" + COURSE_ADS + "/status")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CURSANDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CURSANDO")));
    }

    // ============================================================
    // Bônus: PATCH em vínculo inexistente retorna 404
    // ============================================================
    @Test
    void patchStatus_vinculoInexistente_retorna404() throws Exception {
        // Diego não tem vínculo em ADS — só em DJD.
        Cookie admin = loginAdmin();
        mockMvc.perform(patch("/api/v1/students/" + DIEGO + "/courses/" + COURSE_ADS + "/status")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONCLUIDO\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RES_001")));
    }

    // ============================================================
    // Bônus: GET para aluno inexistente retorna 404
    // ============================================================
    @Test
    void getCourses_alunoInexistente_retorna404() throws Exception {
        Cookie admin = loginAdmin();
        mockMvc.perform(get("/api/v1/students/00000000-0000-0000-0000-000000000000/courses")
                        .cookie(admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RES_001")));
    }

    // ============================================================
    // Helpers — login
    // ============================================================
    private Cookie loginAdmin() throws Exception {
        return doLogin("admin@valora.local", "Admin@123");
    }

    private Cookie loginAna() throws Exception {
        return doLogin("ana.coord@valora.local", "Admin@123");
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
}
