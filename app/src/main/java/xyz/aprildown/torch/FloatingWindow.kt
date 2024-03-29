package xyz.aprildown.torch

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingWindowService : LifecycleService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isRunning = false
    private var turnOnTheFlashlight = false
    private var closeWithTheFlashlight = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_TOGGLE -> {
                if (isRunning) {
                    stopSelf()

                    if (closeWithTheFlashlight) {
                        FlashlightService.turn(this, false)
                    }
                } else {
                    turnOnTheFlashlight =
                        intent.getBooleanExtra(EXTRA_TURN_ON_THE_FLASHLIGHT, false)
                    closeWithTheFlashlight =
                        intent.getBooleanExtra(EXTRA_CLOSE_WITH_THE_FLASHLIGHT, false)

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
            @Suppress("LaunchActivityFromNotification")
            NotificationCompat.Builder(this, FlashlightService.CHANNEL_ID)
                .setContentTitle(getText(R.string.floating_window_service_title))
                .setContentText(getText(R.string.floating_window_service_desp))
                .setContentIntent(
                    PendingIntent.getService(
                        this,
                        0,
                        getToggleIntent(this),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

        val receivedEnabled = mutableListOf<Boolean>()

        /** [CameraManager.registerTorchCallback] */
        val torchCallback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                mainHandler.post {

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

                    if (closeWithTheFlashlight) {
                        receivedEnabled += enabled

                        if (turnOnTheFlashlight) {
                            if (receivedEnabled.lastOrNull() == false &&
                                receivedEnabled.contains(true)
                            ) {
                                stopSelf()
                            }
                        } else {
                            if (receivedEnabled.size > 1 && receivedEnabled.lastOrNull() == false) {
                                stopSelf()
                            }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main.immediate) {
            if (turnOnTheFlashlight && !cm.isTorchOn()) {
                FlashlightService.turn(this@FloatingWindowService, true)
            }

            val floater = Floater(
                context = this@FloatingWindowService,
                view = fab
            )
            floater.show()
            cm.registerTorchCallback(torchCallback, null)
            isRunning = true

            lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        cm.unregisterTorchCallback(torchCallback)
                        floater.dismiss()
                        isRunning = false
                    }
                }
            )
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "toggle"

        const val EXTRA_TURN_ON_THE_FLASHLIGHT = "turn_on_the_flashlight"
        const val EXTRA_CLOSE_WITH_THE_FLASHLIGHT = "close_with_the_flashlight"

        private fun getPureIntent(context: Context): Intent {
            return Intent(context, FloatingWindowService::class.java)
        }

        fun getToggleIntent(
            context: Context,
            turnOnTheFlashlight: Boolean = false,
            closeWithTheFlashlight: Boolean = false,
        ): Intent {
            return getPureIntent(context).setAction(ACTION_TOGGLE)
                .putExtra(EXTRA_TURN_ON_THE_FLASHLIGHT, turnOnTheFlashlight)
                .putExtra(EXTRA_CLOSE_WITH_THE_FLASHLIGHT, closeWithTheFlashlight)
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
        gravity = Gravity.TOP or Gravity.START
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    private val screenWidth: Int
    private val screenHeight: Int

    private var isViewAdded = false
    private var currentOrientation = context.resources.configuration.orientation

    init {
        val displayMetrics = Resources.getSystem().displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    private val orientationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent?.action != Intent.ACTION_CONFIGURATION_CHANGED) return

            val newOrientation = context.resources.configuration.orientation
            if (currentOrientation != newOrientation) {
                currentOrientation = newOrientation
                if (isViewAdded) {
                    if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
                        updatePos(x = 0, y = screenHeight / 3)
                    } else {
                        updatePos(x = 0, y = screenWidth / 3)
                    }
                }
            }
        }
    }

    fun show() {
        val touchListener = FloatViewTouchListener(
            fm = this,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        view.setOnTouchListener(touchListener)
        wm.addView(view, lp)
        isViewAdded = true

        updatePos(x = 0, y = screenHeight / 3)

        context.registerReceiver(
            orientationReceiver,
            IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
        )
    }

    fun dismiss() {
        isViewAdded = false
        wm.removeView(view)

        context.unregisterReceiver(orientationReceiver)
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

private class FloatViewTouchListener(
    private val fm: Floater,
    private val screenWidth: Int,
    private val screenHeight: Int
) : View.OnTouchListener {

    private val context = fm.context
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f

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
