@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.comun

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.tacticaldev.ui.components.*
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.ui.theme.RojoAlerta
import cl.duoc.tacticaldev.ui.theme.VerdeOk
import cl.duoc.tacticaldev.util.Formatos
import cl.duoc.tacticaldev.util.GeoUtils
import cl.duoc.tacticaldev.viewmodel.DetalleVisitaViewModel

/** Evidencia completa de una visita. El supervisor puede aprobarla o rechazarla. */
@Composable
fun DetalleVisitaScreen(visitaId: Long, puedeValidar: Boolean, onTerminado: () -> Unit, onVolver: () -> Unit) {
    val vm = tacticalViewModel(key = "detalle-$visitaId") { DetalleVisitaViewModel(it.visitaRepository, visitaId) }
    val s by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MensajesSnackbar(snackbar, s.error, null) {}
    LaunchedEffect(s.terminado) { if (s.terminado) onTerminado() }

    Scaffold(topBar = { BarraVolver("Visita #$visitaId", onVolver) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        val v = s.visita
        if (s.cargando || v == null) {
            Box(Modifier.padding(padding)) { if (s.cargando) Cargando() else Vacio(s.error ?: "Visita no encontrada") }
            return@Scaffold
        }
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ImagenBase64(v.fotoBase64, Modifier.fillMaxWidth().height(260.dp))

                val color = if (v.ubicacionVerificada) VerdeOk else RojoAlerta
                Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (v.ubicacionVerificada) Icons.Filled.VerifiedUser else Icons.Filled.GppBad, null, tint = color)
                            Spacer(Modifier.width(8.dp))
                            Text(if (v.ubicacionVerificada) "Presencia verificada por GPS" else "Visita sospechosa",
                                color = color, fontWeight = FontWeight.Bold)
                        }
                        Text(v.motivoVerificacion.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${v.tipoSolicitud.etiqueta} · ${v.puntoVentaNombre}", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    EstadoChip(v.estadoValidacion.etiqueta, colorValidacion(v.estadoValidacion))
                }
                v.equipoCodigo?.let { Dato(Icons.Filled.Kitchen, "Equipo", it) }
                Dato(Icons.Filled.Build, "Estado del equipo", v.estadoEquipo.etiqueta)
                Dato(Icons.Filled.Engineering, "Operario", v.operarioNombre ?: "#${v.operarioId}")
                Dato(Icons.Filled.PhotoCamera, "Foto tomada", Formatos.fechaLegible(v.fechaCaptura))
                Dato(Icons.Filled.CloudUpload, "Recibida", Formatos.fechaLegible(v.fechaRegistro))
                Dato(Icons.Filled.MyLocation, "GPS", "%.5f, %.5f".format(v.latitud, v.longitud) +
                        (v.precisionMetros?.let { " (±${it.toInt()} m)" } ?: ""))
                Dato(Icons.Filled.Straighten, "Distancia al local", GeoUtils.formatearDistancia(v.distanciaMetros))
                if (v.ubicacionSimulada) Dato(Icons.Filled.Warning, "Alerta", "GPS simulado detectado")
                v.temperaturaAmbiente?.let { Dato(Icons.Filled.Thermostat, "Temperatura ambiente", "$it °C") }
                v.observaciones?.let {
                    Text("Observaciones", style = MaterialTheme.typography.titleSmall)
                    Text(it)
                }
                v.comentarioSupervisor?.let {
                    Text("Comentario del supervisor", style = MaterialTheme.typography.titleSmall)
                    Text(it)
                }

                AnimatedVisibility(visible = puedeValidar && s.puedeValidar) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider()
                        Text("Validación del supervisor", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = s.comentario,
                            onValueChange = vm::onComentario,
                            label = { Text("Comentario") },
                            isError = s.comentarioError != null,
                            supportingText = s.comentarioError?.let { { Text(it) } },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.validar(false) }, enabled = !s.procesando, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Close, null); Spacer(Modifier.width(4.dp)); Text("Rechazar")
                            }
                            Button(onClick = { vm.validar(true) }, enabled = !s.procesando, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Check, null); Spacer(Modifier.width(4.dp)); Text("Aprobar")
                            }
                        }
                    }
                }
            }
        }
    }
}
