package app.fedilab.android.helper

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import app.fedilab.android.R
import app.fedilab.android.helper.RecyclerViewThreadLines.LineInfo
import app.fedilab.android.client.entities.api.Context as StatusContext

class RecyclerViewThreadLines(context: Context, private val lineInfoList: List<LineInfo>) : DividerItemDecoration(context, VERTICAL) {
    private val lineColors = threadLineColors.map { ResourcesCompat.getColor(context.resources, it, context.theme) }
    private val dashPathEffect = DashPathEffect(floatArrayOf(3.dpToPx, 3.dpToPx), 0F)
    private val borderColor = lineColors[0]
    private val commonPaint = Paint().apply {
        isDither = false
        strokeWidth = 1.5F.dpToPx
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.MITER
        color = borderColor
    }
    private val maxLevel = lineColors.size
    private val fontScale = PreferenceManager.getDefaultSharedPreferences(context).getFloat(context.getString(R.string.SET_FONT_SCALE), 1.1f).toInt()
    private val baseMargin: Int = 6.dpToPx.toInt()
    private val margin: Int = baseMargin * fontScale

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val level = lineInfoList[position].level
        val startMargin = margin * level + margin * fontScale
        if (parent.layoutDirection == View.LAYOUT_DIRECTION_LTR) outRect.left = startMargin else outRect.right = startMargin
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val view = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(view)
            val lineInfo = lineInfoList[position]
            val level = lineInfo.level

            for (j in 0..level) {
                val lineMargin = margin * j.coerceAtLeast(1) + 3.dpToPx
                val lineStart = if (parent.layoutDirection == View.LAYOUT_DIRECTION_LTR) lineMargin else c.width - lineMargin
                var lineTop: Float = (view.top - baseMargin).toFloat()
                val paint = Paint(commonPaint)
                paint.color = if (j > 0) lineColors[j - 1] else Color.GRAY

                // draw lines for below statuses
                if (j != level && j >= lineInfo.fullLinesStart && j <= lineInfo.fullLinesEnd)
                    c.drawLine(lineStart, lineTop, lineStart, view.bottom.toFloat(), paint)

                // draw vertical line for current statuses
                if (j == level && i != 0) {
                    // top the line starts at the middle of the above status
                    if (i > 0) lineTop -= parent.getChildAt(i - 1).height / 2 - 1 // '- 1' is to prevent overlapping with above horizontal line

                    // bottom of the line ends at the middle of the current status
                    var lineBottom = view.bottom.toFloat() - view.height / 2

                    // if below status has a full line for current level, extend the line to the bottom
                    if (i < childCount - 1) {
                        val nextLineInfo = lineInfoList[position + 1]
                        if (level >= nextLineInfo.fullLinesStart && level <= nextLineInfo.fullLinesEnd) {
                            lineBottom = view.bottom.toFloat()
                        }
                    }

                    // if level is max, use a dashed line
                    if (j == maxLevel) paint.pathEffect = dashPathEffect

                    c.drawLine(lineStart, lineTop, lineStart, lineBottom, paint)
                }

                // draw horizontal line for current statuses
                if (j == level) {
                    lineTop = view.bottom.toFloat() - view.height / 2
                    val lineEnd = lineStart + margin * 2
                    c.drawLine(lineStart - 1, lineTop, lineEnd, lineTop, paint) // 'lineStart - 1' is to properly connect with the vertical line
                }
            }
        }
    }

    data class LineInfo(var level: Int, var end: Boolean, var fullLinesStart: Int, var fullLinesEnd: Int)

    private val Int.dpToPx: Float
        get() = this * Resources.getSystem().displayMetrics.density

    private val Float.dpToPx: Float
        get() = this * Resources.getSystem().displayMetrics.density

    companion object {
        val threadLineColors = listOf(R.color.decoration_1, R.color.decoration_2, R.color.decoration_3, R.color.decoration_4, R.color.decoration_5)
    }
}

fun getThreadDecorationInfo(fediContext: StatusContext, selectedStatusId: String): MutableList<LineInfo> {
    val lineInfoList = mutableListOf<LineInfo>()
    repeat(fediContext.ancestors.size) { lineInfoList.add(LineInfo(0, true, 0, 0)) }
    lineInfoList.add(LineInfo(0, fediContext.descendants.isNotEmpty(), 0, 0))
    val descendantsLineInfoList = List(fediContext.descendants.size) { LineInfo(0, false, 0, 0) }
    for (i in fediContext.descendants.indices) {
        fediContext.descendants[i].let { status ->
            var level = 0
            if (status.in_reply_to_id != null) {
                if (status.in_reply_to_id == selectedStatusId)
                    level = 1
                else {
                    var replyToId: String? = status.in_reply_to_id
                    while (replyToId != null && level < RecyclerViewThreadLines.threadLineColors.size) {
                        level += 1
                        replyToId = fediContext.descendants.firstOrNull { it.id == replyToId }?.in_reply_to_id
                    }
                }
            }
            descendantsLineInfoList[i].level = level
            val firstReply = fediContext.descendants.firstOrNull { it.in_reply_to_id == status.id }
            if (firstReply == null) descendantsLineInfoList[i].end = true
        }
    }
    for (i in descendantsLineInfoList.indices) {
        var fullLinesStart = descendantsLineInfoList[i].level
        var fullLinesEnd = descendantsLineInfoList[i].level
        var fullLinesEndSet = false
        for (j in i + 1 until descendantsLineInfoList.lastIndex) {
            if (!fullLinesEndSet && descendantsLineInfoList[j].level < descendantsLineInfoList[i].level) {
                fullLinesEnd = descendantsLineInfoList[j].level
                fullLinesEndSet = true
            }
            fullLinesStart = descendantsLineInfoList[j].level.coerceAtMost(fullLinesStart)
        }
        descendantsLineInfoList[i].fullLinesStart = fullLinesStart
        descendantsLineInfoList[i].fullLinesEnd = fullLinesEnd
    }

    lineInfoList.addAll(descendantsLineInfoList)
    return lineInfoList
}
