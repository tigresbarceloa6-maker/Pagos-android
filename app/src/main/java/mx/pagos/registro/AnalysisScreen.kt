package mx.pagos.registro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.max

private val IncomeTop = Color(0xFF34F5B8)
private val IncomeBottom = Color(0xFF0E9F8E)
private val ExpenseTop = Color(0xFF4DA3FF)
private val ExpenseBottom = Color(0xFF1D4ED8)
private val PurpleTop = Color(0xFFCDA4FF)
private val PurpleBottom = Color(0xFF8B5CF6)

@Composable
fun AnalysisScreen(filter: Filter, onFilter: (Filter) -> Unit, onSettings: () -> Unit) {
    val p = filter.period
    val n = if (p == Period.Mensual) 6 else 5
    val offsets = (-(n - 1)..0).toList()
    val items = Store.items.forAccount(filter.account)
    val inc = offsets.map { o -> items.filter { it.income }.inRange(periodRange(p, o)).sumOf { it.amount } }
    val exp = offsets.map { o -> items.filter { !it.income }.inRange(periodRange(p, o)).sumOf { it.amount } }
    val rest = offsets.indices.map { inc[it] - exp[it] }
    val labels = offsets.map { periodShort(p, it) }
    var sel1 by remember(p) { mutableIntStateOf(n - 1) }
    var sel2 by remember(p) { mutableIntStateOf(n - 1) }
    var cumulative by remember { mutableStateOf(false) }
    val restShown = if (cumulative) rest.runningReduce { a, b -> a + b } else rest
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = LocalBottomPad.current),
    ) {
        TopBar(onSettings)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterPills(filter, onFilter, showKind = false)
        }
        Spacer(Modifier.height(14.dp))

        // Ingresos contra gastos por periodo.
        Column(Modifier.fillMaxWidth().glass(26.dp).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Legend("Ingresos", Ui.IncomeTeal, inc[sel1])
                Legend("Gastos", Ui.ExpenseBlue, exp[sel1])
            }
            Spacer(Modifier.height(16.dp))
            GroupedBars(inc, exp, sel1, { sel1 = it }, grow.value, Modifier.fillMaxWidth().height(200.dp))
            PeriodLabels(labels, sel1, { sel1 = it })
        }
        Spacer(Modifier.height(14.dp))

        // Lo que queda de los ingresos después de los gastos.
        Column(Modifier.fillMaxWidth().glass(26.dp).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (cumulative) "Ahorro acumulado" else "Ingresos restantes",
                        color = Ui.TextMuted,
                        fontSize = 13.sp,
                    )
                    Text(money(restShown[sel2]), color = Ui.Cream, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
                CircleButton(R.drawable.ic_coins, { cumulative = !cumulative }, size = 36.dp, iconSize = 18.dp)
            }
            Spacer(Modifier.height(14.dp))
            RemainingBars(
                income = if (cumulative) restShown.map { max(it, 0.0) } else inc,
                values = restShown,
                selected = sel2,
                onSelect = { sel2 = it },
                grow = grow.value,
                modifier = Modifier.fillMaxWidth().height(190.dp),
            )
            PeriodLabels(labels, sel2, { sel2 = it })
        }
        Spacer(Modifier.height(14.dp))

        CalendarCard(
            offset = if (p == Period.Mensual) offsets[sel1] else 0,
            items = items,
        )
    }
}

