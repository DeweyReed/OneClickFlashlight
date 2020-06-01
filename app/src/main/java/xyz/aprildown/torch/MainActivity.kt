package xyz.aprildown.torch

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(TorchService.getIntent(this))

        finish()
    }
}
