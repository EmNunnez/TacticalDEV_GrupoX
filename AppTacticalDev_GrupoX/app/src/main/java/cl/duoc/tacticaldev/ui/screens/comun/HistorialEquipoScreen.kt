@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.comun

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.tacticaldev.ui.components.BarraVolver
import cl.duoc.tacticaldev.ui.components.Cargando
import cl.duoc.tacticaldev.ui.components.Vacio
import cl.duoc.tacticaldev.ui.screens.operario.TarjetaVisita
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.viewmodel.HistorialViewModel

@Composable
fun HistorialEquipoScreen(equipoId: Long, codigo: String, onVerVisita: (Long) -> Unit, onVolver: () -> Unit) {
    val vm = tacticalViewModel(key = "historial-$equipoId") { HistorialViewModel(it.catalogoRepository, equipoId) }
    val s by vm.estado.collectAsStateWithLifecycle()

    Scaffold(topBar = { BarraVolver("Historial $codigo", onVolver) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                s.cargando -> Cargando()
                s.error != null -> Vacio(s.error!!)
                s.visitas.isEmpty() -> Vacio("Este equipo aún no tiene visitas registradas", Icons.Filled.History)
                else -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text("${s.visitas.size} visita(s) registradas", style = MaterialTheme.typography.titleSmall) }
                    items(s.visitas, key = { it.id }) { v -> TarjetaVisita(v, onClick = { onVerVisita(v.id) }) }
                }
            }
        }
    }
}
