@file:OptIn(ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duoc.tacticaldev.data.model.Clima
import cl.duoc.tacticaldev.data.model.EstadoEquipo
import cl.duoc.tacticaldev.data.model.EstadoSolicitud
import cl.duoc.tacticaldev.data.model.EstadoValidacion
import cl.duoc.tacticaldev.ui.theme.*
import cl.duoc.tacticaldev.util.ImageUtils

// ---------- Colores semánticos por estado ----------

fun colorSolicitud(e: EstadoSolicitud): Color = when (e) {
    EstadoSolicitud.PENDIENTE -> Ambar
    EstadoSolicitud.ASIGNADA -> AzulTactical
    EstadoSolicitud.COMPLETADA -> Color(0xFF6A1B9A)
    EstadoSolicitud.VALIDADA -> VerdeOk
}

fun colorValidacion(e: EstadoValidacion): Color = when (e) {
    EstadoValidacion.PENDIENTE -> Ambar
    EstadoValidacion.SOSPECHOSA -> RojoAlerta
    EstadoValidacion.VALIDADA -> VerdeOk
    EstadoValidacion.RECHAZADA -> GrisNeutro
}

fun colorEquipo(e: EstadoEquipo): Color = when (e) {
    EstadoEquipo.OPERATIVO -> VerdeOk
    EstadoEquipo.CON_FALLA -> RojoAlerta
    EstadoEquipo.EN_REPARACION -> Ambar
    EstadoEquipo.RETIRADO -> GrisNeutro
}

/** Etiqueta de estado con transición animada de color. */
@Composable
fun EstadoChip(texto: String, color: Color, modifier: Modifier = Modifier) {
    val animado by animateColorAsState(color, animationSpec = tween(500), label = "colorChip")
    Surface(
        modifier = modifier,
        color = animado.copy(alpha = 0.15f),
        contentColor = animado,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun Cargando(mensaje: String = "Cargando...") {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Vacio(mensaje: String, icono: ImageVector = Icons.Filled.Inbox) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icono, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        Text(mensaje, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
    }
}

/** Fila "icono  etiqueta: valor" para las tarjetas de detalle. */
@Composable
fun Dato(icono: ImageVector, etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icono, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("$etiqueta: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ImagenBase64(base64: String?, modifier: Modifier = Modifier) {
    val imagen = remember(base64) { ImageUtils.base64AImagen(base64) }
    if (imagen != null) {
        Image(
            bitmap = imagen,
            contentDescription = "Foto de evidencia",
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.HideImage, "Sin foto") }
    }
}

/** Tarjeta con el dato de la API externa (Open-Meteo). */
@Composable
fun ClimaCard(clima: Clima?, noDisponible: Boolean) {
    val fondo = when {
        clima?.riesgoAltaTemperatura == true -> RojoAlerta.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = fondo), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Thermostat, null)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Clima en el punto de venta (Open-Meteo)", style = MaterialTheme.typography.labelMedium)
                when {
                    clima != null -> {
                        Text(
                            "${clima.temperatura} °C · ${clima.descripcion}" + (clima.humedad?.let { " · Humedad $it%" } ?: ""),
                            fontWeight = FontWeight.Bold
                        )
                        if (clima.riesgoAltaTemperatura) {
                            Text("Temperatura alta: revise el sello y el compresor", color = RojoAlerta,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    noDisponible -> Text("No disponible (sin conexión)", style = MaterialTheme.typography.bodySmall)
                    else -> Text("Se obtiene al tomar la foto", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Selector desplegable genérico (Material 3). */
@Composable
fun <T> Selector(
    etiqueta: String,
    opciones: List<T>,
    seleccionado: T?,
    texto: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    habilitado: Boolean = true
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = abierto && habilitado,
        onExpandedChange = { if (habilitado) abierto = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = seleccionado?.let(texto) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text(etiqueta) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = abierto && habilitado, onDismissRequest = { abierto = false }) {
            if (opciones.isEmpty()) {
                DropdownMenuItem(text = { Text("Sin opciones disponibles") }, onClick = { abierto = false }, enabled = false)
            }
            opciones.forEach { o ->
                DropdownMenuItem(text = { Text(texto(o)) }, onClick = {
                    onSeleccion(o)
                    abierto = false
                })
            }
        }
    }
}

/** Muestra error/mensaje del ViewModel en un Snackbar y luego lo limpia. */
@Composable
fun MensajesSnackbar(host: SnackbarHostState, error: String?, mensaje: String?, onMostrado: () -> Unit) {
    LaunchedEffect(error, mensaje) {
        val texto = error ?: mensaje ?: return@LaunchedEffect
        host.showSnackbar(texto, withDismissAction = true)
        onMostrado()
    }
}

@Composable
fun ConfirmarDialog(titulo: String, texto: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** Tarjeta pequeña de indicador para el panel del supervisor. */
@Composable
fun Indicador(valor: Int, etiqueta: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$valor", style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
            Text(etiqueta, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}
