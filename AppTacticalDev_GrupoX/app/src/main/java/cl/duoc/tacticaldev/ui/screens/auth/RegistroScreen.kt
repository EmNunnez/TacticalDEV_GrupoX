@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.tacticaldev.ui.components.BarraVolver
import cl.duoc.tacticaldev.ui.components.MensajesSnackbar
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.viewmodel.RegistroViewModel

/** Registro de clientes. Al terminar inicia sesión y la navegación lo lleva a su inicio. */
@Composable
fun RegistroScreen(onVolver: () -> Unit) {
    val vm = tacticalViewModel { RegistroViewModel(it.authRepository) }
    val s by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MensajesSnackbar(snackbar, s.error, null) {}

    Scaffold(
        topBar = { BarraVolver("Crear cuenta de cliente", onVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Registra tu minimarket para solicitar equipos nuevos, reparaciones o retiros.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Campo("Nombre del local o responsable", s.nombre, vm::onNombre, s.errores.nombre)
                Campo("Correo", s.email, vm::onEmail, s.errores.email, KeyboardType.Email)
                Campo("Teléfono (opcional)", s.telefono, vm::onTelefono, s.errores.telefono, KeyboardType.Phone)
                Campo("Contraseña", s.password, vm::onPassword, s.errores.password, KeyboardType.Password, oculto = true)
                Campo("Confirmar contraseña", s.confirmar, vm::onConfirmar, s.errores.confirmar, KeyboardType.Password, oculto = true)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = s.aceptaTerminos, onCheckedChange = vm::onTerminos)
                    Text("Acepto el uso de cámara y ubicación para verificar las visitas", style = MaterialTheme.typography.bodySmall)
                }
                s.errores.terminos?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                Spacer(Modifier.height(12.dp))
                Button(onClick = vm::registrar, enabled = !s.cargando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if (s.cargando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Crear cuenta")
                }
                Text(
                    "Los operarios y supervisores son creados por TacticalDEV.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    error: String?,
    teclado: KeyboardType = KeyboardType.Text,
    oculto: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (oculto) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}
