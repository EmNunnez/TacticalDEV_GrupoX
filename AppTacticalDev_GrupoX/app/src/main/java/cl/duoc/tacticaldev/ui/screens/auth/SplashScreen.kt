@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.duoc.tacticaldev.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Se muestra mientras DataStore lee la sesión guardada. */
@Composable
fun SplashScreen() {
    val pulso = rememberInfiniteTransition(label = "pulso")
    val escala by pulso.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escala"
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.AcUnit, null, Modifier.size(96.dp).scale(escala), tint = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("TacticalDEV", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text("Trazabilidad de visitas en terreno", color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator(color = Color.White)
    }
}
