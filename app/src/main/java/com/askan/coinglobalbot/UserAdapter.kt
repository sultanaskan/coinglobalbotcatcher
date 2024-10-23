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

class UserAdapter(
    private val context: Context,
    private val userList: List<UserProfile>
) : BaseAdapter() {

    override fun getCount(): Int {
        return userList.size
    }

    override fun getItem(position: Int): Any {
        return userList[position]
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
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val user = userList[position]
        holder.usernameTextView.text = user.username
        holder.emailTextView.text = user.email
        holder.accountAccess.text = user.accountAccess
        return view
    }

    private class ViewHolder {
        lateinit var usernameTextView: TextView
        lateinit var emailTextView: TextView
        lateinit var accountAccess: TextView
    }
}
