package mx.pagos.registro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun BudgetScreen(onSettings: () -> Unit, onEdit: (String) -> Unit) {
    val month = Store.items.filter { !it.income }.inRange(periodRange(Period.Mensual, 0))
    val spentBy = month.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
    val budgets = Config.budgets.toMap()
    val totalBudget = budgets.values.sum()
    val spent = budgets.keys.sumOf { spentBy[it] ?: 0.0 }
    val remaining = totalBudget - spent
    val cal = Calendar.getInstance()
    val daysLeft = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH) + 1
    val withBudget = ExpenseCategories
        .filter { budgets.containsKey(it.name) }
        .sortedByDescending { (spentBy[it.name] ?: 0.0) / (budgets[it.name] ?: 1.0) }
    val without = ExpenseCategories.filter { !budgets.containsKey(it.name) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(950, easing = FastOutSlowInEasing)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = LocalBottomPad.current),
    ) {
        TopBar(onSettings)
        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            BudgetGauge(
                fraction = if (totalBudget > 0) (spent / totalBudget).toFloat() else 0f,
                over = remaining < 0,
                progress = progress.value,
                modifier = Modifier.size(280.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (totalBudget > 0) "Restante este mes" else "Presupuesto mensual",
                    color = Ui.TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(money(remaining), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                if (totalBudget <= 0) {
                    Text(
                        "Toca una categoría\npara definir su límite",
                        color = Ui.TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                } else if (remaining >= 0) {
                    Text("de " + money(totalBudget), color = Ui.TextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    DotText("≈ " + money(remaining / daysLeft) + " por día", Ui.Good)
                } else {
                    Text("de " + money(totalBudget), color = Ui.TextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    DotText("Te pasaste por " + money(-remaining), Ui.Bad)
                }
            }
        }

        if (withBudget.isNotEmpty()) {
            SectionLabel("Con presupuesto", money(spent) + " gastado")
            Column(Modifier.fillMaxWidth().glass(24.dp).padding(vertical = 6.dp)) {
                withBudget.forEach { c ->
                    BudgetRow(c, spentBy[c.name] ?: 0.0, budgets[c.name], progress.value) { onEdit(c.name) }
                }
            }
        }
        SectionLabel("Sin presupuesto")
        Column(Modifier.fillMaxWidth().glass(24.dp).padding(vertical = 6.dp)) {
            without.forEach { c ->
                BudgetRow(c, spentBy[c.name] ?: 0.0, null, progress.value) { onEdit(c.name) }
            }
        }
    }
}

@Composable
private fun BudgetGauge(fraction: Float, over: Boolean, progress: Float, modifier: Modifier) {
    Canvas(modifier) {
        val sw = 20.dp.toPx()
        val radius = (size.minDimension - sw) / 2f
        val c = center
        val topLeft = Offset(c.x - radius, c.y - radius)
        val arc = Size(radius * 2f, radius * 2f)
        drawArc(Color(0x14FFFFFF), 0f, 360f, false, topLeft, arc, style = Stroke(sw))
        val sweep = 360f * fraction.coerceIn(0f, 1f) * progress
        if (sweep > 0.5f) {
            rotate(-90f, c) {
                drawArc(
                    brush = if (over) SolidColor(Ui.Bad) else arcBrush(Ui.IncomeTeal, Color(0xFF3B82F6), sweep, c),
                    startAngle = 0f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun BudgetRow(c: Category, spent: Double, budget: Double?, progress: Float, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(c.name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(money(spent), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (budget != null) {
                    Text(" / " + money(budget), color = Ui.TextMuted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            if (budget != null) {
                val frac = (spent / budget).toFloat()
                Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                    val r = CornerRadius(size.height / 2f)
                    drawRoundRect(Color(0x1AFFFFFF), size = size, cornerRadius = r)
                    val w = size.width * frac.coerceIn(0f, 1f) * progress
                    if (w > 0f) {
                        drawRoundRect(
                            brush = if (frac > 1f) {
                                SolidColor(Ui.Bad)
                            } else {
                                Brush.horizontalGradient(listOf(c.color, c.color2), 0f, size.width)
                            },
                            size = Size(w.coerceAtLeast(size.height), size.height),
                            cornerRadius = r,
                        )
                    }
                }
            } else {
                Text("Toca para definir un límite", color = Ui.TextMuted, fontSize = 11.sp)
            }
        }
    }
}
