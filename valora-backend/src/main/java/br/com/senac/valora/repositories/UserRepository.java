package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.User;
import br.com.senac.valora.entities.UserProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository da entidade {@link User}.
 *
 * <p>Usado no fluxo de autenticação (Story 1.3) para buscar o usuário por
 * email e validar a senha contra o {@code passwordHash} BCrypt.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Busca usuário por email — chave de login. */
    Optional<User> findByEmail(String email);

    /** Lista usuários por profile (Tier C γ — listar coordenadores). */
    List<User> findByProfileOrderByNameAsc(UserProfile profile);
}
