package com.cumartinal.labelscan

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlin.math.roundToInt

class RecyclerAdapterPies(nutritionArray: FloatArray, private val isPale: Boolean,
                          private val backgroundColor: Int, private val isMotionReduced: Boolean) :
    RecyclerView.Adapter<RecyclerAdapterPies.ViewHolder>() {

    // Make Array with names of nutrients THAT HAVE A DAILY VALUE
    // Possible values include: kcal, totFat, satFat,
    // cholesterol, sodium, totCarbs, fiber, addSugars, protein
    // vitD, calcium, iron, potassium
    private val nutrientNames = arrayOf("Calories", "Total fat", "Saturated fat" ,
        "Cholesterol", "Sodium", "Total carbohydrates", "Fiber",
        "Added Sugars", "Protein", "Vitamin D", "Calcium", "Iron",
        "Potassium")
    // Make array with daily values
    // Daily values taken from:
    // https://www.fda.gov/food/new-nutrition-facts-label/daily-value-new-nutrition-and-supplement-facts-labels
    private val nutrientDVs = arrayOf(2000f, 78f, 20f, 300f, 2300f, 275f, 28f, 50f, 50f, 20f,
        1300f, 18f, 4700f)

    // Create array with only the nutrients that have a related DV
    // So excluding total sugars (i=8) and trans fat (i=3)
    private val nutritionPercentageArray = arrayOf(nutritionArray[0], nutritionArray[1],
            nutritionArray[2], nutritionArray[4], nutritionArray[5], nutritionArray[6],
            nutritionArray[7], nutritionArray[9], nutritionArray[10], nutritionArray[11],
            nutritionArray[12], nutritionArray[13], nutritionArray[14])

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
        // Create name of nutrient and percentage, set accessible contentDescriptions
        val percentageDV = (nutritionPercentageArray[position] / nutrientDVs[position]) * 100
        viewHolder.nutrientTextView.text = nutrientNames.get(position) + "\n" + "DV: " + percentageDV.roundToInt() + "%"
        viewHolder.nutrientTextView.contentDescription = nutrientNames.get(position) + ", " + percentageDV.roundToInt() + "% of the Daily Value"

        // Add entries and create PieDataSet
        val entries: ArrayList<PieEntry> = ArrayList()
        entries.add(PieEntry(percentageDV, "%"))
        entries.add(PieEntry(100 - percentageDV, ""))
        val pieDataSet = PieDataSet(entries, "")
        pieDataSet.setDrawIcons(false)
        pieDataSet.setDrawValues(false)

        // Add colors depending on theme
        val colors: ArrayList<Int> = ArrayList()
        val colorToAdd: Int
        when {
            percentageDV >= 20 -> {
                colorToAdd = if (isPale) {
                    Color.parseColor("#EF504E")
                } else {
                    Color.parseColor("#D50000")
                }
                colors.add(colorToAdd)
            }
            percentageDV >= 5 -> {
                colorToAdd = if (isPale) {
                    Color.parseColor("#C76400")
                } else {
                    Color.parseColor("#F56A00")
                }
                colors.add(colorToAdd)
            }
            else -> {
                colorToAdd = if (isPale) {
                    Color.parseColor("#558B2F")
                } else {
                    Color.parseColor("#33691E")
                }
                colors.add(colorToAdd)
            }
        }
        colors.add(backgroundColor)
        pieDataSet.colors = colors

        // Customise appearance and disable direct interaction
        viewHolder.nutrientPieView.data = PieData(pieDataSet)
        viewHolder.nutrientPieView.isDrawHoleEnabled = false
        viewHolder.nutrientPieView.holeRadius = 10f
        viewHolder.nutrientPieView.description.isEnabled = false
        viewHolder.nutrientPieView.setDrawEntryLabels(false)
        viewHolder.nutrientPieView.setTouchEnabled(false)
        viewHolder.nutrientPieView.legend.isEnabled = false
        if (!isMotionReduced)
            viewHolder.nutrientPieView.animateY(1400, Easing.EaseInOutQuad)
        viewHolder.nutrientPieView.invalidate()

    }

    override fun getItemCount(): Int {
        return nutritionPercentageArray.size
    }

}