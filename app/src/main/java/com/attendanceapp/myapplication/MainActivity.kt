package com.attendanceapp.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var logoutButton: Button
    private val auth = FirebaseAuth.getInstance()

    @SuppressLint("MissingInflatedId", "UnsafeIntentLaunch")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        FirebaseAuth.getInstance()
        FirebaseFirestore.getInstance()
        Log.d("FIREBASE", "Firebase Initialized Successfully")

        // Button to open QR Scanner
        val button = findViewById<Button>(R.id.btnOpenScanner)
        button.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            startActivity(intent)
        }
        // Button to open Biometric Attendance
        val biometricButton = findViewById<Button>(R.id.biometricButton)

        biometricButton.setOnClickListener {
            val intent = Intent(this, FingerprintActivity::class.java)
            startActivity(intent)
        }
        // Button to open Admin Panel`
        // Corrected Admin Button in MainActivity.kt
        val adminButton = findViewById<Button>(R.id.btnAdminPanel)
        adminButton.setOnClickListener {
            val adminIntent = Intent(this, DashboardActivity::class.java)
            // This ensures the activity opens correctly without crashing the task stack
            adminIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(adminIntent)
        }

        // Button to open Enroll Students
        val enrollButton = findViewById<Button>(R.id.EnrollButton)
         enrollButton.setOnClickListener {
            val intent = Intent(this, FingerprintEnrollActivity::class.java)
            startActivity(intent)
        }
        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

    }
}

