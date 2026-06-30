package com.example.cursorpad
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

class TrackpadOverlayService: Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var touchpadView: View
    private lateinit var cursorView: View
    private lateinit var listenerView: View

    private var cursorX = 0f
    private var cursorY = 0f
    private var isTouching = false
    private val sensitivity = 1.6f

    private lateinit var touchpadRect: Rect
    private lateinit var cursorAreaRect: Rect

    private var cursorWidth = 0
    private var cursorHeight = 0
    private var totalMovement = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var listenerX = 0f
    private var listenerY = 0f

    private var touchSlop = 0f
    private val scope = MainScope()
    private var longPressJob: Job? = null
    private var longPressTriggered = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()

        computeScreenAreas()
        createTouchpadOverlay()
        createCursorOverlay()
        createListenerOverlay()

        startService(Intent(this, CursorClickAccessibilityService::class.java))
    }

    // Overlay for listening for swipe gesture to toggle touchpad.
    private fun createListenerOverlay() {
        listenerView = View(this).apply {
            setBackgroundColor(0x00FFFFFF.toInt())

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        listenerX = event.rawX
                        listenerY = event.rawY

                        true
                    }

                    MotionEvent.ACTION_MOVE, MotionEvent.ACTION_CANCEL -> {
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        val upX = event.rawX
                        val dx = Math.abs(listenerX - upX)
                        if (dx > (60 * resources.displayMetrics.density) && listenerX > upX) {
                            toggleTouchpadVisibility()
                        }

                        v.performClick()
                        true
                    }

                    else -> false
                }
            }
        }

        val width = (10 * resources.displayMetrics.density).toInt()
        val screenHeight = resources.displayMetrics.heightPixels
        val params = WindowManager.LayoutParams(
            width,
            (screenHeight * 0.3f).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.RIGHT
        }

        windowManager.addView(listenerView, params)
    }

    private fun toggleTouchpadVisibility() {
        if (!::touchpadView.isInitialized) return
        if (!::cursorView.isInitialized) return
        val newVisibility = if (touchpadView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        touchpadView.visibility = newVisibility
        cursorView.visibility = newVisibility

        cursorX = (cursorAreaRect.width() - cursorWidth) / 2f
        cursorY = (cursorAreaRect.height() - cursorHeight) / 2f
        updateCursorPosition()
    }

    private fun createTouchpadOverlay() {
        val cornerRadiusPx = 20f * resources.displayMetrics.density
        val borderWidthPx = (2f * resources.displayMetrics.density).toInt()

        touchpadView = View(this).apply {
            val drawable = GradientDrawable().apply {
                setColor(0xAA333333.toInt())
                cornerRadius = cornerRadiusPx
                setStroke(borderWidthPx, Color.WHITE)
            }
            background = drawable

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        isTouching = true
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY

                        totalMovement = 0f
                        v.performClick()

                        longPressTriggered = false
                        longPressJob?.cancel()
                        // start listening for long press
                        longPressJob = scope.launch {
                            delay(200)

                            longPressTriggered = true
                            // only runs once the delay exceeds 400ms
                            performLongPress()
                        }
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (totalMovement > touchSlop) {
                            longPressJob?.cancel()
                        }

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
                        longPressJob?.cancel()
                        isTouching = false

                        if (totalMovement < touchSlop && !longPressTriggered) {
                            v.performClick()
                            performCursorTap()
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        val params = WindowManager.LayoutParams(
            touchpadRect.width(),
            touchpadRect.height(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.RIGHT
        }

        windowManager.addView(touchpadView, params)
    }

    private fun performCursorTap() {
        if (!isAccessibilityServiceEnabled()) {
            openAccessibilitySettings()
            return
        }

        if (!CursorClickAccessibilityService.isServiceEnabled()) {
            startService(Intent(this, CursorClickAccessibilityService::class.java))
            return
        }

        val location = IntArray(2);
        cursorView.getLocationOnScreen(location);
        val absoluteX = location[0] + cursorView.width / 2f
        val absoluteY = location[1] + cursorView.height / 2f

        val success = CursorClickAccessibilityService.performClick(absoluteX, absoluteY)
        if (!success) {
            // TODO: Log a toast
        }
    }

    private fun performLongPress() {
        if (!isAccessibilityServiceEnabled()) {
            openAccessibilitySettings()
            return
        }

        if (!CursorClickAccessibilityService.isServiceEnabled()) {
            startService(Intent(this, CursorClickAccessibilityService::class.java))
            return
        }

        val location = IntArray(2);
        cursorView.getLocationOnScreen(location);
        val absoluteX = location[0] + cursorView.width / 2f
        val absoluteY = location[1] + cursorView.height / 2f

        val success = CursorClickAccessibilityService.performLongPress(absoluteX, absoluteY)
        if (!success) {
            // TODO: Log a toast
        }
    }

    // PERFORMANCE: Checking if service is enabled every time a click is registered
    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo?.serviceInfo?.packageName == packageName }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
    private fun computeScreenAreas() {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        val cursorAreaHeight = (screenHeight * 0.70f).toInt()

        cursorAreaRect = Rect(0, 0,screenWidth, cursorAreaHeight)
        touchpadRect = Rect((screenWidth * 0.4f).toInt(), cursorAreaHeight, screenWidth, screenHeight)
    }

    private fun createCursorOverlay() {
        val cursorSize = (30f * resources.displayMetrics.density).toInt()
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
        if (::listenerView.isInitialized) windowManager.removeView(listenerView)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
}