package com.cumartinal.labelscan

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_display_text.*
import kotlinx.android.synthetic.main.activity_display_text.scan_extended_fab
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, MySettingsFragment())
                .commit()

        // Set up bottom navigation and its listener
        bottom_navigation_settings.selectedItemId = R.id.settingsItem

        bottom_navigation_settings.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.favoritesItem -> {
                    val contextView = findViewById<View>(R.id.bottom_navigation_settings)
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

    // Called when "+ Scan" button is pressed, creates MainActivity
    fun newScan(view: View) {
        val mediaPlayerNavigationScan = MediaPlayer.create(this, R.raw.ui_tap_02)
        mediaPlayerNavigationScan.start()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

}

class MySettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}