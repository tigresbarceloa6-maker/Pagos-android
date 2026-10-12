package mx.pagos.registro

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Expense(
    val id: Long,
    val amount: Double,
    val category: String,
    val note: String,
    val ts: Long,
)

data class Category(val name: String, val color: Color, val emoji: String)

val Categories = listOf(
    Category("Vivienda", Color(0xFF7C83FF), "🏠"),
    Category("Comida", Color(0xFF38BDF8), "🍽️"),
    Category("Supermercado", Color(0xFFFF5C8A), "🛒"),
    Category("Servicios", Color(0xFFFFC857), "💡"),
    Category("Transporte", Color(0xFFFF8A5B), "🚗"),
    Category("Compras", Color(0xFFF472D0), "🛍️"),
    Category("Ropa", Color(0xFFA3E635), "👕"),
    Category("Salir", Color(0xFF5EEAD4), "🎉"),
    Category("Entretenimiento", Color(0xFFC084FC), "🎬"),
    Category("Otros", Color(0xFF94A3B8), "✨"),
)

fun categoryColor(name: String): Color =
    Categories.firstOrNull { it.name == name }?.color ?: Color(0xFF94A3B8)

fun categoryEmoji(name: String): String =
    Categories.firstOrNull { it.name == name }?.emoji ?: "✨"

private val moneyWhole: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX")).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}
private val moneyCents: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX")).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 2
}

fun money(value: Double): String =
    if (Math.round(value * 100) % 100L == 0L) moneyWhole.format(value) else moneyCents.format(value)

fun monthName(offset: Int): String {
    val c = Calendar.getInstance()
    c.add(Calendar.MONTH, offset)
    return SimpleDateFormat("LLLL", Locale("es", "MX")).format(c.time)
        .replaceFirstChar { it.uppercase() }
}

fun dayStart(ts: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ts
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun dayLabel(dayStartMs: Long): String {
    val today = dayStart(System.currentTimeMillis())
    val yesterday = dayStart(today - 12L * 3600 * 1000)
    return when (dayStartMs) {
        today -> "Hoy"
        yesterday -> "Ayer"
        else -> SimpleDateFormat("d MMM", Locale("es", "MX")).format(Date(dayStartMs))
    }
}

/** Inicio y fin (exclusivo) del mes actual desplazado por [offset] meses. */
fun monthRange(offset: Int): Pair<Long, Long> {
    val c = Calendar.getInstance()
    c.add(Calendar.MONTH, offset)
    c.set(Calendar.DAY_OF_MONTH, 1)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    val start = c.timeInMillis
    c.add(Calendar.MONTH, 1)
    return start to c.timeInMillis
}

fun totalsByCategory(items: List<Expense>, offset: Int): List<Pair<String, Double>> {
    val (start, end) = monthRange(offset)
    return items
        .filter { it.ts in start until end }
        .groupBy { it.category }
        .map { (cat, list) -> cat to list.sumOf { it.amount } }
        .sortedByDescending { it.second }
}

fun countsByCategory(items: List<Expense>, offset: Int): Map<String, Int> {
    val (start, end) = monthRange(offset)
    return items
        .filter { it.ts in start until end }
        .groupingBy { it.category }
        .eachCount()
}

object Store {
    val items = mutableStateListOf<Expense>()
    private var loaded = false

    private fun file(ctx: Context) = File(ctx.applicationContext.filesDir, "gastos.json")

    @Synchronized
    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val f = file(ctx)
        if (!f.exists()) return
        try {
            val arr = JSONArray(f.readText())
            val list = (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Expense(
                    id = o.getLong("id"),
                    amount = o.getDouble("amount"),
                    category = o.getString("category"),
                    note = o.optString("note"),
                    ts = o.getLong("ts"),
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
            )
        }
        file(ctx).writeText(arr.toString())
    }

    fun add(ctx: Context, amount: Double, category: String, note: String) {
        load(ctx)
        val now = System.currentTimeMillis()
        items.add(0, Expense(now, amount, category, note.trim(), now))
        save(ctx)
    }

    fun remove(ctx: Context, id: Long) {
        load(ctx)
        items.removeAll { it.id == id }
        save(ctx)
    }
}

object Prefs {
    /** 0 = Volumen +, 1 = Volumen −, 2 = cualquiera */
    fun button(ctx: Context): Int =
        ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("button", 0)

    fun setButton(ctx: Context, value: Int) {
        ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("button", value).apply()
    }
}
