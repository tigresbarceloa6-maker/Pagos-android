package mx.pagos.registro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Ui {
    // Fondo: color superior por pantalla que se funde en azul marino.
    val Base = Color(0xFF060912)
    val BaseDeep = Color(0xFF020306)
    val TopActivity = Color(0xFF1D52AE)
    val TopSummary = Color(0xFF33466C)
    val TopBudget = Color(0xFF1E4A62)
    val TopAnalysis = Color(0xFF2A3248)
    val TopAccounts = Color(0xFF263B66)

    val Glass = Color(0x12FFFFFF)
    val GlassStrong = Color(0x1FFFFFFF)
    val GlassBorder = Color(0x14FFFFFF)
    val PillBg = Color(0x1AFFFFFF)
    val Menu = Color(0xFF141826)

    val TextMuted = Color(0xFFA39E93)
    val TextSoft = Color(0xCCF2E8D8)

    // Blanco beige para textos, iconos y barras.
    val Cream = Color(0xFFF2E8D8)
    val CreamSoft = Color(0xCCF2E8D8)
    val CreamDim = Color(0x80F2E8D8)
    val CreamFaint = Color(0x59F2E8D8)
    val Accent = Color(0xFFA9B4FF)
    val Blue = Color(0xFF0A84FF)
    val CancelRed = Color(0xFFE5484D)
    val OkGreen = Color(0xFF2FB872)
    val Good = Color(0xFF3FE0C5)
    val Bad = Color(0xFFFF6B81)
    val IncomeTeal = Color(0xFF2EE6A8)
    val ExpenseBlue = Color(0xFF3B8BFF)

    val NavBg = Color(0xEB0E111B)
    val AlertBg = Color(0xF2181B24)
    val KeyBg = Color(0x40FFFFFF)
    val KeyboardBg = Color(0xF50E1016)
}

val AppFont = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private fun Typography.withFont(f: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

fun Modifier.glass(radius: Dp = 20.dp): Modifier =
    this.background(Ui.Glass, RoundedCornerShape(radius))
        .border(1.dp, Ui.GlassBorder, RoundedCornerShape(radius))

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ui.Accent,
            onPrimary = Color(0xFF0B1126),
            background = Ui.Base,
            surface = Ui.Menu,
            onSurface = Ui.Cream,
            onBackground = Ui.Cream,
            surfaceVariant = Color(0xFF1D2547),
            onSurfaceVariant = Ui.TextMuted,
        ),
        typography = Typography().withFont(AppFont),
        content = content,
    )
}

/** Fondo degradado cuyo color superior cambia con animación (pestaña o categoría elegida). */
@Composable
fun AppBackground(top: Color, content: @Composable BoxScope.() -> Unit) {
    val t by animateColorAsState(top, tween(550), label = "fondo")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to t,
                    0.4f to lerp(t, Ui.Base, 0.6f),
                    0.75f to Ui.Base,
                    1f to Ui.BaseDeep,
                )
            ),
        content = content,
    )
}
