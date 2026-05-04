package br.com.senac.valora.dtos;

/**
 * Saldo de horas por (categoria × curso) para uma submissão — exibido no detail.
 * RN-0004 + EXT-02. Story 4.5 (consolidada γ).
 */
public record BalanceDto(int maxHours, int accumulatedHours, int remainingHours) {}
