package br.com.senac.valora.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Shape único de erro para toda a API VALORA — RNF-0009.
 *
 * <p>Substitui o {@code dtos/ErrorResponse} simples da Story 1.3.
 * Construído pelo {@link GlobalExceptionHandler}, pelo
 * {@code JwtAuthenticationEntryPoint} e pelo {@code JwtAccessDeniedHandler};
 * o {@code AuthController.login} também usa este record no caminho 401
 * (controlled flow — não passa por exception).
 *
 * <p>Convenção {@link JsonInclude.Include#NON_NULL}: o campo {@code details}
 * é omitido do JSON quando {@code null} (não retornar {@code null} nem
 * lista vazia ao frontend).
 *
 * @param timestamp instante UTC ISO 8601 do erro
 * @param error     identificador estável do tipo de erro (em inglês)
 * @param message   mensagem user-facing em PT-BR
 * @param code      código de erro do enum {@link ErrorCode}
 * @param path      URI da request que originou o erro
 * @param details   lista de violações de Bean Validation (apenas para
 *                  {@code VALIDATION_ERROR}); {@code null} caso contrário
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        String error,
        String message,
        String code,
        String path,
        List<FieldError> details) {

    /**
     * Detalhe de uma violação de Bean Validation — populado apenas
     * em respostas {@code VALIDATION_ERROR}.
     *
     * @param field         nome do campo que falhou
     * @param rejectedValue valor recebido que foi rejeitado
     * @param constraint    constraint violada (ex.: {@code NotBlank}, {@code Size})
     */
    public record FieldError(String field, Object rejectedValue, String constraint) {}
}
