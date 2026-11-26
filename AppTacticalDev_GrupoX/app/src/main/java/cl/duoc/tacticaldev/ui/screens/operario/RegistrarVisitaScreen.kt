@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.operario

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.tacticaldev.data.model.EstadoEquipo
import cl.duoc.tacticaldev.data.model.TipoSolicitud
import cl.duoc.tacticaldev.repository.ResultadoRegistro
import cl.duoc.tacticaldev.ui.components.*
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.ui.theme.RojoAlerta
import cl.duoc.tacticaldev.ui.theme.VerdeOk
import cl.duoc.tacticaldev.util.Formatos
import cl.duoc.tacticaldev.util.ImageUtils
import cl.duoc.tacticaldev.viewmodel.CapturaEvidencia
import cl.duoc.tacticaldev.viewmodel.VisitaViewModel
import coil.compose.AsyncImage

/**
 * Registro de visita/entrega. Flujo anti "visita fantasma":
 * 1) El operario toma la foto DESDE LA APP (no se permite galería).
 * 2) Al volver la foto se lee el GPS en ese instante y se compara con el punto de venta.
 * 3) Solo si está dentro del radio se habilita "Enviar". El servidor vuelve a verificar.
 */
@Composable
fun RegistrarVisitaScreen(operarioId: Long, solicitudId: Long, onTerminado: () -> Unit, onVolver: () -> Unit) {
    val vm = tacticalViewModel(key = "visita-$solicitudId") {
        VisitaViewModel(it.solicitudRepository, it.visitaRepository, it.climaRepository,
            it.locationProvider, it.imageEncoder, operarioId, solicitudId)
    }
    val s by vm.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    MensajesSnackbar(snackbar, s.error, null) {}

    var uriFoto by rememberSaveable { mutableStateOf<String?>(null) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = uriFoto
        if (ok && u != null) vm.onFotoTomada(u)
    }
    fun abrirCamara() {
        val u = ImageUtils.crearUriTemporal(context)
        uriFoto = u.toString()
        camara.launch(u)
    }
    val permisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r[Manifest.permission.CAMERA] == true && r[Manifest.permission.ACCESS_FINE_LOCATION] == true) abrirCamara()
    }
    val tienePermisos = {
        listOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION).all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    Scaffold(topBar = { BarraVolver("Registrar visita", onVolver) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        val sol = s.solicitud
        if (s.cargando || sol == null) {
            Box(Modifier.padding(padding)) { if (s.cargando) Cargando() else Vacio(s.error ?: "Solicitud no disponible") }
            return@Scaffold
        }
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ---- Resumen de la solicitud ----
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("#${sol.id} ${sol.tipo.etiqueta}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Dato(Icons.Filled.Store, "Local", sol.puntoVenta.nombre)
                        Dato(Icons.Filled.Place, "Dirección", "${sol.puntoVenta.direccion}, ${sol.puntoVenta.comuna}")
                        sol.equipo?.let { Dato(Icons.Filled.Kitchen, "Equipo", "${it.codigo} · ${it.marca} ${it.modelo}") }
                        Text(sol.descripcion, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // ---- Paso 1: Foto + GPS ----
                Text("1. Evidencia (foto + GPS)", style = MaterialTheme.typography.titleSmall)
                val captura = s.captura
                if (captura != null) {
                    AsyncImage(
                        model = captura.fotoUri,
                        contentDescription = "Foto de evidencia",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
                OutlinedButton(
                    onClick = { if (tienePermisos()) abrirCamara() else permisos.launch(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    ) },
                    enabled = !s.capturandoGps && !s.enviando,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.PhotoCamera, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (captura == null) "Tomar foto de evidencia" else "Repetir foto (se vuelve a leer el GPS)")
                }

                AnimatedVisibility(visible = s.capturandoGps) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Leyendo GPS en este instante...")
                    }
                }
                AnimatedVisibility(
                    visible = captura != null,
                    enter = fadeIn(tween(400)) + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    captura?.let { ResultadoGps(it) }
                }
                s.evidenciaError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                // ---- API externa ----
                ClimaCard(s.clima, s.climaNoDisponible)

                // ---- Paso 2: Estado del equipo ----
                Text("2. ¿En qué estado queda el equipo?", style = MaterialTheme.typography.titleSmall)
                val estados = if (sol.tipo == TipoSolicitud.RETIRO) EstadoEquipo.entries
                else EstadoEquipo.entries.filter { it != EstadoEquipo.RETIRADO }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    estados.forEach { e ->
                        FilterChip(
                            selected = s.estadoEquipo == e,
                            onClick = { vm.onEstadoEquipo(e) },
                            label = { Text(e.etiqueta, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (s.estadoEquipo == e) { { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) } } else null
                        )
                    }
                }
                s.estadoEquipoError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                // ---- Paso 3: Observaciones ----
                OutlinedTextField(
                    value = s.observaciones,
                    onValueChange = vm::onObservaciones,
                    label = { Text(if (s.estadoEquipo == EstadoEquipo.CON_FALLA) "Describa la falla *" else "Observaciones (opcional)") },
                    isError = s.observacionesError != null,
                    supportingText = { Text(s.observacionesError ?: "${s.observaciones.length}/500") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = vm::enviar,
                    enabled = s.puedeEnviar,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (s.enviando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Filled.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar visita verificada")
                    }
                }
                if (captura != null && !captura.dentroDelRadio) {
                    Text("No se puede enviar: la foto debe tomarse dentro del punto de venta.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    s.resultado?.let { r -> ResultadoDialog(r, onTerminado) }
}

@Composable
private fun ResultadoGps(c: CapturaEvidencia) {
    val color = if (c.dentroDelRadio) VerdeOk else RojoAlerta
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (c.dentroDelRadio) Icons.Filled.LocationOn else Icons.Filled.WrongLocation, null, tint = color)
                Spacer(Modifier.width(8.dp))
                Text(c.motivo, color = color, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Foto: ${Formatos.fechaLegible(c.fechaCaptura)}  ·  GPS: %.5f, %.5f".format(c.latitud, c.longitud) +
                        (c.precisionMetros?.let { "  ·  ±${it.toInt()} m" } ?: ""),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ResultadoDialog(r: ResultadoRegistro, onOk: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                if (r is ResultadoRegistro.Enviada) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                null, Modifier.size(40.dp)
            )
        },
        title = { Text(if (r is ResultadoRegistro.Enviada) "Visita registrada" else "Guardada sin conexión") },
        text = {
            when (r) {
                is ResultadoRegistro.Enviada -> Text(
                    "El servidor verificó la ubicación: ${r.visita.motivoVerificacion ?: "-"}.\n" +
                            "Estado: ${r.visita.estadoValidacion.etiqueta}. El supervisor la revisará."
                )
                ResultadoRegistro.GuardadaSinConexion -> Text(
                    "La foto y el GPS quedaron guardados en el teléfono. Se enviarán automáticamente cuando vuelva la señal."
                )
            }
        },
        confirmButton = { TextButton(onClick = onOk) { Text("Aceptar") } }
    )
}
