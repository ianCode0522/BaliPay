package com.example.balipay.models

data class PaymentRequest(
    val requestId: String = "",
    val groupId: String = "",
    val userId: String = "",
    val userName: String = "",
    val amount: String = "",
    val status: String = "pending", // pending, approved, rejected
    val timestamp: Long = 0
)