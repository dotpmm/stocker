package com.dotpmm.stocker.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** 24 dp stroke icons, same paths as the approved mockups. Tint them with Icon(tint = ...). */
object StockerIcons {
    val Back = stroke("Back", "M15 5l-7 7 7 7", 1.6f)
    val Refresh = stroke("Refresh", "M20 12a8 8 0 1 1-2.3-5.7M20 4v4h-4", 1.6f)
    val Add = stroke("Add", "M12 5v14M5 12h14", 1.6f)
    val Spark = stroke("Spark", "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z", 2f)
    val Arrow = stroke("Arrow", "M5 12h14M13 6l6 6-6 6", 2f)
    val Watchlist = stroke("Watchlist", "M4 6h16M4 12h16M4 18h10")
    val News = stroke("News", "M5 4h12v16H5zM9 8h4M9 12h4")
    val Events = stroke("Events", "M5 6h14v14H5zM5 10h14M9 3v4M15 3v4")
    val Settings = stroke("Settings", "M4 8h10M18 8h2M4 16h2M10 16h10")

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
