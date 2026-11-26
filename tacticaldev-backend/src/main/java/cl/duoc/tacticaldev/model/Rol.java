package cl.duoc.tacticaldev.model;

/** Los 3 roles del sistema. */
public enum Rol {
    CLIENTE,     // Dueño/encargado del punto de venta: crea solicitudes
    OPERARIO,    // Técnico, vendedor o transportista: realiza visitas en terreno
    SUPERVISOR   // Asigna solicitudes, valida visitas y administra datos maestros
}
