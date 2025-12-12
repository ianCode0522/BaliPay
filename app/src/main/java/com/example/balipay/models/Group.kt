package com.example.balipay.models

data class Group(
    val groupId: String = "",
    val groupName: String = "",
    val contribution: String = "",
    val totalMembers: String = "",
    val schedule: String = "",
    val adminId: String = "",
    val createdAt: Long = 0
)