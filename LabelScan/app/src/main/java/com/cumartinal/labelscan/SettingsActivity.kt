package com.cumartinal.labelscan

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_display_text.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.preferences_custom.*
import kotlinx.android.synthetic.main.preferences_custom.scan_extended_fab

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.preferences_custom)

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

    // Called when "+ Scan" button is pressed, creates MainActivity
    fun newScan(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

}