package com.example.balipay

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.balipay.activities.CreateGroupActivity
import com.example.balipay.activities.JoinGroupActivity
import com.example.balipay.activities.LoginActivity
import com.example.balipay.adapters.GroupAdapter
import com.example.balipay.models.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.balipay.activities.GroupDetailActivity

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var tvUserName: TextView
    private lateinit var btnLogout: ImageView
    private lateinit var cardCreateGroup: CardView
    private lateinit var cardJoinGroup: CardView
    private lateinit var rvGroups: RecyclerView
    private lateinit var tvBalance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize views
        tvUserName = findViewById(R.id.tvUserName)
        btnLogout = findViewById(R.id.btnLogout)
        cardCreateGroup = findViewById(R.id.cardCreateGroup)
        cardJoinGroup = findViewById(R.id.cardJoinGroup)
        rvGroups = findViewById(R.id.rvGroups)
        tvBalance = findViewById(R.id.tvBalance)

        // Load user name
        loadUserName()

        // Logout button
        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Create Group button
        cardCreateGroup.setOnClickListener {
            startActivity(Intent(this, CreateGroupActivity::class.java))
        }

        // Join Group button
        cardJoinGroup.setOnClickListener {
            startActivity(Intent(this, JoinGroupActivity::class.java))
        }

        // Setup RecyclerView
        rvGroups.layoutManager = LinearLayoutManager(this)
        loadGroups()
    }

    private fun loadUserName() {
        val userId = auth.currentUser?.uid
        userId?.let {
            database.reference.child("users").child(it)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                        val balance = snapshot.child("balance").getValue(Double::class.java) ?: 0.0

                        tvUserName.text = name
                        tvBalance.text = "₱${String.format("%.2f", balance)}"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        tvUserName.text = "User"
                        tvBalance.text = "₱0.00"
                    }
                })
        }
    }

    private fun loadGroups() {
        val userId = auth.currentUser?.uid
        userId?.let {
            database.reference.child("groups")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val groupsList = mutableListOf<Group>()

                        for (groupSnapshot in snapshot.children) {
                            val group = groupSnapshot.getValue(Group::class.java)
                            // Check if user is a member
                            val isMember = groupSnapshot.child("members").child(userId).exists()

                            if (group != null && isMember) {
                                groupsList.add(group)
                            }
                        }

                        // Update RecyclerView
                        val adapter = GroupAdapter(groupsList) { group ->
                            val intent = Intent(this@MainActivity, GroupDetailActivity::class.java)
                            intent.putExtra("GROUP_ID", group.groupId)
                            startActivity(intent)
                        }
                        rvGroups.adapter = adapter
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MainActivity, "Failed to load groups", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}