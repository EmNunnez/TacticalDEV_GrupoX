@file:OptIn(ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duoc.tacticaldev.data.model.Sesion
import kotlinx.coroutines.launch

/**
 * Estructura común de las pantallas de inicio: TopAppBar + NavigationDrawer (perfil y
 * cerrar sesión) + NavigationBar opcional.
 */
@Composable
fun AppScaffold(
    titulo: String,
    sesion: Sesion,
    onCerrarSesion: () -> Unit,
    snackbarHost: SnackbarHostState,
    acciones: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    fab: @Composable () -> Unit = {},
    contenido: @Composable (PaddingValues) -> Unit
) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(24.dp)) {
                    Icon(Icons.Filled.AccountCircle, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(sesion.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(sesion.email, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    EstadoChip(
                        texto = sesion.tipoOperario?.let { "${sesion.rol.etiqueta} · ${it.etiqueta}" } ?: sesion.rol.etiqueta,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Cerrar sesión") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawer.close() }
                        onCerrarSesion()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "TacticalDEV · MVP académico DSY1105",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titulo) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Filled.Menu, "Menú") }
                    },
                    actions = acciones,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = bottomBar,
            floatingActionButton = fab,
            snackbarHost = { SnackbarHost(snackbarHost) },
            content = contenido
        )
    }
}

/** TopAppBar con flecha de volver para pantallas secundarias. */
@Composable
fun BarraVolver(titulo: String, onVolver: () -> Unit) {
    TopAppBar(
        title = { Text(titulo) },
        navigationIcon = {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
