package br.com.senac.valora.services;

import br.com.senac.valora.entities.UserProfile;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Geração e validação do JWT que carrega a sessão do VALORA.
 *
 * <p>Migração ADR-0013: substitui {@code io.jsonwebtoken:jjwt} pela lib
 * {@code com.auth0:java-jwt 4.5.x}, em convergência com {@code cldavi/valoraapi}.
 * Difere do colega em dois pontos importantes:
 * <ol>
 *   <li>{@link #validateToken(String)} <b>nunca</b> lança {@code RuntimeException}
 *       — retorna {@link Optional#empty()} em qualquer falha, evitando que o
 *       filter dispare HTTP 500 em token inválido.</li>
 *   <li>Expiração é configurável via {@code valora.jwt.expirationMinutes}
 *       (ADR-0008), em vez de constante hardcoded.</li>
 * </ol>
 *
 * <p>O {@link Clock} é injetável para permitir testes determinísticos com
 * tempo congelado.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String ISSUER = "valora";
    private static final String CLAIM_PROFILE = "profile";
    private static final int MIN_SECRET_LENGTH = 32; // 256 bits em ASCII

    @Value("${valora.jwt.secret}")
    private String secret;

    @Value("${valora.jwt.expirationMinutes}")
    private long expirationMinutes;

    private Clock clock = Clock.systemUTC();
    private Algorithm algorithm;
    private JWTVerifier verifier;

    /**
     * Valida o tamanho do secret e instancia algoritmo + verifier após injeção
     * de dependências. Falha rápido em configuração inválida — preferível em
     * boot a falhar silenciosamente em runtime.
     */
    @PostConstruct
    void init() {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "valora.jwt.secret deve ter no mínimo " + MIN_SECRET_LENGTH
                            + " caracteres (256 bits). Gerar via openssl rand -base64 64.");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
    }

    /**
     * Hook de teste para injetar Clock fixo (não usar em produção).
     * Mantido package-private para limitar surface area.
     */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Gera um JWT HS256 com claims mínimas:
     * {@code iss=valora}, {@code sub=<userId>}, {@code profile=<UserProfile>},
     * {@code iat=<now>}, {@code exp=<now + expirationMinutes>}.
     *
     * <p>Não inclui {@code linkedCourses} no token — esse dado é re-buscado
     * em {@code GET /auth/me} para manter o token enxuto.
     */
    public String generateToken(UUID userId, UserProfile profile) {
        Instant now = Instant.now(clock);
        Instant expiry = now.plusSeconds(expirationMinutes * 60);
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId.toString())
                .withClaim(CLAIM_PROFILE, profile.name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiry))
                .sign(algorithm);
    }

    /**
     * Valida assinatura, issuer e expiração. Retorna {@link Optional#empty()}
     * em qualquer falha (token nulo, malformado, expirado, assinatura inválida).
     *
     * <p>Não loga o token completo — apenas o tipo da exceção em DEBUG, para
     * evitar vazamento em arquivos de log.
     */
    public Optional<DecodedJWT> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(verifier.verify(token));
        } catch (JWTVerificationException ex) {
            log.debug("JWT inválido ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
            return Optional.empty();
        } catch (IllegalArgumentException ex) {
            log.debug("JWT com argumento inválido: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /** Duração total do token, em segundos — usado no {@code Max-Age} do cookie. */
    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
