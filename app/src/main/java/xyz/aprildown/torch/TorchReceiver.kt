package xyz.aprildown.torch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager

class TorchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == ACTION_TOGGLE) {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val callback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    cm.unregisterTorchCallback(this)

                    for (currentCameraId in cm.cameraIdList) {
                        try {
                            cm.setTorchMode(currentCameraId, !enabled)
                            break
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            cm.registerTorchCallback(callback, null)
            // If we unregister here, the first callback will be ignored.
            // Since registerTorchCallback says the callback will be called immediately after being
            // registered, I assume it guarantees the callback code will be ran.
        }
    }

    companion object {
        const val ACTION_TOGGLE = "xyz.aprildown.torch.TOGGLE"
    }
}
