package com.dotpmm.stocker.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.R

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun mono(resId: Int, weight: FontWeight) = Font(resId = resId, weight = weight)

val InterFamily = FontFamily(
    variable(R.font.inter, 400),
    variable(R.font.inter, 500),
    variable(R.font.inter, 600),
    variable(R.font.inter, 700),
)

val DisplayFamily = FontFamily(
    variable(R.font.space_grotesk, 500),
    variable(R.font.space_grotesk, 600),
)

/** IBM Plex Mono — tickers, prices, tables. Tabular by construction. */
val MonoFamily = FontFamily(
    mono(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    mono(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    mono(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

/** Typography scale from design.md §7. Prices never render in a proportional face. */
object Type {
    private const val TABULAR = "tnum"

    /** Hero index numeral: mono, 500, vertical white gradient applied by the caller. */
    val Hero = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1.6).sp,
        fontFeatureSettings = TABULAR, color = Ink.TextPrimary,
    )

    /** Section titles — display face, sentence case, tight tracking. */
    val Section = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp, color = Ink.TextPrimary,
    )

    val PanelTitle = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp, color = Ink.TextPrimary,
    )

    /** Company name in a deep-dive header. */
    val Header = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-0.52).sp, color = Ink.TextPrimary,
    )

    /** Tickers: uppercase mono, 600, 0.04em tracking. */
    val Ticker = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, letterSpacing = 0.4.sp, color = Ink.TextPrimary,
    )

    val TickerSmall = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, letterSpacing = 0.36.sp, color = Ink.TextSecondary,
    )

    /** Prices and percentages — mono + tabular so digits never jitter mid-animation. */
    val Data = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, letterSpacing = (-0.15).sp,
        fontFeatureSettings = TABULAR, color = Ink.TextPrimary,
    )

    val DataSmall = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, fontFeatureSettings = TABULAR, color = Ink.TextSecondary,
    )

    val DataLarge = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 24.sp, letterSpacing = (-0.44).sp,
        fontFeatureSettings = TABULAR, color = Ink.TextPrimary,
    )

    val Body = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 26.sp, color = Ink.TextSecondary,
    )

    val BodyTight = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 23.sp, color = Ink.TextSecondary,
    )

    val BodyStrong = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 21.sp, color = Ink.TextPrimary,
    )

    /** Uppercase micro-labels at 0.6875rem — the monochrome texture of the app. */
    val Micro = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, letterSpacing = 0.9.sp, color = Ink.TextMuted,
    )

    val MicroData = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp, letterSpacing = 0.6.sp,
        fontFeatureSettings = TABULAR, color = Ink.TextMuted,
    )

    val Caption = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, color = Ink.TextMuted,
    )

    val NavLabel = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 0.2.sp,
    )

    /** Slightly larger body copy for explanatory text where Caption read too small. */
    val Note = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, color = Ink.TextSecondary,
    )

    val ButtonLabel = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, letterSpacing = 0.1.sp, color = Ink.TextPrimary,
    )
}
