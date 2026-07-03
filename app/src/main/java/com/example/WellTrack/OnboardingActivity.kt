package com.example.WellTrack

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var skipButton: Button
    private lateinit var nextButton: Button
    
    private val layouts = listOf(
        R.layout.onboarding_screen_1,
        R.layout.onboarding_screen_2,
        R.layout.onboarding_screen_3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.onboarding_viewpager)
        dotsLayout = findViewById(R.id.dots_layout)
        skipButton = findViewById(R.id.skip_button)
        nextButton = findViewById(R.id.next_button)

        // Setup ViewPager
        val adapter = OnboardingAdapter(layouts)
        viewPager.adapter = adapter

        // Setup dots
        setupDots(0)

        // ViewPager page change listener
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setupDots(position)
                
                // Change button text on last page
                if (position == layouts.size - 1) {
                    nextButton.text = "Get Started"
                    skipButton.visibility = View.GONE
                } else {
                    nextButton.text = "Next"
                    skipButton.visibility = View.VISIBLE
                }
            }
        })

        // Button listeners
        skipButton.setOnClickListener {
            finishOnboarding()
        }

        nextButton.setOnClickListener {
            val current = viewPager.currentItem
            if (current < layouts.size - 1) {
                viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun setupDots(currentPage: Int) {
        dotsLayout.removeAllViews()
        
        val dots = Array(layouts.size) { ImageView(this) }
        
        dots.forEachIndexed { index, imageView ->
            imageView.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (index == currentPage) R.drawable.dot_active else R.drawable.dot_inactive
                )
            )
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            
            dotsLayout.addView(imageView, params)
        }
    }

    private fun finishOnboarding() {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("has_seen_onboarding", true).apply()

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private inner class OnboardingAdapter(private val layouts: List<Int>) :
        RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            val view = layoutInflater.inflate(layouts[viewType], parent, false)
            return OnboardingViewHolder(view)
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            // Layout is already inflated with content
        }

        override fun getItemCount(): Int = layouts.size

        override fun getItemViewType(position: Int): Int = position

        inner class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}
