package br.com.senac.valora.security;

import br.com.senac.valora.entities.UserProfile;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtAuthentication extends AbstractAuthenticationToken {

    private final UUID userId;
    private final UserProfile profile;

    public JwtAuthentication(UUID userId, UserProfile profile) {
        super(buildAuthorities(profile));
        this.userId = userId;
        this.profile = profile;
        setAuthenticated(true); 
    }

    private static Collection<GrantedAuthority> buildAuthorities(UserProfile profile) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + profile.name()));
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

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
