package com.smartvendor.ai.ocr.spatial

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class SpatialTextLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Float get() = left + width / 2.0f
    val centerY: Float get() = top + height / 2.0f
}

object SpatialTextReconstructor {

    fun areLinesGroupable(l1: SpatialTextLine, l2: SpatialTextLine): Boolean {
        val minH = min(l1.height, l2.height)
        val maxH = max(l1.height, l2.height)

        if (minH == 0 || maxH.toFloat() / minH > 2.0f) return false

        val yDist = abs(l1.centerY - l2.centerY)
        val isSameLine = yDist < minH * 0.5f

        if (isSameLine) {
            val gap = if (l1.centerX < l2.centerX) {
                l2.left - l1.right
            } else {
                l1.left - l2.right
            }
            if (gap < minH * 2.5f && gap > -minH * 0.5f) return true
        }

        val vGap = if (l1.centerY < l2.centerY) {
            l2.top - l1.bottom
        } else {
            l1.top - l2.bottom
        }

        val isAdjacentLine = vGap < minH * 1.5f && vGap > -minH * 0.5f

        if (isAdjacentLine) {
            val overlapLeft = max(l1.left, l2.left)
            val overlapRight = min(l1.right, l2.right)
            val xOverlap = overlapRight - overlapLeft

            if (xOverlap > -minH * 0.5f) {
                if (vGap < minH * 1.0f) return true
            }
        }
        return false
    }

    private fun sortLines(lines: List<SpatialTextLine>): List<SpatialTextLine> {
        if (lines.isEmpty()) return emptyList()
        val sortedByTop = lines.sortedBy { it.top }

        val rows = mutableListOf<MutableList<SpatialTextLine>>()
        var currentRow = mutableListOf(sortedByTop[0])
        var currentBottom = sortedByTop[0].bottom

        for (i in 1 until sortedByTop.size) {
            val line = sortedByTop[i]
            if (line.top < currentBottom - (line.height * 0.2f)) {
                currentRow.add(line)
                currentBottom = max(currentBottom, line.bottom)
            } else {
                rows.add(currentRow)
                currentRow = mutableListOf(line)
                currentBottom = line.bottom
            }
        }
        rows.add(currentRow)

        val sortedOut = mutableListOf<SpatialTextLine>()
        for (row in rows) {
            sortedOut.addAll(row.sortedBy { it.left })
        }
        return sortedOut
    }

    fun reconstruct(lines: List<SpatialTextLine>): List<SpatialTextLine> {
        if (lines.isEmpty()) return emptyList()

        try {
            val sortedLines = sortLines(lines)
            val groups = mutableListOf<MutableList<SpatialTextLine>>()

            for (line in sortedLines) {
                var merged = false
                for (i in groups.indices.reversed()) {
                    val group = groups[i]
                    val lastLine = group.last()
                    if (areLinesGroupable(lastLine, line)) {
                        group.add(line)
                        merged = true
                        break
                    }
                }
                if (!merged) {
                    groups.add(mutableListOf(line))
                }
            }

            return groups.map { group ->
                val text = group.joinToString(" ") { it.text }
                val left = group.minOf { it.left }
                val top = group.minOf { it.top }
                val right = group.maxOf { it.right }
                val bottom = group.maxOf { it.bottom }
                SpatialTextLine(text, left, top, right, bottom)
            }
        } catch (e: Exception) {
            // Safe fallback if geometry processing throws any unexpected exception
            return lines
        }
    }
    
    fun reconstructToString(lines: List<SpatialTextLine>): String {
        return reconstruct(lines).joinToString("\n") { it.text }
    }
}
