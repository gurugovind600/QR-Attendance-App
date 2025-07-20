package com.attendanceapp.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.*

class StudentActivity : AppCompatActivity() {

    private lateinit var nameTextView: TextView
    private lateinit var bannerViewPager: ViewPager
    private lateinit var logoutButton: Button
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var monthSpinner: Spinner
    private val bannerImages = listOf(R.drawable.banner1, R.drawable.banner2, R.drawable.banner3)

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var currentRoll: String

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)

        nameTextView = findViewById(R.id.helloTextView)
        bannerViewPager = findViewById(R.id.bannerViewPager)
        logoutButton = findViewById(R.id.logoutButton)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        monthSpinner = findViewById(R.id.monthSelector)

        bannerViewPager.adapter = BannerAdapter(this, bannerImages)

        val prefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val savedRoll = prefs.getString("roll", "")
        val savedName = prefs.getString("name", null)

        if (!savedRoll.isNullOrEmpty() && !savedName.isNullOrEmpty()) {
            currentRoll = savedRoll
            nameTextView.text = "Hello, $savedName 👋"
            setupMonthSpinner()
        } else {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("Users").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val roll = doc.getString("roll") ?: ""
                        val name = doc.getString("name") ?: "Student"
                        nameTextView.text = "Hello, $name 👋"
                        prefs.edit().apply {
                            putString("roll", roll)
                            putString("name", name)
                            apply()
                        }
                        currentRoll = roll
                        setupMonthSpinner()
                    }
                    .addOnFailureListener {
                        nameTextView.text = "Hello, Student 👋"
                    }
            }
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        swipeRefreshLayout.setOnRefreshListener {
            setupMonthSpinner()
            swipeRefreshLayout.isRefreshing = false
        }

        // Auto-slide banners
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                bannerViewPager.currentItem = (bannerViewPager.currentItem + 1) % bannerImages.size
                handler.postDelayed(this, 3000)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupMonthSpinner() {
        val monthsList = generateMonthList()
        val adapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, monthsList)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        monthSpinner.adapter = adapter

        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()
                val (monthName, year) = selected.split(" ")
                val formattedMonth = "${convertMonthToNumber(monthName)}-$year"

                loadLineChartForAttendance(currentRoll, formattedMonth)
                loadSummaryCharts(currentRoll, formattedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun generateMonthList(): List<String> {
        val list = mutableListOf<String>()
        val calendar = Calendar.getInstance()

        for (i in 0..11) {
            val date = calendar.time
            val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(date)
            val year = calendar.get(Calendar.YEAR)
            list.add("$month $year")
            calendar.add(Calendar.MONTH, -1)
        }

        return list
    }

    private fun convertMonthToNumber(month: String): String = when (month) {
        "January" -> "01"
        "February" -> "02"
        "March" -> "03"
        "April" -> "04"
        "May" -> "05"
        "June" -> "06"
        "July" -> "07"
        "August" -> "08"
        "September" -> "09"
        "October" -> "10"
        "November" -> "11"
        "December" -> "12"
        else -> "01"
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadLineChartForAttendance(roll: String, monthYear: String) {
        val lineChart = findViewById<LineChart>(R.id.lineChart)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, monthYear.substringBefore("-").toInt() - 1)
        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val entries = mutableListOf<Entry>()
        var loaded = 0

        for (day in 1..totalDays) {
            val dateStr = String.format("%02d-%s", day, monthYear)

            db.collection("Attendance").document(dateStr).collection("Students")
                .document(roll)
                .get()
                .addOnSuccessListener { doc ->
                    val isPresent = if (doc.exists()) 1f else 0f
                    entries.add(Entry(day.toFloat(), isPresent))
                    loaded++

                    if (loaded == totalDays) {
                        val sorted = entries.sortedBy { it.x }
                        val dataSet = LineDataSet(sorted, "Daily Attendance")
                        dataSet.color = ColorTemplate.getHoloBlue()
                        dataSet.setDrawFilled(true)
                        dataSet.setDrawValues(false)
                        dataSet.fillAlpha = 180
                        dataSet.lineWidth = 2f

                        lineChart.data = LineData(dataSet)
                        lineChart.description.text = "Attendance for $monthYear"
                        lineChart.xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            granularity = 1f
                            setDrawGridLines(false)
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return value.toInt().toString()
                                }
                            }
                        }
                        lineChart.axisLeft.axisMinimum = 0f
                        lineChart.axisLeft.axisMaximum = 1.1f
                        lineChart.axisRight.isEnabled = false
                        lineChart.invalidate()
                    }
                }
        }
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadSummaryCharts(roll: String, monthYear: String) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, monthYear.substringBefore("-").toInt() - 1)
        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        var presentCount = 0
        var loadedDays = 0

        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val barChart = findViewById<BarChart>(R.id.barChart)

        for (day in 1..totalDays) {
            val dateStr = String.format("%02d-%s", day, monthYear)

            db.collection("Attendance").document(dateStr).collection("Students")
                .document(roll)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) presentCount++
                    loadedDays++

                    if (loadedDays == totalDays) {
                        val absent = totalDays - presentCount

                        val pieEntries = listOf(
                            PieEntry(presentCount.toFloat(), "Present"),
                            PieEntry(absent.toFloat(), "Absent")
                        )
                        val pieSet = PieDataSet(pieEntries, "Attendance Split")
                        pieSet.colors = listOf(
                            ColorTemplate.rgb("#4CAF50"),
                            ColorTemplate.rgb("#F44336")
                        )
                        pieChart.data = PieData(pieSet)
                        pieChart.description.text = "Attendance - $monthYear"
                        pieChart.invalidate()

                        val barEntries = listOf(
                            BarEntry(0f, presentCount.toFloat()),
                            BarEntry(1f, absent.toFloat())
                        )
                        val barSet = BarDataSet(barEntries, "Summary")
                        barSet.colors = pieSet.colors
                        barChart.data = BarData(barSet)
                        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Present", "Absent"))
                        barChart.xAxis.granularity = 1f
                        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                        barChart.axisLeft.axisMinimum = 0f
                        barChart.axisRight.isEnabled = false
                        barChart.description.text = "Summary - $monthYear"
                        barChart.invalidate()
                    }
                }
        }
    }
}
