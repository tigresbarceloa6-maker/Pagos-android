package mx.pagos.registro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7C8CFF),
            onPrimary = Color(0xFF0B0E1A),
            background = Color(0xFF0B0E1A),
            surface = Color(0xFF151A2E),
            surfaceVariant = Color(0xFF1E2440),
            onSurfaceVariant = Color(0xFFA9B1D0),
        ),
        content = content,
    )
}
