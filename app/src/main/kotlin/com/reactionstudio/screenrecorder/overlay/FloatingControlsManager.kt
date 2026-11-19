package com.reactionstudio.screenrecorder.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FloatingControlsManager(
    private val context: Context,
    private val onPauseResumeClicked: () -> Unit,
    private val onStopClicked: () -> Unit,
    private val onToggleFacecamClicked: () -> Unit,
    private val onBrushClicked: () -> Unit = {},
    private val onScreenshotClicked: () -> Unit = {}
) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var rootView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var timerTextView: TextView? = null
    private var pauseResumeIcon: ImageView? = null
    private var isPaused = false

    fun isShowing(): Boolean = rootView != null

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showControls() {
        if (rootView != null) return

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(20)
            y = dpToPx(260)
        }

        val root = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }
        rootView = root

        val pillContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = dpToPx(14)
            val padV = dpToPx(8)
            setPadding(padH, padV, padH, padV)

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24).toFloat()
                setColor(Color.parseColor("#E61E1E24")) // Dark translucent
                setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"))
            }
            elevation = dpToPx(8).toFloat()
        }

        // Blinking Red Recording Dot
        val recDot = View(context).apply {
            val size = dpToPx(10)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF2222"))
            }
        }
        pillContainer.addView(recDot)

        // Timer Text View
        val timer = TextView(context).apply {
            text = "00:00"
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(12)
            }
        }
        timerTextView = timer
        pillContainer.addView(timer)

        // Divider
        val div1 = createDivider()
        pillContainer.addView(div1)

        // Facecam Toggle Button
        val facecamBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_camera)
            setColorFilter(Color.parseColor("#EEEEEE"))
            val s = dpToPx(24)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                marginEnd = dpToPx(10)
            }
            setOnClickListener {
                onToggleFacecamClicked()
            }
        }
        pillContainer.addView(facecamBtn)

        // Screen Brush / Pen Tool Button
        val brushBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_edit)
            setColorFilter(Color.parseColor("#00E5FF")) // Electric Cyan
            val s = dpToPx(24)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                marginEnd = dpToPx(10)
            }
            setOnClickListener {
                onBrushClicked()
            }
        }
        pillContainer.addView(brushBtn)

        // Pause / Resume Button
        val pauseBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_media_pause)
            setColorFilter(Color.parseColor("#FFCC00"))
            val s = dpToPx(24)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                marginEnd = dpToPx(10)
            }
            setOnClickListener {
                onPauseResumeClicked()
            }
        }
        pauseResumeIcon = pauseBtn
        pillContainer.addView(pauseBtn)

        // Stop Button
        val stopBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.checkbox_on_background)
            setColorFilter(Color.parseColor("#FF3344"))
            val s = dpToPx(24)
            layoutParams = LinearLayout.LayoutParams(s, s)
            setOnClickListener {
                onStopClicked()
            }
        }
        pillContainer.addView(stopBtn)

        root.addView(pillContainer)

        setupDragTouchListener(root)

        try {
            windowManager.addView(root, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
            rootView = null
        }
    }

    private fun createDivider(): View {
        return View(context).apply {
            val w = dpToPx(1)
            val h = dpToPx(18)
            layoutParams = LinearLayout.LayoutParams(w, h).apply {
                marginEnd = dpToPx(10)
            }
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
        }
    }

    fun updateTimer(durationMillis: Long) {
        val totalSec = durationMillis / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        val hours = mins / 60
        val text = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, mins % 60, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
        timerTextView?.text = text
    }

    fun setPaused(paused: Boolean) {
        this.isPaused = paused
        pauseResumeIcon?.apply {
            if (paused) {
                setImageResource(android.R.drawable.ic_media_play)
                setColorFilter(Color.parseColor("#00E676"))
            } else {
                setImageResource(android.R.drawable.ic_media_pause)
                setColorFilter(Color.parseColor("#FFCC00"))
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragTouchListener(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        view.setOnTouchListener { _, event ->
            val lp = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                        isMoving = true
                    }
                    lp.x = initialX + deltaX
                    lp.y = initialY + deltaY
                    try {
                        windowManager.updateViewLayout(view, lp)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isMoving
                }
                else -> false
            }
        }
    }

    fun hideControls() {
        rootView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        rootView = null
        layoutParams = null
        timerTextView = null
        pauseResumeIcon = null
    }
}
