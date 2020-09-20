package xyz.aprildown.torch

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Add the new enum to the end because the enum's ordinal is used by shortcuts.
 */
enum class FlashlightShortcut(@StringRes val nameRes: Int, @DrawableRes val iconRes: Int) {
    Toggle(
        nameRes = R.string.shortcuts_toggle_title,
        iconRes = R.drawable.shortcut_toggle
    ),
    AntiTouch(
        nameRes = R.string.shortcuts_anti_touch_title,
        iconRes = R.drawable.shortcut_anti_touch
    ),
    EphemeralAntiTouch(
        nameRes = R.string.shortcuts_ephemeral_anti_touch_title,
        iconRes = R.drawable.shortcut_ephemeral_anti_touch
    ),
    OnOffAntiTouch(
        nameRes = R.string.shortcuts_on_off_anti_touch_title,
        iconRes = R.drawable.shortcut_on_off_anti_touch
    ),
    BrightScreen(
        nameRes = R.string.shortcuts_bright_screen_title,
        iconRes = R.drawable.shortcut_bright_screen
    ),
    Flashbang(
        nameRes = R.string.shortcuts_flashbang_title,
        iconRes = R.drawable.shortcut_flashbang
    ),
    FloatingWindow(
        nameRes = R.string.shortcuts_floating_window_title,
        iconRes = R.drawable.shortcut_floating_window
    ),
    DelayedAntiTouch(
        nameRes = R.string.shortcuts_delayed_anti_touch_title,
        iconRes = R.drawable.shortcut_delayed_anti_touch
    ),
}
