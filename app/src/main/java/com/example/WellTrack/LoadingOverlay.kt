package com.example.WellTrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

class LoadingOverlay(private val parentView: ViewGroup) {

    private var overlay: FrameLayout? = null

    fun show(message: String = "Loading...") {
        if (overlay == null) {
            overlay = LayoutInflater.from(parentView.context)
                .inflate(R.layout.view_loading_overlay, parentView, false) as FrameLayout
            parentView.addView(overlay)
        }
        overlay?.findViewById<TextView>(R.id.loading_text)?.text = message
        overlay?.visibility = View.VISIBLE
        overlay?.bringToFront()
    }

    fun hide() {
        overlay?.visibility = View.GONE
    }

    fun isShowing(): Boolean = overlay?.visibility == View.VISIBLE
}
