package cl.duoc.tacticaldev.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cl.duoc.tacticaldev.data.model.EstadoEquipo
import cl.duoc.tacticaldev.data.model.VisitaRequest

/**
 * Visita guardada en el teléfono mientras no hay internet.
 * La foto y el GPS ya quedaron ligados en el momento de la captura.
 */
@Entity(tableName = "visitas_pendientes")
data class VisitaPendienteEntity(
    @PrimaryKey val idLocal: String,
    val solicitudId: Long,
    val operarioId: Long,
    val estadoEquipo: String,
    val observaciones: String?,
    val fotoBase64: String,
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Double?,
    val ubicacionSimulada: Boolean,
    val fechaCaptura: String,
    val temperaturaAmbiente: Double?,
    val puntoVentaNombre: String,
    val creadoEn: Long,
    val ultimoError: String? = null
) {
    fun aRequest() = VisitaRequest(
        idLocal = idLocal,
        solicitudId = solicitudId,
        operarioId = operarioId,
        estadoEquipo = EstadoEquipo.valueOf(estadoEquipo),
        observaciones = observaciones,
        fotoBase64 = fotoBase64,
        latitud = latitud,
        longitud = longitud,
        precisionMetros = precisionMetros,
        ubicacionSimulada = ubicacionSimulada,
        fechaCaptura = fechaCaptura,
        temperaturaAmbiente = temperaturaAmbiente
    )

    companion object {
        fun desde(req: VisitaRequest, puntoVentaNombre: String, ahora: Long) = VisitaPendienteEntity(
            idLocal = req.idLocal,
            solicitudId = req.solicitudId,
            operarioId = req.operarioId,
            estadoEquipo = req.estadoEquipo.name,
            observaciones = req.observaciones,
            fotoBase64 = req.fotoBase64,
            latitud = req.latitud,
            longitud = req.longitud,
            precisionMetros = req.precisionMetros,
            ubicacionSimulada = req.ubicacionSimulada,
            fechaCaptura = req.fechaCaptura,
            temperaturaAmbiente = req.temperaturaAmbiente,
            puntoVentaNombre = puntoVentaNombre,
            creadoEn = ahora
        )
    }
}
