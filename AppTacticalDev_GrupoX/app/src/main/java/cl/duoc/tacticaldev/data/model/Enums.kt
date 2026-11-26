package cl.duoc.tacticaldev.data.model

/** Los nombres deben coincidir con los enums de los microservicios (Gson los mapea por nombre). */

enum class Rol(val etiqueta: String) {
    CLIENTE("Cliente"),
    OPERARIO("Operario de terreno"),
    SUPERVISOR("Supervisor")
}

enum class TipoOperario(val etiqueta: String) {
    TECNICO("Técnico"),
    VENDEDOR("Vendedor"),
    TRANSPORTISTA("Transportista")
}

enum class TipoEquipo(val etiqueta: String) {
    VITRINA_FREEZER("Vitrina freezer"),
    CONSERVADORA("Conservadora")
}

enum class EstadoEquipo(val etiqueta: String) {
    OPERATIVO("Operativo"),
    CON_FALLA("Con falla"),
    EN_REPARACION("En reparación"),
    RETIRADO("Retirado")
}

enum class TipoSolicitud(val etiqueta: String) {
    EQUIPO_NUEVO("Equipo nuevo"),
    REPARACION("Reparación"),
    RETIRO("Retiro de equipo"),
    REVISION("Revisión periódica")
}

enum class EstadoSolicitud(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    ASIGNADA("Asignada"),
    COMPLETADA("Por validar"),
    VALIDADA("Cerrada")
}

enum class EstadoValidacion(val etiqueta: String) {
    PENDIENTE("Por validar"),
    SOSPECHOSA("Sospechosa"),
    VALIDADA("Validada"),
    RECHAZADA("Rechazada")
}
