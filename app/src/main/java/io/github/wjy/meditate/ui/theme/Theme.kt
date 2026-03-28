package io.github.wjy.meditate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class AppStyle {
    MD3, MIUI
}

@Immutable
data class CustomColors(
    val cardBackground: Color = Color.Unspecified,
    val cardRadius: Dp = Dp.Unspecified,
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }
val LocalAppStyle = staticCompositionLocalOf { AppStyle.MD3 }

@Composable
fun PaperTheme(
    appStyle: AppStyle = AppStyle.MD3,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // MD3 规范红色
    val md3Red = Color(0xFFB3261E)      // 浅色模式红色
    val md3RedDark = Color(0xFFF2B8B5)  // 深色模式红色
    
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    
    // 应用 MD3 规范的红色
    val colorScheme = baseColorScheme.copy(
        error = if (darkTheme) md3RedDark else md3Red,
        onError = Color.White
    )

    val customColors = when (appStyle) {
        AppStyle.MD3 -> CustomColors(
            cardBackground = colorScheme.surfaceContainerLow, // 适配最新的 MD3 色值
            cardRadius = 12.dp
        )
        AppStyle.MIUI -> CustomColors(
            cardBackground = if (darkTheme) Color(0xFF1C1C1E) else Color.White,
            cardRadius = 24.dp
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 让状态栏颜色与 Surface 同步，实现沉浸感
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalCustomColors provides customColors,
        LocalAppStyle provides appStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
