package xyz.aprildown.torch

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }
}

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var cm: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cm = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.setStorageDeviceProtected()
        setPreferencesFromResource(R.xml.settings, rootKey)

        val context = requireContext()
        findPreference<SwitchPreferenceCompat>(getString(R.string.settings_toggle_key))
            ?.setOnPreferenceClickListener {
                FlashlightService.toggle(context)
                true
            }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            findPreference<SwitchPreferenceCompat>(getString(R.string.settings_toggle_key))
                ?.isChecked = enabled
        }
    }

    override fun onResume() {
        super.onResume()
        cm.registerTorchCallback(torchCallback, null)
    }

    override fun onPause() {
        super.onPause()
        cm.unregisterTorchCallback(torchCallback)
    }
}
