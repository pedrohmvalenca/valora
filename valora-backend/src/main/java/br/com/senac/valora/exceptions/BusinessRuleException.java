package br.com.senac.valora.exceptions;

/**
 * Exceção lançada por Services para violar regras de domínio (RN-0001 a RN-0010).
 *
 * <p>Mapeada para HTTP 409 pelo {@link GlobalExceptionHandler}. O
 * {@link ErrorCode} carrega o código específico da regra (ex.: {@code BIZ_001}
 * para hard delete bloqueado), e {@code message} carrega a explicação
 * user-facing em PT-BR vinda do throw da Service.
 *
 * <p>Esta story (1.4) declara a classe e o enum de códigos {@code BIZ_*},
 * mas o consumo real começa nas stories de domínio (2.3, 3.4, 3.8, 4.5,
 * 4.6, 4.x).
 */
public class BusinessRuleException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessRuleException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
