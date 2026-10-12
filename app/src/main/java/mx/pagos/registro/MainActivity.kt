package mx.pagos.registro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.load(this)
        setContent { AppTheme { Root(tick.intValue) } }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
    }
}

@Composable
private fun Root(tick: Int) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Resumen" to "◔", "Actividad" to "☰", "Gesto" to "⚙")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { i, (label, glyph) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(glyph, fontSize = 20.sp) },
                        label = { Text(label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab != 2) {
                FloatingActionButton(
                    onClick = { ctx.startActivity(Intent(ctx, QuickCaptureActivity::class.java)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) { Text("+", fontSize = 28.sp, color = MaterialTheme.colorScheme.onPrimary) }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> SummaryScreen()
                1 -> ActivityScreen()
                else -> GestureScreen(tick)
            }
        }
    }
}

@Composable
private fun SummaryScreen() {
    val items = Store.items
    val current = totalsByCategory(items, 0)
    val total = current.sumOf { it.second }
    val previous = totalsByCategory(items, -1).sumOf { it.second }
    val delta = if (previous > 0) {
        val pct = ((total - previous) / previous * 100).roundToInt()
        (if (pct >= 0) "+" else "") + "$pct% frente al mes anterior"
    } else "Sin datos del mes anterior"
    val month = SimpleDateFormat("LLLL yyyy", Locale("es", "MX")).format(Date())
        .replaceFirstChar { it.uppercase() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(month, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)

        Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
            val track = MaterialTheme.colorScheme.surfaceVariant
            Canvas(Modifier.size(250.dp)) {
                val stroke = 30.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                if (total > 0) {
                    var start = -90f
                    val gap = if (current.size > 1) 2f else 0f
                    current.forEach { (name, value) ->
                        val sweep = (value / total * 360).toFloat()
                        drawArc(
                            categoryColor(name), start, (sweep - gap).coerceAtLeast(0.5f), false,
                            topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt),
                        )
                        start += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Gastado", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(money(total), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    delta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
            }
        }

        if (current.isEmpty()) {
            Text(
                "Aún no hay gastos este mes. Toca + o usa el gesto del botón de volumen.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        current.forEach { (name, value) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.dp).background(categoryColor(name), CircleShape))
                Spacer(Modifier.size(12.dp))
                Text(name, color = Color.White, modifier = Modifier.weight(1f))
                Text(
                    "${(value / total * 100).roundToInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.size(12.dp))
                Text(money(value), color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
                Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun ActivityScreen() {
    val ctx = LocalContext.current
    val items = Store.items
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale("es", "MX")) }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Sin movimientos. Los gastos que registres aparecerán aquí.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 20.dp, 20.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.dp).background(categoryColor(e.category), CircleShape))
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.note.ifEmpty { e.category }, color = Color.White, maxLines = 1)
                    Text(
                        e.category + " · " + fmt.format(Date(e.ts)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                Text(money(e.amount), color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(Modifier.size(12.dp))
                Text(
                    "✕",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { Store.remove(ctx, e.id) }.padding(4.dp),
                )
            }
        }
    }
}

private fun serviceEnabled(ctx: Context): Boolean {
    val enabled = Settings.Secure.getString(
        ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.contains(ctx.packageName) && enabled.contains("VolumeGestureService")
}
@Composable
private fun GestureScreen(tick: Int) {
    val ctx = LocalContext.current
    val active = remember(tick) { serviceEnabled(ctx) }
    var button by remember(tick) { mutableIntStateOf(Prefs.button(ctx)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Gesto del botón de volumen", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (active) "Estado: activo" else "Estado: inactivo",
                color = if (active) Color(0xFF2ED3B7) else Color(0xFFFFC857),
                fontWeight = FontWeight.Medium,
            )
            Text(
                "1. Abre los ajustes de accesibilidad.\n" +
                    "2. Entra a Apps instaladas (o Servicios descargados).\n" +
                    "3. Elige Pagos y actívalo. No lee la pantalla; solo detecta el botón de volumen.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                Text("Abrir accesibilidad")
            }
        }

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Doble pulsación de", color = Color.White, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Volumen +", "Volumen −", "Cualquiera").forEachIndexed { i, label ->
                    FilterChip(
                        selected = button == i,
                        onClick = { button = i; Prefs.setButton(ctx, i) },
                        label = { Text(label) },
                    )
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Ajustes para Xiaomi (HyperOS)", color = Color.White, fontWeight = FontWeight.Medium)
            Text(
                "Si el gesto deja de responder, el sistema cerró la app. Revisa:\n" +
                    "• Info de la app → Inicio automático: activado.\n" +
                    "• Info de la app → Ahorro de batería: Sin restricciones.\n" +
                    "• Pantalla de apps recientes: mantén presionada la tarjeta de Pagos y fíjala con el candado.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            OutlinedButton(onClick = {
                ctx.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
                )
            }) { Text("Abrir info de la app") }
        }

        OutlinedButton(onClick = { ctx.startActivity(Intent(ctx, QuickCaptureActivity::class.java)) }) {
            Text("Probar registro rápido")
        }
        Spacer(Modifier.height(24.dp))
    }
}
