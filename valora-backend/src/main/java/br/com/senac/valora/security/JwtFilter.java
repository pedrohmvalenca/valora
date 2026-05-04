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
            log.debug("JWT com claim inválida: {}", ex.getClass().getSimpleName());
        }
    }
}
