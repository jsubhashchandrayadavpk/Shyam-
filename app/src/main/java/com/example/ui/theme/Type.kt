package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val RozhaOneFontFamily = FontFamily(
    Font(R.font.rozha_one, FontWeight.Normal)
)

val NotoDevanagariFontFamily = FontFamily(
    Font(R.font.noto_sans_devanagari, FontWeight.Normal),
    Font(R.font.noto_sans_devanagari, FontWeight.Medium),
    Font(R.font.noto_sans_devanagari, FontWeight.SemiBold),
    Font(R.font.noto_sans_devanagari, FontWeight.Bold)
)

val ShyamTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RozhaOneFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = RozhaOneFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 38.sp,
        lineHeight = 46.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RozhaOneFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoDevanagariFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NotoDevanagariFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NotoDevanagariFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoDevanagariFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoDevanagariFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NotoDevanagariFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NotoDevanagariFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
