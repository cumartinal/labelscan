package com.cumartinal.labelscan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
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
        val nutritionArray: IntArray = intent.getSerializableExtra("intArray") as IntArray

        // Set up recycler view
        linearLayoutManager = LinearLayoutManager(this)
        nutrientRecyclerView.layoutManager = linearLayoutManager

        // Set up adapter for recyclerview
        adapter = RecyclerAdapter(nutritionArray)
        nutrientRecyclerView.adapter = adapter

        // Add dividers between items on recyclerview
        nutrientRecyclerView.addItemDecoration(DividerItemDecoration(nutrientRecyclerView.getContext(), linearLayoutManager.getOrientation()))

        val nutrientView = findViewById<RecyclerView>(R.id.nutrientRecyclerView).apply {

        }
    }



    // Called when "+ Scan" button is pressed, creates MainActivity
    fun newScan(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "DetailedScreen"
    }
}