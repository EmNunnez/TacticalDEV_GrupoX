package cl.duoc.tacticaldev.repository;

import cl.duoc.tacticaldev.model.EstadoValidacion;
import cl.duoc.tacticaldev.model.Visita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitaRepository extends JpaRepository<Visita, Long> {
    Optional<Visita> findByIdLocal(String idLocal);
    List<Visita> findAllByOrderByFechaRegistroDesc();
    List<Visita> findByOperarioIdOrderByFechaRegistroDesc(Long operarioId);
    List<Visita> findByEstadoValidacionOrderByFechaRegistroDesc(EstadoValidacion estado);
    List<Visita> findBySolicitudIdOrderByFechaRegistroDesc(Long solicitudId);
    List<Visita> findBySolicitudEquipoIdOrderByFechaRegistroDesc(Long equipoId);
}
