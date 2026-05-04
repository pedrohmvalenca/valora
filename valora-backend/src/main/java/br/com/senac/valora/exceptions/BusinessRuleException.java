package br.com.senac.valora.exceptions;

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
