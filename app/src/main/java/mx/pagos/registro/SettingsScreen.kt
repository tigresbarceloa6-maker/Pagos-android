package mx.pagos.registro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
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

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().glass(24.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Selector de dos o más opciones en forma de cápsula. */
@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0x14FFFFFF))
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val sel = selected == i
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (sel) Color(0x38FFFFFF) else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (sel) Ui.Cream else Ui.TextSoft,
                    fontSize = 13.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(tick: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val canDraw = remember(tick) { Settings.canDrawOverlays(ctx) }
    var enabled by remember(tick) { mutableStateOf(Prefs.handleEnabled(ctx)) }
    var right by remember(tick) { mutableStateOf(Prefs.handleRight(ctx)) }
    val active = enabled && canDraw
    var lockCard by remember(tick) { mutableStateOf(Prefs.lockCard(ctx)) }
    val notifAllowed = remember(tick) {
        Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    var notifGranted by remember(tick) { mutableStateOf(notifAllowed) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifGranted = it
        EdgeHandleService.sync(ctx)
    }

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
            Text("Ajustes", color = Ui.Cream, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Ui.OkGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(R.drawable.ic_notification, Ui.Cream, 18.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Pantalla de bloqueo",
                        color = Ui.Cream,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    DotText(
                        when {
                            lockCard && notifGranted -> "Activa"
                            lockCard -> "Falta el permiso"
                            else -> "Apagada"
                        },
                        if (lockCard && notifGranted) Ui.Good else Color(0xFFFFC857),
                    )
                }
            }
            Text(
                "Muestra la tarjeta \"Registrar gasto\" en la pantalla de bloqueo y en las " +
                    "notificaciones. Al tocarla con el teléfono bloqueado, Android te pide la huella " +
                    "(o tu PIN) y abre el registro.",
                color = Ui.TextMuted,
                fontSize = 13.sp,
            )
            Segmented(listOf("Apagada", "Activa"), if (lockCard) 1 else 0) {
                lockCard = it == 1
                Prefs.setLockCard(ctx, lockCard)
                if (lockCard && !notifGranted && Build.VERSION.SDK_INT >= 33) {
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    EdgeHandleService.sync(ctx)
                }
            }
            if (lockCard && !notifGranted) {
                CapsuleButton("Permitir notificaciones", Ui.Blue, Modifier.fillMaxWidth()) {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    )
                }
            }
            Text(
                "Si no aparece bloqueado: Info de la app → Notificaciones → permite mostrarlas en la " +
                    "pantalla de bloqueo.",
                color = Ui.TextMuted,
                fontSize = 12.sp,
            )
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Ui.Blue),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(R.drawable.ic_gesture, Ui.Cream, 18.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Asa lateral",
                        color = Ui.Cream,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    DotText(
                        when {
                            active -> "Activa"
                            enabled -> "Falta el permiso"
                            else -> "Apagada"
                        },
                        if (active) Ui.Good else Color(0xFFFFC857),
                    )
                }
            }
            Text(
                "Una barrita discreta en el borde de la pantalla, visible sobre cualquier app. " +
                    "Tócala o deslízala hacia adentro para registrar un gasto. Mantenla presionada " +
                    "y arrastra para cambiarla de altura.",
                color = Ui.TextMuted,
                fontSize = 13.sp,
            )
            Segmented(listOf("Apagada", "Activa"), if (enabled) 1 else 0) {
                enabled = it == 1
                Prefs.setHandleEnabled(ctx, enabled)
                EdgeHandleService.sync(ctx)
            }
            if (enabled && !canDraw) {
                Text(
                    "Activa \"Mostrar sobre otras apps\" para Pagos y vuelve a esta pantalla.",
                    color = Ui.TextMuted,
                    fontSize = 13.sp,
                )
                CapsuleButton("Dar permiso", Ui.Blue, Modifier.fillMaxWidth()) {
                    ctx.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + ctx.packageName),
                        )
                    )
                }
            }
            Text("Lado de la pantalla", color = Ui.Cream, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Segmented(listOf("Izquierda", "Derecha"), if (right) 1 else 0) {
                right = it == 1
                Prefs.setHandleRight(ctx, right)
                EdgeHandleService.sync(ctx)
            }
        }

        SettingsCard {
            Text("Otros accesos rápidos", color = Ui.Cream, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                "• Mantén presionado el ícono de Pagos y elige \"Registrar\" (puedes arrastrarlo " +
                    "a la pantalla de inicio).\n" +
                    "• Baja la cortina de notificaciones, edita los mosaicos y agrega \"Registrar gasto\". " +
                    "Funciona bloqueado: te pide la huella.\n" +
                    "• Widget: mantén presionado un espacio vacío de la pantalla de inicio → Widgets → Pagos.",
                color = Ui.TextMuted,
                fontSize = 13.sp,
            )
        }

        SettingsCard {
            Text("Ajustes para Xiaomi (HyperOS)", color = Ui.Cream, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                "Si el asa desaparece, el sistema cerró la app. En Info de la app revisa:\n" +
                    "• Inicio automático: activado.\n" +
                    "• Otros permisos → Mostrar ventanas emergentes en segundo plano: permitido.\n" +
                    "• Ahorro de batería: Sin restricciones.\n" +
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
