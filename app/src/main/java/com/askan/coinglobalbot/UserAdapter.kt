package com.askan.coinglobalbot

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.type.DateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class UserAdapter(
    private val context: Context,
    private var userList: MutableList<UserProfile>
) : BaseAdapter() {

    private var filteredUserList: MutableList<UserProfile> = userList.toMutableList()

    override fun getCount(): Int {
        return filteredUserList.size
    }

    override fun getItem(position: Int): Any {
        return filteredUserList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.user_list_item, parent, false)
            holder = ViewHolder()
            holder.usernameTextView = view.findViewById(R.id.usernameTextView)
            holder.emailTextView = view.findViewById(R.id.emailTextView)
            holder.accountAccess = view.findViewById(R.id.accontAccess)
            holder.coinglobaluid = view.findViewById(R.id.coinglobaluid)
            holder.accessInTime = view.findViewById(R.id.accessInTime)
            holder.accessOutTime = view.findViewById(R.id.accessOutTime)
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val user = filteredUserList[position]
        val i = Date(user.accessInTime!!)
        val o = Date(user.accessOutTime!!)
        holder.usernameTextView.text = user.username
        holder.emailTextView.text = user.email
        holder.accountAccess.text = user.accountAccess
        holder.coinglobaluid.text = user.accountUid
        if(user.accountAccess == "enable"){
            if (user.accessInTime!! > 0L) {
                holder.accessInTime.text = "Access Enable From: ${convertMillisToDate(user.accessInTime!!)}"

                val remaining = ((user.accessOutTime!! - user.accessInTime!!) / (1000 * 60 * 60 * 24))
                if (user.accessOutTime!! > 0L && user.accountAccess == "enable" && remaining > 0) {
                    holder.accessOutTime.text = "Remaining Time: ${remaining} days"
                }
                if(remaining <= 0){
                    holder.accessOutTime.text = "Remaining Time:  Expired"
                }
            }
        }

        return view
    }

    // Method to filter users based on the search query
    fun filter(query: String) {
        println("User List is: $userList")
        filteredUserList = if (query.isEmpty()) {
            userList.toMutableList()
        } else {
            userList.filter {
                it.username!!.contains(query, true) || it.email!!.contains(query, true) || it.accountAccess!!.contains(query, true) || it.accountUid!!.contains(query, true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }




    private class ViewHolder {
        lateinit var usernameTextView: TextView
        lateinit var emailTextView: TextView
        lateinit var accountAccess: TextView
        lateinit var coinglobaluid: TextView
        lateinit var accessInTime: TextView
        lateinit var accessOutTime: TextView
    }

    companion object {
        public fun convertMillisToDate(millis: Long): String {
            // Create a Date object from milliseconds
            val date = Date(millis)

            // Format the date
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return format.format(date)
        }
    }
}
