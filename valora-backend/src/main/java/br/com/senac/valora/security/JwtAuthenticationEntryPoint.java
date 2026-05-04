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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Entry point de autenticação — invocado pelo Spring Security quando a request
 * chega não autenticada em endpoint protegido (token ausente, inválido, expirado).
 *
 * <p>Sem este bean, Spring devolveria 401 com body padrão (vazio ou texto Spring),
 * quebrando o contrato JSON consumido pelo Axios interceptor do frontend.
 * O {@link br.com.senac.valora.exceptions.GlobalExceptionHandler} cobre apenas
 * exceções emergidas <b>dentro</b> de Service/Controller — o filter chain
 * precisa do entry point dedicado.
 *
 * <p>Shape de resposta idêntico ao do {@code GlobalExceptionHandler} — fonte
 * única é o {@link ErrorResponse}.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                "UNAUTHORIZED",
                "Não autenticado",
                ErrorCode.UNAUTHORIZED.code(),
                request.getRequestURI(),
                null);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8"); // garante "Não autenticado" sem quebrar acentos
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
