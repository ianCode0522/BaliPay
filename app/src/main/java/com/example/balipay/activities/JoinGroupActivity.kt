package com.example.balipay.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.balipay.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class JoinGroupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var etGroupCode: EditText
    private lateinit var btnJoinGroup: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_group)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        etGroupCode = findViewById(R.id.etGroupCode)
        btnJoinGroup = findViewById(R.id.btnJoinGroup)

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnJoinGroup.setOnClickListener {
            joinGroup()
        }
    }

    private fun joinGroup() {
        val groupCode = etGroupCode.text.toString().trim()

        if (groupCode.isEmpty()) {
            Toast.makeText(this, "Please enter group code", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid

        database.reference.child("groups").child(groupCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Get group details
                        val totalMembersLimit = snapshot.child("totalMembers").getValue(String::class.java)?.toIntOrNull() ?: 0
                        val currentMembersCount = snapshot.child("members").childrenCount.toInt()

                        // Check if group is full
                        if (currentMembersCount >= totalMembersLimit) {
                            Toast.makeText(this@JoinGroupActivity, "Group is full! (${currentMembersCount}/${totalMembersLimit} members)", Toast.LENGTH_LONG).show()
                            return
                        }

                        // Check if already a member
                        if (snapshot.child("members").child(userId!!).exists()) {
                            Toast.makeText(this@JoinGroupActivity, "You're already a member of this group!", Toast.LENGTH_SHORT).show()
                            return
                        }

                        // Join the group
                        val memberData = hashMapOf(
                            "userId" to userId,
                            "role" to "member",
                            "joinedAt" to System.currentTimeMillis(),
                            "hasPaid" to false
                        )

                        database.reference.child("groups").child(groupCode)
                            .child("members").child(userId).setValue(memberData)
                            .addOnSuccessListener {
                                Toast.makeText(this@JoinGroupActivity, "Joined group successfully!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    } else {
                        Toast.makeText(this@JoinGroupActivity, "Group not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@JoinGroupActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}