package br.com.senac.valora.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private UserProfile profile;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // Sempre adiciona o role do próprio perfil
        authorities.add(new SimpleGrantedAuthority("ROLE_" + profile.name()));

        // Hierarquia plana — espelha lógica do colega para consistência inter-equipe
        switch (profile) {
            case ADMINISTRATOR -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_COORDINATOR"));
                authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
            }
            case COORDINATOR -> authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
            case STUDENT -> { /* sem ramificação adicional */ }
        }
        return authorities;
    }

    /** Spring Security usa {@code username} como credencial — aqui é o email. */
    @Override
    public String getUsername() {
        return email;
    }

    /** O contrato espera o hash BCrypt persistido — Spring Security cuida da comparação. */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Reflete o flag {@code is_active} — usuário inativo é tratado como "desabilitado". */
    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
