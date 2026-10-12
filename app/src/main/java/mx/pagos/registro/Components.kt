package mx.pagos.registro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Cuadro redondeado con borde de color y el emoji de la categoría. */
@Composable
fun CategoryBadge(category: String, size: Dp = 40.dp) {
    val c = categoryColor(category)
    Box(
        modifier = Modifier
            .size(size)
            .background(c.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .border(1.5.dp, c.copy(alpha = 0.9f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(categoryEmoji(category), fontSize = (size.value * 0.45f).sp)
    }
}

@Composable
fun Pill(text: String, selected: Boolean = false) {
    Box(
        modifier = Modifier
            .background(
                if (selected) Color(0x33FFFFFF) else Color(0x14FFFFFF),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DeltaPill(text: String, good: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).background(if (good) Ui.Good else Ui.Bad, CircleShape))
        Text(text, color = Ui.TextMuted, fontSize = 11.sp)
    }
}
