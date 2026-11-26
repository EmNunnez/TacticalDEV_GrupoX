package cl.duoc.tacticaldev.repository;

import cl.duoc.tacticaldev.model.PuntoVenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PuntoVentaRepository extends JpaRepository<PuntoVenta, Long> {
    List<PuntoVenta> findByClienteIdOrderByNombreAsc(Long clienteId);
}
