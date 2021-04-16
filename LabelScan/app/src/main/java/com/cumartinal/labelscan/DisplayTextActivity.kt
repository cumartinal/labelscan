package com.cumartinal.labelscan

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import android.transition.Slide
import android.view.Gravity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_display_text.*


class DisplayTextActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var linearLayoutManagerPies: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private lateinit var adapterPies: RecyclerAdapterPies
    private lateinit var nutritionArray: FloatArray
    private var isPale = false
    private var isViewingPies = false
    private var shortAnimationDuration: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Entry transition
        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            enterTransition = Slide (Gravity.RIGHT)
            enterTransition.propagation = null
        }

        // Color value to pass to RecyclerAdapterPies
        // Apply theme depending on saved preference
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        when (sharedPreferences.getString("theming", "")) {
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
        nutritionArray = intent.getSerializableExtra("floatArray") as FloatArray
        val servingSize = intent.getSerializableExtra("servingSizeString") as String

        // Set TextView with serving size if it has been recognised
        if (servingSize != "") {
            textViewNutrientScroll.visibility = View.VISIBLE
            textViewNutrientPiesScroll.visibility = View.VISIBLE

            textViewNutrientScroll.text = "(Serving size: $servingSize)"
            textViewNutrientPiesScroll.text = "(Serving size: $servingSize)"

            // Very inelegant way to setContentDescription for edge cases
            // But I do not have the time to implement it properly
            when (servingSize) {
                "1/3 cup" -> {
                    textViewNutrientScroll.contentDescription = "(Serving size: 1/3 of a cup)"
                    textViewNutrientPiesScroll.contentDescription = "(Serving size: 1/3 of a cup)"
                }
                "2/3 cup" -> {
                    textViewNutrientScroll.contentDescription = "(Serving size: 2/3 of a cup)"
                    textViewNutrientPiesScroll.contentDescription = "(Serving size: 2/3 of a cup)"
                }
                "3/4 cup" -> {
                    textViewNutrientScroll.contentDescription = "(Serving size: 3/4 of a cup)"
                    textViewNutrientPiesScroll.contentDescription = "(Serving size: 3/4 of a cup)"
                }
            }
        }

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
        backgroundColor = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> {
                Color.argb(255,
                        Color.red(backgroundColor) + 40, Color.green(backgroundColor) + 40,
                        Color.blue(backgroundColor) + 40)
            }
            else -> {
                Color.argb(12, Color.red(backgroundColor), Color.green(backgroundColor), Color.blue(backgroundColor))
            }
        }
        val isMotionReduced = (sharedPreferences.getBoolean("reducedMotion", false))
        adapterPies = RecyclerAdapterPies(nutritionArray, isPale, backgroundColor, isMotionReduced)
        nutrientPiesRecyclerView.adapter = adapterPies

        // Add dividers between items on recyclerviews
        nutrientRecyclerView.addItemDecoration(
            CustomDividerItemDecoration(
                nutrientRecyclerView.context,
                linearLayoutManager.orientation
            )
        )
        nutrientPiesRecyclerView.addItemDecoration(
            CustomDividerItemDecoration(
                nutrientPiesRecyclerView.context,
                linearLayoutManager.orientation
            )
        )

        // Set up bottom navigation
        bottom_navigation_main.menu.getItem(0).isCheckable = false
        bottom_navigation_main.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.favoritesItem -> {
                    val contextView = findViewById<View>(R.id.bottom_navigation_main)
                    Snackbar.make(
                        contextView,
                        "This feature is not yet implemented! Please wait for future updates.",
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

        // Retrieve and cache the system's default "short" animation time
        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        // Set appbar listeners
        topAppBar.setNavigationOnClickListener {
            onBackPressed()
        }
        // Change view and shown icon between list and pies
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.piesMenuItem -> {
                    isViewingPies = true
                    if (isMotionReduced) {
                        nutrientScrollView.visibility = View.GONE
                        nutrientPiesScrollView.visibility = View.VISIBLE
                    } else  {
                        crossfade(nutrientScrollView, nutrientPiesScrollView)
                    }
                    topAppBar.menu.findItem(R.id.piesMenuItem).isVisible = false
                    topAppBar.menu.findItem(R.id.listMenuItem).isVisible = true
                    true
                }
                R.id.listMenuItem -> {
                    isViewingPies = false
                    if (isMotionReduced) {
                        nutrientPiesScrollView.visibility = View.GONE
                        nutrientScrollView.visibility = View.VISIBLE
                    } else {
                        crossfade(nutrientPiesScrollView, nutrientScrollView)
                    }
                    topAppBar.menu.findItem(R.id.piesMenuItem).isVisible = true
                    topAppBar.menu.findItem(R.id.listMenuItem).isVisible = false
                    true
                }
                else -> false
            }
        }

    }

    private fun crossfade(viewToFadeOut: View, viewToFadeIn: View) {
        viewToFadeIn.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                    .alpha(1f)
                    .setDuration(shortAnimationDuration.toLong())
                    .setListener(null)
        }

        viewToFadeOut.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration.toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        viewToFadeOut.visibility = View.GONE
                    }
                })

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

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java).apply {
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "DetailedScreen"
    }
}