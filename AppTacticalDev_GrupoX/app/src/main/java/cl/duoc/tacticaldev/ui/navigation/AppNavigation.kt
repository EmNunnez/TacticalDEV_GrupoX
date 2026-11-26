package cl.duoc.tacticaldev.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.tacticaldev.data.model.Sesion
import cl.duoc.tacticaldev.ui.screens.auth.LoginScreen
import cl.duoc.tacticaldev.ui.screens.auth.RegistroScreen
import cl.duoc.tacticaldev.ui.screens.auth.SplashScreen
import cl.duoc.tacticaldev.ui.screens.cliente.ClienteHomeScreen
import cl.duoc.tacticaldev.ui.screens.cliente.NuevaSolicitudScreen
import cl.duoc.tacticaldev.ui.screens.comun.DetalleVisitaScreen
import cl.duoc.tacticaldev.ui.screens.comun.HistorialEquipoScreen
import cl.duoc.tacticaldev.ui.screens.operario.OperarioHomeScreen
import cl.duoc.tacticaldev.ui.screens.operario.RegistrarVisitaScreen
import cl.duoc.tacticaldev.ui.screens.supervisor.FormEquipoScreen
import cl.duoc.tacticaldev.ui.screens.supervisor.FormPuntoScreen
import cl.duoc.tacticaldev.ui.screens.supervisor.FormUsuarioScreen
import cl.duoc.tacticaldev.ui.screens.supervisor.SupervisorHomeScreen
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.viewmodel.EstadoSesion
import cl.duoc.tacticaldev.viewmodel.SesionViewModel

