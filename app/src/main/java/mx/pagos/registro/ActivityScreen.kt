package mx.pagos.registro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ActivityScreen() {
    val ctx = LocalContext.current
    val items = Store.items
    val total = totalsByCategory(items, 0).sumOf { it.second }
    val prevTotal = totalsByCategory(items, -1).sumOf { it.second }
    val cal = Calendar.getInstance()
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val (start, end) = monthRange(0)

    val daily = DoubleArray(daysInMonth + 1)
    items.forEach { e ->
        if (e.ts in start until end) {
            val c = Calendar.getInstance()
            c.timeInMillis = e.ts
            daily[c.get(Calendar.DAY_OF_MONTH)] += e.amount
        }
    }
    val maxDay = daily.maxOrNull() ?: 0.0
    val avg = if (today > 0) total / today else 0.0

    val pct: Int? = if (prevTotal > 0) ((total - prevTotal) / prevTotal * 100).roundToInt() else null
    val prevName = monthName(-1).take(3)
    val deltaText = if (pct != null) {
        (if (pct >= 0) "+" else "") + pct.toString() + "% frente a " + prevName
    } else {
        "Sin datos de " + prevName
    }

    val groups = items.groupBy { dayStart(it.ts) }
    val fmtTime = SimpleDateFormat("HH:mm", Locale("es", "MX"))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            val grow = remember { Animatable(0f) }
            LaunchedEffect(Unit) { grow.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(monthName(0), color = Ui.TextMuted, fontSize = 13.sp)
                Text(money(total), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                DeltaPill(deltaText, pct == null || pct <= 0)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().height(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Canvas(Modifier.weight(1f).fillMaxHeight()) {
                        val slot = size.width / daysInMonth
                        val barW = slot * 0.5f
                        val maxV = if (maxDay > 0) maxDay else 1.0
                        val g = grow.value
                        for (d in 1..daysInMonth) {
                            val v = daily[d]
                            val full = if (v > 0) {
                                (v / maxV * size.height).toFloat().coerceAtLeast(4.dp.toPx())
                            } else {
                                3.dp.toPx()
                            }
                            val h = full * g
                            val x = (d - 0.5f) * slot
                            drawRoundRect(
                                color = if (v > 0) Color(0xF2FFFFFF) else Color(0x22FFFFFF),
                                topLeft = Offset(x - barW / 2, size.height - h),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(barW / 2, barW / 2),
                            )
                        }
                        if (avg > 0) {
                            val y = size.height - (avg / maxV * size.height).toFloat()
                            drawLine(
                                color = Color(0x99FFFFFF),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                            )
                        }
                    }
                    Column(
                        Modifier.fillMaxHeight().width(52.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(money(maxDay), color = Ui.TextMuted, fontSize = 10.sp)
                        Text("0", color = Ui.TextMuted, fontSize = 10.sp)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(end = 60.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf(1, 9, 16, 24, daysInMonth).forEach {
                        Text(it.toString(), color = Ui.TextMuted, fontSize = 10.sp)
                    }
                }
                Text(
                    "Promedio diario " + money(avg),
                    color = Ui.TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (items.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Sin movimientos. Los gastos que registres aparecerán aquí.",
                    color = Ui.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        }

        groups.forEach { (dayMs, list) ->
            item(key = "day$dayMs") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(dayLabel(dayMs), color = Ui.TextMuted, fontSize = 13.sp)
                    Text(money(list.sumOf { it.amount }), color = Ui.TextMuted, fontSize = 13.sp)
                }
            }
            items(list, key = { it.id }) { e ->
                Row(
                    Modifier.fillMaxWidth().glass(18.dp).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryBadge(e.category, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            e.note.ifEmpty { e.category },
                            color = Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                        )
                        Text(
                            e.category + " · " + fmtTime.format(Date(e.ts)),
                            color = Ui.TextMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Text(money(e.amount), color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "✕",
                        color = Ui.TextMuted,
                        modifier = Modifier.clickable { Store.remove(ctx, e.id) }.padding(4.dp),
                    )
                }
            }
        }
    }
}
