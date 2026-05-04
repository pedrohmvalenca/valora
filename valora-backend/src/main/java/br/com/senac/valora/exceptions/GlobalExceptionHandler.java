package br.com.senac.valora.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Manipulador global de exceções da API VALORA — RNF-0009.
 *
 * <p>Centraliza a serialização de erros em um único shape ({@link ErrorResponse}),
 * impedindo que stack traces, queries SQL ou paths internos vazem para o cliente.
 *
 * <p>Coexiste com {@code JwtAuthenticationEntryPoint} e {@code JwtAccessDeniedHandler}:
 * exceções lançadas <b>dentro</b> de Controllers/Services chegam aqui via
 * {@link RestControllerAdvice}; exceções emergidas no <b>filter chain</b>
 * (token inválido, ausência de cookie) são tratadas pelos handlers do
 * {@link org.springframework.security.web.SecurityFilterChain}. As respostas
 * mantêm o mesmo shape em ambos os caminhos.
 *
 * <p>{@link Order @Order(HIGHEST_PRECEDENCE)} garante que este advice ganhe de
 * outros advices que possam vir de starters do Spring Boot (ex.: validation).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ============================================================
    // 400 — Validation
    // ============================================================

    /**
     * Bean Validation falhou em payload anotado com {@code @Valid}.
     * Retorna 400 com array {@code details} listando cada campo inválido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(
                        fe.getField(),
                        fe.getRejectedValue(),
                        fe.getCode()))
                .toList();
        log.warn("Validation falhou em {} ({} erro(s))", req.getRequestURI(), details.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build("VALIDATION_ERROR", "Requisição com campos inválidos",
                        ErrorCode.VALIDATION_ERROR, req, details));
    }

    /**
     * JSON malformado / corpo da request não parseável. Mensagem genérica
     * — nunca propagar {@code ex.getMessage()} (vaza estrutura interna).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("Corpo de request inválido em {}: {}", req.getRequestURI(),
                ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build("MALFORMED_REQUEST", "Corpo da requisição inválido",
                        ErrorCode.MALFORMED_REQUEST, req, null));
    }

    // ============================================================
    // 401/403 — Auth
    // ============================================================

    /**
     * {@link AccessDeniedException} lançada de dentro de Service/Controller
     * (exceções emergidas no filter chain são tratadas por
     * {@code JwtAccessDeniedHandler}).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Acesso negado em {}: {}", req.getRequestURI(), ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build("FORBIDDEN", "Acesso negado",
                        ErrorCode.FORBIDDEN, req, null));
    }

    /**
     * Credenciais de login inválidas (email inexistente, senha errada ou usuário
     * inativo — {@code AuthService.authenticate} lança {@link BadCredentialsException}
     * sem distinguir os 3 casos para evitar enumeração de usuários, OWASP A07).
     *
     * <p>Handler mais específico que {@link #handleAuthentication}; Spring escolhe
     * este por subtype-match em runtime independentemente da ordem de declaração.
     * Distinção: {@code AUTH_001} é "credenciais inválidas no login";
     * {@code AUTH_002} é "não autenticado em endpoint protegido".
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest req) {
        log.warn("Credenciais inválidas em {}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build("UNAUTHORIZED", "Credenciais inválidas",
                        ErrorCode.INVALID_CREDENTIALS, req, null));
    }

    /**
     * {@link AuthenticationException} lançada de dentro de Service/Controller
     * (subclasses específicas como {@link BadCredentialsException} têm handlers
     * dedicados acima — Spring resolve por especificidade de tipo).
     * O caminho do filter chain é coberto por {@code JwtAuthenticationEntryPoint}.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest req) {
        log.warn("Não autenticado em {}: {}", req.getRequestURI(), ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build("UNAUTHORIZED", "Não autenticado",
                        ErrorCode.UNAUTHORIZED, req, null));
    }

    // ============================================================
    // 404 — Not Found
    // ============================================================

    /** Service lança quando recurso solicitado não existe. */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest req) {
        log.warn("Recurso não encontrado em {}: {}", req.getRequestURI(),
                ex.getClass().getSimpleName());
        // Service controla a mensagem (vem do throw com texto user-facing PT-BR)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build("NOT_FOUND", ex.getMessage(),
                        ErrorCode.NOT_FOUND, req, null));
    }

    /**
     * Path da request não bate com nenhum controller mapeado.
     * Requer {@code spring.mvc.throw-exception-if-no-handler-found=true} +
     * {@code spring.web.resources.add-mappings=false} no {@code application.yml}.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest req) {
        log.warn("Path não mapeado: {}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build("NOT_FOUND", "Recurso não encontrado",
                        ErrorCode.NOT_FOUND, req, null));
    }

    // ============================================================
    // 409 — Conflict
    // ============================================================

    /**
     * Violação de constraint do banco (UNIQUE, FK, NOT NULL etc.).
     * Mensagem genérica intencional — NÃO incluir {@code ex.getMostSpecificCause().getMessage()}
     * (vaza nome de constraint, query parts e às vezes valores).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}", req.getRequestURI(),
                ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build("DATA_INTEGRITY_VIOLATION", "Operação viola integridade de dados",
                        ErrorCode.DATA_INTEGRITY_VIOLATION, req, null));
    }

    /** Regra de domínio violada — Service lança {@link BusinessRuleException}. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest req) {
        log.warn("Regra de negócio violada em {} ({}): {}",
                req.getRequestURI(), ex.errorCode().code(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        Instant.now(),
                        "BUSINESS_RULE_VIOLATION",
                        ex.getMessage(),
                        ex.errorCode().code(),
                        req.getRequestURI(),
                        null));
    }

    // ============================================================
    // 500 — Catch-all
    // ============================================================

    /**
     * Catch-all para qualquer {@link Exception} não tratada explicitamente.
     * NUNCA propagar {@code ex.getMessage()} — RNF-0009 manda ocultar.
     * Stack completo vai apenas para o log do servidor (nível ERROR).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro interno em {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("INTERNAL_ERROR", "Erro interno",
                        ErrorCode.INTERNAL_ERROR, req, null));
    }

    // ============================================================
    // Helper
    // ============================================================

    /** Construtor centralizado de {@link ErrorResponse} — evita duplicação. */
    private ErrorResponse build(
            String error,
            String message,
            ErrorCode code,
            HttpServletRequest req,
            List<ErrorResponse.FieldError> details) {
        return new ErrorResponse(
                Instant.now(),
                error,
                message,
                code.code(),
                req.getRequestURI(),
                details);
    }
}
