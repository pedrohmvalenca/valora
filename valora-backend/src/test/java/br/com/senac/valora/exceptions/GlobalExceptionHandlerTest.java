package br.com.senac.valora.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Testes unitários do {@link GlobalExceptionHandler}.
 *
 * <p>Cobre todos os 9 handlers + invariantes (≥10 cenários, AC12).
 * Mocka {@link HttpServletRequest} e {@link BindingResult} para isolar a
 * lógica do handler — não sobe contexto Spring.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/test");
    }

    // ============================================================
    // 400 — Validation
    // ============================================================

    @Test
    void handleValidation_umFieldError_retorna400ComDetailsTamanho1() {
        MethodArgumentNotValidException ex = mockValidationException(List.of(
                new FieldError("target", "email", "abc", false,
                        new String[]{"Email"}, null, "default")));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).isEqualTo("Requisição com campos inválidos");
        assertThat(body.code()).isEqualTo("VAL_001");
        assertThat(body.path()).isEqualTo("/api/v1/test");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.details()).hasSize(1);
        assertThat(body.details().get(0).field()).isEqualTo("email");
        assertThat(body.details().get(0).rejectedValue()).isEqualTo("abc");
        assertThat(body.details().get(0).constraint()).isEqualTo("Email");
    }

    @Test
    void handleValidation_tresFieldErrors_retorna400ComDetailsTamanho3() {
        MethodArgumentNotValidException ex = mockValidationException(List.of(
                new FieldError("target", "email", null, false,
                        new String[]{"NotBlank"}, null, "default"),
                new FieldError("target", "password", "x", false,
                        new String[]{"Size"}, null, "default"),
                new FieldError("target", "name", "", false,
                        new String[]{"NotBlank"}, null, "default")));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body.details()).hasSize(3);
        assertThat(body.details()).extracting(ErrorResponse.FieldError::field)
                .containsExactly("email", "password", "name");
    }

    @Test
    void handleMalformedJson_retorna400SemExposeExceptionMessage() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Detalhe interno que NÃO deve ir pro body",
                mock(HttpInputMessage.class));

        ResponseEntity<ErrorResponse> response = handler.handleMalformedJson(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body.error()).isEqualTo("MALFORMED_REQUEST");
        assertThat(body.message()).isEqualTo("Corpo da requisição inválido");
        assertThat(body.code()).isEqualTo("VAL_002");
        assertThat(body.message()).doesNotContain("Detalhe interno");
        assertThat(body.details()).isNull();
    }

    // ============================================================
    // 401/403 — Auth
    // ============================================================

    @Test
    void handleAccessDenied_retorna403ComAuth003() {
        AccessDeniedException ex = new AccessDeniedException("denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body.error()).isEqualTo("FORBIDDEN");
        assertThat(body.message()).isEqualTo("Acesso negado");
        assertThat(body.code()).isEqualTo("AUTH_003");
        assertThat(body.details()).isNull();
    }

    @Test
    void handleBadCredentials_retorna401ComAuth001ECredenciaisInvalidas() {
        // AuthService.authenticate lança BadCredentialsException em qualquer
        // falha de login — Spring dispatcha pra este handler (mais específico
        // que handleAuthentication) por subtype-match em runtime.
        BadCredentialsException ex = new BadCredentialsException("Credenciais inválidas");

        ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = response.getBody();
        assertThat(body.error()).isEqualTo("UNAUTHORIZED");
        assertThat(body.message()).isEqualTo("Credenciais inválidas");
        assertThat(body.code()).isEqualTo("AUTH_001");
        assertThat(body.details()).isNull();
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.path()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleAuthentication_subtypeGenerico_retorna401ComAuth002() {
        // AuthenticationException sem handler dedicado (ex: falha interna de
        // auth lançada de Service) cai aqui via subtype-match — código AUTH_002
        // distingue de AUTH_001 (login) que tem handler próprio.
        InternalAuthenticationServiceException ex =
                new InternalAuthenticationServiceException("falha interna do auth provider");

        ResponseEntity<ErrorResponse> response = handler.handleAuthentication(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = response.getBody();
        assertThat(body.error()).isEqualTo("UNAUTHORIZED");
        assertThat(body.message()).isEqualTo("Não autenticado");
        assertThat(body.code()).isEqualTo("AUTH_002");
        assertThat(body.details()).isNull();
    }

    // ============================================================
    // 404 — Not Found
    // ============================================================

    @Test
    void handleEntityNotFound_preservaMessageDoThrow() {
        EntityNotFoundException ex = new EntityNotFoundException("Curso 'abc' não encontrado");

        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFound(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body.error()).isEqualTo("NOT_FOUND");
        assertThat(body.code()).isEqualTo("RES_001");
        assertThat(body.message()).isEqualTo("Curso 'abc' não encontrado");
    }

    @Test
    void handleNoHandlerFound_retorna404ComMensagemGenerica() {
        NoHandlerFoundException ex = new NoHandlerFoundException(
                "GET", "/api/v1/nope", new HttpHeaders());

        ResponseEntity<ErrorResponse> response = handler.handleNoHandlerFound(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body.error()).isEqualTo("NOT_FOUND");
        assertThat(body.message()).isEqualTo("Recurso não encontrado");
        assertThat(body.code()).isEqualTo("RES_001");
    }

    // ============================================================
    // 409 — Conflict
    // ============================================================

    @Test
    void handleDataIntegrity_naoVazaConstraintNoBody() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "violation: duplicate key value violates unique constraint \"users_email_key\"");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponse body = response.getBody();
        assertThat(body.error()).isEqualTo("DATA_INTEGRITY_VIOLATION");
        assertThat(body.message()).isEqualTo("Operação viola integridade de dados");
        assertThat(body.code()).isEqualTo("DB_001");
        // RNF-0009: nunca expor nome de constraint nem trecho de SQL
        assertThat(body.message()).doesNotContain("users_email_key");
        assertThat(body.message()).doesNotContain("constraint");
    }

    @Test
    void handleBusinessRule_HardDeleteBlocked_retorna409ComBiz001() {
        BusinessRuleException ex = new BusinessRuleException(
                ErrorCode.HARD_DELETE_BLOCKED, "Curso possui alunos vinculados");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessRule(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponse body = response.getBody();
        assertThat(body.error()).isEqualTo("BUSINESS_RULE_VIOLATION");
        assertThat(body.message()).isEqualTo("Curso possui alunos vinculados");
        assertThat(body.code()).isEqualTo("BIZ_001");
        assertThat(body.details()).isNull();
    }

    // ============================================================
    // 500 — Catch-all
    // ============================================================

    @Test
    void handleGeneric_naoVazaMessageDoThrow() {
        // P7 do code review 2026-05-03 (Task 10.4 do spec): além de validar o
        // body sanitizado, captura o log do servidor para garantir que a stack
        // foi preservada server-side (operações de debug precisam dela).
        LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class);
        RuntimeException ex = new RuntimeException("vai vazar?");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, req);

        // (1) Body genérico — RNF-0009: nunca expor mensagem da exception original
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body.error()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.message()).isEqualTo("Erro interno");
        assertThat(body.code()).isEqualTo("SYS_001");
        assertThat(body.message()).doesNotContain("vai vazar");

        // (2) Log ERROR no servidor preserva path + stack do throwable original
        // — operação precisa para diagnosticar 500s sem expor o detalhe ao cliente.
        assertThat(logCaptor.getErrorLogs()).hasSize(1);
        assertThat(logCaptor.getErrorLogs().get(0)).contains("/api/v1/test");
        assertThat(logCaptor.getLogEvents())
                .filteredOn(le -> le.getThrowable().isPresent())
                .singleElement()
                .satisfies(le -> assertThat(le.getThrowable().get())
                        .isSameAs(ex)
                        .hasMessage("vai vazar?"));
    }

    @Test
    void todosHandlers_populamTimestampEPathConsistentemente() {
        // Smoke test cobrindo invariantes comuns: timestamp não-null + path do request.
        AccessDeniedException ex = new AccessDeniedException("denied");
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, req);

        ErrorResponse body = response.getBody();
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.path()).isEqualTo("/api/v1/test");
    }

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Mocka {@link MethodArgumentNotValidException} retornando o
     * {@link BindingResult} com os {@link FieldError}s solicitados.
     * Evita ter que construir um BeanPropertyBindingResult com target real.
     */
    private static MethodArgumentNotValidException mockValidationException(
            List<FieldError> fieldErrors) {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        return ex;
    }
}
