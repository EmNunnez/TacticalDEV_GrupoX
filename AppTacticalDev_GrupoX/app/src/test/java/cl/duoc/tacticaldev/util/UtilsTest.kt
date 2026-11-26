package cl.duoc.tacticaldev.util

import cl.duoc.tacticaldev.Fixtures
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime

class GeoUtilsTest : StringSpec({
    "misma coordenada = 0 metros" {
        GeoUtils.distanciaMetros(-33.5, -70.7, -33.5, -70.7) shouldBe (0.0 plusOrMinus 0.01)
    }
    "0.009 grados de latitud ≈ 1 km" {
        GeoUtils.distanciaMetros(-33.5, -70.7, -33.509, -70.7) shouldBe (1000.0 plusOrMinus 10.0)
    }
    "formato de distancia" {
        GeoUtils.formatearDistancia(35.4) shouldBe "35 m"
        GeoUtils.formatearDistancia(1250.0) shouldBe "1.3 km"
    }
})

class FormatosTest : StringSpec({
    "fecha ISO para el servidor" {
        Formatos.fechaIso(LocalDateTime.of(2026, 11, 18, 9, 5, 3, 123)) shouldBe "2026-11-18T09:05:03"
    }
    "fecha legible" {
        Formatos.fechaLegible("2026-11-18T19:57:03.123") shouldBe "18-11-2026 19:57"
        Formatos.fechaLegible(null) shouldBe "-"
        Formatos.fechaLegible("no-es-fecha") shouldBe "no-es-fecha"
    }
})

class ClimaUtilsTest : StringSpec({
    "códigos WMO" {
        ClimaUtils.descripcion(0) shouldBe "Despejado"
        ClimaUtils.descripcion(2) shouldBe "Parcialmente nublado"
        ClimaUtils.descripcion(3) shouldBe "Nublado"
        ClimaUtils.descripcion(45) shouldBe "Neblina"
        ClimaUtils.descripcion(53) shouldBe "Llovizna"
        ClimaUtils.descripcion(63) shouldBe "Lluvia"
        ClimaUtils.descripcion(81) shouldBe "Lluvia"
        ClimaUtils.descripcion(73) shouldBe "Nieve"
        ClimaUtils.descripcion(96) shouldBe "Tormenta"
        ClimaUtils.descripcion(null) shouldBe "Sin datos"
        ClimaUtils.descripcion(150) shouldBe "Condición 150"
    }
    "riesgo por alta temperatura desde 30 °C" {
        ClimaUtils.riesgoAltaTemperatura(29.9) shouldBe false
        ClimaUtils.riesgoAltaTemperatura(30.0) shouldBe true
    }
})

class ApiCallTest : StringSpec({
    "safeCall éxito" {
        safeCall { 42 }.getOrNull() shouldBe 42
    }
    "safeCall convierte HttpException en ApiException con el mensaje del servidor" {
        val r = safeCall<Int> { throw Fixtures.httpError(409, "Ya existe un usuario con ese correo") }
        val e = r.exceptionOrNull()
        e.shouldBeInstanceOf<ApiException>()
        e.message shouldBe "Ya existe un usuario con ese correo"
        e.codigo shouldBe 409
    }
    "safeCall convierte IOException en SinConexionException" {
        safeCall<Int> { throw IOException("timeout") }.exceptionOrNull().shouldBeInstanceOf<SinConexionException>()
    }
    "safeCall envuelve errores inesperados" {
        safeCall<Int> { throw IllegalStateException("raro") }.exceptionOrNull()?.message shouldBe "raro"
    }
    "mensajeDeError usa un texto genérico si el cuerpo no es JSON" {
        mensajeDeError("<html>", 500) shouldContain "500"
        mensajeDeError(null, 404) shouldContain "404"
    }
    "safeCallSinCuerpo: 204 es éxito y 409 es error" {
        safeCallSinCuerpo { Response.success(Unit) }.isSuccess shouldBe true
        val r = safeCallSinCuerpo {
            Response.error(409, """{"mensaje":"Tiene equipos"}""".toResponseBody("application/json".toMediaType()))
        }
        r.exceptionOrNull()?.message shouldBe "Tiene equipos"
    }
    "UbicacionCapturada guarda la evidencia" {
        val u = UbicacionCapturada(-33.5, -70.7, 8.0, false)
        u.copy(esSimulada = true).esSimulada shouldBe true
    }
})
