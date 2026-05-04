package br.com.senac.valora.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        String error,
        String message,
        String code,
        String path,
        List<FieldError> details) {

    public record FieldError(String field, Object rejectedValue, String constraint) {}
}
