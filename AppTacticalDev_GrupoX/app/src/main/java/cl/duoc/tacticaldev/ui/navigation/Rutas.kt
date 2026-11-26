package cl.duoc.tacticaldev.ui.navigation

import cl.duoc.tacticaldev.data.model.Rol

/** Rutas tipadas de la app (sealed class para evitar strings sueltos). */
sealed class Rutas(val ruta: String) {
    data object Splash : Rutas("splash")
    data object Login : Rutas("login")
    data object Registro : Rutas("registro")

    // Cliente
    data object ClienteHome : Rutas("cliente")
    data object NuevaSolicitud : Rutas("cliente/nueva-solicitud")

    // Operario
    data object OperarioHome : Rutas("operario")
    data object RegistrarVisita : Rutas("operario/visita/{solicitudId}") {
        fun crear(solicitudId: Long) = "operario/visita/$solicitudId"
    }

    // Supervisor
    data object SupervisorHome : Rutas("supervisor")
    data object FormPunto : Rutas("supervisor/punto/{id}") {
        fun crear(id: Long?) = "supervisor/punto/${id ?: 0}"
    }
    data object FormEquipo : Rutas("supervisor/equipo/{id}") {
        fun crear(id: Long?) = "supervisor/equipo/${id ?: 0}"
    }
    data object FormUsuario : Rutas("supervisor/usuario/{id}") {
        fun crear(id: Long?) = "supervisor/usuario/${id ?: 0}"
    }

    // Compartidas
    data object DetalleVisita : Rutas("visita/{visitaId}") {
        fun crear(visitaId: Long) = "visita/$visitaId"
    }
    data object Historial : Rutas("historial/{equipoId}/{codigo}") {
        fun crear(equipoId: Long, codigo: String) = "historial/$equipoId/$codigo"
    }

    companion object {
        /**
         * Pantallas a las que se puede entrar sin sesión.
         * Se calcula con un getter (y no en la inicialización estática del companion)
         * para que nunca dependa del orden en que la JVM inicializa los data object:
         * eso provocaba el ExceptionInInitializerError en AppNavigation.
         */
        val publicas: Set<String>
            get() = setOf(Splash.ruta, Login.ruta, Registro.ruta)

        fun inicioPara(rol: Rol): String = when (rol) {
            Rol.CLIENTE -> ClienteHome.ruta
            Rol.OPERARIO -> OperarioHome.ruta
            Rol.SUPERVISOR -> SupervisorHome.ruta
        }
    }
}
