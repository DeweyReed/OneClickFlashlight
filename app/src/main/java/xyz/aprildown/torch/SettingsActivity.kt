package xyz.aprildown.torch

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
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

        findPreference<Preference>(getString(R.string.shortcuts_toggle_key))
            ?.setOnPreferenceClickListener {
                context.pinShortcut(FlashlightShortcut.Toggle)
                true
            }
        findPreference<Preference>(getString(R.string.shortcuts_anti_touch_key))
            ?.setOnPreferenceClickListener {
                context.pinShortcut(FlashlightShortcut.AntiTouch)
                true
            }
        findPreference<Preference>(getString(R.string.shortcuts_ephemeral_anti_touch_key))
            ?.setOnPreferenceClickListener {
                context.pinShortcut(FlashlightShortcut.EphemeralAntiTouch)
                true
            }
        findPreference<Preference>(getString(R.string.shortcuts_on_off_anti_touch_key))
            ?.setOnPreferenceClickListener {
                context.pinShortcut(FlashlightShortcut.OnOffAntiTouch)
                true
            }
        findPreference<Preference>(getString(R.string.shortcuts_bright_screen_key))
            ?.setOnPreferenceClickListener {
                context.pinShortcut(FlashlightShortcut.BrightScreen)
                true
            }
        findPreference<Preference>(getString(R.string.shortcuts_flashbang_key))
            ?.setOnPreferenceClickListener {
                context.pinShortcut(FlashlightShortcut.Flashbang)
                true
            }

        findPreference<Preference>(getString(R.string.shortcuts_floating_window_key))
            ?.setOnPreferenceClickListener {
                if (context.canDrawOverlays()) {
                    context.pinShortcut(FlashlightShortcut.FloatingWindow)
                } else {
                    startActivityForResult(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            .setData(Uri.parse("package:${context.packageName}")),
                        0
                    )
                }

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

private fun Context.canDrawOverlays(): Boolean = if (isMOrLater()) {
    Settings.canDrawOverlays(this)
} else {
    try {
        val manager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val dispatchMethod = AppOpsManager::class.java.getMethod(
            "checkOp",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java
        )
        // AppOpsManager.OP_SYSTEM_ALERT_WINDOW = 24
        AppOpsManager.MODE_ALLOWED == dispatchMethod.invoke(
            manager,
            24,
            Binder.getCallingUid(),
            packageName
        ) as Int
    } catch (e: Exception) {
        false
    }
}
