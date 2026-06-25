package org.myfastingapp.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import kotlin.math.roundToInt

object WidgetProgressRenderer {
    fun render(context: Context, progressFraction: Float, progressColor: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (168f * density).roundToInt().coerceAtLeast(168)
        val strokeWidth = 18f * density
        val outerStroke = 4f * density
        val inset = strokeWidth / 2f + outerStroke + 2f * density
        val rect = RectF(inset, inset, size - inset, size - inset)
        val center = size / 2f
        val progress = progressFraction.coerceAtLeast(0f)
        val baseProgress = progress.coerceIn(0f, 1f)
        val visibleProgress = if (baseProgress in 0f..0.012f && progress > 0f) 0.012f else baseProgress

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            setStrokeWidth(strokeWidth)
            strokeCap = Paint.Cap.ROUND
            color = Color.rgb(240, 236, 230)
        }

        canvas.drawArc(rect, 0f, 360f, false, paint)

        if (visibleProgress > 0f) {
            paint.shader = SweepGradient(
                center,
                center,
                intArrayOf(progressColor, lighten(progressColor), progressColor),
                floatArrayOf(0f, 0.55f, 1f),
            )
            paint.strokeCap = if (visibleProgress >= 0.999f) Paint.Cap.BUTT else Paint.Cap.ROUND
            canvas.drawArc(rect, 90f, 360f * visibleProgress, false, paint)
            paint.shader = null
        }

        if (progress > 1f) {
            val extraProgress = ((progress - 1f) % 1f).takeIf { it > 0.01f } ?: 1f
            val extraInset = inset - 8f * density
            val extraRect = RectF(extraInset, extraInset, size - extraInset, size - extraInset)
            paint.color = WINE
            paint.setStrokeWidth(outerStroke)
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(extraRect, 90f, 360f * extraProgress, false, paint)
        }

        return bitmap
    }

    private fun lighten(color: Int): Int {
        val red = (Color.red(color) + 70).coerceAtMost(255)
        val green = (Color.green(color) + 70).coerceAtMost(255)
        val blue = (Color.blue(color) + 70).coerceAtMost(255)
        return Color.rgb(red, green, blue)
    }

    private const val WINE = 0xFF861342.toInt()
}
