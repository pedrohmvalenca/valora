package br.com.senac.valora.security;

import br.com.senac.valora.exceptions.ErrorCode;
import br.com.senac.valora.exceptions.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Handler de acesso negado — invocado pelo Spring Security quando a request
 * chega autenticada mas o usuário não tem authority/role para o endpoint
 * (ex.: {@code requestMatchers("/actuator/info").hasRole("ADMINISTRATOR")} e
 * o cookie é de COORDINATOR).
 *
 * <p>Coexiste com {@code GlobalExceptionHandler.handleAccessDenied}: este
 * handler cobre o caminho do filter chain (regras do {@code SecurityConfig}),
 * o {@code @ExceptionHandler(AccessDeniedException.class)} cobre quando a
 * exceção vem de dentro de Service/Controller. Mesmo shape de resposta.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                "FORBIDDEN",
                "Acesso negado",
                ErrorCode.FORBIDDEN.code(),
                request.getRequestURI(),
                null);

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8"); // garante "Acesso negado" sem quebrar acentos
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
