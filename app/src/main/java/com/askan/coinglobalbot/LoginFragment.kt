package com.askan.coinglobalbot

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.util.Calendar

class LoginFragment : Fragment() {
    private var emailField: EditText? = null  // Changed to EditText
    private var passwordField: EditText? = null
    private var loginButton: Button? = null
    private var registerButton: TextView? = null // Changed to TextView
    private var mAuth: FirebaseAuth? = null
    private lateinit var email:String
    private lateinit var  password:String
    private lateinit var user: UserProfile
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_login, container, false) // Correct layout name

        mAuth = FirebaseAuth.getInstance()

        // Use correct IDs
        emailField = view.findViewById(R.id.emailField)
        passwordField = view.findViewById(R.id.passwordField)
        loginButton = view.findViewById(R.id.loginButton)
        registerButton = view.findViewById(R.id.registerButton)
        val sharedPref:SharedPreferences = activity?.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)!!
        val loggedUser = sharedPref.getString("user_profile", null)
        if(loggedUser != null) {
            println("Loggeduser: $loggedUser")
            navigateToFragment(MainFragment())
        }


        // Set click listeners
        loginButton?.setOnClickListener { loginUser() }
        registerButton?.setOnClickListener {
            // Navigate to Registration Fragment
            val ft: FragmentTransaction = parentFragmentManager.beginTransaction()
            ft.replace(R.id.fragment_container, RegistrationFragment())
            ft.commit()
        }

        return view
    }

    private fun loginUser() {
         email = emailField?.text.toString() // Use safe calls
         password = passwordField?.text.toString()
        if(email.isEmpty() && password.isEmpty()){
            Toast.makeText(activity,"Fill all field and try again!", Toast.LENGTH_SHORT).show()
            return
        }

        mAuth?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task: Task<AuthResult?> ->
                if (task.isSuccessful) {
                    val user = mAuth?.currentUser
                    // Check if user is admin
                    fetchUserProfile(user!!.uid)
                } else {
                    // Show error message
                    Toast.makeText(activity, "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun fetchUserProfile(uid: String) {
        val db = FirebaseFirestore.getInstance()

        // Reference to the "users" collection in Firestore
        val userDocRef = db.collection("users").document(uid)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    user  = UserProfile(
                        uid = document.getString("uid"),
                        rule = document.getString("rule"),
                        securityCode = document.getString("securityCode"),
                        accountAccess = document.getString("accountAccess"),
                        email = document.getString("email"),
                        accountUid = document.getString("accountUid"),
                        phone = document.getString("phone"),
                        password = document.getString("password"),
                        username = document.getString("username")
                    )

                    println("User is $user")
                    // User profile found, retrieve profile details

                    storeUserProfileInSharedPreferences(user)

                    // Now, decide based on the accountType if the user is an admin
                    if (user.rule == "admin") {
                        // Navigate to ManagementFragment if the user is an admin
                        navigateToFragment(ManagementFragment())
                    } else {
                        // Otherwise, navigate to the MainFragment for regular users
                        navigateToFragment(MainFragment())
                    }
                } else {
                    // Document doesn't exist, handle error
                    Toast.makeText(activity, "Profile not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                // Handle failure when fetching profile
                Toast.makeText(activity, "Error retrieving profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun storeUserProfileInSharedPreferences(userProfile: UserProfile) {
        val sharedPre: SharedPreferences = activity?.getSharedPreferences("UserProfile", Context.MODE_PRIVATE) ?: return
        val editor = sharedPre.edit()
        // Convert the UserProfile object to JSON string
        val gson = Gson()
        val json = gson.toJson(userProfile)

        // Store JSON string in SharedPreferences
        editor.putString("user_profile", json)
        editor.apply()  // Commit the changes
        scheduleProfileDeletion()
        println("User from sharedPreferences: ${sharedPre.getString("user_profile", null)}")
    }

    private fun navigateToFragment(fragment: Fragment) {
        val ft: FragmentTransaction = parentFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, fragment)
        ft.commit()
    }


    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleProfileDeletion() {
        val alarmManager = activity?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(activity, DeleteProfileReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Set the alarm to go off after 12 hours
        val triggerTime = Calendar.getInstance().timeInMillis + 6 * 60 * 60 * 1000

        // Schedule the alarm
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}
