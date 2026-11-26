package cl.duoc.tacticaldev.repository;

import cl.duoc.tacticaldev.model.EstadoSolicitud;
import cl.duoc.tacticaldev.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findAllByOrderByFechaCreacionDesc();
    List<Solicitud> findByClienteIdOrderByFechaCreacionDesc(Long clienteId);
    List<Solicitud> findByOperarioIdOrderByFechaCreacionDesc(Long operarioId);
    List<Solicitud> findByEstadoOrderByFechaCreacionDesc(EstadoSolicitud estado);
    boolean existsByEquipoIdAndEstadoIn(Long equipoId, Collection<EstadoSolicitud> estados);
    boolean existsByEquipoId(Long equipoId);
    boolean existsByPuntoVentaId(Long puntoVentaId);
}
