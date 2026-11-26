@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.cliente

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.tacticaldev.ui.components.BarraVolver
import cl.duoc.tacticaldev.ui.components.Cargando
import cl.duoc.tacticaldev.ui.components.MensajesSnackbar
import cl.duoc.tacticaldev.ui.components.Selector
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.data.model.TipoEquipo
import cl.duoc.tacticaldev.util.ImageUtils
import cl.duoc.tacticaldev.viewmodel.NuevaSolicitudViewModel
import coil.compose.AsyncImage

@Composable
fun NuevaSolicitudScreen(clienteId: Long, onCreada: () -> Unit, onVolver: () -> Unit) {
    val vm = tacticalViewModel { NuevaSolicitudViewModel(it.solicitudRepository, it.catalogoRepository, it.imageEncoder, clienteId) }
    val s by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    MensajesSnackbar(snackbar, s.error, null) {}
    LaunchedEffect(s.creada) { if (s.creada) onCreada() }

    // ---- Recursos nativos: galería y cámara (foto opcional de la falla) ----
    var uriCamara by rememberSaveable { mutableStateOf<String?>(null) }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) vm.onFoto(uri.toString())
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) vm.onFoto(uriCamara)
    }
    val permisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            val u = ImageUtils.crearUriTemporal(context)
            uriCamara = u.toString()
            camara.launch(u)
        }
    }

    Scaffold(topBar = { BarraVolver("Nueva solicitud", onVolver) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        if (s.cargando) {
            Box(Modifier.padding(padding)) { Cargando() }
            return@Scaffold
        }
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("¿Qué necesitas?", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    vm.tiposDisponibles.forEachIndexed { i, tipo ->
                        SegmentedButton(
                            selected = s.tipo == tipo,
                            onClick = { vm.seleccionarTipo(tipo) },
                            shape = SegmentedButtonDefaults.itemShape(i, vm.tiposDisponibles.size)
                        ) { Text(tipo.etiqueta, style = MaterialTheme.typography.labelMedium) }
                    }
                }

                Selector(
                    etiqueta = "Punto de venta",
                    opciones = s.puntos,
                    seleccionado = s.puntos.firstOrNull { it.id == s.puntoId },
                    texto = { "${it.nombre} · ${it.direccion}" },
                    onSeleccion = { vm.seleccionarPunto(it.id) },
                    error = s.errores.punto
                )
                if (s.puntos.isEmpty()) {
                    Text(
                        "Aún no tienes puntos de venta registrados. Pide al supervisor de TacticalDEV que registre tu local.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall
                    )
                }

                if (s.requiereEquipo) {
                    Selector(
                        etiqueta = "Equipo",
                        opciones = s.equiposDelPunto,
                        seleccionado = s.equiposDelPunto.firstOrNull { it.id == s.equipoId },
                        texto = { "${it.codigo} · ${it.tipo.etiqueta} (${it.estado.etiqueta})" },
                        onSeleccion = { vm.seleccionarEquipo(it.id) },
                        error = s.errores.equipo,
                        habilitado = s.puntoId != null
                    )
                } else {
                    Selector(
                        etiqueta = "Tipo de equipo que necesita",
                        opciones = TipoEquipo.entries,
                        seleccionado = s.tipoEquipo,
                        texto = { it.etiqueta },
                        onSeleccion = vm::seleccionarTipoEquipo,
                        error = s.errores.tipoEquipo
                    )
                }

                OutlinedTextField(
                    value = s.descripcion,
                    onValueChange = vm::onDescripcion,
                    label = { Text("Describa el problema o la necesidad") },
                    isError = s.errores.descripcion != null,
                    supportingText = { Text(s.errores.descripcion ?: "${s.descripcion.length}/500") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Foto (opcional)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { permisoCamara.launch(Manifest.permission.CAMERA) }) {
                        Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("Cámara")
                    }
                    OutlinedButton(onClick = { galeria.launch("image/*") }) {
                        Icon(Icons.Filled.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("Galería")
                    }
                }
                AnimatedVisibility(visible = s.fotoUri != null) {
                    Box {
                        AsyncImage(
                            model = s.fotoUri,
                            contentDescription = "Foto adjunta",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))
                        )
                        FilledTonalIconButton(onClick = { vm.onFoto(null) }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                            Icon(Icons.Filled.Close, "Quitar foto")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::enviar, enabled = !s.enviando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if (s.enviando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Enviar solicitud")
                }
            }
        }
    }
}
