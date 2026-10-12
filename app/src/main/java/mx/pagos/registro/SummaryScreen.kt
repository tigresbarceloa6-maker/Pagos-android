package mx.pagos.registro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun Donut(totals: List<Pair<String, Double>>, selected: String?, modifier: Modifier = Modifier) {
    val total = totals.sumOf { it.second }
    Canvas(modifier) {
        val stroke = 26.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(Color(0x14FFFFFF), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        if (total > 0) {
            val gap = if (totals.size > 1) 3f else 0f
            var start = -90f
            totals.forEach { (name, value) ->
                val sweep = (value / total * 360).toFloat()
                val alpha = if (selected == null || selected == name) 1f else 0.22f
                drawArc(
                    color = categoryColor(name).copy(alpha = alpha),
                    startAngle = start + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke),
                )
                start += sweep
            }
        }
    }
}

@Composable
fun SummaryScreen() {
    val items = Store.items
    val current = totalsByCategory(items, 0)
    val total = current.sumOf { it.second }
    val counts = countsByCategory(items, 0)
    val prevTotals = totalsByCategory(items, -1)
    var selected by remember { mutableStateOf<String?>(null) }
    val sel: String? = selected?.takeIf { s -> current.any { it.first == s } }

    val shownTotal = if (sel != null) current.first { it.first == sel }.second else total
    val shownPrev = if (sel != null) {
        prevTotals.firstOrNull { it.first == sel }?.second ?: 0.0
    } else {
        prevTotals.sumOf { it.second }
    }
    val prevName = monthName(-1).take(3)
    val pct: Int? = if (shownPrev > 0) ((shownTotal - shownPrev) / shownPrev * 100).roundToInt() else null
    val deltaText = if (pct != null) {
        (if (pct >= 0) "+" else "") + pct.toString() + "% frente a " + prevName
    } else {
        "Sin datos de " + prevName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
            Donut(current, sel, Modifier.fillMaxSize())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(sel ?: monthName(0), color = Ui.TextMuted, fontSize = 13.sp)
                Text(
                    money(shownTotal),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                DeltaPill(deltaText, pct == null || pct <= 0)
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Pill("Gastos", true)
            Pill("Mensual")
            Pill("Todas las cuentas")
        }

        if (current.isEmpty()) {
            Text(
                "Aún no hay gastos este mes. Toca + o usa la doble pulsación del botón de volumen.",
                color = Ui.TextMuted,
                textAlign = TextAlign.Center,
            )
        } else {
            Column(Modifier.fillMaxWidth().glass(24.dp).padding(vertical = 6.dp)) {
                current.forEach { (name, value) ->
                    CategoryRow(
                        name = name,
                        value = value,
                        total = total,
                        count = counts[name] ?: 0,
                        selected = sel == name,
                        dim = sel != null && sel != name,
                        onClick = { selected = if (sel == name) null else name },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    name: String,
    value: Double,
    total: Double,
    count: Int,
    selected: Boolean,
    dim: Boolean,
    onClick: () -> Unit,
) {
    val share = if (total > 0) value / total * 100 else 0.0
    val shareText = ((share * 10).roundToInt() / 10.0).toString() + "%"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dim) 0.5f else 1f)
            .background(if (selected) Color(0x14FFFFFF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(name)
        Spacer(Modifier.width(12.dp))
        Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .background(Color(0x14FFFFFF), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(count.toString(), color = Ui.TextMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(money(value), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(shareText, color = categoryColor(name), fontSize = 11.sp)
        }
    }
}
