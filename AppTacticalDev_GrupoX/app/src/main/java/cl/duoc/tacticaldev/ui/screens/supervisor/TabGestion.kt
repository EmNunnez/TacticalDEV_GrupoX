@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duoc.tacticaldev.data.model.Equipo
import cl.duoc.tacticaldev.ui.components.*
import cl.duoc.tacticaldev.ui.theme.AzulTactical
import cl.duoc.tacticaldev.viewmodel.GestionUiState

/** CRUD de datos maestros: puntos de venta, equipos y usuarios. */
@Composable
fun TabGestion(
    subTab: Int,
    onSubTab: (Int) -> Unit,
    gestion: GestionUiState,
    onFormPunto: (Long?) -> Unit,
    onFormEquipo: (Long?) -> Unit,
    onFormUsuario: (Long?) -> Unit,
    onHistorial: (Long, String) -> Unit,
    onEliminarPunto: (Long) -> Unit,
    onEliminarEquipo: (Long) -> Unit,
    onEliminarUsuario: (Long) -> Unit,
    onProgramarRevision: (Equipo) -> Unit,
    idSupervisor: Long
) {
    var confirmar by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val titulos = listOf("Puntos de venta", "Equipos", "Usuarios")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTab) {
            titulos.forEachIndexed { i, t -> Tab(selected = subTab == i, onClick = { onSubTab(i) }, text = { Text(t) }) }
        }
        if (gestion.cargando && gestion.puntos.isEmpty()) { Cargando(); return@Column }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp),
            contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (subTab) {
                0 -> items(gestion.puntos, key = { "p${it.id}" }) { p ->
                    val cliente = gestion.clientes.firstOrNull { it.id == p.clienteId }?.nombre ?: "Cliente #${p.clienteId}"
                    FilaGestion(
                        titulo = p.nombre,
                        lineas = listOf("${p.direccion}, ${p.comuna}", "Cliente: $cliente",
                            "GPS: ${p.latitud}, ${p.longitud} · radio ${p.radioMetros} m"),
                        onEditar = { onFormPunto(p.id) },
                        onEliminar = { confirmar = "¿Eliminar el punto ${p.nombre}?" to { onEliminarPunto(p.id) } }
                    )
                }
                1 -> items(gestion.equipos, key = { "e${it.id}" }) { e ->
                    FilaGestion(
                        titulo = e.codigo,
                        chip = { EstadoChip(e.estado.etiqueta, colorEquipo(e.estado)) },
                        lineas = listOf("${e.tipo.etiqueta} · ${e.marca} ${e.modelo}", e.puntoVentaNombre ?: "En bodega"),
                        onEditar = { onFormEquipo(e.id) },
                        onEliminar = { confirmar = "¿Eliminar el equipo ${e.codigo}?" to { onEliminarEquipo(e.id) } },
                        extra = {
                            TextButton(onClick = { onHistorial(e.id, e.codigo) }) { Text("Historial") }
                            if (e.puntoVentaId != null) TextButton(onClick = { onProgramarRevision(e) }) { Text("Programar revisión") }
                        }
                    )
                }
                else -> items(gestion.usuarios, key = { "u${it.id}" }) { u ->
                    FilaGestion(
                        titulo = u.nombre,
                        chip = {
                            EstadoChip(u.tipoOperario?.etiqueta ?: u.rol.etiqueta,
                                if (u.activo) AzulTactical else MaterialTheme.colorScheme.outline)
                        },
                        lineas = listOfNotNull(u.email, u.telefono, if (!u.activo) "DESACTIVADO" else null),
                        onEditar = { onFormUsuario(u.id) },
                        onEliminar = if (u.id == idSupervisor) null
                        else ({ confirmar = "¿Eliminar a ${u.nombre}?" to { onEliminarUsuario(u.id) } })
                    )
                }
            }
        }
    }

    confirmar?.let { (texto, accion) ->
        ConfirmarDialog("Confirmar eliminación", texto, onConfirmar = { accion(); confirmar = null }, onCancelar = { confirmar = null })
    }
}

@Composable
private fun FilaGestion(
    titulo: String,
    lineas: List<String>,
    onEditar: () -> Unit,
    onEliminar: (() -> Unit)?,
    chip: @Composable () -> Unit = {},
    extra: @Composable RowScope.() -> Unit = {}
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 14.dp, top = 12.dp, end = 6.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                chip()
                IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, "Editar") }
                if (onEliminar != null) IconButton(onClick = onEliminar) { Icon(Icons.Filled.Delete, "Eliminar") }
            }
            lineas.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(content = extra)
        }
    }
}
