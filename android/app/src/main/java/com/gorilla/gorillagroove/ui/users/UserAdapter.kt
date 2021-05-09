package com.gorilla.gorillagroove.ui.users

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.entity.DbUser
import kotlinx.android.synthetic.main.simple_text_info_item.view.*
import java.util.*

class UserAdapter(
    private val listener: OnUserListener
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>(), Filterable {

    var userList = listOf<DbUser>()

    fun submitList(users: List<DbUser>) {
        userList = users
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.simple_text_info_item, parent, false
        )
        return UserViewHolder(itemView)
    }

    override fun getItemCount() = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.tvUsername.text = currentUser.name
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val tvUsername: TextView = itemView.textItem

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition

            //in event of animation
            if (position != RecyclerView.NO_POSITION) {
                listener.onUserClick(position)
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val resultsList: List<DbUser> =
                    if (constraint.isNullOrEmpty()) {
                        userList
                    } else {
                        val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                        userList.filter {
                            it.name.toLowerCase(Locale.ROOT).contains(filterPattern)
                        }
                    }

                val filterResults = FilterResults()
                filterResults.values = resultsList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }

    interface OnUserListener {
        fun onUserClick(position: Int)
    }
}
