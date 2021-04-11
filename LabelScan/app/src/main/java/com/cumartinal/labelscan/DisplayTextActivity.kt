package com.cumartinal.labelscan

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_display_text.*


class DisplayTextActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var linearLayoutManagerPies: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var adapterPies: RecyclerAdapterPies
    private lateinit var nutritionArray: IntArray
    private var isPale = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Color value to pass to RecyclerAdapterPies
        // Apply theme depending on saved preference
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        val themingValue = sharedPreferences.getString("theming", "")
        when (themingValue) {
            "Light" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                setTheme(R.style.Theme_LabelScan)
            }
            "Dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.Theme_LabelScan)
            }
            "Pale" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                setTheme(R.style.Theme_LabelScan_Pale)
                isPale = true
            }
            "System" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                setTheme(R.style.Theme_LabelScan)
            }
            else -> {
                Log.d(TAG, "ERROR LOADING THEMING, PREFERENCE VALUE DOES NOT EXIST")
            }
        }
        setContentView(R.layout.activity_display_text)

        // Get the Intent that started this activity and extract the string
        val message = intent.getStringExtra(EXTRA_MESSAGE)
        @Suppress("UNCHECKED_CAST")
        nutritionArray = intent.getSerializableExtra("intArray") as IntArray

        // Set up recycler views
        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManagerPies = LinearLayoutManager(this)
        nutrientRecyclerView.layoutManager = linearLayoutManager
        nutrientPiesRecyclerView.layoutManager = linearLayoutManagerPies

        // Set up adapter for recyclerviews
        adapter = RecyclerAdapter(nutritionArray)
        nutrientRecyclerView.adapter = adapter

        // RecyclerAdapterPies needs to know if the theme we're using is pale
        // And which color the background is
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.backgroundColor, typedValue, true)
        var backgroundColor = typedValue.data
        // Transparency effect will change depending on theme
        // Dark theme needs to have the pie background be lighter than the actual background
        // Light and pale theme need to have it darker (alpha increased)
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> {
                backgroundColor = Color.argb(255,
                        Color.red(backgroundColor) + 40, Color.green(backgroundColor) + 40,
                        Color.blue(backgroundColor) + 40)
            }
            else -> {
                backgroundColor = Color.argb(12, Color.red(backgroundColor), Color.green(backgroundColor), Color.blue(backgroundColor))
            }
        }
        adapterPies = RecyclerAdapterPies(nutritionArray, isPale, backgroundColor)
        nutrientPiesRecyclerView.adapter = adapterPies

        // Add dividers between items on recyclerviews
        nutrientRecyclerView.addItemDecoration(
            CustomDividerItemDecoration(
                nutrientRecyclerView.getContext(),
                linearLayoutManager.getOrientation()
            )
        )
        nutrientPiesRecyclerView.addItemDecoration(
            CustomDividerItemDecoration(
                nutrientPiesRecyclerView.getContext(),
                linearLayoutManager.getOrientation()
            )
        )

        // Set up bottom navigation
        bottom_navigation_main.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.favoritesItem -> {
                    val contextView = findViewById<View>(R.id.bottom_navigation_main)
                    Snackbar.make(
                        contextView,
                        "This feature is not yet implemented! Please wait for future updates",
                        Snackbar.LENGTH_LONG
                    )
                        .setAnchorView(scan_extended_fab)
                        .show()
                    if (sharedPreferences.getBoolean("earcons", true)) {
                        val mediaPlayerNavigationFav = MediaPlayer.create(this, R.raw.ui_tap_03)
                        mediaPlayerNavigationFav.start()
                    }
                    false
                }
                R.id.settingsItem -> {
                    if (sharedPreferences.getBoolean("earcons", true)) {
                        val mediaPlayerNavigationSet = MediaPlayer.create(this, R.raw.ui_tap_01)
                        mediaPlayerNavigationSet.start()
                    }
                    openSettings()
                    true
                }
                else -> false
            }
        }

        topAppBar.setNavigationOnClickListener {
            onBackPressed()
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.graph -> {
                    // Handle graph icon press
                    if (nutrientPiesScrollView.isVisible) {
                        nutrientPiesScrollView.visibility = View.INVISIBLE
                        nutrientScrollView.visibility = View.VISIBLE
                    } else {
                        nutrientPiesScrollView.visibility = View.VISIBLE
                        nutrientScrollView.visibility = View.INVISIBLE
                    }
                    true
                }
                else -> false
            }
        }

    }

    // Called when "+ Scan" button is pressed, creates MainActivity
    fun newScan(view: View) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        if (sharedPreferences.getBoolean("earcons", true)) {
            val mediaPlayerNavigationScan = MediaPlayer.create(this, R.raw.ui_tap_02)
            mediaPlayerNavigationScan.start()
        }
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