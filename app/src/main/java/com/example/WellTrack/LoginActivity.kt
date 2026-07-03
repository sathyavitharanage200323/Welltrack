package com.example.WellTrack

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson

class LoginActivity : AppCompatActivity() {

    private lateinit var formTitle: TextView
    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var authButton: Button
    private lateinit var toggleAuthText: TextView
    
    private var isLoginMode = true
    private val gson = Gson()
    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Initialize views
        formTitle = findViewById(R.id.form_title)
        nameInputLayout = findViewById(R.id.name_input_layout)
        nameInput = findViewById(R.id.name_input)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        authButton = findViewById(R.id.auth_button)
        toggleAuthText = findViewById(R.id.toggle_auth_text)

        // Set click listeners
        authButton.setOnClickListener {
            if (isLoginMode) {
                performLogin()
            } else {
                performSignup()
            }
        }

        toggleAuthText.setOnClickListener {
            toggleAuthMode()
        }
    }

    private fun toggleAuthMode() {
        isLoginMode = !isLoginMode
        
        if (isLoginMode) {
            // Switch to login mode
            formTitle.text = "Welcome Back"
            nameInputLayout.visibility = View.GONE
            authButton.text = "Login"
            toggleAuthText.text = "Don't have an account? Sign Up"
        } else {
            // Switch to signup mode
            formTitle.text = "Create Account"
            nameInputLayout.visibility = View.VISIBLE
            authButton.text = "Sign Up"
            toggleAuthText.text = "Already have an account? Login"
        }
    }

    private fun performLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Get stored user
        val userJson = sharedPreferences.getString("user_$email", null)
        if (userJson == null) {
            Toast.makeText(this, "User not found. Please sign up", Toast.LENGTH_SHORT).show()
            return
        }

        val user = gson.fromJson(userJson, User::class.java)
        if (user.password != password) {
            Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
            return
        }

        // Login successful
        sharedPreferences.edit().apply {
            putBoolean("is_logged_in", true)
            putString("current_user_email", email)
            apply()
        }

        Toast.makeText(this, "Welcome back, ${user.name}!", Toast.LENGTH_SHORT).show()
        navigateToMain()
    }

    private fun performSignup() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user already exists
        val existingUser = sharedPreferences.getString("user_$email", null)
        if (existingUser != null) {
            Toast.makeText(this, "User already exists. Please login", Toast.LENGTH_SHORT).show()
            return
        }

        // Create new user
        val user = User(email, name, password)
        val userJson = gson.toJson(user)
        
        sharedPreferences.edit().apply {
            putString("user_$email", userJson)
            putBoolean("is_logged_in", true)
            putString("current_user_email", email)
            apply()
        }

        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
        navigateToMain()
    }

    private fun navigateToMain() {
        val loadingView = layoutInflater.inflate(R.layout.view_loading_overlay, null)
        loadingView.findViewById<TextView>(R.id.loading_text).text = "Preparing your dashboard..."
        val loadingDialog = AlertDialog.Builder(this)
            .setView(loadingView)
            .setCancelable(false)
            .create()

        loadingDialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            loadingDialog.dismiss()
            finish()
        }, 800)
    }
}
