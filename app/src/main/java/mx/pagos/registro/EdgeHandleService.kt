package mx.pagos.registro

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

/**
 * Asa lateral flotante: una barrita en el borde de la pantalla. Tocarla o
 * deslizarla hacia adentro abre el registro rápido; mantenerla presionada
 * y arrastrar la cambia de altura.
 */
class EdgeHandleService : Service() {
    private var wm: WindowManager? = null
    private var handle: HandleView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        wm = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        removeHandle()
        if (!Prefs.handleEnabled(this) || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        addHandle()
        return START_STICKY
    }

    override fun onDestroy() {
        removeHandle()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Asa de registro rápido", NotificationManager.IMPORTANCE_MIN)
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, QuickCaptureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Registro rápido activo")
            .setContentText("Toca el asa lateral o esta notificación para registrar un gasto.")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun addHandle() {
        val w = wm ?: return
        val density = resources.displayMetrics.density
        val right = Prefs.handleRight(this)
        val height = (HANDLE_HEIGHT_DP * density).toInt()
        val screenH = resources.displayMetrics.heightPixels
        val savedY = Prefs.handleY(this)
        val params = WindowManager.LayoutParams(
            (HANDLE_WIDTH_DP * density).toInt(),
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or (if (right) Gravity.END else Gravity.START)
            x = 0
            y = if (savedY >= 0) savedY.coerceIn(0, screenH - height) else (screenH * 0.38f).toInt()
        }
        val view = HandleView(
            ctx = this,
            onRight = right,
            onOpen = { openCapture() },
            onDrag = { dy, startY ->
                params.y = (startY + dy).coerceIn(0, screenH - height)
                handle?.let { v -> runCatching { w.updateViewLayout(v, params) } }
            },
            currentY = { params.y },
            onDragEnd = { Prefs.setHandleY(this, params.y) },
        )
        try {
            w.addView(view, params)
            handle = view
        } catch (_: Exception) {
            handle = null
        }
    }

    private fun removeHandle() {
        val v = handle ?: return
        runCatching { wm?.removeView(v) }
        handle = null
    }

    private fun openCapture() {
        startActivity(
            Intent(this, QuickCaptureActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(QuickCaptureActivity.EXTRA_GESTURE, true)
        )
    }

    companion object {
        private const val CHANNEL = "asa"
        private const val NOTIFICATION_ID = 7
        private const val HANDLE_WIDTH_DP = 22
        private const val HANDLE_HEIGHT_DP = 120

        /** Arranca, refresca o detiene el asa según los ajustes y el permiso. */
        fun sync(ctx: Context) {
            val intent = Intent(ctx, EdgeHandleService::class.java)
            if (Prefs.handleEnabled(ctx) && Settings.canDrawOverlays(ctx)) {
                try {
                    ctx.startForegroundService(intent)
                } catch (_: Exception) {
                    // El sistema puede impedir el arranque en segundo plano; se reintenta al abrir la app.
                }
            } else {
                ctx.stopService(intent)
            }
        }
    }
}

@SuppressLint("ViewConstructor")
private class HandleView(
    ctx: Context,
    private val onRight: Boolean,
    private val onOpen: () -> Unit,
    private val onDrag: (dy: Int, startY: Int) -> Unit,
    private val currentY: () -> Int,
    private val onDragEnd: () -> Unit,
) : View(ctx) {
    private val density = resources.displayMetrics.density
    private val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xD9F2E8D8.toInt() }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66060912
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val rect = RectF()
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var startY = 0
    private var moving = false
    private var fired = false
    private var held = false
    private val longPress = Runnable {
        moving = true
        held = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, r: Int, bottom: Int) {
        super.onLayout(changed, left, top, r, bottom)
        if (Build.VERSION.SDK_INT >= 29) {
            // Evita que el gesto de "atrás" del sistema se coma los toques del asa.
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = (if (held) 7f else 5f) * density
        val h = height * 0.62f
        val margin = 4f * density
        val left = if (onRight) width - margin - w else margin
        rect.set(left, (height - h) / 2f, left + w, (height + h) / 2f)
        canvas.drawRoundRect(rect, w / 2f, w / 2f, pill)
        canvas.drawRoundRect(rect, w / 2f, w / 2f, edge)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val slop = 10f * density
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX
                downY = e.rawY
                downTime = e.eventTime
                startY = currentY()
                moving = false
                fired = false
                held = true
                invalidate()
                postDelayed(longPress, 450)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - downX
                val dy = e.rawY - downY
                if (moving) {
                    onDrag(dy.toInt(), startY)
                } else if (abs(dx) > slop || abs(dy) > slop) {
                    removeCallbacks(longPress)
                    val inward = if (onRight) -dx else dx
                    if (!fired && inward > 26f * density && inward > abs(dy)) {
                        fired = true
                        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onOpen()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPress)
                val dx = e.rawX - downX
                val dy = e.rawY - downY
                if (moving) {
                    onDragEnd()
                } else if (
                    e.actionMasked == MotionEvent.ACTION_UP && !fired &&
                    abs(dx) < slop && abs(dy) < slop && e.eventTime - downTime < 400
                ) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onOpen()
                }
                moving = false
                held = false
                invalidate()
            }
        }
        return true
    }
}

/** Vuelve a mostrar el asa después de reiniciar el teléfono o actualizar la app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        EdgeHandleService.sync(context)
    }
}
