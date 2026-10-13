package mx.pagos.registro

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Widget con el gasto del mes y el botón "Registrar gasto". Se declara para
 * inicio y pantalla de bloqueo; en la de bloqueo solo aparece en teléfonos
 * cuyo sistema permite widgets de otras apps ahí.
 */
class PagosWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        manager.updateAppWidget(ids, views(context))
    }

    companion object {
        fun updateAll(ctx: Context) {
            try {
                val manager = AppWidgetManager.getInstance(ctx)
                val ids = manager.getAppWidgetIds(ComponentName(ctx, PagosWidget::class.java))
                if (ids.isNotEmpty()) manager.updateAppWidget(ids, views(ctx))
            } catch (_: Exception) {
            }
        }

        private fun views(ctx: Context): RemoteViews {
            Store.load(ctx)
            val month = Store.items.inRange(periodRange(Period.Mensual, 0))
            val spent = month.filter { !it.income }.sumOf { it.amount }
            val capture = PendingIntent.getActivity(
                ctx,
                10,
                Intent(ctx, QuickCaptureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val app = PendingIntent.getActivity(
                ctx,
                11,
                Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            return RemoteViews(ctx.packageName, R.layout.widget_pagos).apply {
                setTextViewText(R.id.widget_month, MonthNames[java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)])
                setTextViewText(R.id.widget_total, money(spent))
                setTextViewText(R.id.widget_count, if (month.size == 1) "1 movimiento" else "${month.size} movimientos")
                setOnClickPendingIntent(R.id.widget_root, app)
                setOnClickPendingIntent(R.id.widget_add, capture)
            }
        }
    }
}
