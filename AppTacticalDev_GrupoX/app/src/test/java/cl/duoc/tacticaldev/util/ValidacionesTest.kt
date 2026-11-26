package cl.duoc.tacticaldev.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ValidacionesTest : StringSpec({

    "nombre: obligatorio, mínimo 3 y máximo 80" {
        Validaciones.nombre("").shouldNotBeNull()
        Validaciones.nombre("Al").shouldNotBeNull()
        Validaciones.nombre("a".repeat(81)).shouldNotBeNull()
        Validaciones.nombre("Minimarket Don Pepe").shouldBeNull()
    }

    "email valida formato" {
        Validaciones.email("") shouldBe "El correo es obligatorio"
        Validaciones.email("sin-arroba.cl") shouldBe "Formato de correo inválido"
        Validaciones.email("cliente@minimarket.cl").shouldBeNull()
    }

    "password exige 8 caracteres, letras y números" {
        Validaciones.password("").shouldNotBeNull()
        Validaciones.password("abc1").shouldNotBeNull()
        Validaciones.password("abcdefgh") shouldBe "Debe incluir letras y números"
        Validaciones.password("12345678") shouldBe "Debe incluir letras y números"
        Validaciones.password("Opera1234").shouldBeNull()
    }

    "password opcional permite vacío en edición" {
        Validaciones.passwordOpcional("").shouldBeNull()
        Validaciones.passwordOpcional("corta").shouldNotBeNull()
    }

    "confirmar password" {
        Validaciones.confirmarPassword("Clave1234", "").shouldNotBeNull()
        Validaciones.confirmarPassword("Clave1234", "Otra1234") shouldBe "Las contraseñas no coinciden"
        Validaciones.confirmarPassword("Clave1234", "Clave1234").shouldBeNull()
    }

    "teléfono es opcional pero con formato" {
        Validaciones.telefono("").shouldBeNull()
        Validaciones.telefono("+56912345678").shouldBeNull()
        Validaciones.telefono("123").shouldNotBeNull()
    }

    "obligatorio y descripción" {
        Validaciones.obligatorio(" ", "La comuna") shouldBe "La comuna es obligatorio"
        Validaciones.obligatorio("Maipú", "La comuna").shouldBeNull()
        Validaciones.descripcion("").shouldNotBeNull()
        Validaciones.descripcion("corta").shouldNotBeNull()
        Validaciones.descripcion("x".repeat(501)).shouldNotBeNull()
        Validaciones.descripcion("La vitrina no enfría").shouldBeNull()
    }

    "coordenadas y radio" {
        Validaciones.latitud("abc").shouldNotBeNull()
        Validaciones.latitud("-91").shouldNotBeNull()
        Validaciones.latitud("-33,5167").shouldBeNull()
        Validaciones.longitud("x").shouldNotBeNull()
        Validaciones.longitud("181").shouldNotBeNull()
        Validaciones.longitud("-70.716").shouldBeNull()
        Validaciones.radio("diez").shouldNotBeNull()
        Validaciones.radio("5").shouldNotBeNull()
        Validaciones.radio("100").shouldBeNull()
    }

    "código de equipo con formato XX-0000" {
        Validaciones.codigoEquipo("").shouldNotBeNull()
        Validaciones.codigoEquipo("VF1").shouldNotBeNull()
        Validaciones.codigoEquipo("VF-0001").shouldBeNull()
    }

    "observación obligatoria solo si el equipo queda con falla" {
        Validaciones.observacionVisita("", equipoConFalla = false).shouldBeNull()
        Validaciones.observacionVisita("mal", equipoConFalla = true).shouldNotBeNull()
        Validaciones.observacionVisita("No enfría, compresor ruidoso", equipoConFalla = true).shouldBeNull()
        Validaciones.observacionVisita("x".repeat(501), equipoConFalla = false).shouldNotBeNull()
    }
})
