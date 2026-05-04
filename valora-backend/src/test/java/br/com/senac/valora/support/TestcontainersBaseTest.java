package br.com.senac.valora.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base abstrata para testes de integração com Postgres real via Testcontainers.
 *
 * <p>Pattern: container <b>estático</b> + {@code withReuse(true)} compartilha
 * a mesma instância Docker entre classes de teste, derrubando o tempo total
 * da suíte. O reuse exige a flag {@code testcontainers.reuse.enable=true} no
 * arquivo {@code ~/.testcontainers.properties} (a primeira execução em cada
 * máquina cria o arquivo).
 *
 * <p>Resolve o deferred work da Story 1.1: {@code ValoraApplicationTests} agora
 * estende esta base e roda em CI sem precisar de {@code DATABASE_URL}/{@code JWT_SECRET}
 * setados no ambiente.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class TestcontainersBaseTest {

    @SuppressWarnings("resource") // container reused — não fechar manualmente
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("valora_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
