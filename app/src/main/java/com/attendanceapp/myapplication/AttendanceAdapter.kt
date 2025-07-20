package com.attendanceapp.myapplication

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceAdapter(private val items: List<Map<String, String>>) :
    RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roll: TextView = view.findViewById(R.id.roll)
        val name: TextView = view.findViewById(R.id.name)
        val timestamp: TextView = view.findViewById(R.id.timestamptext)
        //val dateTextView: TextView = view.findViewById(R.id.dateTextView) // ✅ Add this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.roll.text = "Roll: ${item["roll"]}"
        holder.name.text = "Name: ${item["name"]}"
        holder.timestamp.text = "Time: ${item["timestamp"]}"
        //holder.dateTextView.text = "Date: ${item["date"] ?: "Unknown"}"
    }
}

