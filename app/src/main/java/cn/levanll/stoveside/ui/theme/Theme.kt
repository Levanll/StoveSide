package cn.levanll.stoveside.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 温馨简约配色：奶油底 + 陶土橙主色 + 少量鼠尾草绿点缀。
 * 主色偏暖，深色模式用深可可底而不是纯黑，避免夜间刺眼。
 */
private val Cream = Color(0xFFFDF8F3)
private val Clay = Color(0xFFC96F4A)
private val ClayDeep = Color(0xFF9C4F2F)
private val Sage = Color(0xFF7E9B80)
private val Ink = Color(0xFF3D3229)
private val InkSoft = Color(0xFF7A6A5D)
private val Surface = Color(0xFFFFFDFA)
private val Cocoa = Color(0xFF211A16)
private val CocoaSurface = Color(0xFF2C2320)
private val ClayLight = Color(0xFFE8A88C)
private val CreamText = Color(0xFFF3E9E0)

val WarmLight = lightColorScheme(
    primary = Clay,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7E2D6),
    onPrimaryContainer = ClayDeep,
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EDE3),
    onSecondaryContainer = Color(0xFF3C4F3C),
    background = Cream,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2E8DF),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFD8C7B8),
    error = Color(0xFFB3261E),
)

val WarmDark = darkColorScheme(
    primary = ClayLight,
    onPrimary = Color(0xFF3A1D10),
    primaryContainer = Color(0xFF5A3423),
    onPrimaryContainer = Color(0xFFF7D9C8),
    secondary = Color(0xFF9FBB9F),
    onSecondary = Color(0xFF1E2C1E),
    secondaryContainer = Color(0xFF354535),
    onSecondaryContainer = Color(0xFFD9E7D8),
    background = Cocoa,
    onBackground = CreamText,
    surface = CocoaSurface,
    onSurface = CreamText,
    surfaceVariant = Color(0xFF3A2F2A),
    onSurfaceVariant = Color(0xFFC4B3A6),
    outline = Color(0xFF5A4B42),
    error = Color(0xFFF2B8B5),
)

@Composable
fun StoveTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (dark) WarmDark else WarmLight
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
