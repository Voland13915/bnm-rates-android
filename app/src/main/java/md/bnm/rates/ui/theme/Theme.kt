package md.bnm.rates.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F4E79),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F5),
    onPrimaryContainer = Color(0xFF0B2A47),
    secondaryContainer = Color(0xFFFBEAC6),
    onSecondaryContainer = Color(0xFF5C3B00)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C4EE),
    onPrimary = Color(0xFF0B2A47),
    primaryContainer = Color(0xFF2A4A70),
    onPrimaryContainer = Color(0xFFD6E4F5),
    secondaryContainer = Color(0xFF4E3F22),
    onSecondaryContainer = Color(0xFFF2CD88)
)

/** Цвета тенденции: рост и падение курса. */
object TrendColors {
    val up = Color(0xFF2E9E4F)
    val down = Color(0xFFD64541)
}

@Composable
fun RatesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
