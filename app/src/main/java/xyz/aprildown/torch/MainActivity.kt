package xyz.aprildown.torch

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sendBroadcast(
            Intent(this, TorchReceiver::class.java)
                .setAction(TorchReceiver.ACTION_TOGGLE)
        )

        finish()
    }
}
