package com.attendanceapp.myapplication

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
//import android.util.Log
//import android.os.Environment
//import android.view.View

class AdminActivity : AppCompatActivity() {

    private lateinit var dateInput: EditText
    private lateinit var fetchButton: Button
    private lateinit var attendanceRecyclerView: RecyclerView
    // Declare new views
    private lateinit var searchBox: EditText
    private lateinit var searchButton: Button

    private val db = FirebaseFirestore.getInstance()
    private val attendanceList = mutableListOf<Map<String, String>>()
    private lateinit var attendanceAdapter: AttendanceAdapter

    private var selectedDateForExport: String = ""  // Store readable date

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val exportButton = findViewById<Button>(R.id.exportButton)
        exportButton.setOnClickListener {
            exportToCSV()
        }

        searchBox = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)

        searchButton.setOnClickListener {
            val rollNumber = searchBox.text.toString().trim()
            if (rollNumber.isNotEmpty() && selectedDateForExport.isNotEmpty()) {
                searchAttendanceByRoll(rollNumber, selectedDateForExport)
            } else {
                Toast.makeText(this, "Enter a roll number and select a date", Toast.LENGTH_SHORT).show()
            }
        }

        dateInput = findViewById(R.id.dateTextView)
        fetchButton = findViewById(R.id.fetchButton)
        attendanceRecyclerView = findViewById(R.id.recyclerView)

        attendanceAdapter = AttendanceAdapter(attendanceList)
        attendanceRecyclerView.layoutManager = LinearLayoutManager(this)
        attendanceRecyclerView.adapter = attendanceAdapter

        dateInput.setOnClickListener {
            showDatePicker()
        }

        fetchButton.setOnClickListener {
            val date = dateInput.text.toString().trim()
            if (date.isEmpty()) {
                Toast.makeText(this, "📅 Please select a date", Toast.LENGTH_SHORT).show()
            } else {
                selectedDateForExport = date // Store selected date for file name
                fetchAttendance(date)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%02d-%02d-%04d", dayOfMonth, month + 1, year)
                dateInput.setText(selectedDate)
                selectedDateForExport = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchAttendance(date: String) {
        db.collection("Attendance")
            .document(date)
            .collection("Students")
            .get()
            .addOnSuccessListener { result ->
                attendanceList.clear()
                for (doc in result) {
                    val data = doc.data.toMutableMap()
                    data["roll"] = doc.id

                    // Safe casting: convert Any? to String only if all values are strings
                    val safeData = data.mapNotNull {
                        val key = it.key
                        val value = it.value
                        if (value is String) key to value else null
                    }.toMap()

                    attendanceList.add(safeData)
                }
                attendanceAdapter.notifyDataSetChanged()
                Toast.makeText(this, "✅ Fetched ${attendanceList.size} records", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to fetch attendance", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun searchAttendanceByRoll(rollNumber: String, date: String) {
        attendanceList.clear()

        db.collection("Attendance")
            .document(date)
            .collection("Students")
            .document(rollNumber)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["roll"] = rollNumber
                    data["date"] = date

                    val safeData = data.mapNotNull {
                        val key = it.key
                        val value = it.value
                        if (value is String) key to value else null
                    }.toMap()

                    attendanceList.add(safeData)
                    attendanceAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "✅ Record found for Roll No: $rollNumber", Toast.LENGTH_SHORT).show()
                } else {
                    attendanceAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "❌ No record found for Roll No: $rollNumber on $date", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                attendanceAdapter.notifyDataSetChanged()
                Toast.makeText(this, "❌ Error searching for roll number", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exportToCSV() {
        if (attendanceList.isEmpty()) {
            Toast.makeText(this, "No attendance data to export", Toast.LENGTH_SHORT).show()
            return
        }

        val csvHeader = attendanceList[0].keys.joinToString(",")
        val csvBody = attendanceList.joinToString("\n") { it.values.joinToString(",") }

        val csvContent = "$csvHeader\n$csvBody"

        // 🟢 Use readable date instead of timestamp
        val dateForFilename = selectedDateForExport.ifBlank {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        }
        val fileName = "Attendance_$dateForFilename.csv"

        val downloadsDir = getExternalFilesDir(null)
        val file = File(downloadsDir, fileName)

        try {
            file.writeText(csvContent)
            Toast.makeText(this, "✅ CSV exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
            shareCSV(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "❌ Failed to export CSV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCSV(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "📤 Share CSV using"))
    }
}

