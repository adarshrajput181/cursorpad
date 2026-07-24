package com.example.cursorpad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class CursorView(context: Context): View(context) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

     private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
         style = Paint.Style.STROKE
         color = Color.WHITE
     }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    var cursorColor: Int = Color.RED
        set(value) {
            field = value
            fillPaint.color = value
            invalidate()
        }

    var borderSize: Float = 0f
        set(value) {
            field = value
            strokePaint.strokeWidth = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f

        // Draw the outer circle
        val outerRadius = width / 2f
        canvas.drawCircle(centerX, centerY, outerRadius, fillPaint)

        // Draw the border
        val strokeRadius = outerRadius - (borderSize / 2f)
        canvas.drawCircle(centerX, centerY, strokeRadius, strokePaint)

        // Draw the inner dot
        val dotRadius = 3f
        canvas.drawCircle(centerX, centerY, dotRadius, dotPaint)
    }
}