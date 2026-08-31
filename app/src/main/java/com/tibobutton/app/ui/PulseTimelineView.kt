package com.tibobutton.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.tibobutton.app.R
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class PulseTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var model = PulseTimelineModel(emptyList(), null)
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.card_outline)
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.accent)
        style = Paint.Style.FILL
    }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.accent_soft)
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val intervalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_tertiary)
        textSize = sp(9f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val dateFormatter = DateTimeFormatter.ofPattern("M/d", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.section_pulse)
    }

    fun setModel(newModel: PulseTimelineModel) {
        model = newModel
        contentDescription = when {
            newModel.points.isEmpty() -> context.getString(R.string.pulse_empty_no_history)
            newModel.points.size == 1 -> context.getString(R.string.pulse_empty_one)
            else -> context.getString(
                R.string.pulse_timeline_accessibility,
                newModel.points.size,
                newModel.averageIntervalHours?.let(::formatDuration) ?: "—"
            )
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val labelBaselineGap = maxOf(dp(23f), labelPaint.textSize + dp(8f))
        val timeBaselineGap = maxOf(
            dp(38f),
            labelBaselineGap + labelPaint.textSize + dp(5f)
        )
        val desiredHeight = maxOf(
            dp(112f),
            dp(51f) + timeBaselineGap + dp(12f)
        ).roundToInt()
        val width = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val points = model.points
        if (points.isEmpty()) return

        val left = dp(16f)
        val right = width - dp(16f)
        if (right <= left) return
        val labelBaselineGap = maxOf(dp(23f), labelPaint.textSize + dp(8f))
        val timeBaselineGap = maxOf(
            dp(38f),
            labelBaselineGap + labelPaint.textSize + dp(5f)
        )
        val intervalBaselineGap = maxOf(dp(13f), intervalPaint.textSize + dp(4f))
        val axisY = minOf(dp(51f), height - timeBaselineGap - dp(12f))
            .coerceAtLeast(dp(24f))
        val step = if (points.size == 1) 0f else (right - left) / (points.lastIndex.toFloat())

        if (points.size > 1) {
            canvas.drawLine(left, axisY, right, axisY, linePaint)
        }

        points.forEachIndexed { index, point ->
            val x = if (points.size == 1) (left + right) / 2f else left + step * index
            if (index == points.lastIndex) {
                canvas.drawCircle(x, axisY, dp(9f), haloPaint)
            }
            canvas.drawCircle(x, axisY, if (index == points.lastIndex) dp(5f) else dp(4f), pointPaint)

            point.intervalHours?.let { interval ->
                val previousX = left + step * (index - 1)
                canvas.drawText(
                    formatDuration(interval),
                    (previousX + x) / 2f,
                    axisY - intervalBaselineGap,
                    intervalPaint
                )
            }
        }

        val labelIndices = when {
            points.size <= 3 -> points.indices.toSet()
            else -> setOf(0, points.size / 2, points.lastIndex)
        }
        labelIndices.forEach { index ->
            val x = if (points.size == 1) (left + right) / 2f else left + step * index
            val local = points[index].occurredAt.atZone(ZoneId.systemDefault())
            canvas.drawText(local.format(dateFormatter), x, axisY + labelBaselineGap, labelPaint)
            canvas.drawText(local.format(timeFormatter), x, axisY + timeBaselineGap, labelPaint)
        }
    }

    private fun formatDuration(hours: Long): String = when {
        hours < 24 -> "${hours}h"
        hours % 24 == 0L -> "${hours / 24}d"
        else -> "${hours / 24}d${hours % 24}h"
    }

    private fun dp(value: Float): Float = value * density
    private fun sp(value: Float): Float = value * scaledDensity
}
