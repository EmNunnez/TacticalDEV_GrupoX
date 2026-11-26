package cl.duoc.tacticaldev.dto;

import cl.duoc.tacticaldev.model.Equipo;
import cl.duoc.tacticaldev.model.EstadoEquipo;
import cl.duoc.tacticaldev.model.TipoEquipo;

public record EquipoResponse(Long id, String codigo, TipoEquipo tipo, String marca, String modelo,
                             EstadoEquipo estado, Long puntoVentaId, String puntoVentaNombre) {
    public static EquipoResponse from(Equipo e) {
        if (e == null) return null;
        return new EquipoResponse(e.getId(), e.getCodigo(), e.getTipo(), e.getMarca(), e.getModelo(), e.getEstado(),
                e.getPuntoVenta() == null ? null : e.getPuntoVenta().getId(),
                e.getPuntoVenta() == null ? null : e.getPuntoVenta().getNombre());
    }
}
