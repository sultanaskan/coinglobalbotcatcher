package com.askan.coinglobalbot

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.firestore.FirebaseFirestore



class ManagementFragment : Fragment() {

    private lateinit var userListView: ListView
    private lateinit var userAdapter: UserAdapter
    private var userList: MutableList<UserProfile> = mutableListOf()
    private lateinit var backtobotcontroller:Button

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_management, container, false)
        backtobotcontroller = view.findViewById(R.id.btnBackToBotController)
        userListView = view.findViewById(R.id.userListView)
        getAllUsers()
        userListView.setOnItemClickListener { parent, view, position, id ->
            val clickedItem = userList[position]
            showCustomDialog(clickedItem)
           // Toast.makeText(activity, "Clicked on $clickedItem", Toast.LENGTH_SHORT).show()
        }
        backtobotcontroller.setOnClickListener{
            val ft: FragmentTransaction = parentFragmentManager.beginTransaction()
            ft.replace(R.id.fragment_container, MainFragment())
            ft.commit()
        }

        return view
    }

    private fun getAllUsers() {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val user = document.toObject(UserProfile::class.java)
                    userList.add(user)
                }

                // Initialize the adapter and set it to the ListView
                userAdapter = UserAdapter(requireContext(), userList)
                userListView.adapter = userAdapter
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }
    @SuppressLint("SetTextI18n")
    private fun showCustomDialog(userProfile: UserProfile) {
        val dialogView = requireActivity().layoutInflater.inflate(R.layout.dialog_custom, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()) // Use requireContext()
        val spinner = dialogView.findViewById<Spinner>(R.id.action_bar_spinner)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        dialogTitle.text = "Update permission of ${userProfile.username}"
        ArrayAdapter.createFromResource(requireContext(), R.array.spinner_items, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                // Get selected item text
                val selectedItem = parent.getItemAtPosition(position).toString()
                updateUserPermission(userProfile, selectedItem){
                    result ->
                    if(result){
                        Toast.makeText(activity,"User permission updated Successfully", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(activity,"User permission updated Failed", Toast.LENGTH_SHORT).show()
                    }
                }
               // Toast.makeText(activity, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action if nothing is selected
            }
        }
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()

        dialogView.findViewById<Button>(R.id.dialogButton).setOnClickListener {
          dialog.dismiss()
        }
        dialog.show()
    }




    private  fun updateUserPermission(user: UserProfile, updatedAccess: String, callback: (Boolean )-> Unit){
        val db = FirebaseFirestore.getInstance()
        println("update User permission called!")
        db.collection("users").document(user.uid!!).update(hashMapOf("accountAccess" to updatedAccess) as Map<String, Any>).addOnSuccessListener {
            callback(true)
        }.addOnFailureListener{
            callback(true)
        }
    }


}
