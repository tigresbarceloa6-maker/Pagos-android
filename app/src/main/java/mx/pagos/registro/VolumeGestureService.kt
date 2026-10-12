package mx.pagos.registro

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Detecta la doble pulsación del botón de volumen y abre el registro rápido.
 * No consume las teclas, así que el volumen sigue cambiando con normalidad.
 */
class VolumeGestureService : AccessibilityService() {

    private var lastCode = 0
    private var lastTime = 0L

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false
        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) return false

        val allowed = when (Prefs.button(this)) {
            0 -> code == KeyEvent.KEYCODE_VOLUME_UP
            1 -> code == KeyEvent.KEYCODE_VOLUME_DOWN
            else -> true
        }
        if (!allowed) return false

        val now = SystemClock.uptimeMillis()
        if (code == lastCode && now - lastTime <= WINDOW_MS) {
            lastTime = 0L
            lastCode = 0
            openCapture()
        } else {
            lastCode = code
            lastTime = now
        }
        return false
    }

    private fun openCapture() {
        val intent = Intent(this, QuickCaptureActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(QuickCaptureActivity.EXTRA_GESTURE, true)
        startActivity(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object {
        private const val WINDOW_MS = 450L
    }
}
