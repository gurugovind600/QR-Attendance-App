package com.attendanceapp.myapplication.viewmodel

//import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendanceapp.myapplication.model.Student
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.N)
class StudentViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 1. Initialize variables BEFORE init to prevent crash
    private val currentMonthYear: String = java.text.SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(java.util.Date())

    private val _selectedMonth = MutableStateFlow(currentMonthYear)
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _studentState = MutableStateFlow(Student())
    val studentState: StateFlow<Student> = _studentState.asStateFlow()

    init {
        fetchUserData()
    }

    fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return
        val filterMonth = _selectedMonth.value

        viewModelScope.launch {
            try {
                // 1. Student Profile se Name aur Roll uthao
                val studentDoc = db.collection("students").document(userId).get().await()
                val name = studentDoc.getString("name") ?: "Student"
                val roll = studentDoc.getString("roll") ?: ""

                val calendar = Calendar.getInstance()
                val parts = filterMonth.split("-")
                if (parts.size == 2) {
                    calendar.set(Calendar.YEAR, parts[1].toInt())
                    calendar.set(Calendar.MONTH, parts[0].toInt() - 1)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                }

                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                var pCount = 0
                var aCount = 0
                val dailyAttendance = FloatArray(daysInMonth) { 0f }

                // 2. Loop through every day
                for (day in 1..daysInMonth) {
                    val dateStr = String.format(Locale.getDefault(), "%02d-%s", day, filterMonth)
                    val studentsRef = db.collection("Attendance").document(dateStr).collection("Students")

                    // PRIMARY: Check by roll (new standard), FALLBACK: Check by UID (legacy support)
                    val rollDoc = if (roll.isNotEmpty()) studentsRef.document(roll).get().await() else null
                    val uidDoc = if (rollDoc?.exists() != true) studentsRef.document(userId).get().await() else null

                    // Prioritize roll-based lookup, fallback to UID for backward compatibility
                    val finalDoc = if (rollDoc?.exists() == true) rollDoc else if (uidDoc?.exists() == true) uidDoc else null

                    if (finalDoc != null) {
                        val status = (finalDoc.getString("status") ?: finalDoc.getString("Status"))?.trim()
                        if (status.equals("Present", ignoreCase = true)) {
                            pCount++
                            dailyAttendance[day - 1] = 1f
                        } else if (status.equals("Absent", ignoreCase = true)) {
                            aCount++
                        }
                    }
                }

                // 3. UI Update
                _studentState.value = _studentState.value.copy(
                    name = name,
                    rollNo = roll,
                    presentCount = pCount,
                    absentCount = aCount,
                    attendancePercentage = if (pCount > 0) (pCount / 26f * 100f).coerceAtMost(100f) else 0f,
                    monthlyAttendance = dailyAttendance.toList()
                )

                Log.d("AttendanceResult", "Month: $filterMonth | Present: $pCount (Check complete)")

            } catch (e: Exception) {
                Log.e("StudentViewModel", "Fetch Error: ${e.message}")
            }
        }
    }


    fun toggleBiometric(isEnabled: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        _studentState.value = _studentState.value.copy(bioMetricEnabled = isEnabled)

        // We use set(..., SetOptions.merge()) instead of update()
        // This ensures that if the document is missing, it creates it instead of crashing!
        val data = mapOf("bioMetricEnabled" to isEnabled)

        db.collection("students").document(userId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener {
                // Try lowercase collection if uppercase fails
                db.collection("students").document(userId)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnFailureListener {
                        // Revert UI if both fail
                        _studentState.value = _studentState.value.copy(bioMetricEnabled = !isEnabled)
                    }
            }
    }


    // Merged logic from your old 'loadSummaryCharts' and 'loadLineChartForAttendance'
    // ... inside StudentViewModel ...

    private fun fetchAttendanceCharts(roll: String, monthYear: String) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val parts = monthYear.split("-")
            if (parts.size == 2) {
                calendar.set(Calendar.MONTH, parts[0].toInt() - 1)
                calendar.set(Calendar.YEAR, parts[1].toInt())
            }

            val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            // We only want to build the chart line data here.
            // We should NOT reset the total counts (presentCount/absentCount)
            // because those are strictly managed by the 'students' profile document now.

            val dailyAttendance = FloatArray(totalDays) { 0f }
            var chartLoadedDays = 0
           // val userId = auth.currentUser?.uid ?: return@launch
            for (day in 1..totalDays) {
                val dateStr = String.format(Locale.getDefault(), "%02d-%s", day, monthYear)

                db.collection("Attendance")
                    .document(dateStr)
                    .collection("Students")
                    .document(roll) // Note: This might need to be UserId, check your schema!
                    // If your Attendance collection uses UserID, passed 'roll' variable might be wrong if it's actually Roll No.
                    // However, to fix the "Zeroing out" issue, we simply stop updating the main counters here.
                    .get()
                    .addOnCompleteListener { task ->
                        chartLoadedDays++

                        // Just update local array for the line chart
                        if (task.isSuccessful && task.result != null && task.result.exists()) {
                            dailyAttendance[day - 1] = 1f
                        }

                        // When all days are checked, ONLY update the chart data
                        if (chartLoadedDays == totalDays) {
                            _studentState.value = _studentState.value.copy(
                                monthlyAttendance = dailyAttendance.toList()
                                // FIX: We REMOVED the lines that were overwriting presentCount/absentCount with 0
                            )
                        }
                    }
            }
        }
    }
    fun markAttendance(lat: Double, lng: Double) {
        val userId = auth.currentUser?.uid ?: return
        val todayDate = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())

        viewModelScope.launch {
            try {
                // 1. Get student's roll number first
                val studentDoc = db.collection("students").document(userId).get().await()
                val roll = studentDoc.getString("roll") ?: return@launch
                val name = studentDoc.getString("name") ?: "Student"

                // 2. Check if already marked for today to prevent duplicates
                val docRef = db.collection("Attendance")
                    .document(todayDate)
                    .collection("Students")
                    .document(roll) // Using roll as document ID
                val docSnapshot = docRef.get().await()

                if (docSnapshot.exists()) {
                    _studentState.value = _studentState.value // Trigger UI update
                    return@launch // Already marked
                }

                // 3. Mark Attendance with roll as document ID
                val attendanceData = hashMapOf(
                    "name" to name,
                    "roll" to roll,
                    "status" to "Present",
                    "timestamp" to java.util.Date(),
                    "location_lat" to lat,
                    "location_lng" to lng,
                    "method" to "location"
                )

                docRef.set(attendanceData).await()

                // 4. Update Profile Counters (Increment Present Count)
                // We use FieldValue.increment so we don't need to read the old value first
                db.collection("students").document(userId)
                    .update("presentCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .await()

                // 5. Refresh Data
                fetchUserData()

            } catch (e: com.google.firebase.FirebaseNetworkException) {
                Log.e("StudentViewModel", "Network error: No internet connection")
            } catch (e: Exception) {
                Log.e("StudentViewModel", "Attendance marking failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    // Add this new variable to hold the selected month filter



    // Function to change month and reload stats
    fun changeMonthFilter(newMonthYear: String) {
        _selectedMonth.value = newMonthYear
        fetchUserData()

        // Reload chart data for this specific month
        // Note: For a real production app, you might want to query Fire-store
        // for that specific month's sub-collection count.
        // For now, we will just update the chart visualization trigger.
        val studentRoll = _studentState.value.rollNo
        if (studentRoll.isNotEmpty()) {
            fetchAttendanceCharts(studentRoll, newMonthYear)
        }
    }

    // Helper to get list of available months (e.g., for Dropdown)
    fun getAvailableMonths(): List<String> {
        val months = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        // Add current month and previous 5 months
        val dateFormat = java.text.SimpleDateFormat("MM-yyyy", Locale.getDefault())
        for (i in 0..9) {
            months.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.MONTH, -1)
        }
        return months
    }

    fun updateStudentProfile(address: String, category: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("students").document(userId)
                    .update(mapOf(
                        "address" to address,
                        "category" to category
                    )).await()
                fetchUserData() // Refresh local data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
