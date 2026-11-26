@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.operario

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.tacticaldev.data.model.Sesion
import cl.duoc.tacticaldev.data.model.Solicitud
import cl.duoc.tacticaldev.data.model.Visita
import cl.duoc.tacticaldev.ui.components.*
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.ui.theme.Ambar
import cl.duoc.tacticaldev.ui.theme.RojoAlerta
import cl.duoc.tacticaldev.ui.theme.VerdeOk
import cl.duoc.tacticaldev.util.Formatos
import cl.duoc.tacticaldev.util.GeoUtils
import cl.duoc.tacticaldev.viewmodel.OperarioViewModel

@Composable
fun OperarioHomeScreen(
    sesion: Sesion,
    recargar: Boolean,
    onRecargado: () -> Unit,
    onRegistrarVisita: (Long) -> Unit,
    onVerVisita: (Long) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val vm = tacticalViewModel(key = "operario-${sesion.id}") {
        OperarioViewModel(it.solicitudRepository, it.visitaRepository, sesion.id)
    }
    val s by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(recargar) { if (recargar) { vm.cargar(); onRecargado() } }
    MensajesSnackbar(snackbar, s.error, s.mensaje, vm::limpiarMensajes)

    AppScaffold(
        titulo = sesion.tipoOperario?.etiqueta ?: "Operario",
        sesion = sesion,
        onCerrarSesion = onCerrarSesion,
        snackbarHost = snackbar,
        acciones = { IconButton(onClick = vm::cargar) { Icon(Icons.Filled.Refresh, "Actualizar") } },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = {
                        BadgedBox(badge = { if (s.asignadas.isNotEmpty()) Badge { Text("${s.asignadas.size}") } }) {
                            Icon(Icons.Filled.Map, null)
                        }
                    },
                    label = { Text("Mis visitas de hoy") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.History, null) }, label = { Text("Historial") }
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Aviso animado de visitas guardadas sin conexión
            AnimatedVisibility(
                visible = s.pendientesSync > 0,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(color = Ambar.copy(alpha = 0.18f)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudOff, null, tint = Ambar)
                        Spacer(Modifier.width(8.dp))
                        Text("${s.pendientesSync} visita(s) guardada(s) sin conexión", Modifier.weight(1f))
                        Button(onClick = vm::sincronizar, enabled = !s.sincronizando) {
                            if (s.sincronizando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Enviar")
                        }
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                when {
                    s.cargando && s.asignadas.isEmpty() && s.visitas.isEmpty() -> Cargando()
                    tab == 0 -> ListaAsignadas(s.asignadas, s.solicitudesConPendiente, onRegistrarVisita)
                    else -> ListaVisitas(s.visitas, onVerVisita)
                }
            }
        }
    }
}

@Composable
private fun ListaAsignadas(lista: List<Solicitud>, conPendiente: Set<Long>, onRegistrar: (Long) -> Unit) {
    if (lista.isEmpty()) {
        Vacio("No tienes visitas asignadas", Icons.Filled.EventAvailable)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(340.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(lista, key = { it.id }) { sol ->
            val pendiente = sol.id in conPendiente
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${sol.id} ${sol.tipo.etiqueta}", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (pendiente) EstadoChip("Por enviar", Ambar)
                    }
                    Spacer(Modifier.height(6.dp))
                    Dato(Icons.Filled.Store, "Local", sol.puntoVenta.nombre)
                    Dato(Icons.Filled.Place, "Dirección", "${sol.puntoVenta.direccion}, ${sol.puntoVenta.comuna}")
                    sol.equipo?.let { Dato(Icons.Filled.Kitchen, "Equipo", "${it.codigo} · ${it.tipo.etiqueta}") }
                    Dato(Icons.Filled.MyLocation, "Radio permitido", "${sol.puntoVenta.radioMetros} m")
                    Spacer(Modifier.height(4.dp))
                    Text(sol.descripcion, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onRegistrar(sol.id) }, enabled = !pendiente, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.AddAPhoto, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (pendiente) "Esperando conexión" else "Registrar visita")
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaVisitas(lista: List<Visita>, onVer: (Long) -> Unit) {
    if (lista.isEmpty()) {
        Vacio("Aún no registras visitas", Icons.Filled.History)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(340.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(lista, key = { it.id }) { v -> TarjetaVisita(v, onClick = { onVer(v.id) }) }
    }
}

/** Tarjeta reutilizada por operario, supervisor e historial del equipo. */
@Composable
fun TarjetaVisita(v: Visita, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (v.ubicacionVerificada) Icons.Filled.VerifiedUser else Icons.Filled.GppBad,
                    null, tint = if (v.ubicacionVerificada) VerdeOk else RojoAlerta
                )
                Spacer(Modifier.width(6.dp))
                Text("${v.tipoSolicitud.etiqueta} · ${v.puntoVentaNombre}", fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                EstadoChip(v.estadoValidacion.etiqueta, colorValidacion(v.estadoValidacion))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${Formatos.fechaLegible(v.fechaCaptura)} · a ${GeoUtils.formatearDistancia(v.distanciaMetros)}" +
                        (v.equipoCodigo?.let { " · $it" } ?: "") + (v.operarioNombre?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall
            )
            v.motivoVerificacion?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
        }
    }
}
