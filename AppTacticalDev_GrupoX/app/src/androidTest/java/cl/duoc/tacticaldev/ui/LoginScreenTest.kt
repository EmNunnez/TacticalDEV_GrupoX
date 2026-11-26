package cl.duoc.tacticaldev.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.duoc.tacticaldev.ui.screens.auth.LoginContenido
import cl.duoc.tacticaldev.ui.theme.TacticalTheme
import cl.duoc.tacticaldev.viewmodel.LoginUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Prueba de UI (Compose UI Test + JUnit4). Se ejecuta en emulador o dispositivo. */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun muestraErroresDeValidacion() {
        compose.setContent {
            TacticalTheme {
                LoginContenido(
                    estado = LoginUiState(emailError = "Formato de correo inválido", passwordError = "La contraseña es obligatoria"),
                    onEmail = {}, onPassword = {}, onLogin = {}, onIrRegistro = {}
                )
            }
        }
        compose.onNodeWithText("Formato de correo inválido").assertIsDisplayed()
        compose.onNodeWithText("La contraseña es obligatoria").assertIsDisplayed()
    }

    @Test
    fun escribirYPresionarIngresar() {
        var email = ""
        var presionado = false
        compose.setContent {
            TacticalTheme {
                LoginContenido(
                    estado = LoginUiState(),
                    onEmail = { email = it }, onPassword = {}, onLogin = { presionado = true }, onIrRegistro = {}
                )
            }
        }
        compose.onNodeWithTag("campoEmail").performTextInput("tecnico@tacticaldev.cl")
        compose.onNodeWithTag("botonLogin").performClick()
        assertEquals("tecnico@tacticaldev.cl", email)
        assertTrue(presionado)
    }

    @Test
    fun muestraErrorDelServidor() {
        compose.setContent {
            TacticalTheme {
                LoginContenido(
                    estado = LoginUiState(error = "Correo o contraseña incorrectos"),
                    onEmail = {}, onPassword = {}, onLogin = {}, onIrRegistro = {}
                )
            }
        }
        compose.onNodeWithTag("errorLogin").assertTextEquals("Correo o contraseña incorrectos")
    }

    @Test
    fun botonDeshabilitadoMientrasCarga() {
        compose.setContent {
            TacticalTheme {
                LoginContenido(estado = LoginUiState(cargando = true), onEmail = {}, onPassword = {}, onLogin = {}, onIrRegistro = {})
            }
        }
        compose.onNodeWithTag("botonLogin").assertIsNotEnabled()
    }
}
