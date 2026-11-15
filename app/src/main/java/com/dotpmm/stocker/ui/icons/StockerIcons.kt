package com.dotpmm.stocker.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** 24 dp stroke icons. Tint them with Icon(tint = …); they carry no colour of their own. */
object StockerIcons {
    val Back = stroke("Back", "M15 5l-7 7 7 7", 1.6f)
    val Close = stroke("Close", "M6 6l12 12M18 6L6 18", 1.6f)
    val Search = stroke("Search", "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14M16.5 16.5L21 21", 1.7f)
    val Refresh = stroke("Refresh", "M20 12a8 8 0 1 1-2.3-5.7M20 4v4h-4", 1.6f)
    val Add = stroke("Add", "M12 5v14M5 12h14", 1.6f)
    val Spark = stroke("Spark", "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z", 1.8f)
    val Arrow = stroke("Arrow", "M5 12h14M13 6l6 6-6 6", 1.8f)
    val External = stroke("External", "M14 4h6v6M20 4l-9 9M18 14v5H5V6h5", 1.6f)
    val Filter = stroke("Filter", "M4 7h16M7 12h10M10 17h4", 1.7f)
    val Grid = stroke("Grid", "M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z", 1.5f)
    val News = stroke("News", "M5 4h12v16H5zM9 8h4M9 12h4", 1.6f)
    val Settings = stroke("Settings", "M4 8h9M17 8h3M4 16h3M10 16h10", 1.7f)
    val Info = stroke("Info", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18M12 11v6M12 8v.5", 1.6f)
    val Check = stroke("Check", "M5 12l5 5L19 7", 1.9f)
    val Reset = stroke("Reset", "M4 12a8 8 0 1 0 2.3-5.7M4 4v4h4", 1.6f)
    val Compass = stroke("Compass", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18M15.5 8.5l-2 5-5 2 2-5z", 1.55f)
    val Chevron = stroke("Chevron", "M9 6l6 6-6 6", 1.7f)
    val Watchlist = stroke("Watchlist", "M4 6h16M4 12h16M4 18h10", 1.7f)
    val Events = stroke("Events", "M5 6h14v14H5zM5 10h14M9 3v4M15 3v4", 1.6f)
    val Bookmark = stroke("Bookmark", "M6 4h12v17l-6-4-6 4z", 1.6f)

    private fun stroke(name: String, pathData: String, width: Float = 1.7f): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()
}
