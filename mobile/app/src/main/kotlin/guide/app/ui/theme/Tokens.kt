package guide.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** DESIGN-TOKENS.md as Compose values. One accent per screen (SPEC §5.6). */
object GuideTokens {
    val Bg = Color(0xFFFAFAF7)
    val Surface = Color(0xFFFFFFFF)
    val Border = Color(0xFFE9E6E0)
    val Primary = Color(0xFF2F5D50)
    val Highlight = Color(0xFFC97B4A)
    val Text = Color(0xFF1F1D1B)
    val Text2 = Color(0xFF6B6862)
    val Danger = Color(0xFFB3453A)

    val CardRadius = 10.dp
    val ButtonRadius = 8.dp

    val Heading = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Text)
    val Body = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Text)
    val Chrome = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Text2)
}
