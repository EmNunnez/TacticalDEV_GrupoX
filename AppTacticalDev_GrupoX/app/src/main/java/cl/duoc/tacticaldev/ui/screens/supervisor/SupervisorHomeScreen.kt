@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.supervisor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.ui.components.*
import cl.duoc.tacticaldev.ui.screens.operario.TarjetaVisita
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.ui.theme.*
import cl.duoc.tacticaldev.util.Formatos
import cl.duoc.tacticaldev.viewmodel.GestionViewModel
import cl.duoc.tacticaldev.viewmodel.SupervisorUiState
import cl.duoc.tacticaldev.viewmodel.SupervisorViewModel

@Composable
fun SupervisorHomeScreen(
    sesion: Sesion,
    recargar: Boolean,
    onRecargado: () -> Unit,
    onVerVisita: (Long) -> Unit,
    onFormPunto: (Long?) -> Unit,
    onFormEquipo: (Long?) -> Unit,
    onFormUsuario: (Long?) -> Unit,
    onHistorial: (Long, String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val vm = tacticalViewModel(key = "supervisor") {
        SupervisorViewModel(it.solicitudRepository, it.visitaRepository, it.usuarioRepository, it.catalogoRepository)
    }
    val gestionVm = tacticalViewModel(key = "gestion") {
        GestionViewModel(it.catalogoRepository, it.usuarioRepository, it.solicitudRepository, it.locationProvider)
    }
    val s by vm.estado.collectAsStateWithLifecycle()
    val g by gestionVm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    var aAsignar by remember { mutableStateOf<Solicitud?>(null) }

    LaunchedEffect(recargar) { if (recargar) { vm.cargar(); gestionVm.cargar(); onRecargado() } }
    MensajesSnackbar(snackbar, s.error, s.mensaje, vm::limpiarMensajes)
    MensajesSnackbar(snackbar, g.error, g.mensaje) {
        gestionVm.limpiarMensajes()
        vm.cargar() // por si se programó una revisión
    }

    AppScaffold(
        titulo = "Panel supervisor",
        sesion = sesion,
        onCerrarSesion = onCerrarSesion,
        snackbarHost = snackbar,
        acciones = {
            IconButton(onClick = { vm.cargar(); gestionVm.cargar() }) { Icon(Icons.Filled.Refresh, "Actualizar") }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { BadgedBox(badge = { if (s.pendientes > 0) Badge { Text("${s.pendientes}") } }) { Icon(Icons.Filled.Assignment, null) } },
                    label = { Text("Solicitudes") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            if (s.visitasPorRevisar.isNotEmpty()) Badge(containerColor = if (s.sospechosas > 0) RojoAlerta else Ambar) {
                                Text("${s.visitasPorRevisar.size}")
                            }
                        }) { Icon(Icons.Filled.FactCheck, null) }
                    },
                    label = { Text("Validar") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Gestión") }
                )
            }
        },
        fab = {
            if (tab == 2) FloatingActionButton(onClick = {
                when (subTab) { 0 -> onFormPunto(null); 1 -> onFormEquipo(null); else -> onFormUsuario(null) }
            }) { Icon(Icons.Filled.Add, "Agregar") }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> TabSolicitudes(s, g.equiposConFalla, vm::cambiarFiltro, onAsignar = { aAsignar = it })
                1 -> TabValidar(s.visitasPorRevisar, onVerVisita)
                else -> TabGestion(
                    subTab = subTab, onSubTab = { subTab = it },
                    gestion = g,
                    onFormPunto = onFormPunto, onFormEquipo = onFormEquipo, onFormUsuario = onFormUsuario,
                    onHistorial = onHistorial,
                    onEliminarPunto = gestionVm::eliminarPunto,
                    onEliminarEquipo = gestionVm::eliminarEquipo,
                    onEliminarUsuario = gestionVm::eliminarUsuario,
                    onProgramarRevision = gestionVm::programarRevision,
                    idSupervisor = sesion.id
                )
            }
        }
    }

    aAsignar?.let { sol ->
        AsignarDialog(
            solicitud = sol,
            operarios = vm.operariosCompatibles(sol.tipo),
            equipos = if (sol.tipo == TipoSolicitud.EQUIPO_NUEVO) vm.equiposCompatibles(sol) else emptyList(),
            onAsignar = { op, eq -> vm.asignar(sol, op, eq); aAsignar = null },
            onCancelar = { aAsignar = null }
        )
    }
}

// ======================= TAB SOLICITUDES =======================

