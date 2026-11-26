package cl.duoc.tacticaldev.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta "cadena de frío": azul profundo + celeste hielo
val AzulTactical = Color(0xFF0B3D91)
val Hielo = Color(0xFF7FD3FF)
val VerdeOk = Color(0xFF2E7D32)
val RojoAlerta = Color(0xFFC62828)
val Ambar = Color(0xFFEF8F00)
val GrisNeutro = Color(0xFF616161)

private val Claro = lightColorScheme(
    primary = AzulTactical,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001945),
    secondary = Color(0xFF00658E),
    secondaryContainer = Color(0xFFC7E7FF),
    tertiary = Color(0xFF6B5778),
    error = RojoAlerta,
    background = Color(0xFFF8F9FF),
    surface = Color(0xFFF8F9FF)
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFB0C6FF),
    onPrimary = Color(0xFF002C70),
    primaryContainer = Color(0xFF0B3D91),
    secondary = Hielo,
    error = Color(0xFFFFB4AB)
)

@Composable
fun TacticalTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) Oscuro else Claro,
        typography = Typography(),
        content = content
    )
}
