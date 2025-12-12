package com.example.balipay.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.balipay.R
import com.example.balipay.adapters.ContributionAdapter
import com.example.balipay.adapters.PayoutScheduleAdapter
import com.example.balipay.models.Group
import com.example.balipay.models.Member
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var tvGroupName: TextView
    private lateinit var tvContribution: TextView
    private lateinit var tvSchedule: TextView
    private lateinit var tvGroupCode: TextView
    private lateinit var tvCurrentRecipient: TextView
    private lateinit var tvPayoutAmount: TextView
    private lateinit var btnCopyCode: Button
    private lateinit var rvContributions: RecyclerView
    private lateinit var rvPayoutSchedule: RecyclerView
    private lateinit var btnMarkPaid: Button
    private lateinit var btnManagePayments: Button
    private lateinit var etPaymentAmount: EditText
    private lateinit var tvPaymentLabel: TextView
    private lateinit var btnClaimPayout: Button

    // Payment Summary Views
    private lateinit var tvTotalCollected: TextView
    private lateinit var tvStillNeeded: TextView
    private lateinit var tvMembersPaid: TextView
    private lateinit var tvWaitingMessage: TextView

    // Admin Self-Payment Views
    private lateinit var etAdminPaymentAmount: EditText
    private lateinit var tvAdminPaymentLabel: TextView
    private lateinit var btnAdminMarkPaid: Button

    private var groupId: String = ""
    private var contributionAmount: String = ""
    private var totalMembers: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get group ID from intent
        groupId = intent.getStringExtra("GROUP_ID") ?: ""

        // Initialize views
        tvGroupName = findViewById(R.id.tvGroupName)
        tvContribution = findViewById(R.id.tvContribution)
        tvSchedule = findViewById(R.id.tvSchedule)
        tvGroupCode = findViewById(R.id.tvGroupCode)
        tvCurrentRecipient = findViewById(R.id.tvCurrentRecipient)
        tvPayoutAmount = findViewById(R.id.tvPayoutAmount)
        btnCopyCode = findViewById(R.id.btnCopyCode)
        rvContributions = findViewById(R.id.rvContributions)
        rvPayoutSchedule = findViewById(R.id.rvPayoutSchedule)
        btnMarkPaid = findViewById(R.id.btnMarkPaid)
        btnManagePayments = findViewById(R.id.btnManagePayments)
        etPaymentAmount = findViewById(R.id.etPaymentAmount)
        tvPaymentLabel = findViewById(R.id.tvPaymentLabel)
        btnClaimPayout = findViewById(R.id.btnClaimPayout)

        // Payment Summary
        tvTotalCollected = findViewById(R.id.tvTotalCollected)
        tvStillNeeded = findViewById(R.id.tvStillNeeded)
        tvMembersPaid = findViewById(R.id.tvMembersPaid)
        tvWaitingMessage = findViewById(R.id.tvWaitingMessage)

        // Admin Self-Payment
        etAdminPaymentAmount = findViewById(R.id.etAdminPaymentAmount)
        tvAdminPaymentLabel = findViewById(R.id.tvAdminPaymentLabel)
        btnAdminMarkPaid = findViewById(R.id.btnAdminMarkPaid)

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Copy code button
        btnCopyCode.setOnClickListener {
            copyGroupCode()
        }

        // Mark as Paid button (for members)
        btnMarkPaid.setOnClickListener {
            submitPaymentRequest()
        }

        // Manage Payments button (for admin)
        btnManagePayments.setOnClickListener {
            val intent = Intent(this, AdminPaymentActivity::class.java)
            intent.putExtra("GROUP_ID", groupId)
            intent.putExtra("GROUP_NAME", tvGroupName.text.toString())
            startActivity(intent)
        }

        // Claim Payout button
        btnClaimPayout.setOnClickListener {
            claimPayout()
        }

        // Admin Mark as Paid button
        btnAdminMarkPaid.setOnClickListener {
            adminMarkPaid()
        }

        // Setup RecyclerViews
        rvContributions.layoutManager = LinearLayoutManager(this)
        rvPayoutSchedule.layoutManager = LinearLayoutManager(this)

        // Check if user is admin
        checkIfAdmin()

        // Load group details
        loadGroupDetails()
    }

    private fun loadGroupDetails() {
        database.reference.child("groups").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)
                    group?.let {
                        tvGroupName.text = it.groupName
                        tvContribution.text = "₱${it.contribution}"
                        tvSchedule.text = it.schedule
                        tvGroupCode.text = groupId
                        contributionAmount = it.contribution
                        totalMembers = it.totalMembers.toIntOrNull() ?: 0
                    }

                    // Load members
                    loadMembers()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GroupDetailActivity, "Failed to load group", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun loadMembers() {
        database.reference.child("groups").child(groupId).child("members")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val membersList = mutableListOf<Member>()
                    var processedCount = 0
                    val totalCount = snapshot.childrenCount.toInt()

                    if (totalCount == 0) {
                        updateUI(emptyList())
                        return
                    }

                    snapshot.children.forEachIndexed { index, memberSnapshot ->
                        val userId = memberSnapshot.key
                        val role = memberSnapshot.child("role").getValue(String::class.java) ?: "member"
                        val hasPaid = memberSnapshot.child("hasPaid").getValue(Boolean::class.java) ?: false
                        val joinedAt = memberSnapshot.child("joinedAt").getValue(Long::class.java) ?: 0

                        userId?.let {
                            // Load user name
                            database.reference.child("users").child(it).child("name")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(userSnapshot: DataSnapshot) {
                                        val name = userSnapshot.getValue(String::class.java) ?: "Unknown"

                                        val member = Member(
                                            userId = userId,
                                            name = name,
                                            role = role,
                                            payoutOrder = index + 1,
                                            hasPaid = hasPaid,
                                            joinedAt = joinedAt
                                        )

                                        membersList.add(member)
                                        processedCount++

                                        if (processedCount == totalCount) {
                                            // Sort by payout order
                                            val sortedMembers = membersList.sortedBy { it.payoutOrder }
                                            updateUI(sortedMembers)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        processedCount++
                                        if (processedCount == totalCount) {
                                            updateUI(membersList.sortedBy { it.payoutOrder })
                                        }
                                    }
                                })
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GroupDetailActivity, "Failed to load members", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI(members: List<Member>) {
        // Calculate payout based on ACTUAL members who joined
        val actualMembersCount = members.size
        val totalPayout = (contributionAmount.toDoubleOrNull() ?: 0.0) * actualMembersCount
        tvPayoutAmount.text = "Receives: ₱${String.format("%.2f", totalPayout)}"

        // Show current recipient (first in the list)
        if (members.isNotEmpty()) {
            tvCurrentRecipient.text = members[0].name
        } else {
            tvCurrentRecipient.text = "No members yet"
        }

        // Calculate Payment Summary
        val membersPaidCount = members.count { it.hasPaid }
        val expectedTotal = (contributionAmount.toDoubleOrNull() ?: 0.0) * actualMembersCount

        // Get actual payment amounts from Firebase
        calculatePaymentSummary(actualMembersCount, membersPaidCount, expectedTotal)

        // Update Contribution Status
        val contributionAdapter = ContributionAdapter(members)
        rvContributions.adapter = contributionAdapter

        // Update Payout Schedule
        val payoutAdapter = PayoutScheduleAdapter(members, contributionAmount)
        rvPayoutSchedule.adapter = payoutAdapter

        // Check if group is full
        val isGroupFull = actualMembersCount >= totalMembers
        checkGroupStatus(isGroupFull, actualMembersCount)

        // Check if current user can claim payout
        checkIfCanClaimPayout(members)
    }

    private fun checkGroupStatus(isGroupFull: Boolean, actualMembers: Int) {
        if (isGroupFull) {
            // Group is full - hide waiting message, enable payments
            tvWaitingMessage.visibility = View.GONE

            // Re-check admin status to show proper buttons
            checkIfAdmin()
        } else {
            // Group not full - show waiting message, disable payments
            val membersNeeded = totalMembers - actualMembers
            tvWaitingMessage.text = "⏳ Waiting for $membersNeeded more member${if (membersNeeded > 1) "s" else ""} to join..."
            tvWaitingMessage.visibility = View.VISIBLE

            // Disable all payment buttons
            btnMarkPaid.visibility = View.GONE
            etPaymentAmount.visibility = View.GONE
            tvPaymentLabel.visibility = View.GONE

            btnAdminMarkPaid.visibility = View.GONE
            etAdminPaymentAmount.visibility = View.GONE
            tvAdminPaymentLabel.visibility = View.GONE
        }
    }

    private fun calculatePaymentSummary(totalMembers: Int, membersPaid: Int, expectedTotal: Double) {
        // Get all approved payment requests to calculate actual collected amount
        database.reference.child("paymentRequests").child(groupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalCollected = 0.0

                    for (requestSnapshot in snapshot.children) {
                        val status = requestSnapshot.child("status").getValue(String::class.java)
                        val amount = requestSnapshot.child("amount").getValue(String::class.java)

                        if (status == "approved") {
                            totalCollected += amount?.toDoubleOrNull() ?: 0.0
                        }
                    }

                    val stillNeeded = expectedTotal - totalCollected

                    // Update UI
                    tvTotalCollected.text = "₱${String.format("%.2f", totalCollected)}"
                    tvStillNeeded.text = "₱${String.format("%.2f", if (stillNeeded < 0) 0.0 else stillNeeded)}"
                    tvMembersPaid.text = "Members Paid: $membersPaid/$totalMembers"
                }

                override fun onCancelled(error: DatabaseError) {
                    // Show zeros if error
                    tvTotalCollected.text = "₱0.00"
                    tvStillNeeded.text = "₱${String.format("%.2f", expectedTotal)}"
                    tvMembersPaid.text = "Members Paid: $membersPaid/$totalMembers"
                }
            })
    }

    private fun copyGroupCode() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Group Code", groupId)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Group code copied!", Toast.LENGTH_SHORT).show()
    }

    private fun checkIfAdmin() {
        val userId = auth.currentUser?.uid ?: return

        database.reference.child("groups").child(groupId).child("members").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.child("role").getValue(String::class.java)

                    // Check if group is full first
                    database.reference.child("groups").child(groupId).child("members")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(membersSnapshot: DataSnapshot) {
                                val actualMembers = membersSnapshot.childrenCount.toInt()
                                val isGroupFull = actualMembers >= totalMembers

                                // Only show payment options if group is full
                                if (!isGroupFull) {
                                    return
                                }

                                if (role == "admin") {
                                    // Show admin buttons and self-payment
                                    btnManagePayments.visibility = View.VISIBLE
                                    etAdminPaymentAmount.visibility = View.VISIBLE
                                    tvAdminPaymentLabel.visibility = View.VISIBLE
                                    btnAdminMarkPaid.visibility = View.VISIBLE

                                    // Hide member payment
                                    btnMarkPaid.visibility = View.GONE
                                    etPaymentAmount.visibility = View.GONE
                                    tvPaymentLabel.visibility = View.GONE
                                } else {
                                    // Show member payment
                                    btnMarkPaid.visibility = View.VISIBLE
                                    etPaymentAmount.visibility = View.VISIBLE
                                    tvPaymentLabel.visibility = View.VISIBLE

                                    // Hide admin buttons
                                    btnManagePayments.visibility = View.GONE
                                    etAdminPaymentAmount.visibility = View.GONE
                                    tvAdminPaymentLabel.visibility = View.GONE
                                    btnAdminMarkPaid.visibility = View.GONE
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun adminMarkPaid() {
        val userId = auth.currentUser?.uid ?: return

        val paymentAmount = etAdminPaymentAmount.text.toString().trim()
        if (paymentAmount.isEmpty()) {
            Toast.makeText(this, "Please enter payment amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = paymentAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Check admin's balance
        database.reference.child("users").child(userId).child("balance")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val balance = snapshot.getValue(Double::class.java) ?: 0.0

                    if (balance < amount) {
                        Toast.makeText(this@GroupDetailActivity, "Insufficient balance!", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Admin pays themselves instantly (no approval needed)
                    // Deduct from balance (money stays with admin since they manage it)
                    val newBalance = balance - amount
                    database.reference.child("users").child(userId).child("balance").setValue(newBalance)

                    // Then add it back (admin holds the money)
                    val finalBalance = balance // Balance stays same
                    database.reference.child("users").child(userId).child("balance").setValue(finalBalance)

                    // Mark as paid
                    database.reference.child("groups").child(groupId).child("members")
                        .child(userId).child("hasPaid").setValue(true)

                    // Create approved payment record
                    val requestId = database.reference.child("paymentRequests").push().key ?: return

                    database.reference.child("users").child(userId).child("name")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(nameSnapshot: DataSnapshot) {
                                val userName = nameSnapshot.getValue(String::class.java) ?: "Admin"

                                val paymentRequest = hashMapOf(
                                    "requestId" to requestId,
                                    "groupId" to groupId,
                                    "userId" to userId,
                                    "userName" to userName,
                                    "amount" to paymentAmount,
                                    "status" to "approved",
                                    "timestamp" to System.currentTimeMillis()
                                )

                                database.reference.child("paymentRequests").child(groupId).child(requestId)
                                    .setValue(paymentRequest)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@GroupDetailActivity, "Payment recorded! ₱${String.format("%.2f", amount)}", Toast.LENGTH_SHORT).show()
                                        etAdminPaymentAmount.text.clear()
                                        loadGroupDetails() // Reload to update UI
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@GroupDetailActivity, "Error recording payment", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GroupDetailActivity, "Error checking balance", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun submitPaymentRequest() {
        val userId = auth.currentUser?.uid ?: return

        val paymentAmount = etPaymentAmount.text.toString().trim()
        if (paymentAmount.isEmpty()) {
            Toast.makeText(this, "Please enter payment amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = paymentAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if already submitted a request
        database.reference.child("paymentRequests").child(groupId)
            .orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasPendingRequest = false

                    for (requestSnapshot in snapshot.children) {
                        val status = requestSnapshot.child("status").getValue(String::class.java)
                        if (status == "pending") {
                            hasPendingRequest = true
                            break
                        }
                    }

                    if (hasPendingRequest) {
                        Toast.makeText(this@GroupDetailActivity, "You already have a pending payment request", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Get user name
                    database.reference.child("users").child(userId).child("name")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(nameSnapshot: DataSnapshot) {
                                val userName = nameSnapshot.getValue(String::class.java) ?: "Unknown"
                                createPaymentRequest(userId, userName)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@GroupDetailActivity, "Error getting user info", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GroupDetailActivity, "Error checking requests", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createPaymentRequest(userId: String, userName: String) {
        val requestId = database.reference.child("paymentRequests").push().key ?: return

        val paymentRequest = hashMapOf(
            "requestId" to requestId,
            "groupId" to groupId,
            "userId" to userId,
            "userName" to userName,
            "amount" to etPaymentAmount.text.toString().trim(),
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        database.reference.child("paymentRequests").child(groupId).child(requestId)
            .setValue(paymentRequest)
            .addOnSuccessListener {
                Toast.makeText(this, "Payment request submitted! Waiting for admin approval.", Toast.LENGTH_LONG).show()
                etPaymentAmount.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to submit payment request", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkIfCanClaimPayout(members: List<Member>) {
        val userId = auth.currentUser?.uid ?: return

        if (members.isEmpty()) {
            btnClaimPayout.visibility = View.GONE
            return
        }

        // Check if current user is the first recipient
        val firstRecipient = members[0]
        val isCurrentRecipient = firstRecipient.userId == userId

        // Check if all members have paid
        val allPaid = members.all { it.hasPaid }

        // Show button only if user is recipient AND all members paid
        if (isCurrentRecipient && allPaid) {
            btnClaimPayout.visibility = View.VISIBLE
        } else {
            btnClaimPayout.visibility = View.GONE
        }
    }

    private fun claimPayout() {
        val userId = auth.currentUser?.uid ?: return

        // Get actual member count first
        database.reference.child("groups").child(groupId).child("members")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(membersSnapshot: DataSnapshot) {
                    val actualMembersCount = membersSnapshot.childrenCount.toInt()
                    val totalPayout = (contributionAmount.toDoubleOrNull() ?: 0.0) * actualMembersCount

                    // Get admin ID
                    database.reference.child("groups").child(groupId).child("adminId")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val adminId = snapshot.getValue(String::class.java) ?: return

                                // Get admin balance
                                database.reference.child("users").child(adminId).child("balance")
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(adminSnapshot: DataSnapshot) {
                                            val adminBalance = adminSnapshot.getValue(Double::class.java) ?: 0.0

                                            if (adminBalance < totalPayout) {
                                                Toast.makeText(this@GroupDetailActivity, "Insufficient funds in group!", Toast.LENGTH_SHORT).show()
                                                return
                                            }

                                            // Get recipient balance
                                            database.reference.child("users").child(userId).child("balance")
                                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                                    override fun onDataChange(recipientSnapshot: DataSnapshot) {
                                                        val recipientBalance = recipientSnapshot.getValue(Double::class.java) ?: 0.0

                                                        // Transfer money
                                                        val newAdminBalance = adminBalance - totalPayout
                                                        val newRecipientBalance = recipientBalance + totalPayout

                                                        // Update admin balance
                                                        database.reference.child("users").child(adminId).child("balance").setValue(newAdminBalance)

                                                        // Update recipient balance
                                                        database.reference.child("users").child(userId).child("balance").setValue(newRecipientBalance)

                                                        // Reset all members' hasPaid status to false
                                                        database.reference.child("groups").child(groupId).child("members")
                                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                                override fun onDataChange(resetSnapshot: DataSnapshot) {
                                                                    for (memberSnapshot in resetSnapshot.children) {
                                                                        memberSnapshot.ref.child("hasPaid").setValue(false)
                                                                    }

                                                                    Toast.makeText(this@GroupDetailActivity, "Payout claimed! ₱${String.format("%.2f", totalPayout)} received!", Toast.LENGTH_LONG).show()

                                                                    // Reload group details
                                                                    loadGroupDetails()
                                                                }

                                                                override fun onCancelled(error: DatabaseError) {}
                                                            })
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                        Toast.makeText(this@GroupDetailActivity, "Error claiming payout", Toast.LENGTH_SHORT).show()
                                                    }
                                                })
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(this@GroupDetailActivity, "Error getting admin balance", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@GroupDetailActivity, "Error getting admin info", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GroupDetailActivity, "Error counting members", Toast.LENGTH_SHORT).show()
                }
            })
    }
}