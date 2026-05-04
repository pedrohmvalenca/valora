package br.com.senac.valora.exceptions;

public enum ErrorCode {

    // Auth (AUTH_*)
    INVALID_CREDENTIALS("AUTH_001"),
    UNAUTHORIZED("AUTH_002"),
    FORBIDDEN("AUTH_003"),

    // Validação (VAL_*)
    VALIDATION_ERROR("VAL_001"),
    MALFORMED_REQUEST("VAL_002"),

    // Recursos (RES_*)
    NOT_FOUND("RES_001"),

    // Banco (DB_*)
    DATA_INTEGRITY_VIOLATION("DB_001"),

    // Sistema (SYS_*)
    INTERNAL_ERROR("SYS_001"),

    // Regras de negócio (BIZ_*) — slots reservados para stories de domínio
    HARD_DELETE_BLOCKED("BIZ_001"),
    REJECTION_REASON_TOO_SHORT("BIZ_002"),
    PROOF_REQUIRED("BIZ_003"),
    CATEGORY_HOURS_LIMIT_EXCEEDED("BIZ_004"),
    DUPLICATE_PENDING_SUBMISSION("BIZ_005"),
    COORDINATOR_NOT_LINKED_TO_COURSE("BIZ_006"),
    SUBMISSION_STATUS_IMMUTABLE("BIZ_007");  // RN-0009 — Story 4.5 (consolidada γ)

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    /** Código estável consumido pelo frontend (Axios interceptor). */
    public String code() {
        return code;
    }
}
