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

/**
 * Identidade base unificada do VALORA: email + senha (hash BCrypt) + perfil.
 *
 * <p>Mapeia a tabela {@code users} criada na migration {@code V1__init_identity.sql}
 * (Story 1.1). Implementa {@link UserDetails} para integrar com Spring Security
 * via {@link br.com.senac.valora.services.AuthService#loadUserByUsername(String)}.
 *
 * <p>Não modela vínculos de cursos aqui — esses ficam em
 * {@link CoordinatorCourse} (e futuramente em {@code StudentCourse} na Story 3.1).
 */
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

    // ============================================================
    // UserDetails — contrato Spring Security
    // ============================================================

    /**
     * Retorna a hierarquia de roles em forma plana, replicando o que o colega
     * {@code cldavi/valoraapi} faz: ADMINISTRATOR carrega também ROLE_COORDINATOR
     * e ROLE_STUDENT; COORDINATOR carrega também ROLE_STUDENT.
     *
     * <p>Permite que checagens futuras com {@code hasRole('STUDENT')} aceitem
     * Coordenadores e Administradores sem precisar de {@code RoleHierarchy}.
     */
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
