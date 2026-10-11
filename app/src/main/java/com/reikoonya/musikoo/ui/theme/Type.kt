package com.reikoonya.musikoo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val baseTypography = Typography()

// Expressive typography: dense, heavy headings with slightly tight tracking,
// bold labels. The system font is used, so nothing needs to be added to the project.
val Typography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
    displayMedium = baseTypography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
    headlineLarge = baseTypography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
    headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = baseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = baseTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
)

// Plain Material 3 typography (as before the redesign), enabled in Settings
val ClassicTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
