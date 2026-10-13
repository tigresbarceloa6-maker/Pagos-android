package mx.pagos.registro

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class Expense(
    val id: Long,
    val amount: Double,
    val category: String,
    val note: String,
    val ts: Long,
    val income: Boolean = false,
    val account: String = "",
)

/** Categoría con su degradado (color → color2) y emoji. */
data class Category(val name: String, val color: Color, val color2: Color, val emoji: String)

val ExpenseCategories = listOf(
    Category("Vivienda", Color(0xFF7B61FF), Color(0xFF4C7DFF), "🏠"),
    Category("Comida", Color(0xFF1E90FF), Color(0xFF3DB8FF), "🍽️"),
    Category("Supermercado", Color(0xFFFF4D7D), Color(0xFFFF7A9C), "🛒"),
    Category("Servicios", Color(0xFFF7B267), Color(0xFFFFD39A), "💡"),
    Category("Transporte", Color(0xFFFF6B57), Color(0xFFFF9466), "🚗"),
    Category("Compras", Color(0xFFF06BD8), Color(0xFFB86BFF), "🛍️"),
    Category("Ropa", Color(0xFF36C6F4), Color(0xFF7BE0FF), "👕"),
    Category("Salir", Color(0xFF3D7BFF), Color(0xFF6AA0FF), "🍻"),
    Category("Entretenimiento", Color(0xFFFF9F43), Color(0xFFFFC27A), "🍿"),
    Category("Suscripción", Color(0xFFFF4F6D), Color(0xFFFF8A9E), "📆"),
    Category("Extras", Color(0xFFA77BFF), Color(0xFFC9A8FF), "💁"),
    Category("Salud", Color(0xFF2FD5A0), Color(0xFF7BEFC6), "💊"),
)

val IncomeCategories = listOf(
    Category("Sueldo", Color(0xFF2EE6A8), Color(0xFF14B8A6), "💼"),
    Category("Ventas", Color(0xFF22C55E), Color(0xFF86EFAC), "🏷️"),
    Category("Regalo", Color(0xFFF472B6), Color(0xFFF9A8D4), "🎁"),
    Category("Otros ingresos", Color(0xFF38BDF8), Color(0xFF7DD3FC), "💰"),
)

private val AllCategories = ExpenseCategories + IncomeCategories

fun category(name: String): Category =
    AllCategories.firstOrNull { it.name == name }
        ?: Category(name, Color(0xFF94A3B8), Color(0xFFCBD5E1), "✨")

fun isIncomeCategory(name: String): Boolean = IncomeCategories.any { it.name == name }

private val moneyWhole: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX")).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}
private val moneyCents: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX")).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 2
}

fun money(value: Double): String {
    val v = abs(value)
    val s = if (Math.round(v * 100) % 100L == 0L) moneyWhole.format(v) else moneyCents.format(v)
    return if (value < 0) "-$s" else s
}

/** Texto plano para editar un monto (sin separadores). */
fun plainAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)

/** Etiqueta corta para ejes: 787, 6.4k, 22k. */
fun compact(value: Double): String {
    val a = abs(value)
    val s = when {
        a >= 1_000_000 -> oneDecimal(a / 1_000_000) + "M"
        a >= 10_000 -> (a / 1_000).roundToInt().toString() + "k"
        a >= 1_000 -> oneDecimal(a / 1_000) + "k"
        else -> a.roundToInt().toString()
    }
    return if (value < 0) "-$s" else s
}

private fun oneDecimal(x: Double): String {
    val r = (x * 10).roundToInt() / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
}

val MonthNames = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)
val MonthShort = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sept", "Oct", "Nov", "Dic")
private val DayShort = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

