package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.User;
import br.com.senac.valora.entities.UserProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    List<User> findByProfileOrderByNameAsc(UserProfile profile);
}
