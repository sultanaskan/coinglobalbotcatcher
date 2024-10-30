package com.askan.coinglobalbot


import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.gson.Gson

class MainFragment : Fragment() {

    private lateinit var miniOrderPrice: EditText
    private lateinit var startBotButton: Button
    private lateinit var btnmanageusers: Button
    private lateinit var imageView: ImageView
    private lateinit var stopButton: Button
    private lateinit var btnlogout: Button
    private lateinit var auth: FirebaseAuth
    private var isLogin = true
    private lateinit var user:UserProfile

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        // Initialize Firebase
        FirebaseApp.initializeApp(requireContext())
        auth = FirebaseAuth.getInstance()

        // Initialize UI components
        miniOrderPrice = view.findViewById(R.id.targetText)
        startBotButton = view.findViewById(R.id.startBotButton)
        btnmanageusers = view.findViewById(R.id.btnManageUser)
        imageView = view.findViewById(R.id.imageView)
        btnlogout = view.findViewById(R.id.btnLogout)
        stopButton = view.findViewById(R.id.StopBotButton)
        user = getUserProfileFromSharedPreferences()!!
        if(user.rule == "admin"){
           btnmanageusers.visibility = View.VISIBLE
           btnmanageusers.setOnClickListener{
               val ft: FragmentTransaction = parentFragmentManager.beginTransaction()
               ft.replace(R.id.fragment_container, ManagementFragment())
               ft.commit()
           }
        }

        btnlogout.setOnClickListener{
            if(removeUserFromSharedPref()){
                Toast.makeText(activity, "Logout success",Toast.LENGTH_SHORT).show()
                val ft:FragmentTransaction = parentFragmentManager.beginTransaction()
                ft.replace(R.id.fragment_container, LoginFragment()).commit()
            }else{
                Toast.makeText(activity, "Logout success",Toast.LENGTH_SHORT).show()
            }
        }

        // Start bot button action
        startBotButton.setOnClickListener {
            if(user.accountAccess.equals("enable")) {
                if (isAccessibilityServiceEnabled()) {
                    startBotService()
                } else {
                    openAccessibilitySettings()
                }
            }else{
                Toast.makeText(activity,"YOU HAVE NO PERMISSION TO USE OUR BOT! PLEASE CONTRACT WITH US!",Toast.LENGTH_SHORT).show()
            }
        }

        // Stop bot button action
        stopButton.setOnClickListener {
            stopBotService()
        }

        return view
    }

    @SuppressLint("SuspiciousIndentation")
    private fun stopBotService() {
        val intent = Intent(requireContext(), BotService::class.java)
            intent.putExtra(BotService.constants.SECURITY_CODE, "stop")
            intent.putExtra(BotService.constants.MINI_ORDER_PRICE, "999999")
            intent.putExtra(BotService.constants.ACCOUNT_UID, "stop")
            requireActivity().stopService(intent)
            Toast.makeText(requireContext(), "Bot stop successfully", Toast.LENGTH_SHORT).show()
            ActivityCompat.finishAffinity(requireActivity())
    }

    private fun startBotService() {
        val intent = Intent(requireContext(), BotService::class.java)
        val sec = user.securityCode!!
        val acUid = user.accountUid
        val mini = miniOrderPrice.text.toString()
        if (sec.isNotEmpty() && mini.isNotEmpty() && acUid!!.isNotEmpty()) {
            Toast.makeText(requireContext(), "Starting bot with \nSecurity Code: $sec \nMinimum Price: $mini and \n AccountUID: $acUid", Toast.LENGTH_SHORT).show()
            intent.putExtra(BotService.constants.SECURITY_CODE, user.securityCode)
            intent.putExtra(BotService.constants.MINI_ORDER_PRICE, mini)
            intent.putExtra(BotService.constants.ACCOUNT_UID, acUid)
            requireActivity().startService(intent)
        } else {
            Toast.makeText(requireContext(), "Please fill the security code and minimum price field", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = requireContext().getSystemService(AccessibilityManager::class.java)
        val services = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in services) {
            if (info.id == "${requireActivity().packageName}/.BotService") {
                return true
            }
        }
        return false
    }

    private fun openAccessibilitySettings() {
        AlertDialog.Builder(requireContext())
            .setTitle("Accessibility Service Not Enabled")
            .setMessage("This app requires accessibility services to function correctly. Please enable the service in the settings.")
            .setPositiveButton("Open Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun removeUserFromSharedPref():Boolean{
        val sharedPref: SharedPreferences = activity?.getSharedPreferences("UserProfile", Context.MODE_PRIVATE) ?: return false
        sharedPref.edit().putString("user_profile", null).apply()
        return  true
    }

    private fun getUserProfileFromSharedPreferences(): UserProfile? {
        val sharedPref: SharedPreferences = activity?.getSharedPreferences("UserProfile", Context.MODE_PRIVATE) ?: return null

        // Retrieve the JSON string
        val json = sharedPref.getString("user_profile", null)

        // Convert the JSON string back to a UserProfile object
        return if (json != null) {
            val gson = Gson()
            gson.fromJson(json, UserProfile::class.java)
        } else {
            null
        }
    }

}

