package cl.duoc.tacticaldev.viewmodel

import cl.duoc.tacticaldev.Fixtures
import cl.duoc.tacticaldev.ViewModelSpec
import cl.duoc.tacticaldev.data.model.Rol
import cl.duoc.tacticaldev.repository.AuthRepository
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow

class SesionViewModelTest : ViewModelSpec({
    "sin sesión guardada -> SinSesion; con sesión -> Activa con el rol" {
        val flujo = MutableStateFlow<cl.duoc.tacticaldev.data.model.Sesion?>(null)
        val auth = mockk<AuthRepository>(relaxed = true)
        every { auth.sesion } returns flujo
        val vm = SesionViewModel(auth)
        vm.estado.value shouldBe EstadoSesion.SinSesion
        flujo.value = Fixtures.sesion(rol = Rol.SUPERVISOR)
        val e = vm.estado.value
        e.shouldBeInstanceOf<EstadoSesion.Activa>()
        e.sesion.rol shouldBe Rol.SUPERVISOR
        vm.cerrarSesion()
        coVerify { auth.cerrarSesion() }
    }
})

class LoginViewModelTest : ViewModelSpec({
    "campos vacíos muestran errores y no llaman al servidor" {
        val auth = mockk<AuthRepository>()
        val vm = LoginViewModel(auth)
        vm.login()
        vm.estado.value.emailError.shouldNotBeNull()
        vm.estado.value.passwordError.shouldNotBeNull()
        coVerify(exactly = 0) { auth.login(any(), any()) }
    }

    "al escribir se limpian los errores" {
        val vm = LoginViewModel(mockk())
        vm.validar() shouldBe false
        vm.onEmailChange("a@b.cl")
        vm.onPasswordChange("x")
        vm.estado.value.emailError.shouldBeNull()
        vm.estado.value.passwordError.shouldBeNull()
    }

    "login correcto marca éxito" {
        val auth = mockk<AuthRepository>()
        coEvery { auth.login("tecnico@tacticaldev.cl", "Opera1234") } returns Result.success(Fixtures.usuario())
        val vm = LoginViewModel(auth)
        vm.onEmailChange("tecnico@tacticaldev.cl")
        vm.onPasswordChange("Opera1234")
        vm.login()
        vm.estado.value.exito shouldBe true
        vm.estado.value.cargando shouldBe false
    }

    "login incorrecto muestra el mensaje del servidor" {
        val auth = mockk<AuthRepository>()
        coEvery { auth.login(any(), any()) } returns Result.failure(Exception("Correo o contraseña incorrectos"))
        val vm = LoginViewModel(auth)
        vm.onEmailChange("tecnico@tacticaldev.cl")
        vm.onPasswordChange("mala")
        vm.login()
        vm.estado.value.error shouldBe "Correo o contraseña incorrectos"
        vm.estado.value.exito shouldBe false
    }
})

class RegistroViewModelTest : ViewModelSpec({
    fun llenar(vm: RegistroViewModel) {
        vm.onNombre("Minimarket Don Pepe")
        vm.onEmail("pepe@minimarket.cl")
        vm.onTelefono("+56912345678")
        vm.onPassword("Cliente1234")
        vm.onConfirmar("Cliente1234")
        vm.onTerminos(true)
    }

    "formulario vacío tiene errores en todos los campos obligatorios" {
        val vm = RegistroViewModel(mockk())
        vm.validarFormulario() shouldBe false
        with(vm.estado.value.errores) {
            nombre.shouldNotBeNull(); email.shouldNotBeNull(); password.shouldNotBeNull()
            confirmar.shouldNotBeNull(); terminos.shouldNotBeNull(); telefono.shouldBeNull()
            hayErrores shouldBe true
        }
    }

    "contraseñas distintas no permiten registrar" {
        val vm = RegistroViewModel(mockk())
        llenar(vm)
        vm.onConfirmar("Otra1234")
        vm.validarFormulario() shouldBe false
        vm.estado.value.errores.confirmar shouldBe "Las contraseñas no coinciden"
    }

    "registro correcto crea el cliente e inicia sesión" {
        val auth = mockk<AuthRepository>()
        coEvery { auth.registrarCliente(any(), any(), any(), any()) } returns Result.success(Fixtures.usuario(rol = Rol.CLIENTE))
        coEvery { auth.login("pepe@minimarket.cl", "Cliente1234") } returns Result.success(Fixtures.usuario(rol = Rol.CLIENTE))
        val vm = RegistroViewModel(auth)
        llenar(vm)
        vm.registrar()
        coVerify { auth.login("pepe@minimarket.cl", "Cliente1234") }
        vm.estado.value.error.shouldBeNull()
        vm.estado.value.cargando shouldBe false
    }

    "correo repetido muestra el error" {
        val auth = mockk<AuthRepository>()
        coEvery { auth.registrarCliente(any(), any(), any(), any()) } returns Result.failure(Exception("Ya existe un usuario con ese correo"))
        val vm = RegistroViewModel(auth)
        llenar(vm)
        vm.registrar()
        vm.estado.value.error shouldBe "Ya existe un usuario con ese correo"
        coVerify(exactly = 0) { auth.login(any(), any()) }
    }
})
