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

    void setClock(Clock clock) {
        this.clock = clock;
    }

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

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
