package br.com.senac.valora.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.senac.valora.entities.UserProfile;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários do {@link JwtService}.
 *
 * <p>Os campos {@code @Value} são injetados via reflection (ao invés de
 * {@code @SpringBootTest}) para manter o teste rápido e isolado. O Clock é
 * substituído por um clock fixo para verificar comportamento de expiração de
 * forma determinística.
 */
class JwtServiceTest {

    private static final String VALID_SECRET =
            "test-only-secret-min-256-bits-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String OTHER_SECRET =
            "OUTRO-secret-totalmente-diferente-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    private JwtService service;
    private Clock fixedClock;

    @BeforeEach
    void setUp() throws Exception {
        // Clock fixo em 2026-05-03T12:00:00Z — valores determinísticos
        fixedClock = Clock.fixed(Instant.parse("2026-05-03T12:00:00Z"), ZoneOffset.UTC);
        service = buildService(VALID_SECRET, 240L);
    }

    @Test
    void postConstruct_rejeitaSecretMenorQue32Chars() {
        // Reflection embrulha a exception do @PostConstruct em InvocationTargetException.
        assertThatThrownBy(() -> buildService("curto-demais", 240L))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .rootCause()
                .hasMessageContaining("256 bits");
    }

    @Test
    void generateToken_produzTokenComClaimsCorretas() throws Exception {
        // O verifier do auth0-jwt 4.x usa System.currentTimeMillis() (Clock interno
        // é package-private — não acessível). Para que validateToken não rejeite
        // o token (iat ≤ now é checado por default), o iat precisa ser "no presente".
        // Usamos um clock fixo poucos segundos no passado + expiração longa.
        Instant base = Instant.now().minusSeconds(5);
        // Trunca para segundos para casar com a precisão do Date no JWT
        base = base.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        Clock fixed = Clock.fixed(base, ZoneOffset.UTC);
        JwtService localService = buildService(VALID_SECRET, 60L * 24); // 1 dia de validade
        setClock(localService, fixed);

        UUID userId = UUID.randomUUID();
        String token = localService.generateToken(userId, UserProfile.ADMINISTRATOR);

        Optional<DecodedJWT> result = localService.validateToken(token);
        assertThat(result).isPresent();
        DecodedJWT jwt = result.get();
        assertThat(jwt.getIssuer()).isEqualTo("valora");
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaim("profile").asString()).isEqualTo("ADMINISTRATOR");
        assertThat(jwt.getIssuedAt()).isEqualTo(java.util.Date.from(base));
        // exp = iat + 1 dia
        assertThat(jwt.getExpiresAt()).isEqualTo(java.util.Date.from(base.plusSeconds(60L * 60 * 24)));
    }

    @Test
    void validateToken_rejeitaTokenAssinadoComOutroSecret() throws Exception {
        JwtService outro = buildService(OTHER_SECRET, 240L);
        String token = outro.generateToken(UUID.randomUUID(), UserProfile.STUDENT);

        Optional<DecodedJWT> result = service.validateToken(token);
        assertThat(result).isEmpty();
    }

    @Test
    void validateToken_rejeitaTokenExpirado() throws Exception {
        // Gera com clock antigo (1h atrás); valida com clock atual + expiração 30min => expirado
        Clock past = Clock.fixed(Instant.parse("2026-05-03T10:00:00Z"), ZoneOffset.UTC);
        JwtService geradorAntigo = buildService(VALID_SECRET, 30L); // 30min de validade
        setClock(geradorAntigo, past);
        String token = geradorAntigo.generateToken(UUID.randomUUID(), UserProfile.COORDINATOR);

        // service principal está com clock fixo em 12:00 — token gerado às 10:00 com 30min já expirou
        Optional<DecodedJWT> result = service.validateToken(token);
        assertThat(result).isEmpty();
    }

    @Test
    void validateToken_rejeitaTokenMalformado() {
        assertThat(service.validateToken("garbage-not-jwt")).isEmpty();
        assertThat(service.validateToken("")).isEmpty();
        assertThat(service.validateToken(null)).isEmpty();
    }

    @Test
    void getExpirationSeconds_retornaMinutosVezes60() {
        assertThat(service.getExpirationSeconds()).isEqualTo(240L * 60);
    }

    // ============================================================
    // Helpers — instancia JwtService injetando @Value via reflection
    // ============================================================
    private JwtService buildService(String secret, long expirationMinutes) throws Exception {
        JwtService s = new JwtService();
        setField(s, "secret", secret);
        setField(s, "expirationMinutes", expirationMinutes);
        // chama @PostConstruct manualmente
        java.lang.reflect.Method init = JwtService.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(s);
        setClock(s, fixedClock);
        return s;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = JwtService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void setClock(JwtService s, Clock clock) throws Exception {
        java.lang.reflect.Method m = JwtService.class.getDeclaredMethod("setClock", Clock.class);
        m.setAccessible(true);
        m.invoke(s, clock);
    }
}
