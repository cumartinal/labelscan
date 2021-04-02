package com.cumartinal.labelscan

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_display_text.*
import kotlinx.android.synthetic.main.activity_settings.scan_extended_fab
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.activity_settings.bottom_navigation_main


class SettingsActivity : AppCompatActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        // Set up bottom navigation and its listener
        bottom_navigation_main.selectedItemId = R.id.settingsItem
        bottom_navigation_main.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.favoritesItem -> {
                    val contextView = findViewById<View>(R.id.bottom_navigation_main)
                    Snackbar.make(contextView, "This feature is not yet implemented! Please wait for future updates", Snackbar.LENGTH_LONG)
                        .setAnchorView(scan_extended_fab)
                        .show()
                    false
                }
                R.id.settingsItem -> {
                    true
                }
                else -> false
            }
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, MySettingsFragment())
                .commit()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        listener =
                SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences, key: String ->
                    if (key == "theming") {
                        val preferenceValue = sharedPreferences.getString(key, "")
                        Log.i(TAG, "Theming preference value was updated to: " + preferenceValue)

                        when (preferenceValue) {
                            "Light" -> setDefaultNightMode(MODE_NIGHT_NO)
                            "Dark" -> setDefaultNightMode(MODE_NIGHT_YES)
                            "System" -> setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                            else -> {
                                Log.d(TAG, "ERROR CHANGING THEMING, PREFERENCE VALUE DOES NOT EXIST")
                            }
                        }
                    }
                }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(listener)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                pref.fragment)
        fragment.arguments = args
        fragment.setTargetFragment(caller, 0)

        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(null)
                .commit()
        return true
    }

    /*
    // Apply preferences when change
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "theming") {
            val preferenceValue = sharedPreferences.getString(key, "")
            Log.i(TAG, "Theming preference value was updated to: " + preferenceValue)

            when (preferenceValue) {
                "Light" -> setDefaultNightMode(MODE_NIGHT_NO)
                "Dark" -> setDefaultNightMode(MODE_NIGHT_YES)
                "System" -> setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                else -> {
                    Log.d(TAG, "ERROR CHANGING THEMING, PREFERENCE VALUE DOES NOT EXIST")
                }
            }
        }
    }
    */

    // Called when "+ Scan" button is pressed, creates MainActivity
    fun newScan(view: View) {
        val mediaPlayerNavigationScan = MediaPlayer.create(this, R.raw.ui_tap_02)
        mediaPlayerNavigationScan.start()
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivityIfNeeded(intent, 0)
    }

    companion object {
        private const val TAG = "SettingsActivity"
    }

}

class MySettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}