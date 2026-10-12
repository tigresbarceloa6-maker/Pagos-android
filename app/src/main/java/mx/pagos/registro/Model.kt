package mx.pagos.registro

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class Expense(
    val id: Long,
    val amount: Double,
    val category: String,
    val note: String,
    val ts: Long,
)

data class Category(val name: String, val color: Color)

val Categories = listOf(
    Category("Vivienda", Color(0xFF8E7CFF)),
    Category("Comida", Color(0xFF3FA9FF)),
    Category("Supermercado", Color(0xFF2ED3B7)),
    Category("Servicios", Color(0xFFFFC857)),
    Category("Transporte", Color(0xFFFF7A59)),
    Category("Compras", Color(0xFFFF5C9D)),
    Category("Ropa", Color(0xFFB0E05A)),
    Category("Salir", Color(0xFF5B6CFF)),
    Category("Entretenimiento", Color(0xFFC77DFF)),
    Category("Otros", Color(0xFF9AA3BF)),
)

fun categoryColor(name: String): Color =
    Categories.firstOrNull { it.name == name }?.color ?: Color(0xFF9AA3BF)

private val moneyFormat: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale("es", "MX"))

fun money(value: Double): String = moneyFormat.format(value)

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
