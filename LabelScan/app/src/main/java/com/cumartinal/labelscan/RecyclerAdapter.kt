package com.cumartinal.labelscan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter(private val nutritionArray: IntArray) :
        RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    // Make Array with names of nutrients
    // Possible values include: kcal, totFat, satFat, traFat
    // cholesterol, sodium, totCarbs, fiber, sugars, protein
    // vitD, calcium, iron, potassium
    private val nutrientNames = arrayOf("Calories", "Total fat", "Saturated fat", "Trans fat",
                                "Cholesterol", "Sodium", "Total carbohydrates", "Fiber",
                                "Sugars", "Protein", "Vitamin D", "Calcium", "Iron",
                                "Potassium")
    // Make array with units of nutrients
    private val nutrientUnits = arrayOf("kcal","g", "g", "g", "mg", "mg", "g", "g", "g", "g",
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
        if (nutritionArray != null) {
            viewHolder.nutrientTextView.text = nutrientNames.get(position)
            viewHolder.valueTextView.text = nutritionArray.get(position).toString()  + nutrientUnits.get(position)

            // Talkback does not automatically read mcg as micrograms, so we need to specify it manually
            if (nutrientUnits.get(position) == "mcg")
                viewHolder.valueTextView.contentDescription = nutritionArray.get(position).toString() + "micrograms"
        }

    }

    override fun getItemCount(): Int {
        if (nutritionArray != null) {
            return nutritionArray.size
        } else {
            return 0
        }
    }

}