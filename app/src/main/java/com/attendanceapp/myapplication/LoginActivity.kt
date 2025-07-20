package com.attendanceapp.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var forgotPasswordText: TextView
    private lateinit var rememberMeCheckBox: CheckBox

    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: SharedPreferences

    private val adminEmail = "admin@cvru.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        // Auto-login if remembered
        if (prefs.getBoolean("rememberMe", false) && auth.currentUser != null) {
            routeToDashboard(auth.currentUser?.email)
            return
        }

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email & password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        prefs.edit {
                            putBoolean("rememberMe", rememberMeCheckBox.isChecked)
                        }
                        routeToDashboard(email)
                    } else {
                        Toast.makeText(this, "Login failed: ${it.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        forgotPasswordText.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reset link sent to your email!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Enter email first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun routeToDashboard(email: String?) {
        if (email == adminEmail) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, StudentActivity::class.java))
        }
        finish()
    }
}


