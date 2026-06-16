package com.example.cursorpad
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import kotlin.math.hypot

class TrackpadOverlayService: Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var touchpadView: View
    private lateinit var cursorView: View

    private var cursorX = 0f
    private var cursorY = 0f
    private var isTouching = false
    private val sensitivity = 2f

    private lateinit var touchpadRect: Rect
    private lateinit var cursorAreaRect: Rect

    private var cursorWidth = 0
    private var cursorHeight = 0
    private var totalMovement = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var touchSlop = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()

        computeScreenAreas()
        createTouchpadOverlay()
        createCursorOverlay()
    }

    private fun createTouchpadOverlay() {
        touchpadView = View(this).apply {
            setBackgroundColor(0xAA333333.toInt())

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        isTouching = true
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY

                        totalMovement = 0f
                        v.performClick()
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (isTouching) {
                            val dx = event.rawX - lastTouchX
                            val dy = event.rawY - lastTouchY

                            totalMovement += hypot(dx, dy)
                            moveCursorRelative(dx, dy)
                            lastTouchX = event.rawX
                            lastTouchY = event.rawY

                        }
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isTouching = false

                        if (totalMovement < touchSlop) {
                            v.performClick()
                        }
                        // TODO: perform click logic goes here
                        true
                    }

                    else -> false
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            touchpadRect.height(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        windowManager.addView(touchpadView, params)
    }

    private fun computeScreenAreas() {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        val cursorAreaHeight = (screenHeight * 0.65f).toInt()

        cursorAreaRect = Rect(0, 0,screenWidth, cursorAreaHeight)
        touchpadRect = Rect(0, cursorAreaHeight, screenWidth, screenHeight)
    }

    private fun createCursorOverlay() {
        val cursorSize = (20 * resources.displayMetrics.density).toInt()
        cursorView = View(this).apply {
            setBackgroundResource(R.drawable.cursor_circle)
        }

        val params = WindowManager.LayoutParams (
            cursorSize,
            cursorSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        windowManager.addView(cursorView, params)

        cursorView.post {
            cursorWidth = cursorView.width
            cursorHeight = cursorView.height

            if (cursorWidth > cursorAreaRect.width()) {
                cursorWidth = cursorAreaRect.width()
            }

            if (cursorHeight > cursorAreaRect.height()) {
                cursorHeight = cursorAreaRect.height()
            }

            cursorX = (cursorAreaRect.width() - cursorWidth) / 2f
            cursorY = (cursorAreaRect.height() - cursorHeight) / 2f
            updateCursorPosition()
        }
    }

    /**
     * Moves cursor relative to its current position (like a laptop trackpad) based on the displacement.
     * Movement magnitude is derived based on the ratio of the cursor area and touchpad dimensions.
     */
    private fun moveCursorRelative(dx: Float, dy: Float) {
        if (cursorWidth == 0 || cursorHeight == 0) return

        val displacementX = dx * sensitivity
        val displacementY = dy * sensitivity

        var newX = cursorX + displacementX
        var newY = cursorY + displacementY

        val maxX = (cursorAreaRect.width() - cursorWidth).toFloat()
        val maxY = (cursorAreaRect.height() - cursorHeight).toFloat()

        if (maxX >= 0) newX = newX.coerceIn(0f, maxX)
        if (maxY >= 0) newY = newY.coerceIn(0f, maxY)

        cursorX = newX
        cursorY = newY
        updateCursorPosition()
    }

    /**
     * Update cursor position by updating the x, y values in the cursor view.
     */
    private fun updateCursorPosition() {
        if (!::cursorView.isInitialized) return
        val params = cursorView.layoutParams as WindowManager.LayoutParams
        params.x = cursorAreaRect.left + cursorX.toInt()
        params.y = cursorAreaRect.top + cursorY.toInt()

        try {
            windowManager.updateViewLayout(cursorView, params)
        } catch (_: IllegalArgumentException) {
            // View may have been removed
        }
    }

    override fun onDestroy() {
        if (::touchpadView.isInitialized) windowManager.removeView(touchpadView)
        if (::cursorView.isInitialized) windowManager.removeView(cursorView)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
}