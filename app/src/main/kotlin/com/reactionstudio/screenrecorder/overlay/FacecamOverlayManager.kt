package com.reactionstudio.screenrecorder.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.reactionstudio.screenrecorder.model.CameraLens
import com.reactionstudio.screenrecorder.model.FacecamBorderColor
import com.reactionstudio.screenrecorder.model.FacecamShape
import com.reactionstudio.screenrecorder.model.FacecamSize
import kotlin.math.abs

class FacecamOverlayManager(
    private val context: Context,
    private val onCloseClicked: () -> Unit = {},
    private val onCameraFlipped: (CameraLens) -> Unit = {}
) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var rootView: FrameLayout? = null
    private var previewContainer: FrameLayout? = null
    private var previewView: PreviewView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var borderView: View? = null
    private var fullScreenBtn: ImageView? = null
    private var resizeBtn: ImageView? = null

    private var isFullScreen = false
    private var savedX = 0
    private var savedY = 0

    private var currentLens: CameraLens = CameraLens.FRONT
    private var currentShape: FacecamShape = FacecamShape.CIRCLE
    private var currentSize: FacecamSize = FacecamSize.MEDIUM
    private var currentBorderColor: FacecamBorderColor = FacecamBorderColor.RED
    private var lifecycleOwner: LifecycleOwner? = null
    private var cameraProvider: ProcessCameraProvider? = null

    fun isShowing(): Boolean = rootView != null
    fun isFullScreenActive(): Boolean = isFullScreen

    fun showFacecam(
        owner: LifecycleOwner,
        lens: CameraLens = CameraLens.FRONT,
        shape: FacecamShape = FacecamShape.CIRCLE,
        size: FacecamSize = FacecamSize.MEDIUM,
        borderColor: FacecamBorderColor = FacecamBorderColor.RED
    ) {
        if (rootView != null) return

        this.lifecycleOwner = owner
        this.currentLens = lens
        this.currentShape = shape
        this.currentSize = size
        this.currentBorderColor = borderColor

        createOverlayWindow()
        startCameraStream()
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayWindow() {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val pxSize = dpToPx(currentSize.sizeDp)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(24)
            y = dpToPx(80)
        }

        val root = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }
        rootView = root

        // Preview Container with rounded border/card appearance
        val container = FrameLayout(context).apply {
            val params = FrameLayout.LayoutParams(pxSize, pxSize)
            params.gravity = Gravity.CENTER
            layoutParams = params
            elevation = dpToPx(8).toFloat()
        }
        previewContainer = container

        // Camera PreviewView - CRITICAL: Must use COMPATIBLE (TextureView) mode so MediaProjection captures the camera!
        val pView = PreviewView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        previewView = pView
        container.addView(pView)

        applyShapeToContainer(container, currentShape)

        // Overlay Border frame
        val border = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            background = getBorderDrawable(currentShape)
        }
        borderView = border
        container.addView(border)

        // Mini action bar below or on top of camera (Flip, Fullscreen, Resize, Close)
        val actionToolbar = createActionToolbar()
        container.addView(actionToolbar)

        root.addView(container)

        // Drag & Double-Tap handling on both root and container for maximum responsiveness
        setupDragTouchListener(root)

        try {
            windowManager.addView(root, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
            rootView = null
        }
    }

    private fun getBorderDrawable(shape: FacecamShape): GradientDrawable {
        return GradientDrawable().apply {
            setStroke(dpToPx(3), Color.parseColor(currentBorderColor.hex))
            if (shape == FacecamShape.CIRCLE) {
                this.shape = GradientDrawable.OVAL
            } else {
                this.shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(18).toFloat()
            }
        }
    }

    fun updateBorderColor(color: FacecamBorderColor) {
        currentBorderColor = color
        borderView?.background = getBorderDrawable(currentShape)
    }

    private fun applyShapeToContainer(container: FrameLayout, shape: FacecamShape) {
        container.clipToOutline = true
        container.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                if (shape == FacecamShape.CIRCLE) {
                    outline.setOval(0, 0, view.width, view.height)
                } else {
                    outline.setRoundRect(0, 0, view.width, view.height, dpToPx(18).toFloat())
                }
            }
        }
    }

    private fun createActionToolbar(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#99000000")) // Semi-transparent sleek dark

            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(30)
            ).apply {
                gravity = Gravity.BOTTOM
            }
            layoutParams = lp

            // Flip Camera button
            val flipBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_rotate)
                setColorFilter(Color.WHITE)
                val pad = dpToPx(5)
                setPadding(pad, pad, pad, pad)
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                layoutParams = p
                setOnClickListener {
                    flipCamera()
                }
            }
            addView(flipBtn)

            // Fullscreen Toggle button (Double tap shortcut)
            val fsBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_zoom)
                setColorFilter(Color.WHITE)
                val pad = dpToPx(5)
                setPadding(pad, pad, pad, pad)
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                layoutParams = p
                setOnClickListener {
                    toggleFullScreen()
                }
            }
            fullScreenBtn = fsBtn
            addView(fsBtn)

            // Resize button
            val resBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_crop)
                setColorFilter(Color.WHITE)
                val pad = dpToPx(5)
                setPadding(pad, pad, pad, pad)
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                layoutParams = p
                setOnClickListener {
                    cycleSize()
                }
            }
            resizeBtn = resBtn
            addView(resBtn)

            // Close button
            val closeBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.WHITE)
                val pad = dpToPx(5)
                setPadding(pad, pad, pad, pad)
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                layoutParams = p
                setOnClickListener {
                    hideFacecam()
                    onCloseClicked()
                }
            }
            addView(closeBtn)
        }
    }

    private fun cycleSize() {
        if (isFullScreen) return
        val newSize = when (currentSize) {
            FacecamSize.SMALL -> FacecamSize.MEDIUM
            FacecamSize.MEDIUM -> FacecamSize.LARGE
            FacecamSize.LARGE -> FacecamSize.SMALL
        }
        updateSize(newSize)
    }

    fun updateSize(size: FacecamSize) {
        currentSize = size
        if (isFullScreen) return
        val pxSize = dpToPx(size.sizeDp)
        previewContainer?.let { container ->
            val lp = container.layoutParams as FrameLayout.LayoutParams
            lp.width = pxSize
            lp.height = pxSize
            container.layoutParams = lp
            container.invalidateOutline()
        }
        rootView?.let { root ->
            layoutParams?.let { lp ->
                try {
                    windowManager.updateViewLayout(root, lp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateShape(shape: FacecamShape) {
        currentShape = shape
        if (isFullScreen) return
        previewContainer?.let { container ->
            applyShapeToContainer(container, shape)
            container.invalidateOutline()
            borderView?.background = getBorderDrawable(shape)
        }
    }

    /**
     * Toggles between Floating Camera Bubble and Full Screen Camera with zero lag.
     * Can be invoked via Double Tap or the action bar Fullscreen button.
     */
    fun toggleFullScreen() {
        if (isFullScreen) {
            exitFullScreen()
        } else {
            enterFullScreen()
        }
    }

    fun enterFullScreen() {
        val lp = layoutParams ?: return
        val root = rootView ?: return
        val container = previewContainer ?: return

        isFullScreen = true
        savedX = lp.x
        savedY = lp.y

        // Set WindowManager to cover the entire display edge-to-edge
        lp.x = 0
        lp.y = 0
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT

        // Expand container to match screen
        val containerLp = container.layoutParams as FrameLayout.LayoutParams
        containerLp.width = FrameLayout.LayoutParams.MATCH_PARENT
        containerLp.height = FrameLayout.LayoutParams.MATCH_PARENT
        container.layoutParams = containerLp

        // Remove rounded/circular clipping in full screen
        container.clipToOutline = false
        borderView?.visibility = View.GONE
        resizeBtn?.visibility = View.GONE
        fullScreenBtn?.setColorFilter(Color.parseColor("#FFD700")) // Golden glow active

        try {
            windowManager.updateViewLayout(root, lp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exitFullScreen() {
        val lp = layoutParams ?: return
        val root = rootView ?: return
        val container = previewContainer ?: return

        isFullScreen = false
        val pxSize = dpToPx(currentSize.sizeDp)

        // Restore window position and wrap_content bounds
        lp.x = savedX
        lp.y = savedY
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT

        // Restore container bubble size
        val containerLp = container.layoutParams as FrameLayout.LayoutParams
        containerLp.width = pxSize
        containerLp.height = pxSize
        container.layoutParams = containerLp

        // Reapply shape clipping & border
        applyShapeToContainer(container, currentShape)
        container.invalidateOutline()
        borderView?.visibility = View.VISIBLE
        borderView?.background = getBorderDrawable(currentShape)
        resizeBtn?.visibility = View.VISIBLE
        fullScreenBtn?.setColorFilter(Color.WHITE)

        try {
            windowManager.updateViewLayout(root, lp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragTouchListener(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onDoubleTap(e: MotionEvent): Boolean {
                toggleFullScreen()
                return true
            }
        })

        view.setOnTouchListener { _, event ->
            // Pass to gesture detector for instant double tap recognition
            gestureDetector.onTouchEvent(event)

            // When in full screen, dragging is locked so camera stays rock-solid
            if (isFullScreen) {
                return@setOnTouchListener true
            }

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
                    if (abs(deltaX) > 8 || abs(deltaY) > 8) {
                        isMoving = true
                        lp.x = initialX + deltaX
                        lp.y = initialY + deltaY
                        try {
                            windowManager.updateViewLayout(view, lp)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
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

    fun flipCamera() {
        val newLens = if (currentLens == CameraLens.FRONT) CameraLens.BACK else CameraLens.FRONT
        currentLens = newLens
        startCameraStream()
        onCameraFlipped(newLens)
    }

    private fun startCameraStream() {
        val owner = lifecycleOwner ?: return
        val pView = previewView ?: return

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                val provider = cameraProvider ?: return@addListener

                provider.unbindAll()

                val cameraSelector = if (currentLens == CameraLens.FRONT) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(pView.surfaceProvider)
                }

                provider.bindToLifecycle(owner, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun hideFacecam() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        rootView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        rootView = null
        previewContainer = null
        previewView = null
        borderView = null
        fullScreenBtn = null
        resizeBtn = null
        layoutParams = null
        lifecycleOwner = null
        isFullScreen = false
    }
}
