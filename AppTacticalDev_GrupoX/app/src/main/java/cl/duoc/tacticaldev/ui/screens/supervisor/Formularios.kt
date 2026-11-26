@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.supervisor

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.ui.components.BarraVolver
import cl.duoc.tacticaldev.ui.components.MensajesSnackbar
import cl.duoc.tacticaldev.ui.components.Selector
import cl.duoc.tacticaldev.ui.tacticalViewModel
import cl.duoc.tacticaldev.viewmodel.GestionUiState
import cl.duoc.tacticaldev.viewmodel.GestionViewModel

@Composable
private fun gestionVm(clave: String): GestionViewModel = tacticalViewModel(key = clave) {
    GestionViewModel(it.catalogoRepository, it.usuarioRepository, it.solicitudRepository, it.locationProvider)
}

/** Estructura común de los 3 formularios (Scaffold + scroll + botón guardar). */
@Composable
private fun FormularioBase(
    titulo: String,
    g: GestionUiState,
    onVolver: () -> Unit,
    onGuardar: () -> Unit,
    onGuardado: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    MensajesSnackbar(snackbar, g.error, null) {}
    LaunchedEffect(g.guardado) { if (g.guardado) onGuardado() }
    Scaffold(topBar = { BarraVolver(titulo, onVolver) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                contenido()
                Spacer(Modifier.height(12.dp))
                Button(onClick = onGuardar, enabled = !g.guardando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if (g.guardando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text("Guardar")
                }
            }
        }
    }
}

@Composable
private fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    error: String?,
    teclado: KeyboardType = KeyboardType.Text,
    oculto: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = valor, onValueChange = onCambio, label = { Text(etiqueta) },
        isError = error != null, supportingText = error?.let { { Text(it) } }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        visualTransformation = if (oculto) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = modifier
    )
}

// ============================ PUNTO DE VENTA ============================

