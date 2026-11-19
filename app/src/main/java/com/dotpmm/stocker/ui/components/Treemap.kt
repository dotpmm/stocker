package com.dotpmm.stocker.ui.components

import kotlin.math.max
import kotlin.math.min

/**
 * One laid-out treemap tile in pixel coordinates. [index] points back into the caller's weight
 * list, so callers can keep their own model objects alongside the geometry.
 */
data class TreemapCell(
    val index: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val area: Float get() = width * height
}

/**
 * Squarified treemap layout (Bruls, Huizing & van Wijk). Tiles are packed into rows along the
 * shorter side and the row is closed as soon as adding another item would make the worst aspect
 * ratio worse. The result fills exactly [width] × [height] with no overlapping cells.
 *
 * Weights are sorted descending internally; the returned [TreemapCell.index] still refers to the
 * original list. Non-positive or non-finite weights are skipped.
 */
fun squarify(weights: List<Float>, width: Float, height: Float): List<TreemapCell> {
    if (width <= 0f || height <= 0f || !width.isFinite() || !height.isFinite()) return emptyList()
    val ordered = weights.mapIndexedNotNull { index, weight ->
        if (weight.isFinite() && weight > 0f) index to weight else null
    }.sortedByDescending { it.second }
    if (ordered.isEmpty()) return emptyList()

    val total = ordered.sumOf { it.second.toDouble() }.toFloat()
    if (total <= 0f) return emptyList()
    val scale = width * height / total
    val areas = ordered.map { (index, weight) -> index to weight * scale }

    val cells = ArrayList<TreemapCell>(areas.size)
    var x = 0f
    var y = 0f
    var remainingWidth = width
    var remainingHeight = height
    var start = 0
    val epsilon = 0.0001f

    while (start < areas.size && remainingWidth > epsilon && remainingHeight > epsilon) {
        val side = min(remainingWidth, remainingHeight)
        val row = ArrayList<Pair<Int, Float>>()
        var rowArea = 0f
        var rowWorst = Float.POSITIVE_INFINITY
        var next = start

        while (next < areas.size) {
            val area = areas[next].second
            val candidate = worstAspect(row, area, rowArea, side)
            if (row.isEmpty() || candidate <= rowWorst) {
                row.add(areas[next])
                rowArea += area
                rowWorst = candidate
                next += 1
            } else {
                break
            }
        }

        val limit = if (remainingWidth >= remainingHeight) remainingWidth else remainingHeight
        val thickness = (rowArea / side).coerceIn(0f, limit)
        if (thickness <= epsilon) break

        if (remainingWidth >= remainingHeight) {
            // A vertical strip: items stack down its height.
            var offset = y
            for ((index, area) in row) {
                val itemHeight = area / thickness
                cells.add(TreemapCell(index, x, offset, thickness, itemHeight))
                offset += itemHeight
            }
            x += thickness
            remainingWidth -= thickness
        } else {
            // A horizontal strip: items sit side by side across its width.
            var offset = x
            for ((index, area) in row) {
                val itemWidth = area / thickness
                cells.add(TreemapCell(index, offset, y, itemWidth, thickness))
                offset += itemWidth
            }
            y += thickness
            remainingHeight -= thickness
        }
        start = next
    }
    return cells
}

private fun worstAspect(
    row: List<Pair<Int, Float>>,
    nextArea: Float,
    rowArea: Float,
    side: Float,
): Float {
    val total = rowArea + nextArea
    if (total <= 0f || side <= 0f) return Float.POSITIVE_INFINITY
    val thickness = total / side
    var worst = 0f
    for ((_, area) in row) {
        worst = max(worst, aspect(area, thickness))
    }
    return max(worst, aspect(nextArea, thickness))
}

private fun aspect(area: Float, thickness: Float): Float {
    if (area <= 0f || thickness <= 0f) return 0f
    val length = area / thickness
    return max(thickness / length, length / thickness)
}
