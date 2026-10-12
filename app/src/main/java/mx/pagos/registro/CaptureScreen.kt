package mx.pagos.registro

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")

@Composable
fun CaptureScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }

    fun press(key: String) {
        amount = when (key) {
            "⌫" -> amount.dropLast(1)
            "." -> if (amount.contains('.')) amount else (if (amount.isEmpty()) "0." else "$amount.")
            else -> {
                val decimals = amount.substringAfter('.', "")
                if (amount.length >= 9 || (amount.contains('.') && decimals.length >= 2)) amount
                else if (amount == "0") key else amount + key
            }
        }
    }

    val valid = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && cat != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Nuevo gasto",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
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
                                    .height(52.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable { press(key) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(key, color = Color.White, fontSize = 22.sp)
                            }
                        }
                    }
                }

                Text(
                    "Categoría",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Categories.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            val selected = cat == c.name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        if (selected) c.color.copy(alpha = 0.28f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .border(
                                        BorderStroke(
                                            if (selected) 2.dp else 0.dp,
                                            if (selected) c.color else Color.Transparent,
                                        ),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable { cat = c.name },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    c.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Nota (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        enabled = valid,
                        onClick = {
                            Store.add(ctx, amount.toDouble(), cat!!, note)
                            Toast.makeText(ctx, "Gasto guardado", Toast.LENGTH_SHORT).show()
                            onClose()
                        },
                        modifier = Modifier.weight(1f),
