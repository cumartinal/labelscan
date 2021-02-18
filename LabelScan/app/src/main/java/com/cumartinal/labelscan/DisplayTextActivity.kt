package com.cumartinal.labelscan

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView

class DisplayTextActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_text)

        // Get the Intent that started this activity and extract the string
        val message = intent.getStringExtra(EXTRA_MESSAGE)
        @Suppress("UNCHECKED_CAST")
        val nutritionMap: HashMap<String, Int>? = intent.getSerializableExtra("hashMap") as? HashMap<String, Int>

        // Capture the layout's TextView and set the string as its text
        val textView = findViewById<TextView>(R.id.textViewRecognized).apply {
            text = message
        }

        if (nutritionMap != null) {
            nutritionMap.forEach { (key, value) -> Log.d(TAG, "$key : $value") }
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