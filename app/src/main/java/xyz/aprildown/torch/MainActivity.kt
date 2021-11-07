package xyz.aprildown.torch

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            ACTION_SHORTCUT_CREATED -> {
                Toast.makeText(this, R.string.shortcut_created, Toast.LENGTH_SHORT).show()
            }
            ACTION_FLOATING_WINDOW -> {
                startService(FloatingWindowService.getToggleIntent(this))
            }
            else -> {
                FlashlightService.toggle(this)
            }
        }

        finish()
    }

    companion object {
        private const val ACTION_SHORTCUT_CREATED = "short_created"
        private const val ACTION_FLOATING_WINDOW = "floating_window"

        fun getIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }

        fun getShortcutCreatedIntentSender(context: Context): IntentSender {
            return PendingIntent.getActivity(
                context,
                0,
                getIntent(context).setAction(ACTION_SHORTCUT_CREATED),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ).intentSender
        }

        fun getFloatingWindowIntent(context: Context): Intent {
            return getIntent(context).setAction(ACTION_FLOATING_WINDOW)
        }
    }
}
