package br.com.senac.valora.exceptions;

/**
 * Códigos de erro do VALORA.
 *
 * <p>Estes códigos são consumidos NESTA story (1.4) — usados pelo
 * {@link GlobalExceptionHandler}, {@code JwtAuthenticationEntryPoint} e
 * {@code JwtAccessDeniedHandler}:
 * <ul>
 *   <li>{@code AUTH_*} — autenticação e autorização</li>
 *   <li>{@code VAL_*} — validação de payload</li>
 *   <li>{@code RES_*} — recurso não encontrado</li>
 *   <li>{@code DB_*}  — integridade de dados</li>
 *   <li>{@code SYS_*} — erro de sistema</li>
 * </ul>
 *
 * <p>Os entries {@code BIZ_*} são <b>slots reservados</b> para regras de
 * negócio — declarados mas não consumidos nesta story. Cada um já cita a
 * story-alvo que vai consumir; YAGNI manda não criar até a story chegar:
 * <ul>
 *   <li>{@link #HARD_DELETE_BLOCKED} — RN-0008 (Stories 2.3 / 3.4 / 3.8)</li>
 *   <li>{@link #REJECTION_REASON_TOO_SHORT} — RN-0006 (Story 4.6)</li>
 *   <li>{@link #PROOF_REQUIRED} — RN-0005 (Story 4.x)</li>
 *   <li>{@link #CATEGORY_HOURS_LIMIT_EXCEEDED} — RN-0004 (Story 4.5)</li>
 *   <li>{@link #DUPLICATE_PENDING_SUBMISSION} — RN-0010 (Story 4.x)</li>
 *   <li>{@link #COORDINATOR_NOT_LINKED_TO_COURSE} — RN-0001 (Stories 4.x)</li>
 * </ul>
 */
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
