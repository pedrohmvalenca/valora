package br.com.senac.valora.security;

import br.com.senac.valora.entities.UserProfile;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * {@link org.springframework.security.core.Authentication} populado pelo
 * {@link JwtFilter} a partir do JWT do cookie {@code AUTH_TOKEN}.
 *
 * <p>O principal é o UUID do usuário (não a entidade completa) — economiza
 * round-trip ao banco quando o controller só precisa do ID. Para obter o
 * payload de {@code GET /auth/me}, o controller pede o User via
 * {@link br.com.senac.valora.services.AuthService#buildMeResponse(UUID)}.
 *
 * <p><b>Ajuste Story 1.4:</b> antes desta story, {@code getAuthorities()}
 * retornava lista expandida hierarquicamente (ADMINISTRATOR também tinha
 * ROLE_COORDINATOR + ROLE_STUDENT). Agora retorna apenas <b>uma</b>
 * authority — a do profile atual. A expansão hierárquica é responsabilidade
 * do {@link MethodSecurityConfig#roleHierarchy()} (fonte única). Isso
 * elimina a duplicação de lógica entre {@code User.getAuthorities} e
 * {@code JwtAuthentication.buildAuthorities} apontada no review da Story 1.3.
 */
public class JwtAuthentication extends AbstractAuthenticationToken {

    private final UUID userId;
    private final UserProfile profile;

    public JwtAuthentication(UUID userId, UserProfile profile) {
        super(buildAuthorities(profile));
        this.userId = userId;
        this.profile = profile;
        setAuthenticated(true); // o JwtFilter só constrói este objeto após verify() bem-sucedido
    }

    private static Collection<GrantedAuthority> buildAuthorities(UserProfile profile) {
        // Apenas a authority do profile — a hierarquia (ADMINISTRATOR > COORDINATOR > STUDENT)
        // é expandida pelo RoleHierarchy bean em runtime.
        return List.of(new SimpleGrantedAuthority("ROLE_" + profile.name()));
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    /** Sem credenciais a expor — JWT já foi verificado pelo filter. */
    @Override
    public Object getCredentials() {
        return null;
    }

    public UUID userId() {
        return userId;
    }

    public UserProfile profile() {
        return profile;
    }
}
