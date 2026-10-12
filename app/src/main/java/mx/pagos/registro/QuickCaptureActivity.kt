package mx.pagos.registro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class QuickCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        Store.load(this)
        setContent {
            AppTheme { CaptureScreen(onClose = { finish() }) }
        }
    }
}
