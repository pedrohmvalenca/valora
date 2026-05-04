package br.com.senac.valora.security;

import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.services.JwtService;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter que lê o cookie {@code AUTH_TOKEN} (httpOnly), valida o JWT e popula
 * o {@code SecurityContext} com {@link JwtAuthentication}.
 *
 * <p>Diferenças intencionais em relação ao {@code cldavi/valoraapi}:
 * <ul>
 *   <li>Lê do cookie httpOnly, <b>não</b> do header {@code Authorization}
 *       (defesa contra XSS — token nunca acessível via JS).</li>
 *   <li>Falha silenciosa: token ausente/inválido apenas <b>não autentica</b>
 *       e continua o chain. O {@code SecurityFilterChain} responde 401.
 *       <b>Nunca</b> dispara HTTP 500 — anti-pattern do colega corrigido.</li>
 * </ul>
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final String COOKIE_NAME = "AUTH_TOKEN";
    private static final String CLAIM_PROFILE = "profile";

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Optional<String> tokenOpt = extractTokenFromCookie(request);

        if (tokenOpt.isPresent()) {
            jwtService.validateToken(tokenOpt.get()).ifPresent(jwt -> populateContext(jwt));
        }

        // Independentemente de autenticar ou não, prossegue o chain.
        // Se SecurityContext continuar vazio, SecurityFilterChain devolve 401.
        chain.doFilter(request, response);
    }

    private Optional<String> extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private void populateContext(DecodedJWT jwt) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            UserProfile profile = UserProfile.valueOf(jwt.getClaim(CLAIM_PROFILE).asString());
            JwtAuthentication auth = new JwtAuthentication(userId, profile);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (IllegalArgumentException | NullPointerException ex) {
            // Token assinado, mas com claim corrompida — segue como não autenticado.
            // Não logar o token; só o tipo da falha em DEBUG.
            log.debug("JWT com claim inválida: {}", ex.getClass().getSimpleName());
        }
    }
}
