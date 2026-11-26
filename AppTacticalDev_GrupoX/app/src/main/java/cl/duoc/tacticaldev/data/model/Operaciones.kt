package cl.duoc.tacticaldev.data.model

data class PuntoVenta(
    val id: Long,
    val nombre: String,
    val direccion: String,
    val comuna: String,
    val latitud: Double,
    val longitud: Double,
    val radioMetros: Int,
    val clienteId: Long
)

data class PuntoVentaRequest(
    val nombre: String,
    val direccion: String,
    val comuna: String,
    val latitud: Double,
    val longitud: Double,
    val radioMetros: Int,
    val clienteId: Long
)

data class Equipo(
    val id: Long,
    val codigo: String,
    val tipo: TipoEquipo,
    val marca: String,
    val modelo: String,
    val estado: EstadoEquipo,
    val puntoVentaId: Long?,
    val puntoVentaNombre: String?
)

data class EquipoRequest(
    val codigo: String,
    val tipo: TipoEquipo,
    val marca: String,
    val modelo: String,
    val estado: EstadoEquipo,
    val puntoVentaId: Long?
)

data class Solicitud(
    val id: Long,
    val tipo: TipoSolicitud,
    val estado: EstadoSolicitud,
    val descripcion: String,
    val tipoEquipoSolicitado: TipoEquipo?,
    val tieneFotoCliente: Boolean,
    val fotoClienteBase64: String?,
    val clienteId: Long,
    val clienteNombre: String?,
    val puntoVenta: PuntoVenta,
    val equipo: Equipo?,
    val operarioId: Long?,
    val operarioNombre: String?,
    val fechaCreacion: String?,
    val fechaAsignacion: String?,
    val fechaCierre: String?
)

data class SolicitudRequest(
    val tipo: TipoSolicitud,
    val descripcion: String,
    val clienteId: Long,
    val puntoVentaId: Long,
    val equipoId: Long?,
    val tipoEquipoSolicitado: TipoEquipo?,
    val fotoClienteBase64: String?
)

data class SolicitudUpdateRequest(val descripcion: String)

data class AsignarRequest(val operarioId: Long, val equipoId: Long?)

data class Visita(
    val id: Long,
    val idLocal: String,
    val solicitudId: Long,
    val tipoSolicitud: TipoSolicitud,
    val puntoVentaNombre: String,
    val equipoCodigo: String?,
    val operarioId: Long,
    val operarioNombre: String?,
    val estadoEquipo: EstadoEquipo,
    val observaciones: String?,
    val fotoBase64: String?,
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Double?,
    val ubicacionSimulada: Boolean,
    val fechaCaptura: String?,
    val fechaRegistro: String?,
    val distanciaMetros: Double,
    val ubicacionVerificada: Boolean,
    val motivoVerificacion: String?,
    val temperaturaAmbiente: Double?,
    val estadoValidacion: EstadoValidacion,
    val comentarioSupervisor: String?,
    val fechaValidacion: String?
)

data class VisitaRequest(
    val idLocal: String,
    val solicitudId: Long,
    val operarioId: Long,
    val estadoEquipo: EstadoEquipo,
    val observaciones: String?,
    val fotoBase64: String,
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Double?,
    val ubicacionSimulada: Boolean,
    /** Formato ISO-8601 sin zona: 2026-11-18T19:57:03 */
    val fechaCaptura: String,
    val temperaturaAmbiente: Double?
)

data class ValidarRequest(val aprobada: Boolean, val comentario: String?)
