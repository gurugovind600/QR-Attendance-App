package com.attendanceapp.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.attendanceapp.myapplication.utils.BiometricHelper
import com.attendanceapp.myapplication.utils.LocationHelper
import com.attendanceapp.myapplication.viewmodel.StudentViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.edit
import com.attendanceapp.myapplication.ui.ui.StudentDashboardScreen

class StudentActivity : AppCompatActivity() {

private val viewModel: StudentViewModel by viewModels()

    // Permission Launcher for Location
    @RequiresApi(Build.VERSION_CODES.N)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                checkLocationAndMark()
            } else {
                Toast.makeText(
                    this,
                    "Location permission needed to mark attendance",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val studentData by viewModel.studentState.collectAsState()
            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "No Email"

            StudentDashboardScreen(
                student = studentData,
                email = userEmail,
                viewModel = viewModel,

                // 1. LOGOUT LOGIC
                onLogoutClick = {
                    // Sign out of Firebase
                    FirebaseAuth.getInstance().signOut()

                    // Clear SharedPreferences (Optional)
                    val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
                    prefs.edit { clear() }

                     // Navigate back to Login
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                },

                // 2. BIOMETRIC LOGIC
                onBiometricToggle = { shouldEnable ->
                    if (shouldEnable) {
                        // User wants to turn it ON. Verify identity first!
                        if (BiometricHelper.isBiometricAvailable(this@StudentActivity)) {
                            BiometricHelper.showPrompt(
                                activity = this@StudentActivity,
                                onSuccess = {
                                    // Fingerprint matched! Now save it.
                                    viewModel.toggleBiometric(true)
                                    Toast.makeText(
                                        this@StudentActivity,
                                        "Biometrics Enabled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = {
                                    // Fingerprint failed.
                                    Toast.makeText(
                                        this@StudentActivity,
                                        "Authentication Failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        } else {
                            Toast.makeText(
                                this@StudentActivity,
                                "Biometrics not supported on device",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        // User wants to turn it OFF. No need to scan, just disable.
                        viewModel.toggleBiometric(false)
                    }
                },

                // 3. ATTENDANCE (LOCATION) LOGIC
                onMarkAttendanceClick = {
                    if (ContextCompat.checkSelfPermission(
                            this, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        checkLocationAndMark()
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                onRefreshTriggered = {
                    viewModel.fetchUserData()
                },
            )
        }
    }

    // This function is now correctly placed OUTSIDE of onCreate
    @RequiresApi(Build.VERSION_CODES.N)
    private fun checkLocationAndMark() {
        Toast.makeText(this, "Verifying Location...", Toast.LENGTH_SHORT).show()

        LocationHelper.getCurrentLocation(this)
            .addOnSuccessListener { location ->
                if (location != null) {
                    if (LocationHelper.isWithinRange(location)) {
                        // SUCCESS: Inside Campus
                        // CALL VIEWMODEL HERE
                        viewModel.markAttendance(location.latitude, location.longitude)
                        Toast.makeText(this, "✅ Attendance Marked Successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "❌ Too far from campus!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Error: GPS signal not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Location Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
