package com.reactionstudio.screenrecorder.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
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

    private var currentLens: CameraLens = CameraLens.FRONT
    private var currentShape: FacecamShape = FacecamShape.CIRCLE
    private var currentSize: FacecamSize = FacecamSize.MEDIUM
    private var currentBorderColor: FacecamBorderColor = FacecamBorderColor.RED
    private var lifecycleOwner: LifecycleOwner? = null
    private var cameraProvider: ProcessCameraProvider? = null

    fun isShowing(): Boolean = rootView != null

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
        // Default PERFORMANCE mode uses SurfaceView which renders to a hardware overlay plane and is NOT captured by MediaProjection VirtualDisplay.
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
        val borderView = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            background = getBorderDrawable(currentShape)
        }
        container.addView(borderView)

        // Mini action bar below or on top of camera (Flip, Resize, Close)
        val actionToolbar = createActionToolbar()
        container.addView(actionToolbar)

        root.addView(container)

        // Drag & Touch handling
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
        previewContainer?.let { container ->
            if (container.childCount > 1) {
                container.getChildAt(1).background = getBorderDrawable(currentShape)
            }
        }
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
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent dark

            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(28)
            ).apply {
                gravity = Gravity.BOTTOM
            }
            layoutParams = lp

            // Flip Camera button
            val flipBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_rotate)
                setColorFilter(Color.WHITE)
                val pad = dpToPx(4)
                setPadding(pad, pad, pad, pad)
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                layoutParams = p
                setOnClickListener {
                    flipCamera()
                }
            }
            addView(flipBtn)

            // Resize button
            val resizeBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_crop)
                setColorFilter(Color.WHITE)
                val pad = dpToPx(4)
                setPadding(pad, pad, pad, pad)
                val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                layoutParams = p
                setOnClickListener {
                    cycleSize()
                }
            }
            addView(resizeBtn)

            // Close button
            val closeBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.WHITE)
                val pad = dpToPx(4)
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
        val newSize = when (currentSize) {
            FacecamSize.SMALL -> FacecamSize.MEDIUM
            FacecamSize.MEDIUM -> FacecamSize.LARGE
            FacecamSize.LARGE -> FacecamSize.SMALL
        }
        updateSize(newSize)
    }

    fun updateSize(size: FacecamSize) {
        currentSize = size
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
        previewContainer?.let { container ->
            applyShapeToContainer(container, shape)
            container.invalidateOutline()
            if (container.childCount > 1) {
                container.getChildAt(1).background = getBorderDrawable(shape)
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
        layoutParams = null
        lifecycleOwner = null
    }
}
