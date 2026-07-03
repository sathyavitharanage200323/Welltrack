package com.example.WellTrack

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val gson = Gson()
    
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var memberSinceText: TextView
    private lateinit var themeSwitch: SwitchMaterial
    private lateinit var logoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // Initialize views
        userNameText = view.findViewById(R.id.user_name_text)
        userEmailText = view.findViewById(R.id.user_email_text)
        memberSinceText = view.findViewById(R.id.member_since_text)
        themeSwitch = view.findViewById(R.id.theme_switch)
        logoutButton = view.findViewById(R.id.logout_button)

        loadUserData()
        setupThemeSwitch()

        logoutButton.setOnClickListener {
            performLogout()
        }

        return view
    }

    private fun loadUserData() {
        val currentEmail = sharedPreferences.getString("current_user_email", null)
        if (currentEmail == null) {
            // User not logged in, redirect to login
            navigateToLogin()
            return
        }

        val userJson = sharedPreferences.getString("user_$currentEmail", null)
        if (userJson == null) {
            navigateToLogin()
            return
        }

        val user = gson.fromJson(userJson, User::class.java)
        userNameText.text = user.name
        userEmailText.text = user.email
        
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        memberSinceText.text = dateFormat.format(Date(user.createdAt))
    }

    private fun setupThemeSwitch() {
        // Load current theme preference
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        themeSwitch.isChecked = isDarkMode

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save preference
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()
            
            // Apply theme
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun performLogout() {
        sharedPreferences.edit().apply {
            putBoolean("is_logged_in", false)
            remove("current_user_email")
            apply()
        }

        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
