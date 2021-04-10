package com.cumartinal.labelscan

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.data.*

class RecyclerAdapterPies(private val nutritionArray: IntArray, private val secondaryColor: Int,
                          private val backgroundColor: Int) :
    RecyclerView.Adapter<RecyclerAdapterPies.ViewHolder>() {

    // Make Array with names of nutrients
    // Possible values include: kcal, totFat, satFat, traFat
    // cholesterol, sodium, totCarbs, fiber, sugars, protein
    // vitD, calcium, iron, potassium
    val nutrientNames = arrayOf("Calories", "Total fat", "Saturated fat", "Trans fat",
        "Cholesterol", "Sodium", "Total carbohydrates", "Fiber",
        "Sugars", "Protein", "Vitamin D", "Calcium", "Iron",
        "Potassium")
    // Make array with units of nutrients
    val nutrientUnits = arrayOf("kcal","g", "g", "g", "mg", "mg", "g", "g", "g", "g",
        "mcg", "mg", "mg", "mg")
    // Make array with daily values
    // Daily values taken from:
    // https://www.fda.gov/food/new-nutrition-facts-label/daily-value-new-nutrition-and-supplement-facts-labels
    val nutrientDVs = arrayOf(2000f, 78f, 20f, 2f, 300f, 2300f, 275f, 28f, 50f, 50f, 20f,
        1300f, 18f, 4700f)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nutrientTextView: TextView
        val nutrientPieView: com.github.mikephil.charting.charts.PieChart

        init {
            nutrientTextView = view.findViewById(R.id.graphNutrientName)
            nutrientPieView = view.findViewById(R.id.nutrientPieChart)
        }
    }

    // Create new views, invoked by the layout manager
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.nutrient_graph_recyclerview_row, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view invoked by the layout manager
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (nutritionArray != null) {
            viewHolder.nutrientTextView.text = nutrientNames.get(position)

            // Add entries and create PieDataSet
            val entries: ArrayList<PieEntry> = ArrayList()
            val percentageDV = (nutritionArray[position] / nutrientDVs[position]) * 100
            entries.add(PieEntry(percentageDV, "%"))
            entries.add(PieEntry(100 - percentageDV, ""))
            val pieDataSet = PieDataSet(entries, "")
            pieDataSet.setDrawIcons(false)
            pieDataSet.setDrawValues(false)

            // Add colors
            val colors: ArrayList<Int> = ArrayList()
            colors.add(secondaryColor)
            colors.add(backgroundColor)
            pieDataSet.colors = colors

            viewHolder.nutrientPieView.data = PieData(pieDataSet)
            viewHolder.nutrientPieView.isDrawHoleEnabled = false
            viewHolder.nutrientPieView.description.isEnabled = false
            viewHolder.nutrientPieView.setDrawEntryLabels(false)
            viewHolder.nutrientPieView.animateY(1400, Easing.EaseInOutQuad)
            viewHolder.nutrientPieView.invalidate()
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