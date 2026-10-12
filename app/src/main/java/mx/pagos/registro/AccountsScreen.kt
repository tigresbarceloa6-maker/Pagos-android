package mx.pagos.registro

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccountEmoji = listOf("💵", "💳", "🏦", "👛", "🪙", "💼")

@Composable
fun AccountsScreen(onSettings: () -> Unit, onAddAccount: () -> Unit) {
    val ctx = LocalContext.current
    val items = Store.items
    val accounts = Config.accounts.toList()
    val def = Config.defaultAccount.value
    val balances = accounts.map { a ->
        a to items.filter { it.account == a }.sumOf { if (it.income) it.amount else -it.amount }
    }
    val total = balances.sumOf { it.second }
    val month = items.inRange(periodRange(Period.Mensual, 0))
    val inc = month.filter { it.income }.sumOf { it.amount }
    val exp = month.filter { !it.income }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = LocalBottomPad.current),
    ) {
        TopBar(onSettings)
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Saldo total", color = Ui.TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(money(total), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DotText("Ingresos " + money(inc), Ui.IncomeTeal)
                DotText("Gastos " + money(exp), Ui.ExpenseBlue)
            }
            Spacer(Modifier.height(2.dp))
            Text("Este mes", color = Ui.TextMuted, fontSize = 11.sp)
        }

        SectionLabel("Cuentas", accounts.size.toString())
        Column(Modifier.fillMaxWidth().glass(24.dp).padding(vertical = 6.dp)) {
            balances.forEachIndexed { i, (name, balance) ->
                val count = Store.countFor(name)
                AccountRow(
                    name = name,
                    emoji = AccountEmoji[i % AccountEmoji.size],
                    balance = balance,
                    count = count,
                    isDefault = name == def,
                    canDelete = accounts.size > 1 && count == 0,
                    onDefault = { Config.setDefault(ctx, name) },
                    onDelete = { Config.removeAccount(ctx, name) },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddAccount)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x1A0A84FF)),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(R.drawable.ic_add, Ui.Blue, 20.dp)
                }
                Spacer(Modifier.width(12.dp))
                Text("Agregar cuenta", color = Ui.Blue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
        Text(
            "Los movimientos nuevos se guardan en la cuenta predeterminada. Toca una cuenta para cambiarla.",
            color = Ui.TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp),
        )
    }
}

@Composable
private fun AccountRow(
    name: String,
    emoji: String,
    balance: Double,
    count: Int,
    isDefault: Boolean,
    canDelete: Boolean,
    onDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { menu = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 19.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isDefault) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Predeterminada",
                            color = Ui.Accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(Color(0x26A9B4FF), RoundedCornerShape(50))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    if (count == 1) "1 movimiento" else "$count movimientos",
                    color = Ui.TextMuted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                money(balance),
                color = when {
                    balance > 0 -> Ui.Good
                    balance < 0 -> Ui.Bad
                    else -> Color.White
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            modifier = Modifier.background(Ui.Menu),
        ) {
            if (!isDefault) {
                DropdownMenuItem(
                    text = { Text("Usar como predeterminada", color = Color.White) },
                    onClick = {
                        menu = false
                        onDefault()
                    },
                )
            }
            if (canDelete) {
                DropdownMenuItem(
                    text = { Text("Eliminar cuenta", color = Ui.Bad) },
                    onClick = {
                        menu = false
                        onDelete()
                    },
                )
            }
            if (isDefault && !canDelete) {
                DropdownMenuItem(
                    text = { Text("Ya es la cuenta predeterminada", color = Ui.TextMuted) },
                    onClick = { menu = false },
                )
            }
        }
    }
}
