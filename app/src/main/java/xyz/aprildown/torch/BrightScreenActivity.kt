package xyz.aprildown.torch

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class BrightScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val view = View(this)
        view.setBackgroundColor(Color.WHITE)
        setContentView(view)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        window?.run {
            attributes = attributes.apply { screenBrightness = 1f }
            statusBarColor = Color.WHITE
            navigationBarColor = Color.WHITE
        }

        val ordinal = intent?.getIntExtra(EXTRA_TYPE, 0) ?: 0
        if (FlashlightShortcut.values()[ordinal] == FlashlightShortcut.Flashbang) {
            lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onCreate(owner: LifecycleOwner) {
                        FlashlightService.turn(this@BrightScreenActivity, on = true)
                    }

                    override fun onDestroy(owner: LifecycleOwner) {
                        FlashlightService.turn(this@BrightScreenActivity, on = false)
                    }
                }
            )
        }
    }

    companion object {
        private const val EXTRA_TYPE = "type"

        fun getIntent(context: Context, type: FlashlightShortcut): Intent {
            return Intent(context, BrightScreenActivity::class.java)
                .putExtra(EXTRA_TYPE, type.ordinal)
        }
    }
}
