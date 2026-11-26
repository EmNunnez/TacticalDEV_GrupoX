package cl.duoc.tacticaldev.model;

/**
 * PENDIENTE  -> creada, sin operario.
 * ASIGNADA   -> tiene operario, esperando visita verificada.
 * COMPLETADA -> visita con foto+GPS verificada, esperando validación del supervisor.
 * VALIDADA   -> cerrada por el supervisor.
 */
public enum EstadoSolicitud { PENDIENTE, ASIGNADA, COMPLETADA, VALIDADA }
