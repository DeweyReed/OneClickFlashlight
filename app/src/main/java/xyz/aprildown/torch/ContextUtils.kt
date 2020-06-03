package xyz.aprildown.torch

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

fun isMOrLater(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

fun isNOrLater(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

private val Context.safeContext: Context
    get() = takeIf { isNOrLater() && !isDeviceProtectedStorage }?.let {
        ContextCompat.createDeviceProtectedStorageContext(it) ?: it
    } ?: this

val Context.safeSharedPreference: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(safeContext)
