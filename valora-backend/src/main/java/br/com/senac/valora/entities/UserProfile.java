package br.com.senac.valora.entities;

/**
 * Perfis (RBAC) do VALORA — substitui o termo "Role" para evitar colisão com
 * o prefixo {@code ROLE_*} do Spring Security.
 *
 * <p>Hierarquia (ADR-0011 + Story 1.4):
 * <ul>
 *   <li>{@link #ADMINISTRATOR} — herda permissões de COORDINATOR e STUDENT</li>
 *   <li>{@link #COORDINATOR}   — herda permissões de STUDENT</li>
 *   <li>{@link #STUDENT}       — base</li>
 * </ul>
 *
 * <p>A hierarquia é traduzida para {@code GrantedAuthority} no método
 * {@code User.getAuthorities()} e no {@code JwtAuthentication}.
 */
public enum UserProfile {
    ADMINISTRATOR,
    COORDINATOR,
    STUDENT
}
