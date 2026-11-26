package cl.duoc.tacticaldev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.duoc.tacticaldev.ui.navigation.AppNavigation
import cl.duoc.tacticaldev.ui.theme.TacticalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TacticalTheme {
                AppNavigation()
            }
        }
    }
}
