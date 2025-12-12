package com.example.balipay.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.balipay.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var etGroupName: EditText
    private lateinit var etContribution: EditText
    private lateinit var etMembers: EditText
    private lateinit var rgSchedule: RadioGroup
    private lateinit var btnCreateGroup: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        etGroupName = findViewById(R.id.etGroupName)
        etContribution = findViewById(R.id.etContribution)
        etMembers = findViewById(R.id.etMembers)
        rgSchedule = findViewById(R.id.rgSchedule)
        btnCreateGroup = findViewById(R.id.btnCreateGroup)

        // Back button - ADD THIS HERE
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnCreateGroup.setOnClickListener {
            createGroup()
        }
    }

    private fun createGroup() {
        val groupName = etGroupName.text.toString().trim()
        val contribution = etContribution.text.toString().trim()
        val members = etMembers.text.toString().trim()

        if (groupName.isEmpty() || contribution.isEmpty() || members.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val schedule = when (rgSchedule.checkedRadioButtonId) {
            R.id.rbDaily -> "Daily"
            R.id.rbWeekly -> "Weekly"
            R.id.rbMonthly -> "Monthly"
            else -> "Weekly"
        }

        val groupId = UUID.randomUUID().toString()
        val userId = auth.currentUser?.uid

        val groupData = hashMapOf(
            "groupId" to groupId,
            "groupName" to groupName,
            "contribution" to contribution,
            "totalMembers" to members,
            "schedule" to schedule,
            "adminId" to userId,
            "createdAt" to System.currentTimeMillis()
        )

        database.reference.child("groups").child(groupId).setValue(groupData)
            .addOnSuccessListener {
                // Add admin as member
                val memberData = hashMapOf(
                    "userId" to userId,
                    "role" to "admin",
                    "joinedAt" to System.currentTimeMillis()
                )
                database.reference.child("groups").child(groupId).child("members").child(userId!!).setValue(memberData)

                Toast.makeText(this, "Group created successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show()
            }
    }
}