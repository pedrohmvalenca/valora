package br.com.senac.valora.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Testes unitários do {@link JwtAuthenticationEntryPoint}.
 * Usa {@link MockHttpServletResponse} para capturar status, headers e body.
 */
class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    private final JwtAuthenticationEntryPoint entryPoint =
            new JwtAuthenticationEntryPoint(objectMapper);

    @Test
    void commence_retorna401ComErrorResponseAuth002() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/protegido");
        MockHttpServletResponse res = new MockHttpServletResponse();

        entryPoint.commence(req, res, new BadCredentialsException("não importa"));

        assertThat(res.getStatus()).isEqualTo(401);
        // Content-Type inclui charset=UTF-8 para preservar acentos PT-BR
        assertThat(res.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(res.getCharacterEncoding()).isEqualTo("UTF-8");

        JsonNode body = objectMapper.readTree(res.getContentAsString());
        assertThat(body.get("error").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(body.get("message").asText()).isEqualTo("Não autenticado");
        assertThat(body.get("code").asText()).isEqualTo("AUTH_002");
        assertThat(body.get("path").asText()).isEqualTo("/api/v1/protegido");
        assertThat(body.has("timestamp")).isTrue();
        assertThat(body.has("details")).isFalse(); // @JsonInclude(NON_NULL)
    }

    @Test
    void commence_setaContentTypeApplicationJson() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/qualquer");
        MockHttpServletResponse res = new MockHttpServletResponse();

        entryPoint.commence(req, res, new BadCredentialsException("x"));

        assertThat(res.getContentType()).contains("application/json");
    }
}
