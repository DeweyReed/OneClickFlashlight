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
                    if (isTorchOn()) {
                        setTorchMode(false)
                        stopSelf()
                    } else {
                        setTorchMode(true)
                        toForeground()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun setTorchMode(enable: Boolean) {
        for (currentCameraId in cm.cameraIdList) {
            try {
                cm.setTorchMode(currentCameraId, enable)
                break
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun toForeground() {
        val nm = NotificationManagerCompat.from(this)
        if (nm.getNotificationChannel(CHANNEL_ID) == null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getText(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.channel_desp)
                    setShowBadge(false)
                }
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(R.string.notif_desp))
            .setContentIntent(
                PendingIntent.getService(
                    this,
                    0,
                    getIntent(this),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        cm.unregisterTorchCallback(torchMonitor)
    }

    private suspend fun isTorchOn(): Boolean = try {
        withTimeout(500) {
            suspendCancellableCoroutine<Boolean> { cont ->
                var isResumed = false
                val callback = object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        cm.unregisterTorchCallback(this)
                        if (!isResumed) {
                            isResumed = true
                            cont.resume(enabled)
                        }
                    }
                }
                cont.invokeOnCancellation { cm.unregisterTorchCallback(callback) }
                cm.registerTorchCallback(callback, null)
            }
        }
    } catch (e: TimeoutCancellationException) {
        // This may be triggered if the camera's turned on by other apps using Camera 1 or 2 APIs.
        // So we return true to turn off the torch
        e.printStackTrace()
        true
    }

    companion object {
        private const val CHANNEL_ID = "channel"

        private const val ACTION_TOGGLE = "toggle"

        private fun getIntent(context: Context): Intent =
            Intent(context, FlashlightService::class.java)
                .setAction(ACTION_TOGGLE)

        fun toggle(context: Context) {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                Toast.makeText(context, R.string.flashlight_not_found, Toast.LENGTH_LONG).show()
                return
            }
            context.startService(getIntent(context))
        }
    }
}
