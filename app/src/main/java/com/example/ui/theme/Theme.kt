package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class BemoExtendedColors(
    val primaryTint: Color,
    val onPrimaryTint: Color,
    val surfaceAlt: Color,
    val border: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val warning: Color,
    val onWarning: Color,
    val warningTint: Color,
    val danger: Color,
    val dangerText: Color,
    val dangerTint: Color,
    val success: Color,
    val successText: Color,
    val successTint: Color,
    val incomeLine: Color,
    val expenseLine: Color
)

val LocalBemoColors = staticCompositionLocalOf {
    BemoExtendedColors(
        primaryTint = BemoLightPrimaryTint,
        onPrimaryTint = BemoLightOnPrimaryTint,
        surfaceAlt = BemoLightSurfaceAlt,
        border = BemoLightBorder,
        textMuted = BemoLightTextMuted,
        textDisabled = BemoLightTextDisabled,
        warning = BemoLightWarning,
        onWarning = BemoLightOnWarning,
        warningTint = BemoLightWarningTint,
        danger = BemoLightDanger,
        dangerText = BemoLightDangerText,
        dangerTint = BemoLightDangerTint,
        success = BemoLightSuccess,
        successText = BemoLightSuccessText,
        successTint = BemoLightSuccessTint,
        incomeLine = BemoLightIncomeLine,
        expenseLine = BemoLightExpenseLine
    )
}

private val LightColorScheme = lightColorScheme(
    primary = BemoLightPrimary,
    onPrimary = BemoLightOnPrimary,
    primaryContainer = BemoLightPrimaryTint,
    onPrimaryContainer = BemoLightOnPrimaryTint,
    secondary = BemoLightPrimaryBrand,
    onSecondary = Color.White,
    secondaryContainer = BemoLightSurfaceAlt,
    onSecondaryContainer = BemoLightText,
    tertiary = BemoLightSuccessText,
    onTertiary = Color.White,
    background = BemoLightBg,
    onBackground = BemoLightText,
    surface = BemoLightSurface,
    onSurface = BemoLightText,
    surfaceVariant = BemoLightSurfaceAlt,
    onSurfaceVariant = BemoLightTextMuted,
    outline = BemoLightBorder,
    outlineVariant = BemoLightBorder,
    error = BemoLightDangerText,
    onError = Color.White,
    errorContainer = BemoLightDangerTint,
    onErrorContainer = BemoLightDangerText
)

private val DarkColorScheme = darkColorScheme(
    primary = BemoDarkPrimary,
    onPrimary = BemoDarkOnPrimary,
    primaryContainer = BemoDarkPrimaryTint,
    onPrimaryContainer = BemoDarkOnPrimaryTint,
    secondary = BemoDarkPrimaryBrand,
    onSecondary = Color.White,
    secondaryContainer = BemoDarkSurfaceAlt,
    onSecondaryContainer = BemoDarkText,
    tertiary = BemoDarkSuccessText,
    onTertiary = Color.White,
    background = BemoDarkBg,
    onBackground = BemoDarkText,
    surface = BemoDarkSurface,
    onSurface = BemoDarkText,
    surfaceVariant = BemoDarkSurfaceAlt,
    onSurfaceVariant = BemoDarkTextMuted,
    outline = BemoDarkBorder,
    outlineVariant = BemoDarkBorder,
    error = BemoDarkDangerText,
    onError = Color.White,
    errorContainer = BemoDarkDangerTint,
    onErrorContainer = BemoDarkDangerText
)

@Composable
fun BemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        BemoExtendedColors(
            primaryTint = BemoDarkPrimaryTint,
            onPrimaryTint = BemoDarkOnPrimaryTint,
            surfaceAlt = BemoDarkSurfaceAlt,
            border = BemoDarkBorder,
            textMuted = BemoDarkTextMuted,
            textDisabled = BemoDarkTextDisabled,
            warning = BemoDarkWarning,
            onWarning = BemoDarkOnWarning,
            warningTint = BemoDarkWarningTint,
            danger = BemoDarkDanger,
            dangerText = BemoDarkDangerText,
            dangerTint = BemoDarkDangerTint,
            success = BemoDarkSuccess,
            successText = BemoDarkSuccessText,
            successTint = BemoDarkSuccessTint,
            incomeLine = BemoDarkIncomeLine,
            expenseLine = BemoDarkExpenseLine
        )
    } else {
        BemoExtendedColors(
            primaryTint = BemoLightPrimaryTint,
            onPrimaryTint = BemoLightOnPrimaryTint,
            surfaceAlt = BemoLightSurfaceAlt,
            border = BemoLightBorder,
            textMuted = BemoLightTextMuted,
            textDisabled = BemoLightTextDisabled,
            warning = BemoLightWarning,
            onWarning = BemoLightOnWarning,
            warningTint = BemoLightWarningTint,
            danger = BemoLightDanger,
            dangerText = BemoLightDangerText,
            dangerTint = BemoLightDangerTint,
            success = BemoLightSuccess,
            successText = BemoLightSuccessText,
            successTint = BemoLightSuccessTint,
            incomeLine = BemoLightIncomeLine,
            expenseLine = BemoLightExpenseLine
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalBemoColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
