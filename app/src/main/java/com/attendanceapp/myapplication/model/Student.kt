package com.attendanceapp.myapplication.model

import com.google.firebase.firestore.PropertyName

data class Student(
    // Old code fetched from "Users" collection where fields are likely lowercase "name", "roll"
    @get:PropertyName("name") val name: String = "",
    @get:PropertyName("roll") val rollNo: String = "",
    val email: String = "",
    val address: String = "",
    val category: String = "",

    // Dashboard Stats
    val attendancePercentage: Float = 0.0f,
    val totalClasses: Int = 0,
    val classesAttended: Int = 0,

    // Security
    val bioMetricEnabled: Boolean = false,

    // Chart Data (Simple lists for now)
    val monthlyAttendance: List<Float> = emptyList(), // For Line Chart
    val presentCount: Int = 0,
    val absentCount: Int = 0
)

