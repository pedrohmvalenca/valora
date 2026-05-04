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

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("Corpo de request inválido em {}: {}", req.getRequestURI(),
                ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build("MALFORMED_REQUEST", "Corpo da requisição inválido",
                        ErrorCode.MALFORMED_REQUEST, req, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Acesso negado em {}: {}", req.getRequestURI(), ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build("FORBIDDEN", "Acesso negado",
                        ErrorCode.FORBIDDEN, req, null));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest req) {
        log.warn("Credenciais inválidas em {}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build("UNAUTHORIZED", "Credenciais inválidas",
                        ErrorCode.INVALID_CREDENTIALS, req, null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest req) {
        log.warn("Não autenticado em {}: {}", req.getRequestURI(), ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build("UNAUTHORIZED", "Não autenticado",
                        ErrorCode.UNAUTHORIZED, req, null));
    }


    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest req) {
        log.warn("Recurso não encontrado em {}: {}", req.getRequestURI(),
                ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build("NOT_FOUND", ex.getMessage(),
                        ErrorCode.NOT_FOUND, req, null));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest req) {
        log.warn("Path não mapeado: {}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build("NOT_FOUND", "Recurso não encontrado",
                        ErrorCode.NOT_FOUND, req, null));
    }

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro interno em {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("INTERNAL_ERROR", "Erro interno",
                        ErrorCode.INTERNAL_ERROR, req, null));
    }

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
