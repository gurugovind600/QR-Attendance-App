package com.attendanceapp.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FingerprintEnrollActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var rollEditText: EditText
    private lateinit var idEditText: EditText
    private lateinit var captureButton: Button
    private lateinit var saveButton: Button
    private lateinit var statusTextView: TextView

    private var capturedTemplate: String? = null

    private val db = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint_enroll)

        nameEditText = findViewById(R.id.nameEditText)
        rollEditText = findViewById(R.id.rollEditText)
        idEditText = findViewById(R.id.idEditText)
        captureButton = findViewById(R.id.captureButton)
        saveButton = findViewById(R.id.enrollButton)
        statusTextView = findViewById(R.id.statusTextView)

        captureButton.setOnClickListener {
            captureFingerprint()
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val roll = rollEditText.text.toString().trim()
            val id = idEditText.text.toString().trim()

            if (name.isEmpty() || roll.isEmpty() || id.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (capturedTemplate == null) {
                Toast.makeText(this, "Please capture fingerprint first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

           // val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

            val student = hashMapOf(
                "name" to name,
                "roll" to roll,
                "id" to id,
                "timestamp" to timestamp,
                "fingerprintTemplate" to capturedTemplate!!
            )

            db.collection("EnrolledStudents")
                .document(roll)
                .set(student)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Fingerprint enrolled successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "❌ Failed to save fingerprint", Toast.LENGTH_SHORT).show()
                }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun captureFingerprint() {
        val pidOptionsXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PidOptions ver="1.0">
                <Opts fCount="1" fType="0" format="0" pidVer="2.0"
                      timeout="10000" posh="UNKNOWN" env="P" wadh="" />
            </PidOptions>
        """.trimIndent()

        val requestBody = pidOptionsXml.toRequestBody("text/xml".toMediaType())
        val request = Request.Builder()
            .url("http://127.0.0.1:11100/rd/capture") // RD Service running locally
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
                    val template = extractBase64FromPidXml(pidXml)
                    if (template != null) {
                        capturedTemplate = template
                        runOnUiThread {
                            statusTextView.text = "✅ Fingerprint captured"
                        }
                    } else {
                        runOnUiThread {
                            statusTextView.text = "❌ Could not extract template"
                        }
                    }
                } else {
                    runOnUiThread {
                        statusTextView.text = "❌ Fingerprint Failed"
                    }
                }
            }
        })
    }

    private fun extractBase64FromPidXml(xml: String): String? {
        val start = xml.indexOf("<Data>")
        val end = xml.indexOf("</Data>")
        return if (start != -1 && end != -1) {
            xml.substring(start + 6, end)
        } else null
    }
}

