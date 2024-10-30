package com.askan.coinglobalbot

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.firestore.FirebaseFirestore
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import java.sql.Time
import java.util.Calendar
import java.util.Date

class ManagementFragment : Fragment() {

    private lateinit var userListView: ListView
    private lateinit var userAdapter: UserAdapter
    private var userList: MutableList<UserProfile> = mutableListOf()
    private lateinit var backToBotController: Button
    private lateinit var searchUserEditText: EditText
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var totalUserView: TextView
    private lateinit var enableUserView: TextView
    private lateinit var disabledDisabledUserView: TextView
    private lateinit var draftUserView: TextView
    private lateinit var accessDaysEditText: EditText
    private lateinit var todayDateTextView: TextView
    private lateinit var expiredUserTextview : TextView
    private var accessOutTime: Long = 0
    private var accessInTime: Long = 0
    private  var totalUser: Int = 0
    private var enableUser: Int = 0
    private  var disableUser: Int = 0
    private  var draftUser: Int = 0
    private var expiredUser: Int = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_management, container, false)

        // Initialize views
        backToBotController = view.findViewById(R.id.btnBackToBotController)
        userListView = view.findViewById(R.id.userListView)
        searchUserEditText = view.findViewById(R.id.searchUserEditText)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        totalUserView = view.findViewById(R.id.totalUsersTextView)
        enableUserView = view.findViewById(R.id.enabledUsersTextView)
        disabledDisabledUserView = view.findViewById(R.id.disabledUsersTextView)
        draftUserView = view.findViewById(R.id.draftUsersTextView)
        expiredUserTextview = view.findViewById(R.id.expiredUsersTextView)
        accessInTime = System.currentTimeMillis()


        // Fetch all users from Firestore with loading indicator
        getAllUsers(){
            result ->
            if(result){
                totalUserView.text = "Total Users: $totalUser"
                enableUserView.text = "Enabled: $enableUser"
                disabledDisabledUserView.text = "Disabled: $disableUser"
                draftUserView.text = "Draft: $draftUser"
                expiredUserTextview.text = "Expired: $expiredUser"
            }
        }

        // Set up search functionality to filter the list
        searchUserEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s!=null) {
                    userAdapter.filter(s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        // Set up click user permission changer
        userListView.setOnItemClickListener { parent, view, position, id ->
            val clickedItem = userAdapter.getItem(position) as UserProfile
            userPermissionDialog(clickedItem)
        }
        userListView.setOnItemLongClickListener { parent, view, position, id ->
            val user = userAdapter.getItem(position) as UserProfile
            val uid = user.uid

            // Show confirmation dialog
            val alertDialog = AlertDialog.Builder(requireContext())
            alertDialog.setTitle("Delete User")
            alertDialog.setMessage("Are you sure you want to delete ${user.username}'s account?")

            // If "Yes" is clicked, proceed with deletion
            alertDialog.setPositiveButton("Yes") { dialog, which ->
                if (uid != null) {
                    deleteUserFromFirestore(uid) { result ->
                        if (result) {
                            Toast.makeText(requireContext(), "User account deleted successfully.", Toast.LENGTH_SHORT).show()
                            // Remove the user from the list and update the adapter
                            userList.removeAt(position)
                            userAdapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(requireContext(), "Failed to delete user account.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // If "No" is clicked, dismiss the dialog
            alertDialog.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }

            alertDialog.show()

            true // Return true to indicate the event was handled
        }
        // Set up back button to return to the main fragment
        backToBotController.setOnClickListener {
            val ft: FragmentTransaction = parentFragmentManager.beginTransaction()
            ft.replace(R.id.fragment_container, MainFragment())
            ft.commit()
        }

        return view
    }

    // Fetch all users from Firestore
    private fun getAllUsers(callback: (Boolean) -> Unit) {
        // Show the loading progress bar
        loadingProgressBar.visibility = View.VISIBLE
        val db = FirebaseFirestore.getInstance();
        db.collection("users").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val user = document.toObject(UserProfile::class.java)
                    totalUser++
                    if(user.accountAccess == "enable"){
                        enableUser++
                    }else if(user.accountAccess == "disable"){
                        disableUser++
                    }else if(user.accountAccess == "draft"){
                        draftUser++
                    }
                    if(user.accessOutTime!! < System.currentTimeMillis() && user.accountAccess == "enable"){
                        expiredUser++
                    }
                    userList.add(user)
                    callback(true)
                }
                // Initialize the adapter and set it to the ListView
                userAdapter = UserAdapter(requireContext(), userList)
                userListView.adapter = userAdapter
                // Hide the loading progress bar once the data is loaded
                loadingProgressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
                // Hide the loading progress bar in case of failure
                loadingProgressBar.visibility = View.GONE
            }
    }

    // Show a custom dialog to update user permission
    @SuppressLint("SetTextI18n")
    private fun userPermissionDialog(userProfile: UserProfile) {
        val dialogView = requireActivity().layoutInflater.inflate(R.layout.dialog_custom, null)
        val btnDialogSubmit = dialogView.findViewById<Button>(R.id.dialogButton)
        val dialogBuilder = AlertDialog.Builder(requireContext())
        accessDaysEditText = dialogView.findViewById(R.id.accessDaysEditText)
        todayDateTextView = dialogView.findViewById(R.id.todayDateTextView)
        todayDateTextView.text =  "Today is:  ${UserAdapter.convertMillisToDate(System.currentTimeMillis())}"

        // Initialize dialog elements
        val spinner = dialogView.findViewById<Spinner>(R.id.action_bar_spinner)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        dialogTitle.text = "Update permission for ${userProfile.username}"

        // Set up spinner options from the resource array
        ArrayAdapter.createFromResource(requireContext(), R.array.spinner_items, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        // Handle spinner item selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                btnDialogSubmit.setOnClickListener {
                    updateUserPermission(userProfile, selectedItem) { result ->
                        if (result) {
                            Toast.makeText(
                                activity,
                                "User permission updated Successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                activity,
                                "User permission update Failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Create and show the dialog
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()

        // Close dialog on button click
        dialogView.findViewById<Button>(R.id.dialogButton).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    // Update user permission in Firestore
    private fun updateUserPermission(user: UserProfile, updatedAccess: String, callback: (Boolean) -> Unit) {
        if(accessDaysEditText.text.toString().isNotEmpty()) {
            accessOutTime = accessDayInDate(accessDaysEditText.text.toString().toInt())
        }else{
            Toast.makeText(activity, "Please fill out all field", Toast.LENGTH_SHORT).show()
            return
        }
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid!!).update("accountAccess", updatedAccess, "accessInTime", accessInTime, "accessOutTime", accessOutTime)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    // Function to delete user profile from Firestore
    private fun deleteUserFromFirestore(uid: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        // Delete the user document from the 'users' collection
        db.collection("users").document(uid)
            .delete()
            .addOnSuccessListener {
                // Successfully deleted user profile
                callback(true)
            }
            .addOnFailureListener { exception ->
                // Failed to delete user profile
                Log.e("Firestore", "Error deleting user profile: ", exception)
                callback(false)
            }
    }

    //Function to shoe date picker dialog
    private fun accessDayInDate(day:Int):Long{
        if (day < 0) return 0
        val dateInMilis:Long = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * day

        return dateInMilis
    }
}
