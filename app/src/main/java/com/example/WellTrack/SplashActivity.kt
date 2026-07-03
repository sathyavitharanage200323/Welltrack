package com.example.WellTrack

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SplashActivity : AppCompatActivity() {

    private val splashDelay = 2500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        setContentView(R.layout.activity_splash)

        startFadeInAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, splashDelay)
    }

    private fun startFadeInAnimation() {
        val logo = findViewById<View>(R.id.logo_image)
        val appName = findViewById<View>(R.id.app_name)
        val tagline = findViewById<View>(R.id.tagline)
        val spinner = findViewById<View>(R.id.loading_spinner)

        ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f).apply {
            duration = 600L
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 200L
            start()
        }

        ObjectAnimator.ofFloat(appName, View.ALPHA, 0f, 1f).apply {
            duration = 500L
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 500L
            start()
        }

        ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f).apply {
            duration = 500L
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 700L
            start()
        }

        ObjectAnimator.ofFloat(spinner, View.ALPHA, 0f, 1f).apply {
            duration = 400L
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 1000L
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    spinner.visibility = View.VISIBLE
                }
            })
            start()
        }
    }

    private fun navigateToNextScreen() {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val hasSeenOnboarding = sharedPreferences.getBoolean("has_seen_onboarding", false)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

        val intent = when {
            !hasSeenOnboarding -> Intent(this, OnboardingActivity::class.java)
            !isLoggedIn -> Intent(this, LoginActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
