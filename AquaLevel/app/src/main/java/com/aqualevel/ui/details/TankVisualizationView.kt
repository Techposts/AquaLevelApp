package com.aqualevel.ui.details

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.aqualevel.R
import kotlin.math.min

/**
 * Custom view for visualizing water tank level
 */
class TankVisualizationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint objects
    private val tankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 15f
    }

    private val tankBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.FILL
    }

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 60
        style = Paint.Style.FILL
    }

    private val levelMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        alpha = 120
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val tankRect = RectF()
    private val waterRect = RectF()
    private val markerRect = RectF()

    // Wave properties
    private val wavePath = Path()
    private var wavePhase = 0f
    private val waveSpeed = 0.1f
    private val waveAmplitude = 10f

    // Water level (0.0 to 1.0)
    private var waterLevel = 0.5f

    // Colors
    private var waterColorStart = ContextCompat.getColor(context, R.color.blue_water_light)
    private var waterColorEnd = ContextCompat.getColor(context, R.color.blue_water_dark)

    // Animation
    private var animator: android.animation.ValueAnimator? = null

    init {
        // Get attributes from XML if any
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TankVisualizationView)

        try {
            waterLevel = typedArray.getFloat(R.styleable.TankVisualizationView_waterLevel, 0.5f)
            waterColorStart = typedArray.getColor(
                R.styleable.TankVisualizationView_waterColorStart,
                ContextCompat.getColor(context, R.color.blue_water_light)
            )
            waterColorEnd = typedArray.getColor(
                R.styleable.TankVisualizationView_waterColorEnd,
                ContextCompat.getColor(context, R.color.blue_water_dark)
            )
        } finally {
            typedArray.recycle()
        }

        // Start wave animation
        startWaveAnimation()
    }

    /**
     * Set water level (0.0 to 1.0)
     */
    fun setWaterLevel(level: Float) {
        val newLevel = level.coerceIn(0f, 1f)

        // Animate to new level
        animator?.cancel()

        animator = android.animation.ValueAnimator.ofFloat(waterLevel, newLevel).apply {
            duration = 1000
            interpolator = android.view.animation.DecelerateInterpolator()

            addUpdateListener { animation ->
                waterLevel = animation.animatedValue as Float
                invalidate()
            }

            start()
        }
    }

    /**
     * Set water colors
     */
    fun setWaterColors(startColor: Int, endColor: Int) {
        waterColorStart = startColor
        waterColorEnd = endColor
        invalidate()
    }

    private fun startWaveAnimation() {
        val animator = android.animation.ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat())
        animator.duration = 2000
        animator.repeatCount = android.animation.ValueAnimator.INFINITE
        animator.interpolator = android.view.animation.LinearInterpolator()

        animator.addUpdateListener { animation ->
            wavePhase = animation.animatedValue as Float
            invalidate()
        }

        animator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calculate tank dimensions
        val padding = tankPaint.strokeWidth / 2f
        val tankWidth = min(w.toFloat(), h.toFloat() * 0.6f)
        val tankHeight = h - padding * 2

        val left = (w - tankWidth) / 2f
        val top = padding

        tankRect.set(left, top, left + tankWidth, top + tankHeight)

        // Set water gradient
        waterPaint.shader = LinearGradient(
            0f, 0f, 0f, tankHeight,
            waterColorStart, waterColorEnd,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw tank background
        canvas.drawRoundRect(tankRect, 20f, 20f, tankBackgroundPaint)

        // Draw water
        val waterHeight = tankRect.height() * waterLevel
        val waterTop = tankRect.bottom - waterHeight

        waterRect.set(tankRect.left, waterTop, tankRect.right, tankRect.bottom)

        // Adjust water rect to account for tank border
        val borderWidth = tankPaint.strokeWidth
        val adjustedWaterRect = RectF(
            waterRect.left + borderWidth / 2,
            waterRect.top,
            waterRect.right - borderWidth / 2,
            waterRect.bottom - borderWidth / 2
        )

        // Draw basic water fill
        canvas.drawRect(adjustedWaterRect, waterPaint)

        // Draw wave effect on top of water
        drawWave(canvas, adjustedWaterRect)

        // Draw level markers
        drawLevelMarkers(canvas)

        // Draw tank border
        canvas.drawRoundRect(tankRect, 20f, 20f, tankPaint)
    }

    private fun drawWave(canvas: Canvas, rect: RectF) {
        // Reset path
        wavePath.reset()

        // Start at bottom left
        wavePath.moveTo(rect.left, rect.bottom)

        // Draw wave across top of water
        val width = rect.width()
        val segmentWidth = width / 16

        for (i in 0..16) {
            val x = rect.left + i * segmentWidth
            val y = rect.top + waveAmplitude * kotlin.math.sin(wavePhase + i * 0.5f)

            wavePath.lineTo(x, y)
        }

        // Complete the path
        wavePath.lineTo(rect.right, rect.bottom)
        wavePath.close()

        // Draw the wave
        canvas.drawPath(wavePath, wavePaint)
    }

    private fun drawLevelMarkers(canvas: Canvas) {
        // Draw level markers at 25%, 50%, and 75%
        val markerWidth = tankRect.width() * 0.1f

        for (level in arrayOf(0.25f, 0.5f, 0.75f)) {
            val y = tankRect.bottom - tankRect.height() * level

            markerRect.set(
                tankRect.left - markerWidth / 2,
                y - 1,
                tankRect.left + markerWidth,
                y + 1
            )

            canvas.drawLine(
                tankRect.left,
                y,
                tankRect.left + markerWidth,
                y,
                levelMarkerPaint
            )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}