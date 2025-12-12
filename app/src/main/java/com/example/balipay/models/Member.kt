package com.example.balipay.models

data class Member(
    val userId: String = "",
    val name: String = "",
    val role: String = "member",
    val payoutOrder: Int = 0,
    val hasPaid: Boolean = false,
    val joinedAt: Long = 0
)