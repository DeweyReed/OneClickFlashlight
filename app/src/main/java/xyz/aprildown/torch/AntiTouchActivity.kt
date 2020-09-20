package xyz.aprildown.torch

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import xyz.aprildown.torch.databinding.ActivityAntiTouchBinding

class AntiTouchActivity : AppCompatActivity() {

    private var countDownHandler = Handler(Looper.getMainLooper())

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAntiTouchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fullScreenSystemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN

        fun toFullScreen() {
            binding.root.systemUiVisibility = fullScreenSystemUiVisibility
        }

        toFullScreen()
        window?.decorView?.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                toFullScreen()
            }
            true
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                toFullScreen()
            }
        })

        val ordinal = intent?.getIntExtra(EXTRA_TYPE, 0) ?: 0
        val observer = when (FlashlightShortcut.values()[ordinal]) {
            FlashlightShortcut.AntiTouch -> {
                object : DefaultLifecycleObserver {
                    override fun onCreate(owner: LifecycleOwner) {
                        on()
                    }

                    override fun onDestroy(owner: LifecycleOwner) {
                        done()
                    }
                }
            }
            FlashlightShortcut.EphemeralAntiTouch -> {
                object : DefaultLifecycleObserver {
                    override fun onCreate(owner: LifecycleOwner) {
                        on()
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        done()
                    }
                }
            }
            FlashlightShortcut.OnOffAntiTouch -> {
                object : DefaultLifecycleObserver {
                    override fun onCreate(owner: LifecycleOwner) {
                        on()
                    }

                    override fun onStart(owner: LifecycleOwner) {
                        on()
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        off()
                    }

                    override fun onDestroy(owner: LifecycleOwner) {
                        done()
                    }
                }
            }
            FlashlightShortcut.DelayedAntiTouch -> {
                object : DefaultLifecycleObserver {
                    override fun onCreate(owner: LifecycleOwner) {
                        on()
                    }

                    override fun onStart(owner: LifecycleOwner) {
                        countDownHandler.removeCallbacksAndMessages(null)
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        countDownHandler.postDelayed(
                            {
                                done()
                            },
                            intent?.getLongExtra(EXTRA_DELAY, 0L) ?: 0L
                        )
                    }
                }
            }
            else -> null
        }
        if (observer != null) {
            lifecycle.addObserver(observer)
        } else {
            finish()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            done()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun on() {
        FlashlightService.turn(this, on = true)
    }

    private fun off() {
        FlashlightService.turn(this, on = false)
    }

    private fun done() {
        countDownHandler.removeCallbacksAndMessages(null)
        off()
        finish()
    }

    companion object {
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_DELAY = "delay"

        fun getIntent(
            context: Context,
            type: FlashlightShortcut,
            delayInMilli: Long = 0L
        ): Intent {
            return Intent(context, AntiTouchActivity::class.java)
                .putExtra(EXTRA_TYPE, type.ordinal)
                .putExtra(EXTRA_DELAY, delayInMilli)
        }
    }
}
