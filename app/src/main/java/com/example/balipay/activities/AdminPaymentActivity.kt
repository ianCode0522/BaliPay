package com.example.balipay.activities

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.balipay.R
import com.example.balipay.models.PaymentRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.balipay.adapters.PaymentRequestAdapter
import com.google.firebase.auth.FirebaseAuth

class AdminPaymentActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var tvGroupName: TextView
    private lateinit var rvPaymentRequests: RecyclerView
    private lateinit var auth: FirebaseAuth

    private var groupId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_payment)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get group ID from intent
        groupId = intent.getStringExtra("GROUP_ID") ?: ""
        val groupName = intent.getStringExtra("GROUP_NAME") ?: ""

        // Initialize views
        tvGroupName = findViewById(R.id.tvGroupName)
        rvPaymentRequests = findViewById(R.id.rvPaymentRequests)

        tvGroupName.text = groupName

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Setup RecyclerView
        rvPaymentRequests.layoutManager = LinearLayoutManager(this)

        // Load payment requests
        loadPaymentRequests()
    }

    private fun loadPaymentRequests() {
        database.reference.child("paymentRequests").child(groupId)
            .orderByChild("status").equalTo("pending")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requestsList = mutableListOf<PaymentRequest>()

                    for (requestSnapshot in snapshot.children) {
                        val request = requestSnapshot.getValue(PaymentRequest::class.java)
                        request?.let { requestsList.add(it) }
                    }

                    // Update RecyclerView
                    val adapter = PaymentRequestAdapter(requestsList) { request, action ->
                        handlePaymentAction(request, action)
                    }
                    rvPaymentRequests.adapter = adapter

                    if (requestsList.isEmpty()) {
                        Toast.makeText(this@AdminPaymentActivity, "No pending requests", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminPaymentActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handlePaymentAction(request: PaymentRequest, action: String) {
        val message = if (action == "approve") {
            "Approve ${request.userName}'s payment of ₱${request.amount}?"
        } else {
            "Reject ${request.userName}'s payment request?"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Action")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                if (action == "approve") {
                    approvePayment(request)
                } else {
                    rejectPayment(request)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun approvePayment(request: PaymentRequest) {
        val amount = request.amount.toDoubleOrNull() ?: 0.0
        val adminId = auth.currentUser?.uid ?: return

        // Get member's current balance
        database.reference.child("users").child(request.userId).child("balance")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val memberBalance = snapshot.getValue(Double::class.java) ?: 0.0

                    // Check if member has enough balance
                    if (memberBalance < amount) {
                        Toast.makeText(this@AdminPaymentActivity, "Member doesn't have enough balance!", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Get admin's current balance
                    database.reference.child("users").child(adminId).child("balance")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(adminSnapshot: DataSnapshot) {
                                val adminBalance = adminSnapshot.getValue(Double::class.java) ?: 0.0

                                // Transfer money
                                val newMemberBalance = memberBalance - amount
                                val newAdminBalance = adminBalance + amount

                                // Update member balance
                                database.reference.child("users").child(request.userId).child("balance").setValue(newMemberBalance)

                                // Update admin balance
                                database.reference.child("users").child(adminId).child("balance").setValue(newAdminBalance)

                                // Update payment request status
                                database.reference.child("paymentRequests").child(groupId)
                                    .child(request.requestId).child("status").setValue("approved")

                                // Update member's hasPaid status
                                database.reference.child("groups").child(groupId)
                                    .child("members").child(request.userId).child("hasPaid").setValue(true)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@AdminPaymentActivity, "Payment approved! ₱${String.format("%.2f", amount)} transferred.", Toast.LENGTH_LONG).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@AdminPaymentActivity, "Failed to approve payment", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@AdminPaymentActivity, "Error getting admin balance", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminPaymentActivity, "Error getting member balance", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun rejectPayment(request: PaymentRequest) {
        database.reference.child("paymentRequests").child(groupId)
            .child(request.requestId).child("status").setValue("rejected")
            .addOnSuccessListener {
                Toast.makeText(this, "Payment rejected", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to reject payment", Toast.LENGTH_SHORT).show()
            }
    }
}