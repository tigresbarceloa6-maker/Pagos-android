package mx.pagos.registro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Ui {
    val BgTop = Color(0xFF121B3D)
    val BgMid = Color(0xFF0B1126)
    val BgBottom = Color(0xFF070A14)
    val Glass = Color(0x14FFFFFF)
    val GlassBorder = Color(0x1FFFFFFF)
    val TextMuted = Color(0xFF8D97B8)
    val Accent = Color(0xFFA9B4FF)
    val Good = Color(0xFF34D399)
    val Bad = Color(0xFFFF7A7A)
}

fun Modifier.glass(radius: Dp = 20.dp): Modifier =
    this.background(Ui.Glass, RoundedCornerShape(radius))
        .border(1.dp, Ui.GlassBorder, RoundedCornerShape(radius))

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ui.Accent,
            onPrimary = Color(0xFF0B1126),
            background = Ui.BgBottom,
            surface = Color(0xFF151C38),
            surfaceVariant = Color(0xFF1D2547),
            onSurfaceVariant = Ui.TextMuted,
        ),
        content = content,
    )
}

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ui.BgTop, Ui.BgMid, Ui.BgBottom))),
        content = content,
    )
}
