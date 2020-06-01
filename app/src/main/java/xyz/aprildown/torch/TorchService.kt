package xyz.aprildown.torch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TorchService : LifecycleService() {

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
            .setContentTitle(getString(R.string.app_name))
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

    private suspend fun isTorchOn(): Boolean = suspendCancellableCoroutine { cont ->
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                cm.unregisterTorchCallback(this)
                cont.resume(enabled)
            }
        }
        cont.invokeOnCancellation { cm.unregisterTorchCallback(callback) }
        cm.registerTorchCallback(callback, null)
    }

    companion object {
        private const val CHANNEL_ID = "channel"

        private const val ACTION_TOGGLE = "toggle"

        fun getIntent(context: Context): Intent =
            Intent(context, TorchService::class.java)
                .setAction(ACTION_TOGGLE)
    }
}
