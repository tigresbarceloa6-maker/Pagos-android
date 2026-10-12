package mx.pagos.registro

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/** Espacio inferior que deja libre la barra de navegación flotante. */
val LocalBottomPad = compositionLocalOf { 110.dp }

@Composable
fun Modifier.tapNoRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
)

@Composable
fun GlyphIcon(res: Int, tint: Color = Color.White, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(res),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}

@Composable
fun CircleButton(
    res: Int,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Ui.GlassStrong)
            .border(1.dp, Ui.GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(res, Color.White, iconSize)
    }
}

/** Botón redondo de ajustes en la esquina superior izquierda. */
@Composable
fun TopBar(onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        CircleButton(R.drawable.ic_settings, onSettings)
    }
}

/** Cuadro redondeado con borde degradado del color de la categoría y su emoji. */
@Composable
fun CategoryBadge(name: String, size: Dp = 36.dp) {
    val c = category(name)
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(c.color.copy(alpha = 0.16f))
            .border(1.6.dp, Brush.linearGradient(listOf(c.color, c.color2)), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(c.emoji, fontSize = (size.value * 0.5f).sp)
    }
}

@Composable
fun Pill(text: String, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(Ui.PillBg)
            .border(1.dp, Ui.GlassBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Pastilla que abre un menú con opciones. */
@Composable
fun MenuPill(text: String, options: List<String>, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Pill(text, onClick = { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(Ui.Menu),
        ) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (option == text) Color.White else Ui.TextSoft,
                            fontWeight = if (option == text) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        open = false
                        onPick(i)
                    },
                )
            }
        }
    }
}

/** Pastillas "Gastos", "Mensual" y "Todas las cuentas". */
@Composable
fun FilterPills(filter: Filter, onFilter: (Filter) -> Unit, showKind: Boolean = true) {
    if (showKind) {
        MenuPill(if (filter.income) "Ingresos" else "Gastos", listOf("Gastos", "Ingresos")) {
            onFilter(filter.copy(income = it == 1))
        }
    }
    MenuPill(if (filter.period == Period.Mensual) "Mensual" else "Anual", listOf("Mensual", "Anual")) {
        onFilter(filter.copy(period = if (it == 0) Period.Mensual else Period.Anual))
    }
    val accounts = Config.accounts.toList()
    MenuPill(filter.account ?: "Todas las cuentas", listOf("Todas las cuentas") + accounts) {
        onFilter(filter.copy(account = if (it == 0) null else accounts[it - 1]))
    }
}

/** "Este mes ⌄": abre la lista de periodos anteriores. */
@Composable
fun PeriodSelector(period: Period, offset: Int, onOffset: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { open = true }
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(periodTitle(period, offset), color = Ui.TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(3.dp))
            GlyphIcon(R.drawable.ic_chevron_down, Ui.TextMuted, 16.dp)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(Ui.Menu),
        ) {
            val count = if (period == Period.Mensual) 12 else 5
            for (i in 0 downTo -(count - 1)) {
                DropdownMenuItem(
                    text = {
                        Text(
                            periodTitle(period, i),
                            color = if (i == offset) Color.White else Ui.TextSoft,
                            fontWeight = if (i == offset) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        open = false
                        onOffset(i)
                    },
                )
            }
        }
    }
}

/** "● 1% frente a Jul" con flecha: verde si conviene, rojo si no. */
@Composable
fun DeltaLine(pct: Int?, prevLabel: String, good: Boolean) {
    if (pct == null) {
        Text("Sin datos de $prevLabel", color = Ui.TextMuted, fontSize = 13.sp)
        return
    }
    val color = if (good) Ui.Good else Ui.Bad
    val up = pct > 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(13.dp)) {
            drawCircle(color)
            val w = size.width
            val h = size.height
            val p = Path()
            if (up) {
                p.moveTo(w * 0.5f, h * 0.24f)
                p.lineTo(w * 0.78f, h * 0.64f)
                p.lineTo(w * 0.22f, h * 0.64f)
            } else {
                p.moveTo(w * 0.5f, h * 0.76f)
                p.lineTo(w * 0.78f, h * 0.36f)
                p.lineTo(w * 0.22f, h * 0.36f)
            }
            p.close()
            drawPath(p, Color(0xFF0B1126))
        }
        Spacer(Modifier.width(5.dp))
        Text(
            abs(pct).toString() + "% frente a " + prevLabel,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Texto con punto de color (leyendas). */
@Composable
fun DotText(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Deslizar a la derecha va al periodo anterior; a la izquierda, al siguiente. */
fun Modifier.swipePeriods(offset: Int, onOffset: (Int) -> Unit): Modifier = pointerInput(offset) {
    var drag = 0f
    detectHorizontalDragGestures(
        onDragStart = { drag = 0f },
        onDragEnd = {
            val limit = 56.dp.toPx()
            if (drag > limit) {
                onOffset(offset - 1)
            } else if (drag < -limit && offset < 0) {
                onOffset(offset + 1)
            }
        },
    ) { change, amount ->
        change.consume()
        drag += amount
    }
}

fun periodTransition(forward: Boolean): ContentTransform {
    val dir = if (forward) 1 else -1
    return (slideInHorizontally(tween(320)) { it * dir / 4 } + fadeIn(tween(320))) togetherWith
        (slideOutHorizontally(tween(320)) { -it * dir / 4 } + fadeOut(tween(180))) using
        SizeTransform(clip = false)
}

@Composable
fun AxisText(text: String, x: Dp, y: Dp, strong: Boolean = false) {
    Text(
        text,
        modifier = Modifier.offset(x = x, y = y),
        color = if (strong) Color.White else Ui.TextMuted,
        fontSize = 11.sp,
        fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
    )
}

@Composable
fun CapsuleButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) color else color.copy(alpha = color.alpha * 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) Color.White else Color(0x80FFFFFF),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SectionLabel(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp)) {
        Text(text, color = Ui.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = Ui.TextMuted, fontSize = 13.sp)
    }
}

@Composable
fun EmptyCard(text: String) {
    Text(
        text,
        color = Ui.TextMuted,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().glass(22.dp).padding(22.dp),
    )
}

private val NavItems = listOf(
    "Actividad" to R.drawable.ic_tab_activity,
    "Resumen" to R.drawable.ic_tab_summary,
    "Presupuesto" to R.drawable.ic_tab_budget,
    "Análisis" to R.drawable.ic_tab_analysis,
    "Cuentas" to R.drawable.ic_tab_accounts,
)

/** Barra flotante tipo cristal con indicador que se desliza a la pestaña elegida. */
@Composable
fun NavBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Ui.NavBg)
            .border(1.dp, Ui.GlassBorder, RoundedCornerShape(32.dp))
            .padding(5.dp),
    ) {
        val itemW = maxWidth / NavItems.size
        val x by animateDpAsState(
            targetValue = itemW * selected,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
            label = "indicador",
        )
        Box(
            Modifier
                .offset(x = x)
                .width(itemW)
                .fillMaxHeight()
                .clip(RoundedCornerShape(27.dp))
                .background(Color(0x2EFFFFFF))
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            NavItems.forEachIndexed { i, (label, icon) ->
                val sel = i == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .tapNoRipple { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    GlyphIcon(icon, if (sel) Color.White else Color(0xCCFFFFFF), 21.dp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        color = if (sel) Color.White else Color(0xB3FFFFFF),
                        fontSize = 10.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
