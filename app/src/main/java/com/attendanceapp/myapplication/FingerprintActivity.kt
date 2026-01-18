package com.attendanceapp.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Document
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

class FingerprintActivity : AppCompatActivity() {

    private lateinit var rollEditText: EditText
    private lateinit var scanButton: Button
    private lateinit var statusTextView: TextView
    private val db = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint)

        rollEditText = findViewById(R.id.rollEditText)
        scanButton = findViewById(R.id.scanButton)
        statusTextView = findViewById(R.id.statusTextView)

        scanButton.setOnClickListener {
            val roll = rollEditText.text.toString().trim()
            if (roll.isEmpty()) {
                Toast.makeText(this, "Enter roll number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            captureFingerprint(roll)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun captureFingerprint(roll: String) {
        val pidOptionsXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PidOptions ver="1.0">
                <Opts fCount="1" fType="0" format="0" pidVer="2.0"
                      timeout="10000" posh="UNKNOWN" env="P" wadh="" />
            </PidOptions>
        """.trimIndent()

        val requestBody = pidOptionsXml.toRequestBody("text/xml".toMediaType())
        val request = Request.Builder()
            .url("http://127.0.0.1:11100/rd/capture")
            .post(requestBody)
            .build()

        statusTextView.text = "📡 Capturing fingerprint..."

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    statusTextView.text = "❌ RD Service Error: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val pidXml = response.body?.string()
                if (pidXml?.contains("errCode=\"0\"") == true) {
                    val scannedTemplate = extractPidData(pidXml)
                    if (scannedTemplate != null) {
                        verifyAndSaveAttendance(roll, scannedTemplate)
                    } else {
                        runOnUiThread {
                            statusTextView.text = "❌ Failed to extract fingerprint"
                        }
                    }
                } else {
                    runOnUiThread {
                        statusTextView.text = "❌ Fingerprint capture failed"
                    }
                }
            }
        })
    }

    // Extract Base64 PidData from RD XML
    private fun extractPidData(xml: String): String? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val input = xml.byteInputStream()
            val doc: Document = builder.parse(input)
            doc.documentElement.normalize()
            doc.getElementsByTagName("Data").item(0).textContent
        } catch (e: Exception) {
            null
        }
    }

    // 🔍 Match scanned template with Fire-store and mark attendance
    @SuppressLint("SetTextI18n")
    private fun verifyAndSaveAttendance(roll: String, scannedTemplate: String) {
        val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        db.collection("EnrolledStudents")
            .document(roll)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val savedTemplate = document.getString("fingerprintTemplate") ?: ""

                    if (scannedTemplate == savedTemplate) {
                        val name = document.getString("name") ?: "Unknown"
                        val id = document.getString("id") ?: "N/A"
                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                        val student = hashMapOf(
                            "name" to name,
                            "roll" to roll,
                            "id" to id,
                            "status" to "Present",
                            "timestamp" to Date(),
                            "method" to "fingerprint"
                        )

                        db.collection("Attendance")
                            .document(date)
                            .collection("Students")
                            .document(roll) // Using roll as document ID for consistency
                            .set(student)
                            .addOnSuccessListener {
                                // Increment student stats in students collection
                                incrementFingerprintStats(roll)
                                runOnUiThread {
                                    statusTextView.text = "✅ Attendance marked for Roll: $roll"
                                }
                            }
                            .addOnFailureListener {
                                runOnUiThread {
                                    statusTextView.text = "❌ Failed to save attendance"
                                }
                            }
                    } else {
                        runOnUiThread {
                            statusTextView.text = "❌ Fingerprint doesn't match!"
                        }
                    }
                } else {
                    runOnUiThread {
                        statusTextView.text = "❌ No enrolled data for roll: $roll"
                    }
                }
            }
            .addOnFailureListener {
                runOnUiThread {
                    statusTextView.text = "❌ Fire_store error while matching"
                }
            }
    }

    // Helper function to increment presentCount for fingerprint attendance
    private fun incrementFingerprintStats(roll: String) {
        // Find student by roll number and increment their presentCount
        db.collection("students")
            .whereEqualTo("roll", roll)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val studentDoc = documents.documents[0]
                    val userId = studentDoc.id
                    
                    db.collection("students").document(userId)
                        .update("presentCount", com.google.firebase.firestore.FieldValue.increment(1))
                }
            }
    }
}