fun dayStart(ts: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ts
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun dayName(ms: Long): String {
    val c = Calendar.getInstance()
    c.timeInMillis = ms
    return DayShort[c.get(Calendar.DAY_OF_WEEK) - 1] + " " + c.get(Calendar.DAY_OF_MONTH) + " " +
        MonthShort[c.get(Calendar.MONTH)].lowercase()
}

fun dayLabel(dayStartMs: Long): String {
    val today = dayStart(System.currentTimeMillis())
    val c = Calendar.getInstance()
    c.timeInMillis = today
    c.add(Calendar.DAY_OF_YEAR, -1)
    return when (dayStartMs) {
        today -> "Hoy"
        c.timeInMillis -> "Ayer"
        else -> dayName(dayStartMs)
    }
}

enum class Period { Mensual, Anual }

private fun periodStart(period: Period, offset: Int): Calendar {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    if (period == Period.Mensual) {
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.add(Calendar.MONTH, offset)
    } else {
        c.set(Calendar.DAY_OF_YEAR, 1)
        c.add(Calendar.YEAR, offset)
    }
    return c
}

/** Inicio y fin (exclusivo) del periodo actual desplazado por [offset]. */
fun periodRange(period: Period, offset: Int): Pair<Long, Long> {
    val c = periodStart(period, offset)
    val start = c.timeInMillis
    c.add(if (period == Period.Mensual) Calendar.MONTH else Calendar.YEAR, 1)
    return start to c.timeInMillis
}

fun periodTitle(period: Period, offset: Int): String {
    if (offset == 0) return if (period == Period.Mensual) "Este mes" else "Este año"
    val c = periodStart(period, offset)
    if (period == Period.Anual) return c.get(Calendar.YEAR).toString()
    val name = MonthNames[c.get(Calendar.MONTH)]
    return if (c.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)) name else name + " " + c.get(Calendar.YEAR)
}

fun periodShort(period: Period, offset: Int): String {
    val c = periodStart(period, offset)
    return if (period == Period.Mensual) MonthShort[c.get(Calendar.MONTH)] else c.get(Calendar.YEAR).toString()
}

/** Cubetas del periodo: días del mes o meses del año. */
fun bucketCount(period: Period, offset: Int): Int =
    if (period == Period.Mensual) periodStart(period, offset).getActualMaximum(Calendar.DAY_OF_MONTH) else 12

fun bucketOf(period: Period, ts: Long): Int {
    val c = Calendar.getInstance()
    c.timeInMillis = ts
    return if (period == Period.Mensual) c.get(Calendar.DAY_OF_MONTH) - 1 else c.get(Calendar.MONTH)
}

/** Cubetas ya transcurridas, para el promedio. En periodos pasados son todas. */
fun elapsedBuckets(period: Period, offset: Int): Int {
    if (offset < 0) return bucketCount(period, offset)
    val c = Calendar.getInstance()
    return if (period == Period.Mensual) c.get(Calendar.DAY_OF_MONTH) else c.get(Calendar.MONTH) + 1
}

/** Filtros compartidos por las pastillas: Gastos/Ingresos, Mensual/Anual y cuenta. */
data class Filter(
    val income: Boolean = false,
    val period: Period = Period.Mensual,
    val account: String? = null,
)

fun List<Expense>.matching(f: Filter): List<Expense> =
    filter { it.income == f.income && (f.account == null || it.account == f.account) }

fun List<Expense>.forAccount(account: String?): List<Expense> =
    if (account == null) this else filter { it.account == account }

fun List<Expense>.inRange(range: Pair<Long, Long>): List<Expense> =
    filter { it.ts >= range.first && it.ts < range.second }

fun List<Expense>.byCategory(): List<Pair<String, Double>> =
    groupBy { it.category }
        .map { (cat, list) -> cat to list.sumOf { it.amount } }
        .sortedByDescending { it.second }

fun deltaPct(current: Double, previous: Double): Int? =
    if (previous > 0.0) ((current - previous) / previous * 100).roundToInt() else null

object Store {
    val items = mutableStateListOf<Expense>()
    private var loaded = false

    private fun file(ctx: Context) = File(ctx.applicationContext.filesDir, "gastos.json")

