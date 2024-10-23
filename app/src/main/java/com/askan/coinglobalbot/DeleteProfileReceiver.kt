package com.askan.coinglobalbot
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast

class DeleteProfileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPre: SharedPreferences = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val editor = sharedPre.edit()
        editor.remove("user_profile")
        editor.apply()

        Toast.makeText(context, "User profile deleted after 12 hours", Toast.LENGTH_SHORT).show()
    }
}
