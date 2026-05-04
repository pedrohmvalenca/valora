package br.com.senac.valora.entities;

/**
 * Status de submissão (RN-0009 — imutável após decisão).
 * <ul>
 *   <li>{@code PENDING} — aguardando decisão de Coord/Admin</li>
 *   <li>{@code APPROVED} — aprovada (com ou sem ajuste de horas)</li>
 *   <li>{@code REJECTED} — reprovada com motivo (RN-0006 — ≥20 chars)</li>
 * </ul>
 */
public enum SubmissionStatus {
    PENDING, APPROVED, REJECTED
}
