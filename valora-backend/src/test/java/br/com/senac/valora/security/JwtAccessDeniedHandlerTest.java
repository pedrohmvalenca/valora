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
import org.springframework.security.access.AccessDeniedException;

/** Testes unitários do {@link JwtAccessDeniedHandler}. */
class JwtAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    private final JwtAccessDeniedHandler accessDeniedHandler =
            new JwtAccessDeniedHandler(objectMapper);

    @Test
    void handle_retorna403ComErrorResponseAuth003() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/actuator/info");
        MockHttpServletResponse res = new MockHttpServletResponse();

        accessDeniedHandler.handle(req, res, new AccessDeniedException("denied"));

        assertThat(res.getStatus()).isEqualTo(403);
        // Content-Type inclui charset=UTF-8 para preservar acentos PT-BR
        assertThat(res.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(res.getCharacterEncoding()).isEqualTo("UTF-8");

        JsonNode body = objectMapper.readTree(res.getContentAsString());
        assertThat(body.get("error").asText()).isEqualTo("FORBIDDEN");
        assertThat(body.get("message").asText()).isEqualTo("Acesso negado");
        assertThat(body.get("code").asText()).isEqualTo("AUTH_003");
        assertThat(body.get("path").asText()).isEqualTo("/actuator/info");
        assertThat(body.has("timestamp")).isTrue();
    }
}
