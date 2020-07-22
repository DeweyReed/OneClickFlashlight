package xyz.aprildown.torch

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

fun Context.pinShortcut(type: FlashlightShortcut) {
    ShortcutManagerCompat.requestPinShortcut(
        this,
        ShortcutInfoCompat.Builder(this, System.currentTimeMillis().toString())
            .setShortLabel(getText(type.nameRes))
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
                    FlashlightShortcut.BrightScreen,
                    FlashlightShortcut.Flashbang -> {
                        BrightScreenActivity.getIntent(this@pinShortcut, type)
                    }
                    FlashlightShortcut.FloatingWindow -> {
                        MainActivity.getFloatingWindowIntent(this@pinShortcut)
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
