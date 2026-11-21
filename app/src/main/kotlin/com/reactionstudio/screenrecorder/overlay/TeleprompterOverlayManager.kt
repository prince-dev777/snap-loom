package com.reactionstudio.screenrecorder.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.app.AlertDialog
import kotlin.math.abs

/**
 * Floating Teleprompter overlay for content creators, reaction YouTubers, and educators.
 * Allows reading scripts smoothly on top of any game or video while recording.
 */
class TeleprompterOverlayManager(
    private val context: Context,
    private val onCloseClicked: () -> Unit = {}
) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = context.getSharedPreferences("teleprompter_prefs", Context.MODE_PRIVATE)

    private var rootView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var scriptTextView: TextView? = null
    private var scrollView: ScrollView? = null
    private var playPauseButton: TextView? = null

    private var isScrolling = false
    private var scrollSpeed = 1 // 1, 2, 3
    private var currentOpacity = 0.85f // 0.4, 0.7, 0.9

    private val scrollHandler = Handler(Looper.getMainLooper())
    private val scrollRunnable = object : Runnable {
        override fun run() {
            if (isScrolling && scrollView != null) {
                scrollView?.smoothScrollBy(0, scrollSpeed * 2)
                scrollHandler.postDelayed(this, 40)
            }
        }
    }

    fun isShowing(): Boolean = rootView != null

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showTeleprompter() {
        if (rootView != null) return

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val widthPx = dpToPx(280)
        val heightPx = dpToPx(220)

        layoutParams = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }

        val frame = FrameLayout(context).apply {
            val bg = GradientDrawable().apply {
                setColor(Color.argb((currentOpacity * 255).toInt(), 15, 23, 42)) // Slate 900
                cornerRadius = dpToPx(16).toFloat()
                setStroke(dpToPx(1), Color.argb(120, 56, 189, 248)) // Cyan border
            }
            background = bg
        }

        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
        }

        // Header (Title, Drag Handle, Close)
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val title = TextView(context).apply {
            text = "📜 Teleprompter"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val editBtn = TextView(context).apply {
            text = "✏️"
            textSize = 14f
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            setOnClickListener { showEditScriptDialog() }
        }

        val opacityBtn = TextView(context).apply {
            text = "👁️"
            textSize = 14f
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            setOnClickListener { toggleOpacity(frame) }
        }

        val closeBtn = TextView(context).apply {
            text = "✖"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            setOnClickListener {
                hideTeleprompter()
                onCloseClicked()
            }
        }

        header.addView(title)
        header.addView(editBtn)
        header.addView(opacityBtn)
        header.addView(closeBtn)
        mainContainer.addView(header)

        // Script Scroll Area
        scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dpToPx(6)
                bottomMargin = dpToPx(6)
            }
            isVerticalScrollBarEnabled = false
        }

        val savedScript = prefs.getString("user_script", "Welcome to Snap Loom Reaction Studio! Write or paste your talking points here. Use the Play button below to auto-scroll while recording your reaction.") ?: ""
        scriptTextView = TextView(context).apply {
            text = savedScript
            setTextColor(Color.WHITE)
            textSize = 13f
            setLineSpacing(0f, 1.3f)
        }
        scrollView?.addView(scriptTextView)
        mainContainer.addView(scrollView)

        // Bottom Controls Bar (Play/Pause, Speed, Reset)
        val bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        playPauseButton = TextView(context).apply {
            text = "▶ Play"
            setTextColor(Color.parseColor("#10B981"))
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener { toggleAutoScroll() }
        }

        val speedBtn = TextView(context).apply {
            text = "1x"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 11f
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener {
                scrollSpeed = if (scrollSpeed >= 3) 1 else scrollSpeed + 1
                text = "${scrollSpeed}x"
            }
        }

        val resetBtn = TextView(context).apply {
            text = "⏮ Top"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener {
                scrollView?.smoothScrollTo(0, 0)
            }
        }

        bottomBar.addView(playPauseButton)
        bottomBar.addView(speedBtn)
        bottomBar.addView(resetBtn)
        mainContainer.addView(bottomBar)

        frame.addView(mainContainer)

        // Make window draggable from header
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.let { lp ->
                        lp.x = initialX + (event.rawX - initialTouchX).toInt()
                        lp.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(frame, lp)
                    }
                    true
                }
                else -> false
            }
        }

        rootView = frame
        windowManager.addView(rootView, layoutParams)
    }

    private fun toggleAutoScroll() {
        isScrolling = !isScrolling
        if (isScrolling) {
            playPauseButton?.text = "⏸ Pause"
            playPauseButton?.setTextColor(Color.parseColor("#EF4444"))
            scrollHandler.post(scrollRunnable)
        } else {
            playPauseButton?.text = "▶ Play"
            playPauseButton?.setTextColor(Color.parseColor("#10B981"))
            scrollHandler.removeCallbacks(scrollRunnable)
        }
    }

    private fun toggleOpacity(frame: FrameLayout) {
        currentOpacity = when (currentOpacity) {
            0.85f -> 0.55f
            0.55f -> 0.35f
            else -> 0.85f
        }
        val bg = GradientDrawable().apply {
            setColor(Color.argb((currentOpacity * 255).toInt(), 15, 23, 42))
            cornerRadius = dpToPx(16).toFloat()
            setStroke(dpToPx(1), Color.argb(120, 56, 189, 248))
        }
        frame.background = bg
    }

    private fun showEditScriptDialog() {
        val input = EditText(context).apply {
            setText(scriptTextView?.text ?: "")
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            isSingleLine = false
            minLines = 4
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Edit Teleprompter Script")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newScript = input.text.toString()
                scriptTextView?.text = newScript
                prefs.edit().putString("user_script", newScript).apply()
                scrollView?.smoothScrollTo(0, 0)
            }
            .setNegativeButton("Cancel", null)
            .create()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }
        dialog.show()
    }

    fun hideTeleprompter() {
        scrollHandler.removeCallbacks(scrollRunnable)
        isScrolling = false
        rootView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        rootView = null
    }
}
