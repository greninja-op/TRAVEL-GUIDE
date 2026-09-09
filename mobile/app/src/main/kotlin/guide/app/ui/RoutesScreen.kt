package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import guide.app.ui.theme.GuideTokens

/** Phase 2 routes + day-plans: route list, 2h/half/full-day picker, stops. */
@Composable
fun RoutesScreen(
    routes: List<Trio>,
    onPickPlan: (minutes: Int) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Routes & day plans", style = GuideTokens.Heading)
        routes.forEach { Text("• ${it.name} — ${it.detail}", style = GuideTokens.Body) }
        Text("Plan my day", style = GuideTokens.Chrome)
        listOf(120 to "2 hours", 240 to "Half day", 480 to "Full day").forEach { (min, label) ->
            TextButton(onClick = { onPickPlan(min) }) { Text(label) }
        }
    }
}

data class Trio(val name: String, val detail: String)
