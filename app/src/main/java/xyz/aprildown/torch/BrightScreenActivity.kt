package xyz.aprildown.torch

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View

class BrightScreenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val view = View(this)
        view.setBackgroundColor(Color.WHITE)
        setContentView(view)

        window?.run {
            attributes = attributes.apply { screenBrightness = 1f }
            statusBarColor = Color.WHITE
            navigationBarColor = Color.WHITE
        }
    }
}
