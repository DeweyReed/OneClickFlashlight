package xyz.aprildown.torch

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager

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
        cm.unregisterTorchCallback(callback)

        finish()
    }
}
