package br.com.senac.valora;

import br.com.senac.valora.support.TestcontainersBaseTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test do contexto Spring Boot.
 *
 * <p>Antes da Story 1.3 era um {@code @SpringBootTest} bare bones que falhava
 * em CI sem {@code DATABASE_URL}/{@code JWT_SECRET} setados. Agora estende
 * {@link TestcontainersBaseTest} — sobe Postgres em container, aplica Flyway
 * V1 e valida que os beans (incluindo Security, JwtService, AuthService)
 * carregam sem erro.
 */
class ValoraApplicationTests extends TestcontainersBaseTest {

    @Test
    void contextLoads() {
        // ApplicationContext sobe via @SpringBootTest herdado da base.
    }
}
