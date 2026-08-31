package com.example.cursorpad
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.hypot

@SuppressLint("AccessibilityPolicy")
class TrackpadOverlayService: AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private lateinit var cursorView: View
    private var leftStripView: View? = null
    private var rightStripView: View? = null
    private var touchpadViews: MutableMap<String, View> = mutableMapOf()

    private var cursorX = 0f
    private var cursorY = 0f
    private var isTouching = false
    private var sensitivity = 1.6f

    private var separateTouchpad: Boolean = false
    private var leftTouchpadID: String = "left"
    private var rightTouchpadID: String = "right"

    private var touchpadRects: MutableMap<String, Rect> = mutableMapOf()
    private lateinit var cursorAreaRect: Rect

    private var cursorWidth = 0
    private var cursorHeight = 0
    private var totalMovement = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var listenerX = 0f

    private var touchSlop = 0f
    private val scope = MainScope()
    private var longPressJob: Job? = null
    private var longPressTriggered = false
    private val originalCursorColor = Color.GREEN

    private var touchpadActive = false
    private var currentStripState = StripState()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val rotationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                if (touchpadActive) {
                    toggleTouchpadVisibility("")
                }
            }
        }
    }

    companion object {
        // State responsible for enabling/disabling overlays
        val overlayEnabled = MutableStateFlow(false)
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()

        // register screen rotation listener
        val filter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
        registerReceiver(rotationReceiver, filter)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Create all the overlays and make them visible if they are enabled
        computeScreenAreas()
        createTouchpadOverlay(touchpadRects["left"], "left")
        createTouchpadOverlay(touchpadRects["right"], "right")
        createTouchpadOverlay(touchpadRects["shared"],"shared")
        createCursorOverlay()
        createActivationStripOverlay()

        serviceScope.launch {
            overlayEnabled.collect { isEnabled ->
                if (isEnabled) {
                    leftStripView?.visibility = if (currentStripState.leftStrip.active) View.VISIBLE else View.GONE
                    rightStripView?.visibility = if (currentStripState.rightStrip.active) View.VISIBLE else View.GONE
                } else {
                    cursorView.visibility = View.GONE
                    touchpadViews.forEach { (_, touchpadView) ->
                        touchpadView.visibility = View.GONE
                    }

                    leftStripView?.visibility = View.GONE
                    rightStripView?.visibility = View.GONE
                }
            }
        }

        serviceScope.launch {
            dataStore.data
                .map { it.toCursorState() }
                .distinctUntilChanged()
                .collect { updateCursorView(it) }
        }

        val density = resources.displayMetrics.density
        val screenHeight = resources.displayMetrics.heightPixels
        val screenWidth = resources.displayMetrics.widthPixels
        val defaultHeightDp = 140f
        val defaultWidthDp = 140f
        val defaultXDp = screenWidth / density - defaultWidthDp - 24f
        val defaultYDp = screenHeight / density - defaultHeightDp - 24f

        serviceScope.launch {
            dataStore.data
                .map { it.toTouchpadState(defaultLeftX = 24f, defaultRightX = defaultXDp, defaultY = defaultYDp) }
                .distinctUntilChanged()
                .collect { state ->
                    leftTouchpadID = if (state.separateLayout) "left" else "shared"
                    rightTouchpadID = if (state.separateLayout) "right" else "shared"
                    updateTouchpadView(state)
                }
        }

        val defaultTop = screenHeight / density - defaultHeightDp
        serviceScope.launch {
            dataStore.data
                .map { it.toStripState(defaultTop) }
                .distinctUntilChanged()
                .collect { state ->
                    currentStripState = state
                    updateStripView(state)
                }
        }

        serviceScope.launch {
            dataStore.data
                .map { it[TOUCHPAD_SENSITIVITY_KEY] ?: 1.5f }
                .distinctUntilChanged()
                .collect { newSensitivity ->
                    sensitivity = newSensitivity
                }
        }
    }

    private fun updateStripView(state: StripState) {
        updateSingleStripView(leftStripView!!, state.leftStrip)
        updateSingleStripView(rightStripView!!, state.rightStrip)
    }

    private fun updateSingleStripView(view: View, config: StripConfig) {
        view.visibility = if (config.active) View.VISIBLE else View.GONE

        if (!config.active) return
        val params = view.layoutParams as WindowManager.LayoutParams
        val density = resources.displayMetrics.density
        val width = (config.width * density).toInt()
        val height = (config.height * density).toInt()
        val y = (config.top * density).toInt()

        val layoutChanged = params.width != width || params.height != height || params.y != y
        if (layoutChanged) {
            params.width = width
            params.height = height
            params.y = y

            windowManager.updateViewLayout(view, params)
        }
    }

    // Updates the touchpads based on the new settings
    private fun updateTouchpadView(state: TouchpadState) {
        if (state.separateLayout) {
            // Hide shared touchpad
            touchpadViews["shared"]?.visibility = View.GONE

            updateSingleTouchpadView(touchpadViews["left"]!!, state.leftTouchpad)
            updateSingleTouchpadView(touchpadViews["right"]!!, state.rightTouchpad)
        } else {
            // Hide left & right touchpad
            touchpadViews["left"]?.visibility = View.GONE
            touchpadViews["right"]?.visibility = View.GONE

            updateSingleTouchpadView(touchpadViews["shared"]!!, state.sharedTouchpad)
        }
    }

    // Update a specific touchpad view
    private fun updateSingleTouchpadView(view: View, config: TouchpadConfig) {
        // Skip updates if the touchpad is not active
        if (!config.active) {
            view.visibility = View.GONE
            return
        }


        val params = view.layoutParams as WindowManager.LayoutParams
        val density = resources.displayMetrics.density
        val x = (config.x * density).toInt()
        val y = (config.y * density).toInt()
        val width = (config.width * density).toInt()
        val height = (config.height * density).toInt()

        val layoutChanged = params.x != x || params.y != y || params.width != width || params.height != height
        // Only update layout if you must (expensive)
        if (layoutChanged) {
            params.x = x
            params.y = y
            params.width = width
            params.height = height

            windowManager.updateViewLayout(view, params)
        }

        val drawable = view.background as GradientDrawable
        drawable.setColor(config.color)
    }

    // Updates the cursor based on the new settings
    private fun updateCursorView(state: CursorState) {
        val density = resources.displayMetrics.density
        val sizePx = (state.size * density).toInt()
        val params = cursorView.layoutParams as WindowManager.LayoutParams

        // Layout change (expensive avoid if possible)
        if (params.width != sizePx || params.height != sizePx) {
            params.width = sizePx
            params.height = sizePx
            windowManager.updateViewLayout(cursorView, params)
        }

        // Redraw (cheap)
        val view = cursorView as CursorView
        view.borderSize = state.borderSize * density
        view.cursorColor = state.color
        view.showDot = state.showDot
    }

    // Overlay for listening for swipe gesture to toggle touchpad.
    private fun createActivationStripOverlay() {
        leftStripView = createStripOverlay(side = "left")
        rightStripView = createStripOverlay(side = "right")
    }

    private fun createStripOverlay(
        side: String,
    ) : View {
        val view = View(this).apply {
            setBackgroundColor(0x00FFFFF)

            visibility = View.GONE

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        listenerX = event.rawX
                        true
                    }

                    MotionEvent.ACTION_MOVE, MotionEvent.ACTION_CANCEL -> {
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        val upX = event.rawX
                        val dx = abs(listenerX - upX)
                        if (dx > (60 * resources.displayMetrics.density)) {
                            val touchpadID = if (side == "left") leftTouchpadID else rightTouchpadID
                            toggleTouchpadVisibility(touchpadID)
                        }

                        v.performClick()
                        true
                    }

                    else -> false
                }
            }
        }

        val density = resources.displayMetrics.density
        val screenHeight = resources.displayMetrics.heightPixels

        val preferences = runBlocking { applicationContext.dataStore.data.first() }

        val defaultWidthDp = 20f
        val defaultHeightDp = 140f
        val defaultY = screenHeight / density - defaultHeightDp

        val width = preferences[ACTIVATION_STRIP_WIDTH] ?: defaultWidthDp
        var height: Float
        var stripY: Float

        if (side == "left") {
            height = preferences[ACTIVATION_STRIP_LEFT_HEIGHT] ?: defaultHeightDp
            stripY = preferences[ACTIVATION_STRIP_LEFT_TOP] ?: defaultY
        } else {
            height = preferences[ACTIVATION_STRIP_RIGHT_HEIGHT] ?: defaultHeightDp
            stripY = preferences[ACTIVATION_STRIP_RIGHT_TOP] ?: defaultY
        }

        val widthPx = (width * density).toInt()
        val heightPx = (height * density).toInt()
        val stripYPx = (stripY * density).toInt()

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or (if (side == "left") Gravity.START else Gravity.END)
            y = stripYPx
        }

        windowManager.addView(view, params)
        return view
    }

    private fun toggleTouchpadVisibility(touchpadID: String) {
        if (!::cursorView.isInitialized) return

        // turn off the visible touchpad (regardless of touchpadID)
        if (touchpadActive) {
            touchpadViews.forEach { (_, touchpadView) ->
                touchpadView.visibility = View.GONE
            }
        } else {
            // otherwise make the touchpad visible based on touchpadID
            if (!touchpadViews.contains(touchpadID)) return

            touchpadViews[touchpadID]?.let { touchpadView ->
                touchpadView.visibility = View.VISIBLE
            }
        }

        cursorView.visibility = if (touchpadActive) View.GONE else View.VISIBLE
        touchpadActive = !touchpadActive
        cursorX = (cursorAreaRect.width() - cursorWidth) / 2f
        cursorY = (cursorAreaRect.height() - cursorHeight) / 2f
        updateCursorPosition()
    }

    private fun createTouchpadOverlay(touchpadRect: Rect?, id: String) {
        if (touchpadRect == null) {
            Log.e("TrackpadOverlayService", "Touchpad rect is null for id: $id")
            return
        }

        val cornerRadiusPx = 20f * resources.displayMetrics.density
        val borderWidthPx = (2f * resources.displayMetrics.density).toInt()
        val preferences = runBlocking { applicationContext.dataStore.data.first() }
        val touchpadColor = preferences[TOUCHPAD_COLOR_KEY] ?: 0xAA333333.toInt()

        val view  = View(this).apply {
            val drawable = GradientDrawable().apply {
                setColor(touchpadColor)
                cornerRadius = cornerRadiusPx
                setStroke(borderWidthPx, Color.WHITE)
            }
            background = drawable
            visibility = View.GONE

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
                            // runs when the delay exceeds 200ms
                            animateLongPress()
                            longPressTriggered = true
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

                        v.performClick()
                        if (totalMovement < touchSlop && !longPressTriggered) {
                            // initiate the animation
                            animateTouchDown()
                            performCursorTap()
                        }

                        longPressTriggered = false
                        true
                    }

                    else -> false
                }
            }
        }
        touchpadViews[id] = view

        val params = WindowManager.LayoutParams(
            touchpadRect.width(),
            touchpadRect.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = touchpadRect.left
            y = touchpadRect.top
        }

        windowManager.addView(touchpadViews[id], params)
    }

    private fun animateTouchDown() {
        cursorView.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(100)
            .withEndAction {
                cursorView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateLongPress() {
        val scaleDownX = ObjectAnimator.ofFloat(cursorView, "scaleX", 1f, 0.8f)
        val scaleDownY = ObjectAnimator.ofFloat(cursorView, "scaleY", 1f, 0.8f)
        val scaleUpX = ObjectAnimator.ofFloat(cursorView, "scaleX", 0.8f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(cursorView, "scaleY", 0.8f, 1f)
        val circleDrawable = cursorView.background as? GradientDrawable

        if (circleDrawable != null) {
            val colorChange = ValueAnimator.ofArgb(originalCursorColor, Color.BLUE).apply {
                addUpdateListener { animator ->
                    circleDrawable.setColor(animator.animatedValue as Int)
                }
            }

            val colorRevert = ValueAnimator.ofArgb(Color.BLUE, originalCursorColor).apply {
                addUpdateListener { animator ->
                    circleDrawable.setColor(animator.animatedValue as Int)
                }
            }

            val firstHalf = AnimatorSet().apply {
                playTogether(scaleDownX, scaleDownY, colorChange)
                duration = 150L
            }

            val secondHalf = AnimatorSet().apply {
                playTogether(scaleUpX, scaleUpY, colorRevert)
                duration = 150L
            }

            AnimatorSet().apply {
                playSequentially(firstHalf, secondHalf)
                start()
            }
        }
    }

    private fun performCursorTap() {
        val location = IntArray(2)
        cursorView.getLocationOnScreen(location)
        val absoluteX = location[0] + cursorView.width / 2f
        val absoluteY = location[1] + cursorView.height / 2f
        val success = injectClick(absoluteX, absoluteY)
        if (!success) {
            // TODO: Log a toast
        }
    }

    private fun performLongPress() {
        val location = IntArray(2)
        cursorView.getLocationOnScreen(location)
        val absoluteX = location[0] + cursorView.width / 2f
        val absoluteY = location[1] + cursorView.height / 2f

        val success = injectLongPress(absoluteX, absoluteY)
        if (!success) {
            // TODO: Log a toast
        }
    }

    private fun computeScreenAreas() {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        val density = displayMetrics.density
        val cursorAreaHeight = (screenHeight * 0.70f).toInt()
        cursorAreaRect = Rect(0, 0,screenWidth, cursorAreaHeight)

        val preferences = runBlocking { applicationContext.dataStore.data.first() }
        separateTouchpad = preferences[TOUCHPAD_SEPARATE_LAYOUT_KEY] ?: true

        val defaultWidthDp = 140f
        val defaultHeightDp = 140f
        val defaultXDp = screenWidth / density - defaultWidthDp - 24f
        val defaultYDp = screenHeight / density - defaultHeightDp - 24f

        var padX = preferences[TOUCHPAD_LEFT_X_KEY] ?: 24f
        var padY = preferences[TOUCHPAD_LEFT_Y_KEY] ?: defaultYDp
        var padWidth = preferences[TOUCHPAD_LEFT_WIDTH_KEY] ?: defaultWidthDp
        var padHeight = preferences[TOUCHPAD_LEFT_HEIGHT_KEY] ?: defaultHeightDp

        var padXPx = (padX * density).toInt()
        var padYPx = (padY * density).toInt()
        var padWidthPx = (padWidth * density).toInt()
        var padHeightPx = (padHeight * density).toInt()

        val leftRect = Rect(padXPx, padYPx, padXPx + padWidthPx, padYPx + padHeightPx)
        touchpadRects["left"] = leftRect

        padX = preferences[TOUCHPAD_RIGHT_X_KEY] ?: defaultXDp
        padY = preferences[TOUCHPAD_RIGHT_Y_KEY] ?: defaultYDp
        padWidth = preferences[TOUCHPAD_RIGHT_WIDTH_KEY] ?: defaultWidthDp
        padHeight = preferences[TOUCHPAD_RIGHT_HEIGHT_KEY] ?: defaultHeightDp

        padXPx = (padX * density).toInt()
        padYPx = (padY * density).toInt()
        padWidthPx = (padWidth * density).toInt()
        padHeightPx = (padHeight * density).toInt()

        val rightRect = Rect(padXPx, padYPx, padXPx + padWidthPx, padYPx + padHeightPx)
        touchpadRects["right"] = rightRect

        padX = preferences[TOUCHPAD_X_KEY] ?: defaultXDp
        padY = preferences[TOUCHPAD_Y_KEY] ?: defaultYDp
        padWidth = preferences[TOUCHPAD_WIDTH_KEY] ?: defaultWidthDp
        padHeight = preferences[TOUCHPAD_HEIGHT_KEY] ?: defaultHeightDp

        padXPx = (padX * density).toInt()
        padYPx = (padY * density).toInt()
        padWidthPx = (padWidth * density).toInt()
        padHeightPx = (padHeight * density).toInt()

        val touchpadRect = Rect(padXPx, padYPx, padXPx + padWidthPx, padYPx + padHeightPx)
        touchpadRects["shared"] = touchpadRect
    }

    private fun createCursorOverlay() {
        val preferences = runBlocking { applicationContext.dataStore.data.first() }

        val cursorSize = preferences[CURSOR_SIZE_KEY] ?: 30f
        val borderSize = preferences[BORDER_SIZE_KEY] ?: 2f
        val cursorColor = preferences[CURSOR_COLOR_KEY] ?: Color.GREEN
        val showDot = preferences[SHOW_DOT_KEY] ?: true
        val density = resources.displayMetrics.density

        sensitivity = preferences[TOUCHPAD_SENSITIVITY_KEY] ?: 1.6f

        val cursorSizeDP = (cursorSize * density).toInt()
        val borderSizeDP = borderSize * density

        cursorView = CursorView(this).apply {
            this.borderSize = borderSizeDP
            this.cursorColor = cursorColor
            this.showDot = showDot
            this.visibility = View.GONE
        }

        val params = WindowManager.LayoutParams (
            cursorSizeDP,
            cursorSizeDP,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
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
     * Movement magnitude is derived based on the sensitivity.
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

    private fun injectClick(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    private fun injectLongPress(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0,
                ViewConfiguration.getLongPressTimeout().toLong() + 50
            ))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    override fun onDestroy() {
        touchpadViews.values.forEach { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) { /* ignore */ }
        }

        if (::cursorView.isInitialized) windowManager.removeView(cursorView)
        leftStripView?.let { windowManager.removeView(it) }
        rightStripView?.let { windowManager.removeView(it) }

        unregisterReceiver(rotationReceiver)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() {
        Log.d("TrackpadOverlayService", "Service Interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
}