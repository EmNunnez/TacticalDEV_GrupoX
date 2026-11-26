package cl.duoc.tacticaldev.repository;

import cl.duoc.tacticaldev.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    List<Equipo> findByPuntoVentaIdOrderByCodigoAsc(Long puntoVentaId);
    List<Equipo> findByPuntoVentaIsNullOrderByCodigoAsc();
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByPuntoVentaId(Long puntoVentaId);
}
