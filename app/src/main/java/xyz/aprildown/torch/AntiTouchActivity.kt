package xyz.aprildown.torch

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

        setUpShowWhenLocked()
        setUpFullscreen(binding.root)

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
                        if (!isFinishing) {
                            val time = intent?.getLongExtra(EXTRA_DELAY, 0L) ?: 0L
                            countDownHandler.postDelayed({ done() }, time)
                            (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                                PowerManager.PARTIAL_WAKE_LOCK,
                                "OneClickFlashlight:DelayedAntiTouch"
                            ).acquire(time + 500L /* More time for work. */)
                        }
                    }

                    override fun onDestroy(owner: LifecycleOwner) {
                        done()
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP
        ) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP
        ) {
            done()
            return true
        }
        return super.onKeyUp(keyCode, event)
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

    private fun setUpShowWhenLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun setUpFullscreen(rootView: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.getWindowInsetsController(rootView)
            ?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE

        fun toFullScreen() {
            ViewCompat.getWindowInsetsController(rootView)
                ?.hide(WindowInsetsCompat.Type.systemBars())
        }

        toFullScreen()
        @Suppress("ClickableViewAccessibility")
        window?.decorView?.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                toFullScreen()
            }
            true
        }

        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    toFullScreen()
                }
            }
        )
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
