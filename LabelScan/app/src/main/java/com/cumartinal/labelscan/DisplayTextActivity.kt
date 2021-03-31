package com.cumartinal.labelscan

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_display_text.*


class DisplayTextActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_text)

        // Get the Intent that started this activity and extract the string
        val message = intent.getStringExtra(EXTRA_MESSAGE)
        @Suppress("UNCHECKED_CAST")
        val nutritionArray: IntArray = intent.getSerializableExtra("intArray") as IntArray

        // Set up recycler view
        linearLayoutManager = LinearLayoutManager(this)
        nutrientRecyclerView.layoutManager = linearLayoutManager

        // Set up adapter for recyclerview
        adapter = RecyclerAdapter(nutritionArray)
        nutrientRecyclerView.adapter = adapter

        // Add dividers between items on recyclerview
        nutrientRecyclerView.addItemDecoration(DividerItemDecoration(nutrientRecyclerView.getContext(), linearLayoutManager.getOrientation()))

        // Set up bottom navigation
        bottom_navigation_main.selectedItemId = R.id.placeholder
        bottom_navigation_main.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.favoritesItem -> {
                    val contextView = findViewById<View>(R.id.bottom_navigation_main)
                    Snackbar.make(contextView, "This feature is not yet implemented! Please wait for future updates", Snackbar.LENGTH_LONG)
                            .setAnchorView(scan_extended_fab)
                            .show()
                    val mediaPlayerNavigationFav = MediaPlayer.create(this, R.raw.ui_tap_03)
                    mediaPlayerNavigationFav.start()
                    false
                }
                R.id.settingsItem -> {
                    val mediaPlayerNavigationSet = MediaPlayer.create(this, R.raw.ui_tap_01)
                    mediaPlayerNavigationSet.start()
                    openSettings()
                    true
                }
                else -> false
            }
        }

        val nutrientView = findViewById<RecyclerView>(R.id.nutrientRecyclerView).apply {

        }
    }

    // Called when "+ Scan" button is pressed, creates MainActivity
    fun newScan(view: View) {
        val mediaPlayerNavigationScan = MediaPlayer.create(this, R.raw.ui_tap_02)
        mediaPlayerNavigationScan.start()
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivityIfNeeded(intent, 0)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java).apply {
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "DetailedScreen"
    }
}