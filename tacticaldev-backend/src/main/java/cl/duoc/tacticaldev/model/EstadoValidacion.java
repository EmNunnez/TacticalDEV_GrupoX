package cl.duoc.tacticaldev.model;

/**
 * PENDIENTE  -> GPS verificado, falta revisión del supervisor.
 * SOSPECHOSA -> la foto se tomó fuera del radio del punto de venta o con GPS simulado.
 * VALIDADA / RECHAZADA -> decisión del supervisor.
 */
public enum EstadoValidacion { PENDIENTE, SOSPECHOSA, VALIDADA, RECHAZADA }
