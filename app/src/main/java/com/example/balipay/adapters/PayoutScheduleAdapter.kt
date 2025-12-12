package com.example.balipay.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.balipay.R
import com.example.balipay.models.Member

class PayoutScheduleAdapter(
    private val members: List<Member>,
    private val contributionAmount: String
) : RecyclerView.Adapter<PayoutScheduleAdapter.PayoutViewHolder>() {

    class PayoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWeekNumber: TextView = view.findViewById(R.id.tvWeekNumber)
        val tvRecipientName: TextView = view.findViewById(R.id.tvRecipientName)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payout_schedule, parent, false)
        return PayoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: PayoutViewHolder, position: Int) {
        val member = members[position]
        val totalMembers = members.size

        holder.tvWeekNumber.text = "${member.payoutOrder}"
        holder.tvRecipientName.text = member.name

        // Calculate total payout (contribution × number of members)
        val contribution = contributionAmount.toDoubleOrNull() ?: 0.0
        val totalPayout = contribution * totalMembers
        holder.tvAmount.text = "₱${String.format("%.2f", totalPayout)}"
    }

    override fun getItemCount() = members.size
}