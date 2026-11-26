package cl.duoc.tacticaldev.data.remote

import cl.duoc.tacticaldev.BuildConfig
import cl.duoc.tacticaldev.data.local.SesionStore
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.repository.AuthRepository
import cl.duoc.tacticaldev.util.ApiException
import cl.duoc.tacticaldev.util.SinConexionException
import cl.duoc.tacticaldev.util.safeCall
import cl.duoc.tacticaldev.util.safeCallSinCuerpo
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.TimeUnit

/**
 * Pruebas de CONTRATO con el backend Spring Boot desplegado en AWS (EC2, puerto 8080).
 *
 * Un servidor HTTP falso (MockWebServer) responde con el mismo JSON que devuelve el backend real
 * y se usa el Retrofit + Gson REAL de la app. Así se comprueba, sin depender de internet, que:
 *  - cada interfaz llama a la ruta y método HTTP correctos (los mismos @GetMapping/@PostMapping del backend),
 *  - los parámetros ?query= y el cuerpo JSON tienen los nombres que espera el backend,
 *  - las respuestas del backend se convierten bien en los modelos de la app (incluidos los enums),
 *  - los errores {"mensaje": "..."} del backend llegan al usuario como texto legible.
 */
class ContratoBackendTest : StringSpec({

    "ms usuarios: login, registro y CRUD de usuarios" {
        conServidor { s ->
            val api = crear<ApiUsuarios>(s)

            s.json(USUARIO)
            val u = api.login(LoginRequest("tecnico@tacticaldev.cl", "Opera1234"))
            u.nombre shouldBe "Tomás Técnico"
            u.rol shouldBe Rol.OPERARIO
            u.tipoOperario shouldBe TipoOperario.TECNICO
            s.esperar("POST", "/api/auth/login").body.readUtf8() shouldBe
                    """{"email":"tecnico@tacticaldev.cl","password":"Opera1234"}"""

            s.json(USUARIO_CLIENTE, 201)
            api.registro(UsuarioRequest("Don Pepe", "pepe@mini.cl", "Cliente1234", null, Rol.CLIENTE, null)).rol shouldBe Rol.CLIENTE
            s.esperar("POST", "/api/auth/registro").body.readUtf8() shouldContain "\"rol\":\"CLIENTE\""

            // Respuesta real copiada desde http://52.71.145.213:8080/api/usuarios?rol=OPERARIO
            s.json(OPERARIOS_REALES)
            api.listar(Rol.OPERARIO).map { it.tipoOperario } shouldBe
                    listOf(TipoOperario.TRANSPORTISTA, TipoOperario.TECNICO, TipoOperario.VENDEDOR)
            s.esperar("GET", "/api/usuarios").requestUrl!!.queryParameter("rol") shouldBe "OPERARIO"

            s.json("[]")
            api.listar()
            s.esperar("GET", "/api/usuarios").requestUrl!!.queryParameter("rol").shouldBeNull()

            s.json(USUARIO)
            api.obtener(2).id shouldBe 2L
            s.esperar("GET", "/api/usuarios/2")

            val req = UsuarioRequest("Tomás", "t@t.cl", "Opera1234", "+56912345678", Rol.OPERARIO, TipoOperario.TECNICO)
            s.json(USUARIO, 201)
            api.crear(req)
            s.esperar("POST", "/api/usuarios").body.readUtf8() shouldContain "\"tipoOperario\":\"TECNICO\""

            s.json(USUARIO)
            api.actualizar(2, req)
            s.esperar("PUT", "/api/usuarios/2")

            s.sinCuerpo()
            api.eliminar(2).isSuccessful shouldBe true
            s.esperar("DELETE", "/api/usuarios/2")
        }
    }

    "ms operaciones: puntos de venta y equipos" {
        conServidor { s ->
            val api = crear<ApiOperaciones>(s)
            val punto = PuntoVentaRequest("Local", "Calle 1", "Maipú", -33.5, -70.7, 100, 5)
            val equipo = EquipoRequest("VF-0009", TipoEquipo.VITRINA_FREEZER, "FrioMax", "VF-300", EstadoEquipo.OPERATIVO, null)

            s.json("[$PUNTO]")
            api.listarPuntos().first().radioMetros shouldBe 400
            s.esperar("GET", "/api/puntos-venta")

            s.json("[$PUNTO]")
            api.listarPuntos(5).first().clienteId shouldBe 5L
            s.esperar("GET", "/api/puntos-venta").requestUrl!!.queryParameter("clienteId") shouldBe "5"

            s.json(PUNTO, 201)
            api.crearPunto(punto).latitud shouldBe -33.5167
            s.esperar("POST", "/api/puntos-venta").body.readUtf8() shouldContain "\"radioMetros\":100"

            s.json(PUNTO)
            api.actualizarPunto(1, punto)
            s.esperar("PUT", "/api/puntos-venta/1")

            s.sinCuerpo()
            api.eliminarPunto(1)
            s.esperar("DELETE", "/api/puntos-venta/1")

            s.json("[$EQUIPO, $EQUIPO_BODEGA]")
            val equipos = api.listarEquipos()
            equipos[0].estado shouldBe EstadoEquipo.CON_FALLA
            equipos[1].puntoVentaId.shouldBeNull()
            s.esperar("GET", "/api/equipos")

            s.json("[$EQUIPO_BODEGA]")
            api.listarEquipos(enBodega = true)
            s.esperar("GET", "/api/equipos").requestUrl!!.queryParameter("enBodega") shouldBe "true"

            s.json("[$EQUIPO]")
            api.listarEquipos(puntoVentaId = 1)
            s.esperar("GET", "/api/equipos").requestUrl!!.queryParameter("puntoVentaId") shouldBe "1"

            s.json("[$VISITA]")
            api.historialEquipo(2).first().equipoCodigo shouldBe "CN-0002"
            s.esperar("GET", "/api/equipos/2/historial")

            s.json(EQUIPO, 201)
            api.crearEquipo(equipo)
            s.esperar("POST", "/api/equipos").body.readUtf8() shouldContain "\"tipo\":\"VITRINA_FREEZER\""

            s.json(EQUIPO)
            api.actualizarEquipo(2, equipo)
            s.esperar("PUT", "/api/equipos/2")

            s.sinCuerpo()
            api.eliminarEquipo(2)
            s.esperar("DELETE", "/api/equipos/2")
        }
    }

    "ms operaciones: solicitudes" {
        conServidor { s ->
            val api = crear<ApiOperaciones>(s)

            s.json("[$SOLICITUD]")
            val sol = api.listarSolicitudes().first()
            sol.tipo shouldBe TipoSolicitud.REPARACION
            sol.estado shouldBe EstadoSolicitud.PENDIENTE
            sol.puntoVenta.nombre shouldBe "Minimarket Don Pepe - Sede"
            sol.equipo!!.codigo shouldBe "CN-0002"
            sol.operarioId.shouldBeNull()
            sol.fechaCreacion shouldBe "2026-11-26T00:44:57.123"
            s.esperar("GET", "/api/solicitudes")

            s.json("[$SOLICITUD]")
            api.listarSolicitudes(operarioId = 2, estado = EstadoSolicitud.ASIGNADA)
            val r = s.esperar("GET", "/api/solicitudes")
            r.requestUrl!!.queryParameter("operarioId") shouldBe "2"
            r.requestUrl!!.queryParameter("estado") shouldBe "ASIGNADA"
            r.requestUrl!!.queryParameter("clienteId").shouldBeNull()

            s.json(SOLICITUD)
            api.obtenerSolicitud(1).id shouldBe 1L
            s.esperar("GET", "/api/solicitudes/1")

            s.json(SOLICITUD, 201)
            api.crearSolicitud(SolicitudRequest(TipoSolicitud.REPARACION, "No enfría nada", 5, 1, 2, null, null))
            val cuerpo = s.esperar("POST", "/api/solicitudes").body.readUtf8()
            cuerpo shouldContain "\"tipo\":\"REPARACION\""
            cuerpo shouldContain "\"equipoId\":2"

            s.json(SOLICITUD)
            api.actualizarSolicitud(1, SolicitudUpdateRequest("Ahora también gotea"))
            s.esperar("PUT", "/api/solicitudes/1").body.readUtf8() shouldBe """{"descripcion":"Ahora también gotea"}"""

            s.json(SOLICITUD)
            api.asignarSolicitud(1, AsignarRequest(2, null))
            s.esperar("PUT", "/api/solicitudes/1/asignar").body.readUtf8() shouldBe """{"operarioId":2}"""

            s.sinCuerpo()
            api.eliminarSolicitud(1)
            s.esperar("DELETE", "/api/solicitudes/1")
        }
    }

    "ms operaciones: visitas con foto + GPS y validación" {
        conServidor { s ->
            val api = crear<ApiOperaciones>(s)

            s.json("[$VISITA]")
            val v = api.listarVisitas().first()
            v.ubicacionVerificada shouldBe true
            v.estadoValidacion shouldBe EstadoValidacion.PENDIENTE
            v.distanciaMetros shouldBe 14.2
            s.esperar("GET", "/api/visitas")

            s.json("[$VISITA]")
            api.listarVisitas(operarioId = 2)
            s.esperar("GET", "/api/visitas").requestUrl!!.queryParameter("operarioId") shouldBe "2"

            s.json("[]")
            api.listarVisitas(estado = EstadoValidacion.SOSPECHOSA)
            s.esperar("GET", "/api/visitas").requestUrl!!.queryParameter("estado") shouldBe "SOSPECHOSA"

            s.json(VISITA)
            api.obtenerVisita(1).temperaturaAmbiente shouldBe 24.5
            s.esperar("GET", "/api/visitas/1")

            s.json(VISITA, 201)
            api.registrarVisita(
                VisitaRequest(
                    "uuid-1", 1, 2, EstadoEquipo.OPERATIVO, "Se cambió el termostato", "QkFTRTY0",
                    -33.5168, -70.7161, 12.0, false, "2026-11-26T10:00:00", 24.5
                )
            )
            val cuerpo = s.esperar("POST", "/api/visitas").body.readUtf8()
            cuerpo shouldContain "\"fechaCaptura\":\"2026-11-26T10:00:00\""
            cuerpo shouldContain "\"fotoBase64\":\"QkFTRTY0\""
            cuerpo shouldContain "\"ubicacionSimulada\":false"
            cuerpo shouldContain "\"estadoEquipo\":\"OPERATIVO\""

            s.json(VISITA_VALIDADA)
            api.validarVisita(1, ValidarRequest(true, "Evidencia correcta")).estadoValidacion shouldBe EstadoValidacion.VALIDADA
            s.esperar("PUT", "/api/visitas/1/validar").body.readUtf8() shouldBe
                    """{"aprobada":true,"comentario":"Evidencia correcta"}"""
        }
    }

    "API externa Open-Meteo: parámetros y JSON" {
        conServidor { s ->
            val api = crear<ApiClima>(s)
            s.json(CLIMA)
            val r = api.climaActual(-33.5, -70.75)
            r.current!!.temperatura shouldBe 24.3
            r.current!!.humedad shouldBe 40
            r.current!!.codigoClima shouldBe 1
            val url = s.esperar("GET", "/v1/forecast").requestUrl!!
            url.queryParameter("latitude") shouldBe "-33.5"
            url.queryParameter("longitude") shouldBe "-70.75"
            url.queryParameter("current") shouldBe "temperature_2m,relative_humidity_2m,weather_code"
            url.queryParameter("timezone") shouldBe "auto"
        }
    }

    "errores del backend llegan al usuario como mensaje legible" {
        conServidor { s ->
            val usuarios = crear<ApiUsuarios>(s)
            val operaciones = crear<ApiOperaciones>(s)

            s.json("""{"mensaje":"Ya existe un usuario con ese correo"}""", 409)
            val e = safeCall { usuarios.registro(UsuarioRequest("A", "a@a.cl", "Clave1234", null, Rol.CLIENTE, null)) }
                .exceptionOrNull()
            e.shouldBeInstanceOf<ApiException>()
            e.message shouldBe "Ya existe un usuario con ese correo"
            e.codigo shouldBe 409

            s.json("""{"mensaje":"No se puede eliminar: el punto de venta tiene equipos instalados"}""", 409)
            safeCallSinCuerpo { operaciones.eliminarPunto(1) }.exceptionOrNull()?.message shouldBe
                    "No se puede eliminar: el punto de venta tiene equipos instalados"

            s.enqueue(MockResponse().setResponseCode(500).setBody("<html>Error</html>"))
            safeCall { operaciones.listarPuntos() }.exceptionOrNull()?.message shouldBe "Error del servidor (500)"
        }
        // Servidor apagado (EC2 detenida): la app muestra "Sin conexión"
        val apagado = MockWebServer()
        apagado.start()
        val api = crear<ApiOperaciones>(apagado)
        apagado.shutdown()
        safeCall { api.listarSolicitudes() }.exceptionOrNull().shouldBeInstanceOf<SinConexionException>()
    }

    "repositorio + Retrofit real: el login guarda la sesión" {
        conServidor { s ->
            val store = mockk<SesionStore>(relaxed = true)
            val repo = AuthRepository(crear(s), store)
            s.json(USUARIO)
            val u = repo.login(" tecnico@tacticaldev.cl ", "Opera1234").getOrNull().shouldNotBeNull()
            coVerify { store.guardar(u) }
            s.esperar("POST", "/api/auth/login").body.readUtf8() shouldContain "\"email\":\"tecnico@tacticaldev.cl\""
        }
    }

    "RetrofitInstance apunta al backend de la nube" {
        // Un solo backend (EC2) atiende usuarios y operaciones
        BuildConfig.API_USUARIOS_URL shouldBe BuildConfig.API_OPERACIONES_URL
        BuildConfig.API_USUARIOS_URL shouldContain "http://"
        BuildConfig.API_USUARIOS_URL.endsWith("/") shouldBe true
        BuildConfig.API_CLIMA_URL shouldBe "https://api.open-meteo.com/"

        RetrofitInstance.apiUsuarios.shouldNotBeNull()
        RetrofitInstance.apiOperaciones.shouldNotBeNull()
        RetrofitInstance.apiClima.shouldNotBeNull()
        RetrofitInstance.crearRetrofit(BuildConfig.API_USUARIOS_URL).baseUrl().toString() shouldBe BuildConfig.API_USUARIOS_URL

        RetrofitInstance.nivelLog(true) shouldBe HttpLoggingInterceptor.Level.BASIC
        RetrofitInstance.nivelLog(false) shouldBe HttpLoggingInterceptor.Level.NONE
        RetrofitInstance.clienteHttp.connectTimeoutMillis shouldBe 15_000
    }
}) {
    companion object {
        // ---------- JSON tal como lo devuelve el backend Spring Boot (records → JSON) ----------
        const val USUARIO = """{"id":2,"nombre":"Tomás Técnico","email":"tecnico@tacticaldev.cl","telefono":"+56900000000","rol":"OPERARIO","tipoOperario":"TECNICO","activo":true}"""
        const val USUARIO_CLIENTE = """{"id":7,"nombre":"Don Pepe","email":"pepe@mini.cl","telefono":null,"rol":"CLIENTE","tipoOperario":null,"activo":true}"""
        const val OPERARIOS_REALES = """[{"id":4,"nombre":"Tito Transportista","email":"transportista@tacticaldev.cl","telefono":"+56900000000","rol":"OPERARIO","tipoOperario":"TRANSPORTISTA","activo":true},{"id":2,"nombre":"Tomás Técnico","email":"tecnico@tacticaldev.cl","telefono":"+56900000000","rol":"OPERARIO","tipoOperario":"TECNICO","activo":true},{"id":3,"nombre":"Valentina Vendedora","email":"vendedor@tacticaldev.cl","telefono":"+56900000000","rol":"OPERARIO","tipoOperario":"VENDEDOR","activo":true}]"""
        const val PUNTO = """{"id":1,"nombre":"Minimarket Don Pepe - Sede","direccion":"Av. Ficticia 1501","comuna":"Cerrillos","latitud":-33.5167,"longitud":-70.716,"radioMetros":400,"clienteId":5}"""
        const val EQUIPO = """{"id":2,"codigo":"CN-0002","tipo":"CONSERVADORA","marca":"PolarCool","modelo":"CN-120","estado":"CON_FALLA","puntoVentaId":1,"puntoVentaNombre":"Minimarket Don Pepe - Sede"}"""
        const val EQUIPO_BODEGA = """{"id":4,"codigo":"VF-0004","tipo":"VITRINA_FREEZER","marca":"FrioMax","modelo":"VF-300","estado":"OPERATIVO","puntoVentaId":null,"puntoVentaNombre":null}"""
        const val SOLICITUD = """{"id":1,"tipo":"REPARACION","estado":"PENDIENTE","descripcion":"La conservadora no enfría","tipoEquipoSolicitado":null,"tieneFotoCliente":false,"fotoClienteBase64":null,"clienteId":5,"clienteNombre":"Minimarket Don Pepe","puntoVenta":$PUNTO,"equipo":$EQUIPO,"operarioId":null,"operarioNombre":null,"fechaCreacion":"2026-11-26T00:44:57.123","fechaAsignacion":null,"fechaCierre":null}"""
        const val VISITA = """{"id":1,"idLocal":"uuid-1","solicitudId":1,"tipoSolicitud":"REPARACION","puntoVentaNombre":"Minimarket Don Pepe - Sede","equipoCodigo":"CN-0002","operarioId":2,"operarioNombre":"Tomás Técnico","estadoEquipo":"OPERATIVO","observaciones":"Se cambió el termostato","fotoBase64":null,"latitud":-33.5168,"longitud":-70.7161,"precisionMetros":12.0,"ubicacionSimulada":false,"fechaCaptura":"2026-11-26T10:00:00","fechaRegistro":"2026-11-26T10:00:05.5","distanciaMetros":14.2,"ubicacionVerificada":true,"motivoVerificacion":"Foto y GPS dentro del radio (14 m)","temperaturaAmbiente":24.5,"estadoValidacion":"PENDIENTE","comentarioSupervisor":null,"fechaValidacion":null}"""
        const val VISITA_VALIDADA = """{"id":1,"idLocal":"uuid-1","solicitudId":1,"tipoSolicitud":"REPARACION","puntoVentaNombre":"Minimarket Don Pepe - Sede","equipoCodigo":"CN-0002","operarioId":2,"operarioNombre":"Tomás Técnico","estadoEquipo":"OPERATIVO","observaciones":null,"fotoBase64":null,"latitud":-33.5168,"longitud":-70.7161,"precisionMetros":12.0,"ubicacionSimulada":false,"fechaCaptura":"2026-11-26T10:00:00","fechaRegistro":"2026-11-26T10:00:05.5","distanciaMetros":14.2,"ubicacionVerificada":true,"motivoVerificacion":"ok","temperaturaAmbiente":null,"estadoValidacion":"VALIDADA","comentarioSupervisor":"Evidencia correcta","fechaValidacion":"2026-11-26T11:00:00"}"""
        const val CLIMA = """{"latitude":-33.5,"longitude":-70.75,"current":{"time":"2026-11-26T12:00","interval":900,"temperature_2m":24.3,"relative_humidity_2m":40,"weather_code":1}}"""
    }
}

// ---------------------------------------------------------------- helpers

private inline fun <reified T> crear(s: MockWebServer): T =
    RetrofitInstance.crearRetrofit(s.url("/").toString()).create(T::class.java)

private suspend fun conServidor(bloque: suspend (MockWebServer) -> Unit) {
    val servidor = MockWebServer()
    servidor.start()
    try {
        bloque(servidor)
    } finally {
        servidor.shutdown()
    }
}

private fun MockWebServer.json(cuerpo: String, codigo: Int = 200) = enqueue(
    MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json; charset=utf-8").setBody(cuerpo)
)

private fun MockWebServer.sinCuerpo() = enqueue(MockResponse().setResponseCode(204))

/** Toma la petición que hizo la app y verifica método HTTP y ruta. */
private fun MockWebServer.esperar(metodo: String, ruta: String): RecordedRequest {
    val r = takeRequest(5, TimeUnit.SECONDS).shouldNotBeNull()
    r.method shouldBe metodo
    r.requestUrl!!.encodedPath shouldBe ruta
    return r
}
