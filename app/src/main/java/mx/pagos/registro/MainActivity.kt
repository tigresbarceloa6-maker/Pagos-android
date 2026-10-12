package mx.pagos.registro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.load(this)
        setContent { AppTheme { Root(tick.intValue) } }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
    }
}

@Composable
private fun Root(tick: Int) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(1) }
    val tabs = listOf("Actividad" to "☰", "Resumen" to "◔", "Análisis" to "▥", "Gesto" to "◉")

    AppBackground {
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                0 -> ActivityScreen()
                1 -> SummaryScreen()
                2 -> AnalysisScreen()
                else -> GestureScreen(tick)
            }
        }

        if (tab != 3) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 104.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Ui.Accent)
                    .clickable { ctx.startActivity(Intent(ctx, QuickCaptureActivity::class.java)) },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 28.sp, color = Color(0xFF0B1126))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(Color(0xF0141B38), RoundedCornerShape(28.dp))
                .border(1.dp, Ui.GlassBorder, RoundedCornerShape(28.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEachIndexed { i, (label, glyph) ->
                val selected = tab == i
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (selected) Color(0x40A9B4FF) else Color.Transparent)
                        .clickable { tab = i }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(glyph, fontSize = 18.sp, color = if (selected) Color.White else Ui.TextMuted)
                    Text(label, fontSize = 11.sp, color = if (selected) Color.White else Ui.TextMuted)
                }
            }
        }
    }
}
