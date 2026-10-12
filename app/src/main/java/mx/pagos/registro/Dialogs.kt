package mx.pagos.registro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Tarjeta de alerta translúcida, muy redondeada, al estilo de iOS. */
@Composable
fun AlertCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Column(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Ui.AlertBg)
            .border(1.dp, Color(0x1AFFFFFF), shape)
            .tapNoRipple { }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun AlertTitle(text: String) {
    Text(
        text,
        color = Color(0xE6FFFFFF),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
fun AlertButtons(
    onCancel: () -> Unit,
    onOk: () -> Unit,
    okEnabled: Boolean = true,
    okText: String = "OK",
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CapsuleButton("Cancelar", Color(0x24FFFFFF), Modifier.weight(1f), onClick = onCancel)
        CapsuleButton(okText, Ui.Blue, Modifier.weight(1f), enabled = okEnabled, onClick = onOk)
    }
}

/** Campo del monto: texto centrado, cursor azul parpadeante y botón para borrar. */
@Composable
fun AmountField(value: String, onClear: () -> Unit) {
    val blink = rememberInfiniteTransition(label = "cursor")
    val cursor by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(530, easing = LinearEasing), RepeatMode.Reverse),
        label = "parpadeo",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isEmpty()) {
                Box(Modifier.width(2.dp).height(28.dp).alpha(cursor).background(Ui.Blue))
                Text("Monto", color = Color(0x59FFFFFF), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.width(2.dp).height(28.dp).alpha(cursor).background(Ui.Blue))
            }
        }
        if (value.isNotEmpty()) {
            GlyphIcon(
                R.drawable.ic_clear,
                Color(0x80FFFFFF),
                20.dp,
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
            )
        }
    }
}

private val KeyRows = listOf(
    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
    listOf("." to "", "0" to "", "⌫" to ""),
)

/** Teclado numérico acoplado abajo, como el de iOS. */
@Composable
fun NumericKeypad(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Ui.KeyboardBg, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .tapNoRipple { }
            .navigationBarsPadding()
            .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        KeyRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (key, letters) ->
                    val plain = key == "." || key == "⌫"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (plain) Color.Transparent else Ui.KeyBg)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKey(key)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            key == "⌫" -> GlyphIcon(R.drawable.ic_backspace, Color.White, 24.dp)
                            letters.isEmpty() -> Text(key, color = Color.White, fontSize = 25.sp)
                            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(key, color = Color.White, fontSize = 24.sp, lineHeight = 26.sp)
                                Text(
                                    letters,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun pressKey(amount: String, key: String): String = when (key) {
    "⌫" -> amount.dropLast(1)
    "." -> if (amount.contains('.')) amount else if (amount.isEmpty()) "0." else "$amount."
    else -> {
        val decimals = amount.substringAfter('.', "")
        when {
            amount.length >= 9 -> amount
            amount.contains('.') && decimals.length >= 2 -> amount
            amount == "0" -> key
            else -> amount + key
        }
    }
}

/** Campo de texto oscuro con marcador, para la nota o el nombre de una cuenta. */
@Composable
fun NoteField(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    focus: FocusRequester,
    onDone: () -> Unit,
    singleLine: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = onValue,
        textStyle = TextStyle(color = Color.White, fontSize = 17.sp, fontFamily = Inter),
        cursorBrush = SolidColor(Ui.Blue),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (singleLine) 52.dp else 78.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(horizontal = 14.dp, vertical = 15.dp)
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Color(0x59FFFFFF), fontSize = 17.sp)
                }
                inner()
            }
        },
    )
}

/** Valor de 0 a 1 que entra con resorte al aparecer. */
@Composable
fun rememberAppear(): Float {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 420f)) }
    return a.value
}

/** Alerta para capturar un monto dentro de la app (por ejemplo, un presupuesto). */
@Composable
fun AmountPrompt(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf(initial) }
    val a = rememberAppear()
    val shownAlpha = a.coerceIn(0f, 1f)
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * shownAlpha))
            .tapNoRipple(onDismiss)
    ) {
        AlertCard(
            Modifier
                .statusBarsPadding()
                .padding(top = 8.dp)
                .graphicsLayer {
                    val s = 0.92f + 0.08f * a
                    scaleX = s
                    scaleY = s
                    this.alpha = shownAlpha
                }
        ) {
            AlertTitle(title)
            AmountField(amount, onClear = { amount = "" })
            Text(
                "Deja el monto vacío para quitarlo.",
                color = Ui.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp),
            )
            AlertButtons(onCancel = onDismiss, onOk = { onConfirm(amount.toDoubleOrNull() ?: 0.0) })
        }
        NumericKeypad(
            onKey = { amount = pressKey(amount, it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { translationY = (1f - shownAlpha) * 320.dp.toPx() },
        )
    }
}

/** Alerta con un campo de texto de una línea. */
@Composable
fun TextPrompt(title: String, placeholder: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val a = rememberAppear()
    val shownAlpha = a.coerceIn(0f, 1f)
    LaunchedEffect(Unit) {
        delay(200)
        try {
            focus.requestFocus()
        } catch (_: Exception) {
        }
    }
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * shownAlpha))
            .tapNoRipple(onDismiss)
    ) {
        AlertCard(
            Modifier
                .statusBarsPadding()
                .padding(top = 8.dp)
                .graphicsLayer {
                    val s = 0.92f + 0.08f * a
                    scaleX = s
                    scaleY = s
                    this.alpha = shownAlpha
                }
        ) {
            AlertTitle(title)
            NoteField(
                value = text,
                onValue = { text = it.take(30) },
                placeholder = placeholder,
                focus = focus,
                onDone = { if (text.isNotBlank()) onConfirm(text.trim()) },
                singleLine = true,
            )
            AlertButtons(
                onCancel = onDismiss,
                onOk = { onConfirm(text.trim()) },
                okEnabled = text.isNotBlank(),
            )
        }
    }
}
