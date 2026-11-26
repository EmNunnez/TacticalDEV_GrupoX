package cl.duoc.tacticaldev.model;

/**
 * EQUIPO_NUEVO  -> la crea el cliente; la atiende VENDEDOR o TRANSPORTISTA.
 * REPARACION    -> la crea el cliente; la atiende TECNICO.
 * RETIRO        -> la crea el cliente; la atiende VENDEDOR o TRANSPORTISTA.
 * REVISION      -> la programa el supervisor (visita periódica); cualquier operario.
 */
public enum TipoSolicitud { EQUIPO_NUEVO, REPARACION, RETIRO, REVISION }
