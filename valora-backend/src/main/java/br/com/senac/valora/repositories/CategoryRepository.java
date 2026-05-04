package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.Category;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByOrderByNameAsc();
}
