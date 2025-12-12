package com.example.balipay.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.balipay.R
import com.example.balipay.models.Group

class GroupAdapter(
    private val groups: List<Group>,
    private val onGroupClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGroupName: TextView = view.findViewById(R.id.tvGroupName)
        val tvContribution: TextView = view.findViewById(R.id.tvContribution)
        val tvSchedule: TextView = view.findViewById(R.id.tvSchedule)
        val tvMembers: TextView = view.findViewById(R.id.tvMembers)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]

        holder.tvGroupName.text = group.groupName
        holder.tvContribution.text = "₱${group.contribution}"
        holder.tvSchedule.text = group.schedule
        holder.tvMembers.text = "${group.totalMembers} members"

        holder.itemView.setOnClickListener {
            onGroupClick(group)
        }
    }

    override fun getItemCount() = groups.size
}