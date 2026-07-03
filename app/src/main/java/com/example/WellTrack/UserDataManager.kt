package com.example.WellTrack

import android.content.Context
import android.content.SharedPreferences
/*login shred prefernce
 */
object UserDataManager {
    
    private const val USER_PREFS = "user_prefs"
    
    fun getCurrentUserEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
        return prefs.getString("current_user_email", null)
    }
    
    fun getUserSpecificPreferences(context: Context, prefsName: String): SharedPreferences {
        val userEmail = getCurrentUserEmail(context) ?: "default"
        val userSpecificName = "${prefsName}_$userEmail"
        return context.getSharedPreferences(userSpecificName, Context.MODE_PRIVATE)
    }
    
    fun clearUserData(context: Context) {
        val userEmail = getCurrentUserEmail(context) ?: return
        
        // Clear all user-specific data
        val prefsToClean = listOf("habits", "mood_journal", "hydration", "WellTrack_home")
        
        prefsToClean.forEach { prefsName ->
            val userSpecificName = "${prefsName}_$userEmail"
            context.getSharedPreferences(userSpecificName, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }
}
