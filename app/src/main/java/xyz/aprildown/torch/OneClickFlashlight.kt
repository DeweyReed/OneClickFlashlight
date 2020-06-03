package xyz.aprildown.torch

import android.app.Application
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes

class OneClickFlashlight : Application() {
    override fun onCreate() {
        super.onCreate()

        // Just to track crashes during beta.
        AppCenter.start(
            this,
            BuildConfig.APP_CENTER_SECRET,
            Analytics::class.java,
            Crashes::class.java
        )
    }
}
