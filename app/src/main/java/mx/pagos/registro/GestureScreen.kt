package mx.pagos.registro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
fun GestureScreen(tick: Int) {
    val ctx = LocalContext.current
    val active = remember(tick) { serviceEnabled(ctx) }
    var button by remember(tick) { mutableIntStateOf(Prefs.button(ctx)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Gesto del botón de volumen",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Column(
            Modifier.fillMaxWidth().glass(24.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (active) "Estado: activo" else "Estado: inactivo",
                color = if (active) Ui.Good else Color(0xFFFFC857),
                fontWeight = FontWeight.Medium,
            )
            Text(
                "1. Abre los ajustes de accesibilidad.\n" +
                    "2. Entra a Apps instaladas (o Servicios descargados).\n" +
                    "3. Elige Pagos y actívalo. No lee la pantalla; solo detecta el botón de volumen.",
                color = Ui.TextMuted,
                fontSize = 13.sp,
            )
            Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                Text("Abrir accesibilidad")
            }
        }

        Column(
            Modifier.fillMaxWidth().glass(24.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Doble pulsación de", color = Color.White, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Volumen +", "Volumen −", "Cualquiera").forEachIndexed { i, label ->
                    FilterChip(
                        selected = button == i,
                        onClick = {
                            button = i
                            Prefs.setButton(ctx, i)
                        },
                        label = { Text(label) },
                    )
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().glass(24.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Ajustes para Xiaomi (HyperOS)", color = Color.White, fontWeight = FontWeight.Medium)
            Text(
                "Si el gesto deja de responder, el sistema cerró la app. Revisa:\n" +
                    "• Info de la app → Inicio automático: activado.\n" +
                    "• Info de la app → Ahorro de batería: Sin restricciones.\n" +
                    "• Pantalla de apps recientes: mantén presionada la tarjeta de Pagos y fíjala con el candado.",
                color = Ui.TextMuted,
                fontSize = 13.sp,
            )
            OutlinedButton(onClick = {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + ctx.packageName),
                    )
                )
            }) { Text("Abrir info de la app") }
        }

        OutlinedButton(onClick = { ctx.startActivity(Intent(ctx, QuickCaptureActivity::class.java)) }) {
            Text("Probar registro rápido")
        }
    }
}
