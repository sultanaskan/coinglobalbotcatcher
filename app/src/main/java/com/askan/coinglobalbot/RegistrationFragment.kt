package com.askan.coinglobalbot

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationFragment : Fragment() {
    private var usernameField: EditText? = null
    private var emailField: EditText? = null
    private var phoneField: EditText? = null
    private var securityCodeField: EditText? = null
    private var uidField: EditText? = null
    private var passwordField: EditText? = null
    private var registerButton: Button? = null
    private var mAuth: FirebaseAuth? = null
    private lateinit var user: UserProfile
    private lateinit var loadingProgressBar: ProgressBar


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_registration, container, false)

        mAuth = FirebaseAuth.getInstance()

        usernameField = view.findViewById(R.id.editTextUsername)
        emailField = view.findViewById(R.id.editTextEmail)
        phoneField = view.findViewById(R.id.editTextPhone)
        securityCodeField = view.findViewById(R.id.editTextSecurityCode)
        uidField = view.findViewById(R.id.editTextUid)
        passwordField = view.findViewById(R.id.editTextPassword)
        registerButton = view.findViewById(R.id.buttonRegister)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)



        registerButton?.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            // Get the input values
            user = UserProfile(
            uid  = null,
            username = usernameField?.text.toString().trim(),
            email = emailField?.text.toString().trim(),
            phone = phoneField?.text.toString().trim(),
            accountUid = uidField?.text.toString().trim(),
            securityCode = securityCodeField?.text.toString().trim(),
            password = passwordField?.text.toString().trim(),
            rule = "user",
            accountAccess = "draft",
            )
            registerUser(user.email!!, user.password!!) { u ->
                if(u != null) {
                    user.uid = u
                    createProfile(user){
                        result ->
                        if(result == true){
                            Toast.makeText(activity, "Profile created Successfully! \nBut your profile in draft. In order to enable our service. you have to contract with us!", Toast.LENGTH_LONG).show()
                            val ft: FragmentTransaction = parentFragmentManager.beginTransaction()
                            ft.replace(R.id.fragment_container, LoginFragment())
                            ft.commit()
                        }
                    }
                    println("Register user uid  : $u")
                    loadingProgressBar.visibility = View.GONE
                }else{
                    loadingProgressBar.visibility = View.GONE
                }
            }

        }
        return view
    }


    private fun createProfile(user:UserProfile,
                              callback: (Boolean?) -> Unit
    ) {
        // Get Firestore instance
        val db = FirebaseFirestore.getInstance()

        // Create a map to store user profile data
        val userProfile = hashMapOf(
            "uid" to user.uid,
            "username" to user.username,
            "email" to user.email,
            "phone" to user.phone,
            "securityCode" to user.securityCode,
            "password" to user.password,  // It's recommended to encrypt passwords before saving!
            "accountUid" to user.accountUid,
            "rule" to user.rule,
            "accountAccess" to user.accountAccess
        )

        // Save the user profile in Firestore under the "users" collection
        db.collection("users").document(user.uid!!)
            .set(userProfile)
            .addOnSuccessListener {
                // Success: The profile has been created successfully
                callback(true)
                Toast.makeText(activity, "Profile created successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                callback(false)
                // Failure: Something went wrong
                Toast.makeText(activity, "Error creating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun registerUser(email:String, password:String,callback: (String?) -> Unit) {
        // Check if any field is empty
        if (user.username!!.isEmpty() || email.isEmpty() || user.phone!!.isEmpty() ||
            user.securityCode!!.isEmpty() || user.accountUid!!.isEmpty() || password.isEmpty()) {
            Toast.makeText(activity, "Each field must be filled!", Toast.LENGTH_SHORT).show()
            callback(null)
            return
        }
        // Proceed with Firebase registration
        mAuth?.createUserWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task: Task<AuthResult?> ->
                if (task.isSuccessful) {
                    val u =  mAuth?.currentUser?.uid
                    println("inner uid: $u")
                    callback(u)
                } else {
                    // Show error message
                    Toast.makeText(activity, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            }
    }
}
