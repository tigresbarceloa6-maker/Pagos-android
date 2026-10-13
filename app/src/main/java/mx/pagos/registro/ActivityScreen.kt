package mx.pagos.registro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(filter: Filter, onFilter: (Filter) -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    var offset by remember(filter.period) { mutableIntStateOf(0) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<Long?>(null) }

    val cur = Store.items.matching(filter).inRange(periodRange(filter.period, offset))
    val q = query.trim()
    val shown = if (q.isEmpty()) {
        cur
    } else {
        cur.filter { it.note.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true) }
    }
    val groups = shown.sortedByDescending { it.ts }.groupBy { dayStart(it.ts) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = LocalBottomPad.current),
    ) {
        item(key = "cabecera") {
            Column(Modifier.statusBarsPadding()) {
                TopBar(onSettings)
                AnimatedContent(
                    targetState = offset to filter,
                    transitionSpec = { periodTransition(targetState.first > initialState.first) },
                    label = "periodo",
                ) { (off, f) ->
                    ActivityHeader(f, off, onOffset = { offset = it })
                }
                Spacer(Modifier.height(18.dp))
                if (searching) {
                    SearchField(
                        query = query,
                        onQuery = { query = it },
                        onClose = {
                            searching = false
                            query = ""
                        },
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircleButton(R.drawable.ic_search, { searching = true }, size = 38.dp, iconSize = 18.dp)
                        FilterPills(filter, onFilter)
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            item(key = "vacio") {
                Box(Modifier.padding(top = 18.dp)) {
                    EmptyCard(
                        when {
                            q.isNotEmpty() -> "Sin resultados para \"$q\"."
                            filter.income -> "Sin ingresos en este periodo. Regístralos con + eligiendo una categoría de Ingresos."
                            else -> "Sin gastos en este periodo. Toca + o pulsa dos veces el botón de volumen."
                        }
                    )
                }
            }
        }

        groups.forEach { (day, list) ->
            item(key = "dia$day") {
                DayGroup(
                    day = day,
                    list = list,
                    open = open,
                    onToggle = { id -> open = if (open == id) null else id },
                    onDelete = { e ->
                        Store.remove(ctx, e.id)
                        open = null
                    },
                )
            }
        }
    }
}

@Composable
private fun ActivityHeader(f: Filter, off: Int, onOffset: (Int) -> Unit) {
    val base = Store.items.matching(f)
    val cur = base.inRange(periodRange(f.period, off))
    val total = cur.sumOf { it.amount }
    val prev = base.inRange(periodRange(f.period, off - 1)).sumOf { it.amount }
    val pct = deltaPct(total, prev)
    val n = bucketCount(f.period, off)
    val values = DoubleArray(n)
    cur.forEach { e -> values[bucketOf(f.period, e.ts).coerceIn(0, n - 1)] += e.amount }
    val elapsed = elapsedBuckets(f.period, off).coerceIn(1, n)
    val avg = total / elapsed
    val labels = if (f.period == Period.Mensual) {
        listOf(0 to "1", 8 to "9", 15 to "16", 23 to "24", n - 1 to n.toString())
    } else {
        (0 until 12).map { it to MonthShort[it].take(1) }
    }
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(850, easing = FastOutSlowInEasing)) }

    Column(
        modifier = Modifier.fillMaxWidth().swipePeriods(off, onOffset),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        PeriodSelector(f.period, off, onOffset)
        Text(money(total), color = Ui.Cream, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        DeltaLine(
            pct = pct,
            prevLabel = periodShort(f.period, off - 1),
            good = if (f.income) (pct ?: 0) >= 0 else (pct ?: 0) <= 0,
        )
        Spacer(Modifier.height(20.dp))
        BarChart(
            values = values,
            elapsed = elapsed,
            avg = avg,
            labels = labels,
            grow = grow.value,
            modifier = Modifier.fillMaxWidth().height(196.dp),
        )
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focus.requestFocus()
        } catch (_: Exception) {
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(Ui.GlassStrong)
            .border(1.dp, Ui.GlassBorder, RoundedCornerShape(50))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(R.drawable.ic_search, Ui.TextMuted, 18.dp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Buscar", color = Ui.TextMuted, fontSize = 15.sp)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = Ui.Cream, fontSize = 15.sp, fontFamily = AppFont),
                cursorBrush = SolidColor(Ui.Cream),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        GlyphIcon(
            R.drawable.ic_clear,
            Ui.TextMuted,
            20.dp,
            Modifier.clip(CircleShape).clickable(onClick = onClose),
        )
    }
}

@Composable
private fun DayGroup(
    day: Long,
    list: List<Expense>,
    open: Long?,
    onToggle: (Long) -> Unit,
    onDelete: (Expense) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
            Text(dayLabel(day), color = Ui.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(money(list.sumOf { it.amount }), color = Ui.TextMuted, fontSize = 13.sp)
        }
        Column(Modifier.fillMaxWidth().glass(22.dp).padding(vertical = 4.dp)) {
            list.forEach { e ->
                MovementRow(e, open == e.id, onClick = { onToggle(e.id) }, onDelete = { onDelete(e) })
            }
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale("es", "MX"))

@Composable
private fun MovementRow(e: Expense, open: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    var confirm by remember(e.id, open) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Text(category(e.category).emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                e.note.ifBlank { e.category },
                color = Ui.Cream,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (e.income) "+" + money(e.amount) else money(e.amount),
                color = if (e.income) Ui.Good else Ui.Cream,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (open) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 46.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    e.category + " · " + timeFormat.format(Date(e.ts)) + " · " + e.account,
                    color = Ui.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (confirm) "Confirmar" else "Eliminar",
                    color = Ui.Bad,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (confirm) Color(0x26FF6B81) else Color.Transparent)
                        .clickable { if (confirm) onDelete() else confirm = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
