package xyz.aprildown.torch

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

fun Context.pinShortcut(
    type: FlashlightShortcut,

    delayedAntiTouchDelayInMilli: Long = 0L,

    floatingWindowTurnOnTheFlashlight: Boolean = false,
    floatingWindowCloseWithTheFlashlight: Boolean = false,
) {
    ShortcutManagerCompat.requestPinShortcut(
        this,
        ShortcutInfoCompat.Builder(this, System.currentTimeMillis().toString())
            .setShortLabel(
                buildString {
                    if (type == FlashlightShortcut.DelayedAntiTouch && delayedAntiTouchDelayInMilli > 0L) {
                        append(delayedAntiTouchDelayInMilli / 1000L)
                        append("s")
                    }
                    append(getText(type.nameRes))
                }
            )
            .setIcon(IconCompat.createWithResource(this, type.iconRes))
            .apply {
                val intent = when (type) {
                    FlashlightShortcut.Toggle -> {
                        MainActivity.getIntent(this@pinShortcut)
                    }
                    FlashlightShortcut.AntiTouch,
                    FlashlightShortcut.EphemeralAntiTouch,
                    FlashlightShortcut.OnOffAntiTouch -> {
                        AntiTouchActivity.getIntent(this@pinShortcut, type)
                    }
                    FlashlightShortcut.DelayedAntiTouch -> {
                        AntiTouchActivity.getIntent(
                            context = this@pinShortcut,
                            type = type,
                            delayInMilli = delayedAntiTouchDelayInMilli
                        )
                    }
                    FlashlightShortcut.BrightScreen,
                    FlashlightShortcut.Flashbang -> {
                        BrightScreenActivity.getIntent(this@pinShortcut, type)
                    }
                    FlashlightShortcut.FloatingWindow -> {
                        MainActivity.getFloatingWindowIntent(
                            this@pinShortcut,
                            turnOnTheFlashlight = floatingWindowTurnOnTheFlashlight,
                            closeWithTheFlashlight = floatingWindowCloseWithTheFlashlight,
                        )
                    }
                }.run {
                    if (action.isNullOrBlank()) {
                        setAction("")
                    } else {
                        this
                    }
                }
                setIntent(intent)
            }
            .build(),
        MainActivity.getShortcutCreatedIntentSender(this)
    )
}
