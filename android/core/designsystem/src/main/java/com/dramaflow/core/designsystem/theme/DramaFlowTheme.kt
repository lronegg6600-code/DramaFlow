package com.dramaflow.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class DfColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textInverse: Color,
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val border: Color,
    val whiteCard: Color,
)

@Immutable
data class DfGradients(
    val brand: Brush,
    val hero: Brush,
    val premium: Brush,
)

@Immutable
data class DfSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val section: Dp = 28.dp,
)

@Immutable
data class DfElevation(
    val low: Dp = 2.dp,
    val medium: Dp = 6.dp,
    val high: Dp = 12.dp,
)

@Immutable
data class DfShapes(
    val small: RoundedCornerShape = RoundedCornerShape(12.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(18.dp),
    val large: RoundedCornerShape = RoundedCornerShape(24.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(999.dp),
)

private val LightColors = DfColors(
    background = Color(0xFFF8F5FF),
    surface = Color.White,
    surfaceElevated = Color(0xFFF5F7FB),
    surfaceMuted = Color(0xFFF0EEF8),
    textPrimary = Color(0xFF1C1733),
    textSecondary = Color(0xFF6D6784),
    textInverse = Color.White,
    accent = Color(0xFFFF6B7A),
    accentStrong = Color(0xFFEF476F),
    accentSoft = Color(0xFFFFE4EA),
    success = Color(0xFF1DBA91),
    warning = Color(0xFFFFA63D),
    danger = Color(0xFFE55555),
    border = Color(0x1A1C1733),
    whiteCard = Color(0xFFFDFDFF),
)

private val DarkColors = DfColors(
    background = Color(0xFF120F1E),
    surface = Color(0xFF1A1629),
    surfaceElevated = Color(0xFF221D36),
    surfaceMuted = Color(0xFF2A2541),
    textPrimary = Color(0xFFF7F4FF),
    textSecondary = Color(0xFFC0BAD6),
    textInverse = Color(0xFF140F20),
    accent = Color(0xFFFF7A8B),
    accentStrong = Color(0xFFFF5F7A),
    accentSoft = Color(0x33FF7A8B),
    success = Color(0xFF35D2A7),
    warning = Color(0xFFFFB65B),
    danger = Color(0xFFFF7C7C),
    border = Color(0x26F7F4FF),
    whiteCard = Color(0xFF231D35),
)

private val DfTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

private val LocalDfColors = staticCompositionLocalOf { LightColors }
private val LocalDfGradients = staticCompositionLocalOf {
    DfGradients(
        brand = Brush.horizontalGradient(listOf(Color(0xFFFF7A7A), Color(0xFFFF4FB0))),
        hero = Brush.verticalGradient(listOf(Color(0xFFFFF1F4), Color(0xFFEAF6FF))),
        premium = Brush.horizontalGradient(listOf(Color(0xFFFFC96B), Color(0xFFFF8E52))),
    )
}
private val LocalDfSpacing = staticCompositionLocalOf { DfSpacing() }
private val LocalDfElevation = staticCompositionLocalOf { DfElevation() }
private val LocalDfShapes = staticCompositionLocalOf { DfShapes() }

@Composable
fun DramaFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val materialColors = if (darkTheme) {
        darkColorScheme(
            background = colors.background,
            surface = colors.surface,
            primary = colors.accentStrong,
            onPrimary = colors.textInverse,
            onSurface = colors.textPrimary,
            onBackground = colors.textPrimary,
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.surface,
            primary = colors.accentStrong,
            onPrimary = colors.textInverse,
            onSurface = colors.textPrimary,
            onBackground = colors.textPrimary,
        )
    }
    MaterialTheme(
        colorScheme = materialColors,
        typography = DfTypography,
        shapes = Shapes(
            small = DfShapes().small,
            medium = DfShapes().medium,
            large = DfShapes().large,
        ),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDfColors provides colors,
            LocalDfGradients provides LocalDfGradients.current,
            LocalDfSpacing provides DfSpacing(),
            LocalDfElevation provides DfElevation(),
            LocalDfShapes provides DfShapes(),
            content = content,
        )
    }
}

object DramaFlowThemeTokens {
    val colors: DfColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDfColors.current

    val gradients: DfGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalDfGradients.current

    val spacing: DfSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalDfSpacing.current

    val elevation: DfElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalDfElevation.current

    val shapes: DfShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalDfShapes.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}