    @Synchronized
    fun load(ctx: Context) {
        Config.load(ctx)
        if (loaded) return
        loaded = true
        val f = file(ctx)
        if (!f.exists()) return
        try {
            val fallback = Config.accounts.firstOrNull() ?: "Efectivo"
            val arr = JSONArray(f.readText())
            val list = (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Expense(
                    id = o.getLong("id"),
                    amount = o.getDouble("amount"),
                    category = o.getString("category"),
                    note = o.optString("note"),
                    ts = o.getLong("ts"),
                    income = o.optBoolean("income", false),
                    account = o.optString("account").ifEmpty { fallback },
                )
            }
            items.clear()
            items.addAll(list.sortedByDescending { it.ts })
        } catch (_: Exception) {
            // Archivo dañado: se empieza vacío sin borrar el original.
        }
    }

    private fun save(ctx: Context) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("amount", it.amount)
                    .put("category", it.category)
                    .put("note", it.note)
                    .put("ts", it.ts)
                    .put("income", it.income)
                    .put("account", it.account)
            )
        }
        file(ctx).writeText(arr.toString())
    }

    fun add(ctx: Context, amount: Double, category: String, note: String, income: Boolean, account: String) {
        load(ctx)
        val now = System.currentTimeMillis()
        items.add(0, Expense(now, amount, category, note.trim(), now, income, account))
        save(ctx)
    }

    fun remove(ctx: Context, id: Long) {
        load(ctx)
        items.removeAll { it.id == id }
        save(ctx)
    }

    fun countFor(account: String): Int = items.count { it.account == account }
}

/** Cuentas, cuenta predeterminada y presupuestos por categoría. */
object Config {
    val accounts = mutableStateListOf<String>()
    val budgets = mutableStateMapOf<String, Double>()
    val defaultAccount = mutableStateOf("Efectivo")
    private var loaded = false

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    @Synchronized
    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(ctx)
        accounts.clear()
        try {
            val arr = JSONArray(p.getString("accounts", null) ?: "[]")
            for (i in 0 until arr.length()) {
                val name = arr.optString(i).trim()
                if (name.isNotEmpty() && name !in accounts) accounts.add(name)
            }
        } catch (_: Exception) {
        }
        if (accounts.isEmpty()) accounts.addAll(listOf("Efectivo", "Tarjeta"))
        val def = p.getString("defaultAccount", null)
        defaultAccount.value = if (def != null && def in accounts) def else accounts.first()
        budgets.clear()
        try {
            val o = JSONObject(p.getString("budgets", null) ?: "{}")
            o.keys().forEach { k ->
                val v = o.optDouble(k, 0.0)
                if (v > 0) budgets[k] = v
            }
        } catch (_: Exception) {
        }
    }

    private fun save(ctx: Context) {
        val o = JSONObject()
        budgets.forEach { (k, v) -> o.put(k, v) }
        prefs(ctx).edit()
            .putString("accounts", JSONArray(accounts.toList()).toString())
            .putString("defaultAccount", defaultAccount.value)
            .putString("budgets", o.toString())
            .apply()
    }

    fun setBudget(ctx: Context, category: String, value: Double) {
        load(ctx)
        if (value > 0) budgets[category] = value else budgets.remove(category)
        save(ctx)
    }

    fun addAccount(ctx: Context, name: String) {
        load(ctx)
        val n = name.trim()
        if (n.isEmpty() || n in accounts) return
        accounts.add(n)
        save(ctx)
    }

    fun removeAccount(ctx: Context, name: String) {
        load(ctx)
        if (accounts.size <= 1) return
        accounts.remove(name)
        if (defaultAccount.value == name) defaultAccount.value = accounts.first()
        save(ctx)
    }

    fun setDefault(ctx: Context, name: String) {
        load(ctx)
        if (name !in accounts) return
        defaultAccount.value = name
        save(ctx)
    }
}

/** Ajustes del asa lateral flotante. */
object Prefs {
    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun handleEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean("handleEnabled", false)

    fun setHandleEnabled(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean("handleEnabled", value).apply()
    }

    /** true = borde derecho, false = borde izquierdo. */
    fun handleRight(ctx: Context): Boolean = prefs(ctx).getBoolean("handleRight", true)

    fun setHandleRight(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean("handleRight", value).apply()
    }

    /** Posición vertical en píxeles; -1 si nunca se ha movido. */
    fun handleY(ctx: Context): Int = prefs(ctx).getInt("handleY", -1)

    fun setHandleY(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt("handleY", value).apply()
    }
}