@Composable
fun FormPuntoScreen(id: Long?, onGuardado: () -> Unit, onVolver: () -> Unit) {
    val vm = gestionVm("form-punto-${id ?: 0}")
    val g by vm.estado.collectAsStateWithLifecycle()
    val f = g.puntoForm
    val e = g.erroresForm
    LaunchedEffect(g.puntos) { if (id != null) vm.prepararPunto(id) }

    val permisoGps = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.usarUbicacionActual()
    }

    FormularioBase(
        titulo = if (id == null) "Nuevo punto de venta" else "Editar punto de venta",
        g = g, onVolver = onVolver, onGuardar = { vm.guardarPunto(id, f) }, onGuardado = onGuardado
    ) {
        CampoTexto("Nombre del local", f.nombre, { vm.onPuntoForm(f.copy(nombre = it)) }, e["nombre"])
        CampoTexto("Dirección", f.direccion, { vm.onPuntoForm(f.copy(direccion = it)) }, e["direccion"])
        CampoTexto("Comuna", f.comuna, { vm.onPuntoForm(f.copy(comuna = it)) }, e["comuna"])
        Selector(
            etiqueta = "Cliente dueño del local",
            opciones = g.clientes,
            seleccionado = g.clientes.firstOrNull { it.id == f.clienteId },
            texto = { "${it.nombre} (${it.email})" },
            onSeleccion = { vm.onPuntoForm(f.copy(clienteId = it.id)) },
            error = e["cliente"]
        )
        Text("Ubicación para verificar las visitas", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoTexto("Latitud", f.latitud, { vm.onPuntoForm(f.copy(latitud = it)) }, e["latitud"],
                KeyboardType.Decimal, modifier = Modifier.weight(1f))
            CampoTexto("Longitud", f.longitud, { vm.onPuntoForm(f.copy(longitud = it)) }, e["longitud"],
                KeyboardType.Decimal, modifier = Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = { permisoGps.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            enabled = !g.obteniendoUbicacion
        ) {
            if (g.obteniendoUbicacion) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Filled.MyLocation, null)
            Spacer(Modifier.width(8.dp))
            Text("Usar mi ubicación actual")
        }
        CampoTexto("Radio permitido (metros)", f.radio, { vm.onPuntoForm(f.copy(radio = it)) }, e["radio"], KeyboardType.Number)
        Text("El operario debe tomar la foto a menos de esta distancia del local.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

// ============================ EQUIPO ============================

@Composable
fun FormEquipoScreen(id: Long?, onGuardado: () -> Unit, onVolver: () -> Unit) {
    val vm = gestionVm("form-equipo-${id ?: 0}")
    val g by vm.estado.collectAsStateWithLifecycle()
    val f = g.equipoForm
    val e = g.erroresForm
    LaunchedEffect(g.equipos) { if (id != null) vm.prepararEquipo(id) }

    FormularioBase(
        titulo = if (id == null) "Nuevo equipo" else "Editar equipo",
        g = g, onVolver = onVolver, onGuardar = { vm.guardarEquipo(id, f) }, onGuardado = onGuardado
    ) {
        CampoTexto("Código (ej. VF-0007)", f.codigo, { vm.onEquipoForm(f.copy(codigo = it.uppercase())) }, e["codigo"])
        Selector("Tipo", TipoEquipo.entries, f.tipo, { it.etiqueta }, { vm.onEquipoForm(f.copy(tipo = it)) }, error = e["tipo"])
        CampoTexto("Marca", f.marca, { vm.onEquipoForm(f.copy(marca = it)) }, e["marca"])
        CampoTexto("Modelo", f.modelo, { vm.onEquipoForm(f.copy(modelo = it)) }, e["modelo"])
        Selector("Estado", EstadoEquipo.entries, f.estado, { it.etiqueta }, { vm.onEquipoForm(f.copy(estado = it)) })
        // Opción "null" = bodega
        val opciones: List<Pair<Long?, String>> =
            listOf<Pair<Long?, String>>(null to "En bodega (sin instalar)") + g.puntos.map { it.id to it.nombre }
        Selector(
            etiqueta = "Ubicación",
            opciones = opciones,
            seleccionado = opciones.firstOrNull { it.first == f.puntoVentaId },
            texto = { it.second },
            onSeleccion = { vm.onEquipoForm(f.copy(puntoVentaId = it.first)) },
            error = e["punto"]
        )
    }
}

// ============================ USUARIO ============================

@Composable
fun FormUsuarioScreen(id: Long?, onGuardado: () -> Unit, onVolver: () -> Unit) {
    val vm = gestionVm("form-usuario-${id ?: 0}")
    val g by vm.estado.collectAsStateWithLifecycle()
    val f = g.usuarioForm
    val e = g.erroresForm
    LaunchedEffect(g.usuarios) { if (id != null) vm.prepararUsuario(id) }

    FormularioBase(
        titulo = if (id == null) "Nuevo usuario" else "Editar usuario",
        g = g, onVolver = onVolver, onGuardar = { vm.guardarUsuario(id, f) }, onGuardado = onGuardado
    ) {
        CampoTexto("Nombre", f.nombre, { vm.onUsuarioForm(f.copy(nombre = it)) }, e["nombre"])
        CampoTexto("Correo", f.email, { vm.onUsuarioForm(f.copy(email = it)) }, e["email"], KeyboardType.Email)
        CampoTexto("Teléfono (opcional)", f.telefono, { vm.onUsuarioForm(f.copy(telefono = it)) }, e["telefono"], KeyboardType.Phone)
        CampoTexto(
            if (id == null) "Contraseña" else "Nueva contraseña (vacío = no cambiar)",
            f.password, { vm.onUsuarioForm(f.copy(password = it)) }, e["password"], KeyboardType.Password, oculto = true
        )
        Selector("Rol", Rol.entries, f.rol, { it.etiqueta }, { vm.onUsuarioForm(f.copy(rol = it)) }, error = e["rol"])
        if (f.rol == Rol.OPERARIO) {
            Selector("Tipo de operario", TipoOperario.entries, f.tipoOperario, { it.etiqueta },
                { vm.onUsuarioForm(f.copy(tipoOperario = it)) }, error = e["tipoOperario"])
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = f.activo, onCheckedChange = { vm.onUsuarioForm(f.copy(activo = it)) })
            Spacer(Modifier.width(8.dp))
            Text(if (f.activo) "Usuario activo" else "Usuario desactivado (no puede ingresar)")
        }
    }
}
