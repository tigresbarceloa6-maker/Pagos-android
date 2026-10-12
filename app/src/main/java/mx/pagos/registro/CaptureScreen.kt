package mx.pagos.registro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Registro rápido en 3 pasos como en iOS: alerta "Ingresa el monto" arriba con
 * teclado numérico abajo, menú "Categoría" y nota. Al guardar muestra una
 * píldora con ✓ y se cierra sola. Se muestra sobre cualquier app, incluso bloqueada.
 */
@Composable
fun CaptureScreen(fromGesture: Boolean, onFinish: () -> Unit) {
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current
    var step by remember { mutableIntStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var closing by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf(fromGesture) }

    LaunchedEffect(Unit) {
        shown = true
        if (fromGesture) {
            delay(1700)
            banner = false
        }
    }
    LaunchedEffect(closing) {
        if (closing) {
            delay(240)
            onFinish()
        }
    }
    LaunchedEffect(step) {
        if (step == 3) {
            delay(1400)
            closing = true
        }
    }

    BackHandler {
        if (step in 1..2) step -= 1 else closing = true
    }

    fun save() {
        val value = amount.toDoubleOrNull() ?: return
        val c = cat ?: return
        if (value <= 0.0) return
        focusManager.clearFocus()
        Store.add(ctx, value, c, note, isIncomeCategory(c), Config.defaultAccount.value)
        step = 3
    }

    val visible = shown && !closing
    val dim by animateFloatAsState(if (visible && step < 3) 1f else 0f, tween(220), label = "atenuado")

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * dim))
            .tapNoRipple { }
    ) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 6.dp)) {
            AnimatedVisibility(
                visible = banner && visible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Box(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                    GestureBanner()
                }
            }
            AnimatedVisibility(
                visible = visible && step < 3,
                enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.75f, stiffness = 420f), initialScale = 0.9f),
                exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.95f),
            ) {
                AlertCard(Modifier.animateContentSize()) {
                    AnimatedContent(
                        targetState = minOf(step, 2),
                        transitionSpec = {
                            (fadeIn(tween(180)) togetherWith fadeOut(tween(120))) using SizeTransform(clip = false)
                        },
                        label = "paso",
                    ) { s ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (s) {
                                0 -> AmountStep(
                                    amount = amount,
                                    onClear = { amount = "" },
                                    onCancel = { closing = true },
                                    onOk = { step = 1 },
                                )
                                1 -> CategoryStep(
                                    onPick = {
                                        cat = it
                                        step = 2
                                    },
                                    onCancel = { closing = true },
                                )
                                else -> NoteStep(
                                    note = note,
                                    onNote = { note = it },
                                    onCancel = { closing = true },
                                    onOk = { save() },
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = step == 3 && !closing,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 10.dp),
            enter = fadeIn() + scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f), initialScale = 0.6f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f),
        ) {
            DonePill(amount, cat ?: "")
        }

        AnimatedVisibility(
            visible = visible && step == 0,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.9f, stiffness = 500f)) { it },
            exit = slideOutVertically(tween(200)) { it },
        ) {
            NumericKeypad(onKey = { amount = pressKey(amount, it) })
        }
    }
}

@Composable
private fun AmountStep(amount: String, onClear: () -> Unit, onCancel: () -> Unit, onOk: () -> Unit) {
    AlertTitle("Ingresa el monto")
    AmountField(amount, onClear)
    AlertButtons(
        onCancel = onCancel,
        onOk = onOk,
        okEnabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0,
    )
}

@Composable
private fun CategoryStep(onPick: (String) -> Unit, onCancel: () -> Unit) {
    AlertTitle("Categoría")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 440.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x14FFFFFF))
            .verticalScroll(rememberScrollState()),
    ) {
        ExpenseCategories.forEachIndexed { i, c ->
            if (i > 0) MenuDivider()
            MenuRow(c) { onPick(c.name) }
        }
        Text(
            "Ingresos",
            color = Ui.TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        IncomeCategories.forEachIndexed { i, c ->
            if (i > 0) MenuDivider()
            MenuRow(c) { onPick(c.name) }
        }
    }
    CapsuleButton("Cancelar", Color(0x24FFFFFF), Modifier.fillMaxWidth(), onClick = onCancel)
}

@Composable
private fun MenuRow(c: Category, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(c.emoji, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(c.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MenuDivider() {
    Box(
        Modifier
            .padding(start = 46.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0x1FFFFFFF))
    )
}

@Composable
private fun NoteStep(note: String, onNote: (String) -> Unit, onCancel: () -> Unit, onOk: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(250)
        try {
            focus.requestFocus()
        } catch (_: Exception) {
        }
    }
    NoteField(
        value = note,
        onValue = onNote,
        placeholder = "Nota (opcional)",
        focus = focus,
        onDone = onOk,
    )
    AlertButtons(onCancel = onCancel, onOk = onOk)
}

/** Aviso superior, como el de "Tocar atrás" en iOS. */
@Composable
private fun GestureBanner() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xF0121318))
            .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(Ui.Blue),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(R.drawable.ic_volume, Color.White, 16.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Botón de volumen", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Doble pulsación detectada", color = Color(0xB3FFFFFF), fontSize = 11.sp)
        }
    }
}

/** Confirmación tipo isla: píldora negra con ✓. */
@Composable
private fun DonePill(amount: String, category: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black)
            .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(Ui.Good),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(R.drawable.ic_check, Color(0xFF06231D), 18.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Guardado", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                money(amount.toDoubleOrNull() ?: 0.0) + " · " + category,
                color = Color(0xB3FFFFFF),
                fontSize = 12.sp,
            )
        }
    }
}
