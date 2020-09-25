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
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.RequiresApi
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setUpFullscreenR(rootView)
        } else {
            setUpFullscreenPreR(rootView)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ClickableViewAccessibility")
    private fun setUpFullscreenR(rootView: View) {
        window.setDecorFitsSystemWindows(true)
        rootView.windowInsetsController
            ?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_SWIPE

        fun toFullScreen() {
            rootView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
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
    }

    @Suppress("DEPRECATION")
    @SuppressLint("ClickableViewAccessibility")
    private fun setUpFullscreenPreR(rootView: View) {
        val fullScreenSystemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN

        fun toFullScreen() {
            rootView.systemUiVisibility = fullScreenSystemUiVisibility
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
