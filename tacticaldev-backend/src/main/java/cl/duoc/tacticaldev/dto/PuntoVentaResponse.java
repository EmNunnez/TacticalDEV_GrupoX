package cl.duoc.tacticaldev.dto;

import cl.duoc.tacticaldev.model.PuntoVenta;

public record PuntoVentaResponse(Long id, String nombre, String direccion, String comuna,
                                 Double latitud, Double longitud, Integer radioMetros, Long clienteId) {
    public static PuntoVentaResponse from(PuntoVenta p) {
        if (p == null) return null;
        return new PuntoVentaResponse(p.getId(), p.getNombre(), p.getDireccion(), p.getComuna(),
                p.getLatitud(), p.getLongitud(), p.getRadioMetros(), p.getClienteId());
    }
}
