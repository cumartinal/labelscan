package com.cumartinal.labelscan

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
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
    private lateinit var adapter: RecyclerAdapter
    private lateinit var nutritionArray: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Set up recycler view
        linearLayoutManager = LinearLayoutManager(this)
        nutrientRecyclerView.layoutManager = linearLayoutManager

        // Set up adapter for recyclerview
        adapter = RecyclerAdapter(nutritionArray)
        nutrientRecyclerView.adapter = adapter

        // Add dividers between items on recyclerview
        nutrientRecyclerView.addItemDecoration(
            CustomDividerItemDecoration(
                nutrientRecyclerView.getContext(),
                linearLayoutManager.getOrientation()
            )
        )

        // Set up bottom navigation
        bottom_navigation_main.selectedItemId = R.id.placeholder
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

        val nutrientView = findViewById<RecyclerView>(R.id.nutrientRecyclerView).apply {
        }

        topAppBar.setNavigationOnClickListener {
            onBackPressed()
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.graph -> {
                    // Handle graph icon press
                    if (nutrientChart.isVisible) {
                        nutrientChart.visibility = View.INVISIBLE
                        nutrientScrollView.visibility = View.VISIBLE
                    } else {
                        nutrientChart.visibility = View.VISIBLE
                        nutrientScrollView.visibility = View.INVISIBLE
                        makeNutrientChart()
                    }
                    true
                }
                else -> false
            }
        }

    }

    private fun makeNutrientChart() {
        // Create chart and apply data
        val chart = findViewById<View>(R.id.nutrientChart) as HorizontalBarChart
        val data = BarData(getDataSet())
        chart.data = data

        // Customise chart's appearance
        chart.setScaleEnabled(false)
        chart.setTouchEnabled(false)

        // Setting the axis to the actual location on-screen
        // This API is an absolute mess
        val rightAxis = chart.xAxis
        val topAxis = chart.axisLeft
        val bottomAxis = chart.axisRight
        rightAxis.setDrawGridLines(false)
        topAxis.setAxisMaximum(100f)
        topAxis.setDrawLabels(false) // no axis labels
        topAxis.setDrawAxisLine(false) // no axis line
        topAxis.setDrawGridLines(false)
        bottomAxis.setAxisMaximum(100f)
        bottomAxis.setDrawLabels(false) // no axis labels
        bottomAxis.setDrawAxisLine(false) // no axis line
        bottomAxis.setDrawGridLines(false)
        chart.animateXY(500, 500)
        chart.invalidate()
    }

    private fun getDataSet(): BarDataSet? {
        val entries: ArrayList<BarEntry> = ArrayList()
        // Make an entry for each nutrient depending on it's porcentual DV
        // Daily values taken from:
        // https://www.fda.gov/food/new-nutrition-facts-label/daily-value-new-nutrition-and-supplement-facts-labels
        // Calories (2000)
        entries.add(BarEntry(14f, (nutritionArray[0] / 2000f) * 100))
        // Total fat (78)
        entries.add(BarEntry(13f, (nutritionArray[1] / 78f) * 100))
        // Saturated fat (20)
        entries.add(BarEntry(12f, 30f))
        // Trans fat (2, taken from https://medlineplus.gov/ency/patientinstructions/000786.htm#:~:text=You%20should%20limit%20saturated%20fat,or%202%20grams%20per%20day.)
        entries.add(BarEntry(11f, 40f))
        // Cholesterol (300)
        entries.add(BarEntry(10f, 90f))
        // Sodium (2300)
        entries.add(BarEntry(9f, 100f))
        // Total carbs (275)
        entries.add(BarEntry(8f, 0f))
        // Fiber (28)
        entries.add(BarEntry(7f, 20f))
        // Sugars (50) (Should become added sugars)
        entries.add(BarEntry(6f, 80f))
        // Protein (50)
        entries.add(BarEntry(5f, 50f))
        // Vitamin D (20)
        entries.add(BarEntry(4f, 50f))
        // Calcium (1300)
        entries.add(BarEntry(3f, 60f))
        // Iron (18)
        entries.add(BarEntry(2f, 30f))
        // Potassium (4700)
        entries.add(BarEntry(1f, 40f))

        return BarDataSet(entries, "Percentage of daily recommended value")
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