@Composable
private fun Legend(label: String, color: Color, value: Double) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Ui.TextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(money(value), color = Ui.Cream, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PeriodLabels(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, endPad: Dp = 34.dp) {
    Row(Modifier.fillMaxWidth().padding(end = endPad, top = 8.dp)) {
        labels.forEachIndexed { i, label ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    label,
                    color = if (i == selected) Ui.Cream else Ui.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (i == selected) Color(0x33FFFFFF) else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun GroupedBars(
    a: List<Double>,
    b: List<Double>,
    selected: Int,
    onSelect: (Int) -> Unit,
    grow: Float,
    modifier: Modifier,
) {
    val maxV = max(a.maxOrNull() ?: 0.0, b.maxOrNull() ?: 0.0)
    val top = if (maxV > 0) maxV else 1.0
    BoxWithConstraints(modifier) {
        val axisW = 34.dp
        val chartW = maxWidth - axisW
        val chartH = maxHeight
        Canvas(
            Modifier
                .size(chartW, chartH)
                .pointerInput(a.size) {
                    detectTapGestures { pos ->
                        onSelect((pos.x / (size.width.toFloat() / a.size)).toInt().coerceIn(0, a.size - 1))
                    }
                }
        ) {
            val slot = size.width / a.size
            val barW = (slot * 0.2f).coerceIn(5.dp.toPx(), 12.dp.toPx())
            val gap = 4.dp.toPx()
            for (i in a.indices) {
                val cx = slot * (i + 0.5f)
                val alpha = if (i == selected) 1f else 0.6f
                listOf(
                    Triple(cx - gap / 2f - barW / 2f, a[i], IncomeTop to IncomeBottom),
                    Triple(cx + gap / 2f + barW / 2f, b[i], ExpenseTop to ExpenseBottom),
                ).forEach { (x, v, colors) ->
                    if (v > 0) {
                        val h = ((v / top) * size.height).toFloat().coerceAtLeast(barW) * grow
                        glowBar(x, size.height - h, barW, colors.first, colors.second, alpha)
                    } else {
                        drawRoundRect(
                            color = Color(0x40FFFFFF),
                            topLeft = Offset(x - barW / 2f, size.height - 3.dp.toPx()),
                            size = Size(barW, 3.dp.toPx()),
                            cornerRadius = CornerRadius(1.5.dp.toPx()),
                        )
                    }
                }
            }
        }
        if (maxV > 0) {
            AxisText(compact(maxV), chartW + 8.dp, (-6).dp)
            AxisText(compact(maxV / 2), chartW + 8.dp, chartH / 2 - 8.dp)
        }
        AxisText("0", chartW + 8.dp, chartH - 10.dp)
    }
}

/**
 * Barras moradas con lo que queda de los ingresos. Detrás, una cápsula punteada
 * muestra el total de ingresos del periodo; la línea punteada es el promedio.
 */
@Composable
private fun RemainingBars(
    income: List<Double>,
    values: List<Double>,
    selected: Int,
    onSelect: (Int) -> Unit,
    grow: Float,
    modifier: Modifier,
) {
    val maxV = max(income.maxOrNull() ?: 0.0, values.maxOrNull() ?: 0.0)
    val top = if (maxV > 0) maxV else 1.0
    val avg = if (values.isEmpty()) 0.0 else values.sum() / values.size
    BoxWithConstraints(modifier) {
        val axisW = 34.dp
        val chartW = maxWidth - axisW
        val chartH = maxHeight
        Canvas(
            Modifier
                .size(chartW, chartH)
                .pointerInput(values.size) {
                    detectTapGestures { pos ->
                        onSelect((pos.x / (size.width.toFloat() / values.size)).toInt().coerceIn(0, values.size - 1))
                    }
                }
        ) {
            val slot = size.width / values.size
            val w = (slot * 0.62f).coerceAtMost(40.dp.toPx())
            val r = 12.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
            values.forEachIndexed { i, v ->
                val cx = slot * (i + 0.5f)
                val trackH = if (income[i] > 0) ((income[i] / top) * size.height).toFloat().coerceAtLeast(2 * r) else size.height
                drawRoundRect(
                    color = Color(0x0AFFFFFF),
                    topLeft = Offset(cx - w / 2f, size.height - trackH),
                    size = Size(w, trackH),
                    cornerRadius = CornerRadius(r),
                )
                drawRoundRect(
                    color = Color(0x2EFFFFFF),
                    topLeft = Offset(cx - w / 2f, size.height - trackH),
                    size = Size(w, trackH),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = dash),
                )
                if (v > 0) {
                    val h = ((v / top) * size.height).toFloat().coerceAtLeast(2 * r) * grow
                    glowBar(cx, size.height - h, w, PurpleTop, PurpleBottom, if (i == selected) 1f else 0.7f, r)
                } else {
                    drawRoundRect(
                        color = Color(0x4DFFFFFF),
                        topLeft = Offset(cx - w / 2f + 4.dp.toPx(), size.height - 4.dp.toPx()),
                        size = Size(w - 8.dp.toPx(), 4.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
            if (avg > 0) {
                val y = size.height - (avg / top * size.height).toFloat()
                drawLine(
                    color = Ui.CreamSoft,
                    start = Offset(0f, y),
                    end = Offset(size.width + 6.dp.toPx(), y),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = dash,
                )
            }
        }
        if (avg > 0) AxisText(compact(avg), chartW + 8.dp, chartH * (1f - (avg / top).toFloat()) - 8.dp, strong = true)
        AxisText("0", chartW + 8.dp, chartH - 10.dp)
    }
}

private val WeekInitials = listOf("L", "M", "M", "J", "V", "S", "D")

/** Calendario del mes con cada día teñido de azul según lo gastado. */
@Composable
private fun CalendarCard(offset: Int, items: List<Expense>) {
    val (start, end) = periodRange(Period.Mensual, offset)
    val first = Calendar.getInstance().apply { timeInMillis = start }
    val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val lead = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daily = DoubleArray(days)
    val counts = IntArray(days)
    items.forEach { e ->
        if (!e.income && e.ts >= start && e.ts < end) {
            val c = Calendar.getInstance()
            c.timeInMillis = e.ts
            val d = c.get(Calendar.DAY_OF_MONTH) - 1
            daily[d] += e.amount
            counts[d]++
        }
    }
    val maxD = daily.maxOrNull() ?: 0.0
    val today = if (offset == 0) Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1 else -1
    var picked by remember(offset) { mutableIntStateOf(today) }

    Column(Modifier.fillMaxWidth().glass(26.dp).padding(16.dp)) {
        Row {
            Text("Calendario", color = Ui.TextMuted, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(periodTitle(Period.Mensual, offset), color = Ui.TextMuted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeekInitials.forEach {
                Text(
                    it,
                    color = Ui.TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val rows = (lead + days + 6) / 7
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (col in 0 until 7) {
                    val d = r * 7 + col - lead
                    if (d < 0 || d >= days) {
                        Spacer(Modifier.weight(1f).height(34.dp))
                    } else {
                        val v = daily[d]
                        val strength = if (maxD > 0 && v > 0) (0.35 + 0.65 * v / maxD).toFloat() else 0f
                        val shape = RoundedCornerShape(9.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(shape)
                                .background(if (v > 0) Color(0xFF2F6BFF).copy(alpha = strength) else Color(0x0FFFFFFF))
                                .then(
                                    when (d) {
                                        picked -> Modifier.border(1.5.dp, Ui.Cream, shape)
                                        today -> Modifier.border(1.dp, Ui.CreamDim, shape)
                                        else -> Modifier
                                    }
                                )
                                .clickable { picked = d },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                (d + 1).toString(),
                                color = if (v > 0) Ui.Cream else Ui.TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        if (picked in 0 until days) {
            val c = Calendar.getInstance()
            c.timeInMillis = start
            c.add(Calendar.DAY_OF_MONTH, picked)
            val count = counts[picked]
            Spacer(Modifier.height(8.dp))
            Text(
                dayName(c.timeInMillis) + " · " + money(daily[picked]) + " · " +
                    (if (count == 1) "1 gasto" else "$count gastos"),
                color = Ui.TextSoft,
                fontSize = 13.sp,
            )
        }
    }
}