private const val RECARGAR = "recargar"

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val sesionVm = tacticalViewModel { SesionViewModel(it.authRepository) }
    val estado by sesionVm.estado.collectAsStateWithLifecycle()

    // Redirección según la sesión guardada en DataStore y el rol del usuario
    LaunchedEffect(estado) {
        val actual = nav.currentDestination?.route
        when (val e = estado) {
            is EstadoSesion.Activa -> if (actual == null || actual in Rutas.publicas) {
                nav.navigate(Rutas.inicioPara(e.sesion.rol)) { popUpTo(nav.graph.id) { inclusive = true } }
            }
            EstadoSesion.SinSesion -> if (actual != Rutas.Login.ruta && actual != Rutas.Registro.ruta) {
                nav.navigate(Rutas.Login.ruta) { popUpTo(nav.graph.id) { inclusive = true } }
            }
            EstadoSesion.Cargando -> Unit
        }
    }
    val sesion: Sesion? = (estado as? EstadoSesion.Activa)?.sesion

    NavHost(
        navController = nav,
        startDestination = Rutas.Splash.ruta,
        enterTransition = { fadeIn() + slideInHorizontally { it / 4 } },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() + slideOutHorizontally { it / 4 } }
    ) {
        composable(Rutas.Splash.ruta) { SplashScreen() }

        composable(Rutas.Login.ruta) {
            LoginScreen(onIrRegistro = { nav.navigate(Rutas.Registro.ruta) })
        }

        composable(Rutas.Registro.ruta) {
            RegistroScreen(onVolver = { nav.popBackStack() })
        }

        // ---------------- CLIENTE ----------------
        composable(Rutas.ClienteHome.ruta) { entry ->
            val s = sesion ?: return@composable
            val (recargar, listo) = entry.banderaRecarga()
            ClienteHomeScreen(
                sesion = s,
                recargar = recargar,
                onRecargado = listo,
                onNuevaSolicitud = { nav.navigate(Rutas.NuevaSolicitud.ruta) },
                onHistorial = { id, codigo -> nav.navigate(Rutas.Historial.crear(id, codigo)) },
                onCerrarSesion = sesionVm::cerrarSesion
            )
        }
        composable(Rutas.NuevaSolicitud.ruta) {
            val s = sesion ?: return@composable
            NuevaSolicitudScreen(
                clienteId = s.id,
                onCreada = { nav.volverRecargando() },
                onVolver = { nav.popBackStack() }
            )
        }

        // ---------------- OPERARIO ----------------
        composable(Rutas.OperarioHome.ruta) { entry ->
            val s = sesion ?: return@composable
            val (recargar, listo) = entry.banderaRecarga()
            OperarioHomeScreen(
                sesion = s,
                recargar = recargar,
                onRecargado = listo,
                onRegistrarVisita = { id -> nav.navigate(Rutas.RegistrarVisita.crear(id)) },
                onVerVisita = { id -> nav.navigate(Rutas.DetalleVisita.crear(id)) },
                onCerrarSesion = sesionVm::cerrarSesion
            )
        }
        composable(
            Rutas.RegistrarVisita.ruta,
            arguments = listOf(navArgument("solicitudId") { type = NavType.LongType })
        ) { entry ->
            val s = sesion ?: return@composable
            RegistrarVisitaScreen(
                operarioId = s.id,
                solicitudId = entry.arguments?.getLong("solicitudId") ?: 0L,
                onTerminado = { nav.volverRecargando() },
                onVolver = { nav.popBackStack() }
            )
        }

        // ---------------- SUPERVISOR ----------------
        composable(Rutas.SupervisorHome.ruta) { entry ->
            val s = sesion ?: return@composable
            val (recargar, listo) = entry.banderaRecarga()
            SupervisorHomeScreen(
                sesion = s,
                recargar = recargar,
                onRecargado = listo,
                onVerVisita = { id -> nav.navigate(Rutas.DetalleVisita.crear(id)) },
                onFormPunto = { id -> nav.navigate(Rutas.FormPunto.crear(id)) },
                onFormEquipo = { id -> nav.navigate(Rutas.FormEquipo.crear(id)) },
                onFormUsuario = { id -> nav.navigate(Rutas.FormUsuario.crear(id)) },
                onHistorial = { id, codigo -> nav.navigate(Rutas.Historial.crear(id, codigo)) },
                onCerrarSesion = sesionVm::cerrarSesion
            )
        }
        composable(Rutas.FormPunto.ruta, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            FormPuntoScreen(
                id = entry.arguments?.getLong("id")?.takeIf { it > 0 },
                onGuardado = { nav.volverRecargando() },
                onVolver = { nav.popBackStack() }
            )
        }
        composable(Rutas.FormEquipo.ruta, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            FormEquipoScreen(
                id = entry.arguments?.getLong("id")?.takeIf { it > 0 },
                onGuardado = { nav.volverRecargando() },
                onVolver = { nav.popBackStack() }
            )
        }
        composable(Rutas.FormUsuario.ruta, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            FormUsuarioScreen(
                id = entry.arguments?.getLong("id")?.takeIf { it > 0 },
                onGuardado = { nav.volverRecargando() },
                onVolver = { nav.popBackStack() }
            )
        }

        // ---------------- COMPARTIDAS ----------------
        composable(
            Rutas.DetalleVisita.ruta,
            arguments = listOf(navArgument("visitaId") { type = NavType.LongType })
        ) { entry ->
            val s = sesion ?: return@composable
            DetalleVisitaScreen(
                visitaId = entry.arguments?.getLong("visitaId") ?: 0L,
                puedeValidar = s.rol == cl.duoc.tacticaldev.data.model.Rol.SUPERVISOR,
                onTerminado = { nav.volverRecargando() },
                onVolver = { nav.popBackStack() }
            )
        }
        composable(
            Rutas.Historial.ruta,
            arguments = listOf(
                navArgument("equipoId") { type = NavType.LongType },
                navArgument("codigo") { type = NavType.StringType }
            )
        ) { entry ->
            HistorialEquipoScreen(
                equipoId = entry.arguments?.getLong("equipoId") ?: 0L,
                codigo = entry.arguments?.getString("codigo").orEmpty(),
                onVerVisita = { id -> nav.navigate(Rutas.DetalleVisita.crear(id)) },
                onVolver = { nav.popBackStack() }
            )
        }
    }
}

/** Vuelve a la pantalla anterior avisándole que recargue sus datos. */
private fun NavHostController.volverRecargando() {
    previousBackStackEntry?.savedStateHandle?.set(RECARGAR, true)
    popBackStack()
}

/** Lee la bandera de recarga dejada por la pantalla hija. */
@Composable
private fun NavBackStackEntry.banderaRecarga(): Pair<Boolean, () -> Unit> {
    val recargar by savedStateHandle.getStateFlow(RECARGAR, false).collectAsStateWithLifecycle()
    return recargar to { savedStateHandle[RECARGAR] = false }
}
