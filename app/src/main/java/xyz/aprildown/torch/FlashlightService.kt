package xyz.aprildown.torch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

class FlashlightService : LifecycleService() {

    private lateinit var cm: CameraManager
    private var isFirstTorchCallback = true

    override fun onCreate() {
        super.onCreate()
        cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cm.registerTorchCallback(torchMonitor, null)
    }

    private val torchMonitor = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (isFirstTorchCallback) {
                isFirstTorchCallback = false
            } else {
                if (!enabled) {
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_TOGGLE -> {
                lifecycleScope.launch {
                    if (cm.isTorchOn()) {
                        turnOff()
                    } else {
                        turnOn()
                    }
                }
            }
            ACTION_TURN -> {
                lifecycleScope.launch {
                    val shouldOn = intent.getBooleanExtra(EXTRA_TURN_ON, false)
                    if (!shouldOn) {
                        turnOff()
                    } else if (!cm.isTorchOn()) {
                        turnOn()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun turnOn() {
        cm.setTorchMode(true)
        toForeground()
    }

    private fun turnOff() {
        cm.setTorchMode(false)
        stopSelf()
    }

    private fun toForeground() {
        ensureNotificationChannel(this)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(R.string.notif_desp))
            .setContentIntent(
                PendingIntent.getService(
                    this,
                    0,
                    getToggleIntent(this),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setSmallIcon(R.drawable.ic_logo)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        cm.unregisterTorchCallback(torchMonitor)
    }

    companion object {
        const val CHANNEL_ID = "channel"

        private const val ACTION_TOGGLE = "toggle"
        private const val ACTION_TURN = "turn"
        private const val EXTRA_TURN_ON = "on"

        fun ensureNotificationChannel(context: Context) {
            val nm = NotificationManagerCompat.from(context)
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                nm.getNotificationChannel(CHANNEL_ID) == null
            ) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getText(R.string.channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        setShowBadge(false)
                    }
                )
            }
        }

        private fun getPureIntent(context: Context): Intent {
            return Intent(context, FlashlightService::class.java)
        }

        private fun getToggleIntent(context: Context): Intent {
            return getPureIntent(context).setAction(ACTION_TOGGLE)
        }

        private fun getTurnIntent(context: Context, on: Boolean): Intent {
            return getPureIntent(context).setAction(ACTION_TURN)
                .putExtra(EXTRA_TURN_ON, on)
        }

        private fun ensureFlashlightAvailability(context: Context): Boolean {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                Toast.makeText(context, R.string.flashlight_not_found, Toast.LENGTH_LONG).show()
                return false
            }
            return true
        }

        fun toggle(context: Context) {
            if (!ensureFlashlightAvailability(context)) return
            context.startService(getToggleIntent(context))
        }

        fun turn(context: Context, on: Boolean) {
            if (!ensureFlashlightAvailability(context)) return
            context.startService(getTurnIntent(context, on))
        }
    }
}

suspend fun CameraManager.isTorchOn(): Boolean = try {
    withTimeout(500) {
        suspendCancellableCoroutine { cont ->
            var isResumed = false
            val callback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    unregisterTorchCallback(this)
                    if (!isResumed) {
                        isResumed = true
                        cont.resume(enabled)
                    }
                }
            }
            cont.invokeOnCancellation { unregisterTorchCallback(callback) }
            registerTorchCallback(callback, null)
        }
    }
} catch (e: TimeoutCancellationException) {
    // This may be triggered if the camera's turned on by other apps using Camera 1 or 2 APIs.
    // So we return true to turn off the torch
    e.printStackTrace()
    true
}

fun CameraManager.setTorchMode(enable: Boolean) {
    for (currentCameraId in cameraIdList) {
        try {
            setTorchMode(currentCameraId, enable)
            break
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
