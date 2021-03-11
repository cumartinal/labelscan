package com.cumartinal.labelscan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter(private val nutritionMap: HashMap<String, Int>?) :
        RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    // Transform HashMap into Arrays
    // I should not be doing this, maybe make it from the start arrays???

    val keyList = ArrayList(nutritionMap?.keys)
    val valueList = ArrayList(nutritionMap?.values)

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
        if (nutritionMap != null) {
            viewHolder.nutrientTextView.text = keyList.get(position)
            viewHolder.valueTextView.text = valueList.get(position).toString()
        }

    }

    override fun getItemCount(): Int {
        if (nutritionMap != null) {
            return nutritionMap.size
        } else {
            return 0
        }
    }



}