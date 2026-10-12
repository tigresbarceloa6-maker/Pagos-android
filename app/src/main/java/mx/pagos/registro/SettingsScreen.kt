package mx.pagos.registro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun serviceEnabled(ctx: Context): Boolean {
    val enabled = Settings.Secure.getString(
        ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.contains(ctx.packageName) && enabled.contains("VolumeGestureService")
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().glass(24.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun SettingsScreen(tick: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val active = remember(tick) { serviceEnabled(ctx) }
    var button by remember(tick) { mutableIntStateOf(Prefs.button(ctx)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleButton(R.drawable.ic_back, onBack)
            Spacer(Modifier.width(14.dp))
            Text("Ajustes", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Ui.Blue),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(R.drawable.ic_volume, Color.White, 18.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Registro rápido con volumen",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    DotText(
                        if (active) "Activo" else "Inactivo",
                        if (active) Ui.Good else Color(0xFFFFC857),
                    )
                }
            }
            Text(
                "Pulsa dos veces el botón de volumen en cualquier pantalla, incluso con el teléfono " +
                    "bloqueado, para registrar un gasto al instante.",
                color = Ui.TextMuted,
                fontSize = 13.sp,
            )
            Text(
                "1. Abre los ajustes de accesibilidad.\n" +
                    "2. Entra a Apps instaladas (o Servicios descargados).\n" +
                    "3. Elige Pagos y actívalo. No lee la pantalla; solo detecta el botón de volumen.",
                color = Ui.TextMuted,
                fontSize = 13.sp,
            )
            CapsuleButton("Abrir accesibilidad", Ui.Blue, Modifier.fillMaxWidth()) {
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        SettingsCard {
            Text("Doble pulsación de", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x14FFFFFF))
                    .padding(4.dp),
            ) {
                listOf("Volumen +", "Volumen −", "Cualquiera").forEachIndexed { i, label ->
                    val sel = button == i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) Color(0x38FFFFFF) else Color.Transparent)
                            .clickable {
                                button = i
                                Prefs.setButton(ctx, i)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (sel) Color.White else Ui.TextSoft,
                            fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }

        SettingsCard {
            Text("Ajustes para Xiaomi (HyperOS)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                "Si el gesto deja de responder, el sistema cerró la app. Revisa:\n" +
                    "• Info de la app → Inicio automático: activado.\n" +
                    "• Info de la app → Ahorro de batería: Sin restricciones.\n" +
                    "• Pantalla de apps recientes: mantén presionada la tarjeta de Pagos y fíjala con el candado.",
                color = Ui.TextMuted,
                fontSize = 13.sp,
            )
            CapsuleButton("Abrir info de la app", Color(0x24FFFFFF), Modifier.fillMaxWidth()) {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + ctx.packageName),
                    )
                )
            }
        }

        CapsuleButton("Probar registro rápido", Color(0x24FFFFFF), Modifier.fillMaxWidth()) {
            ctx.startActivity(Intent(ctx, QuickCaptureActivity::class.java))
        }
    }
}
