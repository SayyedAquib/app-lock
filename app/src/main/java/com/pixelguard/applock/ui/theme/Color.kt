package com.pixelguard.applock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// Material 3 Dark Color Tokens
// ==========================================
val PixelPrimaryDark = Color(0xFFA8C7FA)
val PixelOnPrimaryDark = Color(0xFF062E6F)
val PixelPrimaryContainerDark = Color(0xFF1B448C)
val PixelOnPrimaryContainerDark = Color(0xFFD6E3FF)

val PixelSecondaryDark = Color(0xFFBCC7DB)
val PixelOnSecondaryDark = Color(0xFF263141)
val PixelSecondaryContainerDark = Color(0xFF3D4758)
val PixelOnSecondaryContainerDark = Color(0xFFD8E3F8)

// OLED & Tonal Surface Container Scale (Dark)
val PixelOledBlack = Color(0xFF000000)
val PixelSurfaceContainerLowest = Color(0xFF0C1014)
val PixelSurfaceContainerLow = Color(0xFF13181E)
val PixelSurfaceContainer = Color(0xFF1A2027)
val PixelSurfaceContainerHigh = Color(0xFF242A33)
val PixelSurfaceContainerHighest = Color(0xFF2D353F)

val PixelBackgroundDark = PixelSurfaceContainerLowest
val PixelSurfaceDark = PixelSurfaceContainer
val PixelSurfaceVariantDark = PixelSurfaceContainerHigh
val PixelOnSurfaceDark = Color(0xFFE1E3E8)
val PixelOnSurfaceVariantDark = Color(0xFFC3C7D0)
val PixelOutlineDark = Color(0xFF8B919A)
val PixelOutlineVariantDark = Color(0xFF43474E)

// ==========================================
// Material 3 Light Color Tokens
// ==========================================
val PixelPrimaryLight = Color(0xFF0B57D0)
val PixelOnPrimaryLight = Color(0xFFFFFFFF)
val PixelPrimaryContainerLight = Color(0xFFD3E3FD)
val PixelOnPrimaryContainerLight = Color(0xFF041E49)

val PixelSecondaryLight = Color(0xFF4A5C74)
val PixelOnSecondaryLight = Color(0xFFFFFFFF)
val PixelSecondaryContainerLight = Color(0xFFD6E4F7)
val PixelOnSecondaryContainerLight = Color(0xFF061B2E)

val PixelBackgroundLight = Color(0xFFF8F9FA)
val PixelSurfaceLight = Color(0xFFFFFFFF)
val PixelSurfaceVariantLight = Color(0xFFE1E2EC)
val PixelOnSurfaceLight = Color(0xFF1A1C1E)
val PixelOnSurfaceVariantLight = Color(0xFF44474E)
val PixelOutlineLight = Color(0xFF74777F)
val PixelOutlineVariantLight = Color(0xFFC4C7D0)

val PixelSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val PixelSurfaceContainerLowLight = Color(0xFFF4F4F6)
val PixelSurfaceContainerLight = Color(0xFFEEEEF2)
val PixelSurfaceContainerHighLight = Color(0xFFE8E8ED)
val PixelSurfaceContainerHighestLight = Color(0xFFE2E2E7)

// Backward-compatible static references
val PixelGreen = Color(0xFF7BD88F)
val PixelRed = Color(0xFFFFB4AB)
val PixelAmber = Color(0xFFF9AB00)

// ==========================================
// Semantic Adaptive Color Contract
// ==========================================
data class SemanticColors(
    val amber: Color,
    val onAmber: Color,
    val amberContainer: Color,
    val onAmberContainer: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

val DarkSemanticColors = SemanticColors(
    amber = Color(0xFFF9AB00),
    onAmber = Color(0xFF000000),
    amberContainer = Color(0xFF432C00),
    onAmberContainer = Color(0xFFFFDDB3),
    success = Color(0xFF7BD88F),
    onSuccess = Color(0xFF003915),
    successContainer = Color(0xFF005322),
    onSuccessContainer = Color(0xFFA6F5AA)
)

val LightSemanticColors = SemanticColors(
    amber = Color(0xFF8F4C00), // Deep ochre >= 4.5:1 on light background
    onAmber = Color(0xFFFFFFFF),
    amberContainer = Color(0xFFFFDDB3),
    onAmberContainer = Color(0xFF2E1500),
    success = Color(0xFF1E8E3E), // Forest green >= 4.5:1 on light background
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFA6F5AA),
    onSuccessContainer = Color(0xFF00210A)
)

val LocalSemanticColors = staticCompositionLocalOf { DarkSemanticColors }

val MaterialTheme.semanticColors: SemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSemanticColors.current
