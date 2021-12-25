package xyz.aprildown.torch

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

val Context.defaultSharedPreferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)
