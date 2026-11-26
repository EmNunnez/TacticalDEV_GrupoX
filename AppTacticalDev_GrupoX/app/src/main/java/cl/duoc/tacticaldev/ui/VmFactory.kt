package cl.duoc.tacticaldev.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.tacticaldev.AppContainer
import cl.duoc.tacticaldev.TacticalApp

/** Crea ViewModels con sus dependencias tomadas del AppContainer. */
@Composable
inline fun <reified VM : ViewModel> tacticalViewModel(
    key: String? = null,
    crossinline crear: (AppContainer) -> VM
): VM {
    val app = LocalContext.current.applicationContext as TacticalApp
    return viewModel(key = key, factory = viewModelFactory { initializer { crear(app.container) } })
}
