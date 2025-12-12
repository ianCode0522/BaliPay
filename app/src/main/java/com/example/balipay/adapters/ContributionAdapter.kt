package com.example.balipay.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.balipay.R
import com.example.balipay.models.Member

class ContributionAdapter(
    private val members: List<Member>
) : RecyclerView.Adapter<ContributionAdapter.ContributionViewHolder>() {

    class ContributionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMemberName: TextView = view.findViewById(R.id.tvMemberName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContributionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contribution, parent, false)
        return ContributionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContributionViewHolder, position: Int) {
        val member = members[position]

        holder.tvMemberName.text = member.name

        if (member.hasPaid) {
            holder.tvStatus.text = "✓ Paid"
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.success))
        } else {
            holder.tvStatus.text = "✗ Not Paid"
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.error))
        }
    }

    override fun getItemCount() = members.size
}