@Composable
private fun TabSolicitudes(
    s: SupervisorUiState,
    equiposConFalla: Int,
    onFiltro: (EstadoSolicitud?) -> Unit,
    onAsignar: (Solicitud) -> Unit
) {
    if (s.cargando && s.solicitudes.isEmpty()) { Cargando(); return }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(340.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Indicador(s.pendientes, "Sin asignar", Ambar, Modifier.weight(1f))
                Indicador(s.solicitudes.count { it.estado == EstadoSolicitud.COMPLETADA }, "Por validar", AzulTactical, Modifier.weight(1f))
                Indicador(s.sospechosas, "Sospechosas", RojoAlerta, Modifier.weight(1f))
                Indicador(equiposConFalla, "Equipos con falla", RojoAlerta, Modifier.weight(1f))
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = s.filtro == null, onClick = { onFiltro(null) }, label = { Text("Todas") })
                EstadoSolicitud.entries.forEach { e ->
                    FilterChip(selected = s.filtro == e, onClick = { onFiltro(e) }, label = { Text(e.etiqueta) })
                }
            }
        }
        if (s.solicitudesFiltradas.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Vacio("No hay solicitudes en este estado") }
        }
        items(s.solicitudesFiltradas, key = { it.id }) { sol ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${sol.id} ${sol.tipo.etiqueta}", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        EstadoChip(sol.estado.etiqueta, colorSolicitud(sol.estado))
                    }
                    Spacer(Modifier.height(6.dp))
                    Dato(Icons.Filled.Person, "Cliente", sol.clienteNombre ?: "#${sol.clienteId}")
                    Dato(Icons.Filled.Store, "Local", "${sol.puntoVenta.nombre} (${sol.puntoVenta.comuna})")
                    sol.equipo?.let { Dato(Icons.Filled.Kitchen, "Equipo", "${it.codigo} · ${it.estado.etiqueta}") }
                    sol.tipoEquipoSolicitado?.let { if (sol.equipo == null) Dato(Icons.Filled.Kitchen, "Pide", it.etiqueta) }
                    Dato(Icons.Filled.CalendarToday, "Creada", Formatos.fechaLegible(sol.fechaCreacion))
                    sol.operarioNombre?.let { Dato(Icons.Filled.Engineering, "Operario", it) }
                    if (sol.tieneFotoCliente) Dato(Icons.Filled.Image, "Foto del cliente", "Adjunta")
                    Text(sol.descripcion, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    if (sol.estado == EstadoSolicitud.PENDIENTE || sol.estado == EstadoSolicitud.ASIGNADA) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            FilledTonalButton(onClick = { onAsignar(sol) }) {
                                Text(if (sol.estado == EstadoSolicitud.PENDIENTE) "Asignar" else "Reasignar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AsignarDialog(
    solicitud: Solicitud,
    operarios: List<Usuario>,
    equipos: List<Equipo>,
    onAsignar: (Long?, Long?) -> Unit,
    onCancelar: () -> Unit
) {
    var operario by remember { mutableStateOf(operarios.firstOrNull { it.id == solicitud.operarioId }) }
    var equipo by remember { mutableStateOf(equipos.firstOrNull { it.id == solicitud.equipo?.id }) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Asignar solicitud #${solicitud.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (solicitud.tipo) {
                        TipoSolicitud.REPARACION -> "Las reparaciones las atiende un Técnico."
                        TipoSolicitud.EQUIPO_NUEVO, TipoSolicitud.RETIRO -> "La atiende un Vendedor o Transportista."
                        TipoSolicitud.REVISION -> "Cualquier operario puede hacer la revisión."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Selector(
                    etiqueta = "Operario",
                    opciones = operarios,
                    seleccionado = operario,
                    texto = { "${it.nombre} (${it.tipoOperario?.etiqueta ?: "-"})" },
                    onSeleccion = { operario = it }
                )
                if (solicitud.tipo == TipoSolicitud.EQUIPO_NUEVO) {
                    Selector(
                        etiqueta = "Equipo de bodega a instalar",
                        opciones = equipos,
                        seleccionado = equipo,
                        texto = { "${it.codigo} · ${it.marca} ${it.modelo}" },
                        onSeleccion = { equipo = it }
                    )
                    if (equipos.isEmpty()) Text("No hay ${solicitud.tipoEquipoSolicitado?.etiqueta} disponibles en bodega",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAsignar(operario?.id, equipo?.id) }) { Text("Asignar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

// ======================= TAB VALIDAR =======================

@Composable
private fun TabValidar(visitas: List<Visita>, onVer: (Long) -> Unit) {
    if (visitas.isEmpty()) { Vacio("No hay visitas pendientes de validación", Icons.Filled.FactCheck); return }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(340.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Las visitas en rojo se tomaron fuera del radio del local o con GPS simulado.",
                style = MaterialTheme.typography.bodySmall)
        }
        items(visitas, key = { it.id }) { v -> TarjetaVisita(v, onClick = { onVer(v.id) }) }
    }
}
