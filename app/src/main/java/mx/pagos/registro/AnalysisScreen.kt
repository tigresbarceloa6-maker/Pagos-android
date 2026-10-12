package mx.pagos.registro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnalysisScreen() {
    val items = Store.items
    val offsets = (-5..0).toList()
    val totals = offsets.map { off -> totalsByCategory(items, off).sumOf { it.second } }
    val labels = offsets.map { monthName(it).take(3) }
    val maxTotal = totals.maxOrNull() ?: 0.0
    val maxV = if (maxTotal > 0) maxTotal else 1.0
    val withData = totals.filter { it > 0 }
    val average = if (withData.isEmpty()) 0.0 else withData.sum() / withData.size
    val top = totalsByCategory(items, 0).firstOrNull()
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Gastos por mes", color = Ui.TextMuted, fontSize = 13.sp)
        Text(
            money(totals.last()),
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Column(Modifier.fillMaxWidth().glass(24.dp).padding(16.dp)) {
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                val slot = size.width / totals.size
                val barW = slot * 0.42f
                val g = grow.value
                totals.forEachIndexed { i, v ->
                    val full = if (v > 0) {
                        (v / maxV * size.height).toFloat().coerceAtLeast(6.dp.toPx())
                    } else {
                        4.dp.toPx()
                    }
                    val h = full * g
                    val x = (i + 0.5f) * slot
                    val top = size.height - h
                    val brush = if (v > 0) {
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF2DD4BF), Color(0xFF3B82F6)),
                            startY = top,
                            endY = size.height,
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color(0x22FFFFFF), Color(0x22FFFFFF)),
                            startY = top,
                            endY = size.height,
                        )
                    }
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x - barW / 2, top),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2, barW / 2),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                labels.forEachIndexed { i, label ->
                    val last = i == labels.lastIndex
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .background(
                                    if (last) Color(0x33FFFFFF) else Color.Transparent,
                                    RoundedCornerShape(50),
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                label,
                                color = if (last) Color.White else Ui.TextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Promedio mensual", money(average), Modifier.weight(1f))
            StatCard(
                "Mayor gasto del mes",
                if (top != null) top.first else "Sin datos",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.glass(20.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = Ui.TextMuted, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}
