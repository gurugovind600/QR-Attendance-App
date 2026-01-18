package com.attendanceapp.myapplication

import android.Manifest
import android.annotation.SuppressLint
//import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.ui.geometry.isEmpty
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
//import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
//import java.text.SimpleDateFormat
//import java.util.Date

class QRScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var db: FirebaseFirestore

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        db = FirebaseFirestore.getInstance()
        barcodeView = findViewById(R.id.barcode_scanner)
        barcodeView.statusView.text = "Scan QR"

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            startScanning()
        }

    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume() // RESUME CAMERA
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause() // PAUSE CAMERA
    }

    private fun startScanning() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    barcodeView.pause()
                    handleScanResult(it.text)
                }
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
        })
    }

    @SuppressLint("SimpleDateFormat")
    // Replace your existing handleScanResult function with this one
    private fun handleScanResult(scannedRollNo: String) {
        Toast.makeText(this, "Processing Roll: $scannedRollNo", Toast.LENGTH_SHORT).show()

        // 1. Search for the student by Roll Number in the NEW 'students' collection
        db.collection("students")
            .whereEqualTo("roll", scannedRollNo.trim())
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(
                        this,
                        "❌ Student not found with Roll: $scannedRollNo",
                        Toast.LENGTH_LONG
                    ).show()
                    barcodeView.resume()
                    return@addOnSuccessListener
                }

                // Found the student!
                val studentDoc = documents.documents[0]
                val userId = studentDoc.id
                val studentName = studentDoc.getString("name") ?: "Unknown"

                // 2. Mark Attendance
                markPresentInDatabase(userId, studentName, scannedRollNo)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking database", Toast.LENGTH_SHORT).show()
                barcodeView.resume()
            }
    }

    private fun markPresentInDatabase(userId: String, name: String, roll: String) {
        val todayDate = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())

        // A. Log it in the Daily Attendance Collection using ROLL as document ID
        val attendanceData = hashMapOf(
            "name" to name,
            "roll" to roll,
            "status" to "Present",
            "timestamp" to java.util.Date(),
            "method" to "qr"
        )

        db.collection("Attendance").document(todayDate).collection("Students")
            .document(roll) // Using roll as document ID for clean, readable records
            .set(attendanceData)
            .addOnSuccessListener {

                // B. CRITICAL: Update the Student's Graph Data (presentCount)
                incrementStudentStats(userId, roll)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save attendance", Toast.LENGTH_SHORT).show()
                barcodeView.resume()
            }
    }

    private fun incrementStudentStats(userId: String, roll: String) {
        val studentRef = db.collection("students").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(studentRef)
            // Read current count, default to 0 if null
            val currentPresent = snapshot.getLong("presentCount") ?: 0

            // Increment by 1
            transaction.update(studentRef, "presentCount", currentPresent + 1)
        }.addOnSuccessListener {
            Toast.makeText(this, "✅ Attendance Marked for Roll: $roll", Toast.LENGTH_LONG).show()

            // Resume camera for the next student
            // Adding a small delay prevents accidental double scans
            barcodeView.postDelayed({ barcodeView.resume() }, 2000)
        }
    }
}