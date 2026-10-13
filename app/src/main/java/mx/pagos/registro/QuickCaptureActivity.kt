package mx.pagos.registro

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class QuickCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        Store.load(this)
        val fromGesture = intent.getBooleanExtra(EXTRA_GESTURE, false)
        setContent {
            AppTheme { CaptureScreen(fromGesture = fromGesture, onFinish = { finish() }) }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_GESTURE = "desde_gesto"
    }
}
