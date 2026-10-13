package mx.pagos.registro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun SummaryScreen(
    filter: Filter,
    onFilter: (Filter) -> Unit,
    onAccent: (Color?) -> Unit,
    onSettings: () -> Unit,
) {
    var offset by remember(filter.period) { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    val cur = Store.items.matching(filter).inRange(periodRange(filter.period, offset))
    val totals = cur.byCategory()
    val counts = cur.groupingBy { it.category }.eachCount()
    val total = totals.sumOf { it.second }
    val sel = selected?.takeIf { s -> totals.any { it.first == s } }

    LaunchedEffect(sel) { onAccent(sel?.let { category(it).color }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = LocalBottomPad.current),
    ) {
        TopBar(onSettings)
        AnimatedContent(
            targetState = offset to filter,
            transitionSpec = { periodTransition(targetState.first > initialState.first) },
            label = "dona",
        ) { (off, f) ->
            DonutHeader(
                f = f,
                off = off,
                sel = if (off == offset && f == filter) sel else null,
                onSelect = { selected = it },
                onOffset = { offset = it },
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterPills(filter, onFilter)
        }
        Spacer(Modifier.height(14.dp))
        if (totals.isEmpty()) {
            EmptyCard(
                if (filter.income) {
                    "Aún no hay ingresos en este periodo."
                } else {
                    "Aún no hay gastos en este periodo. Toca + o pulsa dos veces el botón de volumen."
                }
            )
        } else {
            Column(Modifier.fillMaxWidth().glass(24.dp).padding(vertical = 6.dp)) {
                totals.forEach { (name, value) ->
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
private fun DonutHeader(
    f: Filter,
    off: Int,
    sel: String?,
    onSelect: (String?) -> Unit,
    onOffset: (Int) -> Unit,
) {
    val base = Store.items.matching(f)
    val totals = base.inRange(periodRange(f.period, off)).byCategory()
    val prevTotals = base.inRange(periodRange(f.period, off - 1)).byCategory()
    val total = totals.sumOf { it.second }
    val shown = if (sel != null) totals.firstOrNull { it.first == sel }?.second ?: 0.0 else total
    val prevShown = if (sel != null) {
        prevTotals.firstOrNull { it.first == sel }?.second ?: 0.0
    } else {
        prevTotals.sumOf { it.second }
    }
    val pct = deltaPct(shown, prevShown)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(950, easing = FastOutSlowInEasing)) }
    val anim by animateFloatAsState(shown.toFloat(), tween(450), label = "monto")
    val display = if (abs(anim - shown.toFloat()) < 0.5f) shown else anim.roundToInt().toDouble()

    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp).swipePeriods(off, onOffset),
        contentAlignment = Alignment.Center,
    ) {
        Donut(
            totals = totals,
            selected = sel,
            progress = progress.value,
            modifier = Modifier
                .size(280.dp)
                .pointerInput(totals, sel) {
                    detectTapGestures { p ->
                        val dx = p.x - size.width / 2f
                        val dy = p.y - size.height / 2f
                        val r = sqrt(dx * dx + dy * dy)
                        if (r < size.width * 0.33f || total <= 0.0) {
                            onSelect(null)
                            return@detectTapGestures
                        }
                        var ang = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f
                        if (ang < 0f) ang += 360f
                        var acc = 0f
                        for ((name, value) in totals) {
                            val sweep = (value / total * 360.0).toFloat()
                            if (ang <= acc + sweep) {
                                onSelect(if (sel == name) null else name)
                                break
                            }
                            acc += sweep
                        }
                    }
                },
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (sel != null) {
                Text(
                    sel,
                    color = Ui.TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelect(null) }
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            } else {
                PeriodSelector(f.period, off, onOffset)
            }
            Text(money(display), color = Ui.Cream, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            DeltaLine(
                pct = pct,
                prevLabel = periodShort(f.period, off - 1),
                good = if (f.income) (pct ?: 0) >= 0 else (pct ?: 0) <= 0,
            )
        }
    }
}

private fun pctText(share: Double): String {
    val s = ((share * 100).roundToInt() / 100.0).toString()
    return (if (s.endsWith(".0")) s.dropLast(2) else s) + "%"
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dim) 0.45f else 1f)
            .background(if (selected) Color(0x14FFFFFF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(name)
        Spacer(Modifier.width(12.dp))
        Text(name, color = Ui.Cream, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(count.toString(), color = Ui.TextMuted, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(money(value), color = Ui.Cream, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x0FFFFFFF))
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(pctText(share), color = Ui.TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}
