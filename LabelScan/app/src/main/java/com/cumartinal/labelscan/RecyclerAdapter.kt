package com.cumartinal.labelscan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class RecyclerAdapter(private val nutritionArray: FloatArray) :
        RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    // Make Array with names of nutrients
    // Possible values include: kcal, totFat, satFat, traFat
    // cholesterol, sodium, totCarbs, fiber, sugars, protein
    // vitD, calcium, iron, potassium
    private val nutrientNames = arrayOf("Calories", "Total fat", "Saturated fat", "Trans fat",
                                "Cholesterol", "Sodium", "Total carbohydrates", "Fiber",
                                "Total Sugars", "Added Sugars", "Protein", "Vitamin D", "Calcium", "Iron",
                                "Potassium")
    // Make array with units of nutrients
    private val nutrientUnits = arrayOf("kcal","g", "g", "g", "mg", "mg", "g", "g", "g", "g", "g",
                                "mcg", "mg", "mg", "mg")

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nutrientTextView: TextView
        val valueTextView: TextView

        init {
            nutrientTextView = view.findViewById(R.id.nutrientName)
            valueTextView = view.findViewById(R.id.nutrientValue)
        }
    }

    // Create new views, invoked by the layout manager
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.recyclerview_row, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view invoked by the layout manager
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.nutrientTextView.text = nutrientNames[position]
        if (nutritionArray[position].rem(1) == 0.0f) {
            viewHolder.valueTextView.text = nutritionArray[position].toInt().toString() + nutrientUnits[position]
        } else {
            val roundedValue = (nutritionArray[position] * 10.0f).roundToInt() / 10.0f
            viewHolder.valueTextView.text = roundedValue.toString() + nutrientUnits[position]
        }

        // Talkback does not automatically read mcg as micrograms, so we need to specify it manually
        if (nutrientUnits[position] == "mcg") {
            // Avoid reading 0.0 mcg, just read 0 mcg
            if (nutritionArray[position].rem(1) == 0.0f)
                viewHolder.valueTextView.contentDescription = nutritionArray[position].toInt().toString() + "micrograms"
            else
                viewHolder.valueTextView.contentDescription = nutritionArray[position].toString() + "micrograms"
        }

    }

    override fun getItemCount(): Int {
        return nutritionArray.size
    }

}