package mx.pagos.registro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Degradado a lo largo de un arco que empieza en 0° y recorre [sweep] grados. */
fun arcBrush(c1: Color, c2: Color, sweep: Float, center: Offset): Brush {
    val s = (sweep / 360f).coerceIn(0.0005f, 1f)
    if (s > 0.98f) return Brush.sweepGradient(listOf(c1, c2), center)
    val mid = s + (1f - s) / 2f
    return Brush.sweepGradient(
        0f to c1,
        s to c2,
        mid to c2,
        (mid + 0.0005f) to c1,
        1f to c1,
        center = center,
    )
}

/**
 * Dona por categoría: segmentos con degradado, extremos redondeados y separación.
 * Con una categoría elegida, el resto se tiñe con su color atenuado.
 */
@Composable
fun Donut(
    totals: List<Pair<String, Double>>,
    selected: String?,
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 20.dp,
) {
    val total = totals.sumOf { it.second }
    val selColor = selected?.let { category(it).color }
    Canvas(modifier) {
        val sw = stroke.toPx()
        val radius = (size.minDimension - sw) / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        val topLeft = Offset(c.x - radius, c.y - radius)
        val arc = Size(radius * 2f, radius * 2f)
        if (total <= 0.0) {
            drawCircle(Color(0x1AFFFFFF), radius, c, style = Stroke(sw))
            return@Canvas
        }
        val multi = totals.size > 1
        val capDeg = Math.toDegrees((sw / 2f / radius).toDouble()).toFloat()
        val inset = if (multi) 1.6f + capDeg else 0f
        var start = -90f
        totals.forEach { (name, value) ->
            val full = (value / total * 360.0).toFloat() * progress
            val cat = category(name)
            val (from, sweep) = when {
                !multi -> start to 360f * progress
                full - 2 * inset > 0.2f -> (start + inset) to (full - 2 * inset)
                else -> (start + full / 2f - 0.1f) to 0.2f
            }
            val brush = if (selColor == null || selected == name) {
                arcBrush(cat.color, cat.color2, sweep, c)
            } else {
                SolidColor(selColor.copy(alpha = 0.26f))
            }
            rotate(from, c) {
                drawArc(
                    brush = brush,
                    startAngle = 0f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    style = Stroke(sw, cap = if (multi) StrokeCap.Round else StrokeCap.Butt),
                )
            }
            start += full
        }
    }
}

/** Barra con brillo suave alrededor (capas translúcidas). */
fun DrawScope.glowBar(
    cx: Float,
    top: Float,
    width: Float,
    c1: Color,
    c2: Color,
    alpha: Float = 1f,
    radius: Float = width / 2f,
) {
    val h = size.height - top
    if (h <= 0f) return
    for (k in 1..3) {
        val e = k * 2.5.dp.toPx()
        drawRoundRect(
            color = c1.copy(alpha = 0.06f * alpha),
            topLeft = Offset(cx - width / 2f - e, top - e),
            size = Size(width + 2 * e, h + 2 * e),
            cornerRadius = CornerRadius(radius + e),
        )
    }
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(c1, c2), startY = top, endY = size.height),
        topLeft = Offset(cx - width / 2f, top),
        size = Size(width, h),
        cornerRadius = CornerRadius(radius),
        alpha = alpha,
    )
}

/**
 * Barras blancas delgadas por día (o por mes en vista anual), con línea de
 * promedio punteada y eje a la derecha: máximo, promedio y cero.
 */
@Composable
fun BarChart(
    values: DoubleArray,
    elapsed: Int,
    avg: Double,
    labels: List<Pair<Int, String>>,
    grow: Float,
    modifier: Modifier = Modifier,
) {
    val maxV = values.maxOrNull() ?: 0.0
    val top = if (maxV > 0) maxV else 1.0
    val n = values.size
    BoxWithConstraints(modifier) {
        val axisW = 38.dp
        val labelH = 22.dp
        val chartW = maxWidth - axisW
        val chartH = maxHeight - labelH
        Canvas(Modifier.size(chartW, chartH)) {
            val slot = size.width / n
            val barW = (slot * 0.56f).coerceAtMost(14.dp.toPx())
            for (i in 0 until n) {
                val v = values[i]
                if (i >= elapsed && v <= 0) continue
                val full = if (v > 0) {
                    (v / top * size.height).toFloat().coerceAtLeast(barW)
                } else {
                    3.dp.toPx()
                }
                val h = full * grow
                val x = slot * (i + 0.5f)
                drawRoundRect(
                    color = if (v > 0) Color.White else Color(0x26FFFFFF),
                    topLeft = Offset(x - barW / 2f, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                )
            }
            if (avg > 0 && maxV > 0) {
                val y = size.height - (avg / top * size.height).toFloat()
                drawLine(
                    color = Color(0xB3FFFFFF),
                    start = Offset(0f, y),
                    end = Offset(size.width + 6.dp.toPx(), y),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                )
            }
        }
        val avgY = chartH * (1f - (avg / top).toFloat())
        val hasAvg = avg > 0 && maxV > 0
        if (maxV > 0 && (!hasAvg || avgY > 20.dp)) AxisText(compact(maxV), chartW + 8.dp, (-6).dp)
        if (hasAvg) AxisText(compact(avg), chartW + 8.dp, avgY - 8.dp, strong = true)
        if (!hasAvg || avgY < chartH - 20.dp) AxisText("0", chartW + 8.dp, chartH - 10.dp)
        val slotDp = chartW / n
        labels.forEach { (i, s) ->
            Text(
                s,
                modifier = Modifier
                    .offset(x = slotDp * (i + 0.5f) - 14.dp, y = chartH + 6.dp)
                    .width(28.dp),
                color = Ui.TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
