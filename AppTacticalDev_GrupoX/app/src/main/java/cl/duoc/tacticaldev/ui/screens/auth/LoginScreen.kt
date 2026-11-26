@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.viewmodel.LoginUiState
import cl.duoc.tacticaldev.viewmodel.LoginViewModel

@Composable
fun LoginScreen(onIrRegistro: () -> Unit) {
    val vm = tacticalViewModel { LoginViewModel(it.authRepository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    LoginContenido(
        estado = estado,
        onEmail = vm::onEmailChange,
        onPassword = vm::onPasswordChange,
        onLogin = vm::login,
        onIrRegistro = onIrRegistro
    )
}

/** Separado del ViewModel para poder probarlo con Compose UI Test. */
@Composable
fun LoginContenido(
    estado: LoginUiState,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onLogin: () -> Unit,
    onIrRegistro: () -> Unit
) {
    var verPassword by remember { mutableStateOf(false) }

    Scaffold { padding ->
        // widthIn limita el ancho en tablets / horizontal (adaptabilidad)
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                Icon(Icons.Filled.AcUnit, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                Text("TacticalDEV", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Visitas verificadas con foto + GPS", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = estado.email,
                    onValueChange = onEmail,
                    label = { Text("Correo") },
                    leadingIcon = { Icon(Icons.Filled.Email, null) },
                    isError = estado.emailError != null,
                    supportingText = estado.emailError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("campoEmail")
                )
                OutlinedTextField(
                    value = estado.password,
                    onValueChange = onPassword,
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { verPassword = !verPassword }) {
                            Icon(if (verPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Mostrar contraseña")
                        }
                    },
                    visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = estado.passwordError != null,
                    supportingText = estado.passwordError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag("campoPassword")
                )

                AnimatedVisibility(visible = estado.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(estado.error.orEmpty(), modifier = Modifier.testTag("errorLogin"))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onLogin,
                    enabled = !estado.cargando,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("botonLogin")
                ) {
                    if (estado.cargando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Ingresar")
                }
                TextButton(onClick = onIrRegistro) { Text("¿Tienes un minimarket? Crea tu cuenta de cliente") }

                Spacer(Modifier.height(16.dp))
                CuentasDemo(onElegir = { email, pass -> onEmail(email); onPassword(pass) })
            }
        }
    }
}

/** Atajo para la defensa: rellena el formulario con usuarios ficticios. */
@Composable
private fun CuentasDemo(onElegir: (String, String) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Cuentas de prueba (datos ficticios)", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            val cuentas = listOf(
                Triple("Supervisor", "supervisor@tacticaldev.cl", "Super1234"),
                Triple("Técnico", "tecnico@tacticaldev.cl", "Opera1234"),
                Triple("Vendedor", "vendedor@tacticaldev.cl", "Opera1234"),
                Triple("Cliente", "cliente@minimarket.cl", "Cliente1234")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                cuentas.forEach { (rol, email, pass) ->
                    AssistChip(onClick = { onElegir(email, pass) }, label = { Text(rol, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
    }
}
