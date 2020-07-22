package xyz.aprildown.torch

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs

class FloatingWindowService : LifecycleService() {

    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_TOGGLE -> {
                if (isRunning) {
                    stopSelf()
                } else {
                    bringUp()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun bringUp() {

        // Notification
        FlashlightService.ensureNotificationChannel(this)

        startForeground(
            2,
            NotificationCompat.Builder(this, FlashlightService.CHANNEL_ID)
                .setContentTitle(getText(R.string.floating_window_service_title))
                .setContentText(getText(R.string.floating_window_service_desp))
                .setContentIntent(
                    PendingIntent.getService(
                        this,
                        0,
                        getToggleIntent(this),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
                .setSmallIcon(R.drawable.settings_floating_window)
                .build()
        )

        // Floating View
        val fab = View.inflate(
            ContextThemeWrapper(this, R.style.AppTheme),
            R.layout.view_floating_window,
            null
        ) as FloatingActionButton

        fab.setOnClickListener {
            FlashlightService.toggle(this)
        }

        // UI
        val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val torchCallback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                ImageViewCompat.setImageTintList(
                    fab,
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this@FloatingWindowService,
                            if (enabled) {
                                R.color.colorSecondary
                            } else {
                                android.R.color.white
                            }
                        )
                    )
                )
            }
        }
        var floater: Floater? = null
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    floater = Floater(
                        context = this@FloatingWindowService,
                        view = fab
                    )
                    floater?.show()
                    cm.registerTorchCallback(torchCallback, null)
                    isRunning = true
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    cm.unregisterTorchCallback(torchCallback)
                    floater?.dismiss()
                    floater = null
                    isRunning = false
                }
            }
        )
    }

    companion object {
        private const val ACTION_TOGGLE = "toggle"

        private fun getPureIntent(context: Context): Intent {
            return Intent(context, FloatingWindowService::class.java)
        }

        fun getToggleIntent(context: Context): Intent {
            return getPureIntent(context).setAction(ACTION_TOGGLE)
        }
    }
}

private class Floater(
    val context: Context,
    private val view: View,
    initialWidth: Int = WindowManager.LayoutParams.WRAP_CONTENT,
    initialHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT
) {

    private val wm: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val lp: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        format = PixelFormat.RGBA_8888
        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        width = initialWidth
        height = initialHeight
        gravity = Gravity.TOP or GravityCompat.START
        type = if (isOOrLater()) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    var currentX: Int = 0
        private set
        get() = lp.x

    var currentY: Int = 0
        private set
        get() = lp.y

    private var isViewAdded = false

    fun show() {
        val touchListener = FloatViewTouchListener(this)
        view.setOnTouchListener(touchListener)
        wm.addView(view, lp)
        isViewAdded = true

        updatePos(
            x = touchListener.screenWidth / 2,
            y = touchListener.screenHeight / 2
        )
    }

    fun dismiss() {
        isViewAdded = false
        wm.removeView(view)
    }

    fun updatePos(x: Int = currentX, y: Int = currentY) {
        lp.x = x
        currentX = x
        lp.y = y
        currentY = y
        update()
    }

    private fun update() {
        if (!isViewAdded) return
        wm.updateViewLayout(view, lp)
    }
}

private class FloatViewTouchListener(private val fm: Floater) : View.OnTouchListener {

    private val context = fm.context
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    val screenWidth: Int
    val screenHeight: Int

    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f

    init {
        val displayMetrics = Resources.getSystem().displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.rawX
                startX = x
                lastX = x
                val y = event.rawY
                startY = y
                lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.rawX
                val y = event.rawY
                fm.updatePos(
                    (fm.currentX + (x - lastX)).toInt().coerceIn(
                        0,
                        screenWidth - (v?.width ?: 0)
                    ),
                    (fm.currentY + (y - lastY)).toInt().coerceIn(
                        0,
                        (screenHeight - (v?.height ?: 0))
                    )
                )
                lastX = x
                lastY = y
            }
            MotionEvent.ACTION_UP -> {
                val x = event.rawX
                val y = event.rawY
                if (abs(x - startX) <= slop && abs(y - startY) <= slop) {
                    // Some move is a click
                    v?.performClick()
                } else {
                    // make sure the view is out of screen bottom or top
                    val viewHeight = v?.height ?: 0
                    val floatY = fm.currentY
                    if (floatY < 0) {
                        fm.updatePos(y = 0)
                    } else {
                        if (floatY + viewHeight > screenHeight) {
                            fm.updatePos(y = screenHeight - viewHeight)
                        }
                    }
                }
            }
        }
        return true
    }
}
