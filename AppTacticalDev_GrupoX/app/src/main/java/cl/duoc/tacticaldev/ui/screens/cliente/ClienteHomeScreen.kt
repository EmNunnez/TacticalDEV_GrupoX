@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.tacticaldev.data.model.Equipo
import cl.duoc.tacticaldev.data.model.EstadoSolicitud
import cl.duoc.tacticaldev.data.model.Sesion
import cl.duoc.tacticaldev.data.model.Solicitud
import cl.duoc.tacticaldev.ui.components.*
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.util.Formatos
import cl.duoc.tacticaldev.viewmodel.ClienteViewModel

@Composable
fun ClienteHomeScreen(
    sesion: Sesion,
    recargar: Boolean,
    onRecargado: () -> Unit,
    onNuevaSolicitud: () -> Unit,
    onHistorial: (Long, String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val vm = tacticalViewModel(key = "cliente-${sesion.id}") {
        ClienteViewModel(it.solicitudRepository, it.catalogoRepository, sesion.id)
    }
    val s by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var aCancelar by remember { mutableStateOf<Solicitud?>(null) }
    var aEditar by remember { mutableStateOf<Solicitud?>(null) }

    LaunchedEffect(recargar) { if (recargar) { vm.cargar(); onRecargado() } }
    MensajesSnackbar(snackbar, s.error, s.mensaje, vm::limpiarMensajes)

    AppScaffold(
        titulo = "Mi minimarket",
        sesion = sesion,
        onCerrarSesion = onCerrarSesion,
        snackbarHost = snackbar,
        acciones = { IconButton(onClick = vm::cargar) { Icon(Icons.Filled.Refresh, "Actualizar") } },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = {
                        BadgedBox(badge = { if (s.abiertas > 0) Badge { Text("${s.abiertas}") } }) {
                            Icon(Icons.Filled.Assignment, null)
                        }
                    },
                    label = { Text("Solicitudes") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Kitchen, null) }, label = { Text("Mis equipos") }
                )
            }
        },
        fab = {
            if (tab == 0) ExtendedFloatingActionButton(
                onClick = onNuevaSolicitud,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Nueva solicitud") }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                s.cargando && s.solicitudes.isEmpty() -> Cargando()
                tab == 0 -> ListaSolicitudes(s.solicitudes, onCancelar = { aCancelar = it }, onEditar = { aEditar = it })
                else -> ListaEquipos(s.equipos, onHistorial)
            }
        }
    }

    aCancelar?.let { sol ->
        ConfirmarDialog(
            titulo = "Cancelar solicitud #${sol.id}",
            texto = "La solicitud de ${sol.tipo.etiqueta.lowercase()} será eliminada.",
            onConfirmar = { vm.cancelar(sol); aCancelar = null },
            onCancelar = { aCancelar = null }
        )
    }
    aEditar?.let { sol ->
        EditarDescripcionDialog(
            inicial = sol.descripcion,
            onGuardar = { texto -> vm.editarDescripcion(sol, texto).also { if (it == null) aEditar = null } },
            onCancelar = { aEditar = null }
        )
    }
}

@Composable
private fun ListaSolicitudes(lista: List<Solicitud>, onCancelar: (Solicitud) -> Unit, onEditar: (Solicitud) -> Unit) {
    if (lista.isEmpty()) {
        Vacio("Aún no tienes solicitudes.\nPide un equipo nuevo o una reparación con el botón +")
        return
    }
    // GridCells.Adaptive: 1 columna en teléfono, 2 o más en tablet/horizontal
    LazyVerticalGrid(
        columns = GridCells.Adaptive(340.dp),
        contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(lista, key = { it.id }) { sol ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("#${sol.id} ${sol.tipo.etiqueta}", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        EstadoChip(sol.estado.etiqueta, colorSolicitud(sol.estado))
                    }
                    Spacer(Modifier.height(8.dp))
                    Dato(Icons.Filled.Store, "Local", sol.puntoVenta.nombre)
                    sol.equipo?.let { Dato(Icons.Filled.Kitchen, "Equipo", "${it.codigo} (${it.tipo.etiqueta})") }
                    sol.tipoEquipoSolicitado?.let { if (sol.equipo == null) Dato(Icons.Filled.Kitchen, "Pide", it.etiqueta) }
                    Dato(Icons.Filled.CalendarToday, "Creada", Formatos.fechaLegible(sol.fechaCreacion))
                    sol.operarioNombre?.let { Dato(Icons.Filled.Engineering, "Asignada a", it) }
                    Spacer(Modifier.height(6.dp))
                    Text(sol.descripcion, style = MaterialTheme.typography.bodyMedium)
                    if (sol.estado == EstadoSolicitud.PENDIENTE) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onEditar(sol) }) { Text("Editar") }
                            TextButton(onClick = { onCancelar(sol) }) { Text("Cancelar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaEquipos(equipos: List<Equipo>, onHistorial: (Long, String) -> Unit) {
    if (equipos.isEmpty()) {
        Vacio("No tienes equipos instalados")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(equipos, key = { it.id }) { e ->
            OutlinedCard(onClick = { onHistorial(e.id, e.codigo) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(e.codigo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        EstadoChip(e.estado.etiqueta, colorEquipo(e.estado))
                    }
                    Text("${e.tipo.etiqueta} · ${e.marca} ${e.modelo}")
                    Text(e.puntoVentaNombre.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    Text("Toque para ver el historial", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun EditarDescripcionDialog(inicial: String, onGuardar: (String) -> String?, onCancelar: () -> Unit) {
    var texto by remember { mutableStateOf(inicial) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Editar descripción") },
        text = {
            OutlinedTextField(
                value = texto, onValueChange = { texto = it; error = null },
                isError = error != null, supportingText = error?.let { { Text(it) } },
                minLines = 3, modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { error = onGuardar(texto) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
