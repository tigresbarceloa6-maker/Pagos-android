package mx.pagos.registro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
private val Blue = Color(0xFF0A84FF)

/**
 * Tarjeta flotante de captura en 3 pasos (monto, categoría, nota), con
 * entrada y salida animadas. Se muestra sobre cualquier app sin abrir la principal.
 */
@Composable
fun CaptureScreen(onFinish: () -> Unit) {
    val ctx = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var closing by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { shown = true }
    LaunchedEffect(closing) {
        if (closing) {
            delay(200)
            onFinish()
        }
    }
    LaunchedEffect(step) {
        if (step == 3) {
            delay(800)
            closing = true
        }
    }

    BackHandler {
        if (step in 1..2) {
            step -= 1
        } else {
            closing = true
        }
    }

    val visible = shown && !closing
    val scaleV by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "scale",
    )
    val alphaV by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(180),
        label = "alpha",
    )

    fun press(key: String) {
        amount = when (key) {
            "⌫" -> amount.dropLast(1)
            "." -> if (amount.contains('.')) amount else (if (amount.isEmpty()) "0." else amount + ".")
            else -> {
                val decimals = amount.substringAfter('.', "")
                if (amount.length >= 9 || (amount.contains('.') && decimals.length >= 2)) amount
                else if (amount == "0") key else amount + key
            }
        }
    }

    fun save() {
        val value = amount.toDoubleOrNull() ?: return
        val c = cat ?: return
        Store.add(ctx, value, c, note)
        step = 3
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * alphaV))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { closing = true },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scaleV
                    scaleY = scaleV
                    this.alpha = alphaV
                    translationY = (1f - alphaV) * 48f
                }
                .background(Color(0xF2111835), RoundedCornerShape(28.dp))
                .border(1.dp, Ui.GlassBorder, RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { }
                .animateContentSize()
                .padding(20.dp),
        ) {
            Crossfade(targetState = step, animationSpec = tween(220), label = "step") { s ->
                when (s) {
                    0 -> AmountStep(
                        amount = amount,
                        onKey = { press(it) },
                        onCancel = { closing = true },
                        onOk = { step = 1 },
                    )
                    1 -> CategoryStep(
                        selected = cat,
                        onPick = {
                            cat = it
                            step = 2
                        },
                        onBack = { step = 0 },
                    )
                    2 -> NoteStep(
                        amount = amount,
                        category = cat ?: "",
                        note = note,
                        onNote = { note = it },
                        onBack = { step = 1 },
                        onSave = { save() },
                    )
                    else -> DoneStep(amount = amount, category = cat ?: "")
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = if (primary) {
        if (enabled) Blue else Blue.copy(alpha = 0.35f)
    } else {
        Color(0x1FFFFFFF)
    }
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun AmountStep(
    amount: String,
    onKey: (String) -> Unit,
    onCancel: () -> Unit,
    onOk: () -> Unit,
) {
    val valid = (amount.toDoubleOrNull() ?: 0.0) > 0.0
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ingresa el monto", color = Ui.TextMuted, fontSize = 14.sp)
        Text(
            text = "\$" + amount.ifEmpty { "0" },
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .glass(14.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Cancelar", false, Modifier.weight(1f), onClick = onCancel)
            ActionButton("OK", true, Modifier.weight(1f), enabled = valid, onClick = onOk)
        }
    }
}

@Composable
private fun CategoryStep(selected: String?, onPick: (String) -> Unit, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Categoría", color = Ui.TextMuted, fontSize = 14.sp)
        Column(
            modifier = Modifier
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Categories.forEach { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected == c.name) c.color.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onPick(c.name) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryBadge(c.name, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(c.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        ActionButton("Atrás", false, Modifier.fillMaxWidth(), onClick = onBack)
    }
}

@Composable
private fun NoteStep(
    amount: String,
    category: String,
    note: String,
    onNote: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(300)
        try {
            focus.requestFocus()
        } catch (_: Exception) {
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Nota", color = Ui.TextMuted, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(category, 36.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(category, color = Color.White, fontWeight = FontWeight.Medium)
                Text(money(amount.toDoubleOrNull() ?: 0.0), color = Ui.TextMuted, fontSize = 13.sp)
            }
        }
        OutlinedTextField(
            value = note,
            onValueChange = onNote,
            placeholder = { Text("Nota (opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Atrás", false, Modifier.weight(1f), onClick = onBack)
            ActionButton("OK", true, Modifier.weight(1f), onClick = onSave)
        }
    }
}

@Composable
private fun DoneStep(amount: String, category: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("✓", color = Ui.Good, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text("Gasto guardado", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(
            money(amount.toDoubleOrNull() ?: 0.0) + " · " + category,
            color = Ui.TextMuted,
            fontSize = 13.sp,
        )
    }
}
