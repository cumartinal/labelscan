package com.cumartinal.labelscan

import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_settings.bottom_navigation_main
import kotlinx.android.synthetic.main.activity_settings.scan_extended_fab

class SettingsActivity : AppCompatActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply theme depending on saved preference
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        when (sharedPreferences.getString("theming", "")) {
            "Light" -> {
                setDefaultNightMode(MODE_NIGHT_NO)
                setTheme(R.style.Theme_LabelScan)
            }
            "Dark" -> {
                setDefaultNightMode(MODE_NIGHT_YES)
                setTheme(R.style.Theme_LabelScan)
            }
            "Pale" -> {
                setTheme(R.style.Theme_LabelScan_Pale)
            }
            "System" -> {
                setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                setTheme(R.style.Theme_LabelScan)
            }
            else -> {
                Log.d(TAG, "ERROR LOADING THEMING, PREFERENCE VALUE DOES NOT EXIST")
            }
        }
        setContentView(R.layout.activity_settings)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, MySettingsFragment())
                .commit()

        // Change current screen and overall app theme
        listener =
                SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences, key: String ->
                    if (key == "theming") {
                        val preferenceValue = sharedPreferences.getString(key, "")
                        Log.i(TAG, "Theming preference value was updated to: $preferenceValue")

                        when (preferenceValue) {
                            "Light" -> {
                                // Double switch just in case the previous theme was Pale
                                // Inelegant? Yes. Is there a better way to do this? Probably.
                                // It works though and it's just one extra line.
                                // The fade to black has the same duration too.
                                setDefaultNightMode(MODE_NIGHT_YES)
                                setDefaultNightMode(MODE_NIGHT_NO)
                                setTheme(R.style.Theme_LabelScan)
                            }
                            "Dark" -> {
                                setDefaultNightMode(MODE_NIGHT_YES)
                                setTheme(R.style.Theme_LabelScan)
                            }
                            "Pale" -> {
                                setDefaultNightMode(MODE_NIGHT_NO)
                                setTheme(R.style.Theme_LabelScan_Pale)
                                // Restarts the whole application
                                // HORRIBLE WAY TO DO THIS
                                // Should stay in the same screen, but I can't figure out how to refresh ActivityMain's without switching there
                                // TODO
                                val intent = baseContext.packageManager.getLaunchIntentForPackage(
                                        baseContext.packageName)
                                intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                            }
                            "System" -> {
                                setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                                setTheme(R.style.Theme_LabelScan)
                            }
                            else -> {
                                Log.d(TAG, "ERROR CHANGING THEMING, PREFERENCE VALUE DOES NOT EXIST")
                            }
                        }
                    }
                }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        // Set up bottom navigation and its listener
        bottom_navigation_main.selectedItemId = R.id.settingsItem
        bottom_navigation_main.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.favoritesItem -> {
                    val contextView = findViewById<View>(R.id.bottom_navigation_main)
                    Snackbar.make(contextView, "This feature is not yet implemented! Please wait for future updates.", Snackbar.LENGTH_LONG)
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

    // Called when "+ Scan" button is pressed, creates MainActivity
    fun newScan(view: View) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        if (sharedPreferences.getBoolean("earcons", true)) {
            val mediaPlayerNavigationScan = MediaPlayer.create(this, R.raw.ui_tap_02)
            mediaPlayerNavigationScan.start()
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
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