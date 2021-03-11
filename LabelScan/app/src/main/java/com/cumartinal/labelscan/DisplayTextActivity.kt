package com.cumartinal.labelscan

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        val nutritionMap: HashMap<String, Int>? = intent.getSerializableExtra("hashMap") as? HashMap<String, Int>

        // Set up recycler view
        linearLayoutManager = LinearLayoutManager(this)
        nutrientRecyclerView.layoutManager = linearLayoutManager

        // Set up adapter for recyclerview
        adapter = RecyclerAdapter(nutritionMap)
        nutrientRecyclerView.adapter = adapter

        if (nutritionMap != null) {
            nutritionMap.forEach { (key, value) -> Log.d(TAG, "$key : $value") }
        }

        val nutrientView = findViewById<RecyclerView>(R.id.nutrientRecyclerView).apply {

        }
    }



    // Called when "+ Scan" button is pressed, creates MainActivity
    fun newScan (view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "DetailedScreen"
    }
}