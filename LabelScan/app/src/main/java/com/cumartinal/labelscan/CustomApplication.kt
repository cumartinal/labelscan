package com.cumartinal.labelscan

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

class CustomApplication : Application() {
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    override fun onCreate() {
        super.onCreate()

        // Apply theme depending on saved preference
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        when (sharedPreferences.getString("theming", "")) {
            "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "Pale" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                setTheme(R.style.Theme_LabelScan_Pale)
            }
            "System" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> {
                Log.d("CustomApplication", "ERROR CHANGING THEMING, PREFERENCE VALUE DOES NOT EXIST")
            }
        }
    }
}