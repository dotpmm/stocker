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

/** Text styles taken from the approved Obsidian mockups. Numerals are tabular. */
object StockerText {
    private const val TABULAR = "tnum"

    val ScreenTitle = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.64).sp, color = Obsidian.Text,
    )
    val Price = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = (-0.92).sp,
        fontFeatureSettings = TABULAR, color = Obsidian.Text,
    )
    val RowPrice = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, fontFeatureSettings = TABULAR, color = Obsidian.Text,
    )
    val IndexValue = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, fontFeatureSettings = TABULAR, color = Obsidian.Text,
    )
    val DayNumber = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.68).sp,
        fontFeatureSettings = TABULAR, color = Obsidian.Text,
    )
    val TabLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, letterSpacing = (-0.22).sp, color = Obsidian.Text,
    )
    val TabSmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, letterSpacing = 0.28.sp, color = Obsidian.Text,
    )
    val Ticker = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, color = Obsidian.Text,
    )
    val Headline = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, color = Obsidian.Text,
    )
    val Change = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, fontFeatureSettings = TABULAR,
    )
    val ChangeSmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, fontFeatureSettings = TABULAR,
    )
    val Body = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, color = Obsidian.TextMuted,
    )
    val BodyStrong = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, color = Obsidian.TextSecondary,
    )
    val Meta = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, fontFeatureSettings = TABULAR, color = Obsidian.TextMuted,
    )
    val Caption = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, color = Obsidian.TextMuted,
    )
    val SectionLabel = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, letterSpacing = 1.2.sp, color = Obsidian.TextMuted,
    )
    val Month = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, letterSpacing = 1.44.sp, color = Obsidian.Accent,
    )
    val NavLabel = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp,
    )
    val Action = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, letterSpacing = 0.15.sp, color = androidx.compose.ui.graphics.Color.Black,
    )
}
