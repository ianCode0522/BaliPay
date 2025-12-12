package com.example.balipay.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.balipay.R
import com.example.balipay.models.PaymentRequest
import java.text.SimpleDateFormat
import java.util.*

class PaymentRequestAdapter(
    private val requests: List<PaymentRequest>,
    private val onAction: (PaymentRequest, String) -> Unit
) : RecyclerView.Adapter<PaymentRequestAdapter.PaymentRequestViewHolder>() {

    class PaymentRequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val btnApprove: Button = view.findViewById(R.id.btnApprove)
        val btnReject: Button = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_request, parent, false)
        return PaymentRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentRequestViewHolder, position: Int) {
        val request = requests[position]

        holder.tvUserName.text = request.userName
        holder.tvAmount.text = "₱${request.amount}"

        // Format timestamp
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        holder.tvTimestamp.text = dateFormat.format(Date(request.timestamp))

        holder.btnApprove.setOnClickListener {
            onAction(request, "approve")
        }

        holder.btnReject.setOnClickListener {
            onAction(request, "reject")
        }
    }

    override fun getItemCount() = requests.size
}