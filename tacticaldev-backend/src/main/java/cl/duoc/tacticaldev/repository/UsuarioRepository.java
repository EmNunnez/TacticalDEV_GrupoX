package cl.duoc.tacticaldev.repository;

import cl.duoc.tacticaldev.model.Rol;
import cl.duoc.tacticaldev.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Usuario> findByRolOrderByNombreAsc(Rol rol);
}
