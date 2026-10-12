package mx.pagos.registro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class QuickCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        Store.load(this)
        setContent {
            AppTheme { CaptureScreen(onFinish = { finish() }) }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
