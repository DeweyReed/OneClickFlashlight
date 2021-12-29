package xyz.aprildown.torch

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.torch.databinding.DialogDelayInputBinding
import xyz.aprildown.torch.databinding.DialogFloatingWindowBinding

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

    private val manageOverlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cm = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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
        findPreference<Preference>(getString(R.string.shortcuts_delayed_anti_touch_key))
            ?.setOnPreferenceClickListener {
                context.requestDelay {
                    context.pinShortcut(
                        FlashlightShortcut.DelayedAntiTouch,
                        delayedAntiTouchDelayInMilli = it
                    )
                }
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
                if (Settings.canDrawOverlays(context)) {
                    val binding = DialogFloatingWindowBinding.inflate(LayoutInflater.from(context))
                    binding.layoutTurnOnTheFlashlight.setOnClickListener {
                        binding.switchTurnOnTheFlashlight.toggle()
                    }
                    binding.layoutCloseWithTheFlashlight.setOnClickListener {
                        binding.switchCloseWithTheFlashlight.toggle()
                    }
                    MaterialAlertDialogBuilder(context)
                        .setView(binding.root)
                        .setTitle(R.string.shortcuts_floating_window_title)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            context.pinShortcut(
                                FlashlightShortcut.FloatingWindow,
                                floatingWindowTurnOnTheFlashlight = binding.switchTurnOnTheFlashlight.isChecked,
                                floatingWindowCloseWithTheFlashlight = binding.switchCloseWithTheFlashlight.isChecked,
                            )
                        }
                        .show()
                } else {
                    manageOverlayPermissionLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            .setData(Uri.parse("package:${context.packageName}"))
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

private fun Context.requestDelay(onResult: (Long) -> Unit) {
    val builder = MaterialAlertDialogBuilder(this)
        .setTitle(R.string.shortcuts_delayed_anti_touch_delay_input_title)
        .setPositiveButton(android.R.string.ok, null)

    val view =
        DialogDelayInputBinding.inflate(LayoutInflater.from(this))
    view.editDelayInput.requestFocus()

    builder.setView(view.root)

    val dialog = builder.create()
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    dialog.show()
    dialog.setOnDismissListener {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

    view.editDelayInput.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE && positiveButton.isEnabled) {
            positiveButton.performClick()
            true
        } else {
            false
        }
    }

    view.editDelayInput.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun afterTextChanged(s: Editable?) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val input = s?.toString()?.toIntOrNull()
            if (input != null && input in 1..10) {
                view.inputDelayInput.error = null
                positiveButton.isEnabled = true
            } else {
                view.inputDelayInput.error = "1s ~ 10s"
                positiveButton.isEnabled = false
            }
        }
    })

    positiveButton.setOnClickListener {
        dialog.dismiss()
        onResult.invoke((view.editDelayInput.text?.toString()?.toLongOrNull() ?: 0L) * 1_000L)
    }
}
