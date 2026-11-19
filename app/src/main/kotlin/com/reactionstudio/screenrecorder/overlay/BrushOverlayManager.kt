package com.reactionstudio.screenrecorder.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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

class BrushOverlayManager(
    private val context: Context,
    private val onClosed: () -> Unit = {}
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var rootOverlayView: FrameLayout? = null
    private var drawingCanvasView: DrawingCanvasView? = null

    private var selectedColor = Color.parseColor("#FF0055") // Neon Pink/Red default
    private var strokeWidthPx = dpToPx(5).toFloat()

    fun isShowing(): Boolean = rootOverlayView != null

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showBrushOverlay() {
        if (rootOverlayView != null) return

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val root = FrameLayout(context)
        rootOverlayView = root

        // Drawing Canvas
        val canvasView = DrawingCanvasView(context)
        canvasView.currentColor = selectedColor
        canvasView.currentStrokeWidth = strokeWidthPx
        drawingCanvasView = canvasView
        root.addView(
            canvasView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        // Floating Brush Toolbar (positioned at bottom center)
        val toolbar = createBrushToolbar()
        val toolbarParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dpToPx(36)
        }
        root.addView(toolbar, toolbarParams)

        try {
            windowManager.addView(root, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
            rootOverlayView = null
        }
    }

    private fun createBrushToolbar(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = dpToPx(14)
            val padV = dpToPx(8)
            setPadding(padH, padV, padH, padV)

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24).toFloat()
                setColor(Color.parseColor("#E6212121"))
                setStroke(dpToPx(1), Color.parseColor("#55FFFFFF"))
            }
            elevation = dpToPx(10).toFloat()

            // Color: Neon Red
            addView(createColorDot(Color.parseColor("#FF0055")))
            // Color: Electric Cyan
            addView(createColorDot(Color.parseColor("#00E5FF")))
            // Color: Golden Yellow
            addView(createColorDot(Color.parseColor("#FFD700")))
            // Color: Neon Green
            addView(createColorDot(Color.parseColor("#00E676")))

            // Divider
            addView(createDivider())

            // Undo Button
            val undoBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_revert)
                setColorFilter(Color.WHITE)
                val s = dpToPx(24)
                layoutParams = LinearLayout.LayoutParams(s, s).apply {
                    marginEnd = dpToPx(10)
                }
                setOnClickListener {
                    drawingCanvasView?.undoLastPath()
                }
            }
            addView(undoBtn)

            // Clear All Button
            val clearBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                setColorFilter(Color.WHITE)
                val s = dpToPx(24)
                layoutParams = LinearLayout.LayoutParams(s, s).apply {
                    marginEnd = dpToPx(12)
                }
                setOnClickListener {
                    drawingCanvasView?.clearCanvas()
                }
            }
            addView(clearBtn)

            // Close Brush Tool
            val closeBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.parseColor("#FF4444"))
                val s = dpToPx(26)
                layoutParams = LinearLayout.LayoutParams(s, s)
                setOnClickListener {
                    hideBrushOverlay()
                    onClosed()
                }
            }
            addView(closeBtn)
        }
    }

    private fun createColorDot(color: Int): View {
        return View(context).apply {
            val size = dpToPx(22)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(10)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke(dpToPx(1), Color.WHITE)
            }
            setOnClickListener {
                selectedColor = color
                drawingCanvasView?.currentColor = color
            }
        }
    }

    private fun createDivider(): View {
        return View(context).apply {
            val w = dpToPx(1)
            val h = dpToPx(18)
            layoutParams = LinearLayout.LayoutParams(w, h).apply {
                marginEnd = dpToPx(10)
            }
            setBackgroundColor(Color.parseColor("#44FFFFFF"))
        }
    }

    fun hideBrushOverlay() {
        rootOverlayView?.let { root ->
            try {
                windowManager.removeView(root)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        rootOverlayView = null
        drawingCanvasView = null
    }

    // Custom Canvas View for drawing paths
    private class DrawingCanvasView(context: Context) : View(context) {

        private val paths = mutableListOf<ColoredPath>()
        private var currentPath: Path? = null
        var currentColor: Int = Color.RED
        var currentStrokeWidth: Float = 12f

        private val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val p = Path()
                    p.moveTo(x, y)
                    currentPath = p
                    paths.add(ColoredPath(p, currentColor, currentStrokeWidth))
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentPath?.lineTo(x, y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    currentPath = null
                    invalidate()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            for (cp in paths) {
                paint.color = cp.color
                paint.strokeWidth = cp.strokeWidth
                canvas.drawPath(cp.path, paint)
            }
        }

        fun undoLastPath() {
            if (paths.isNotEmpty()) {
                paths.removeAt(paths.size - 1)
                invalidate()
            }
        }

        fun clearCanvas() {
            paths.clear()
            currentPath = null
            invalidate()
        }

        private data class ColoredPath(
            val path: Path,
            val color: Int,
            val strokeWidth: Float
        )
    }
}
