package com.example.couplefriendly.data

data class User(
    val uid: String = "",
    val email: String = "",
    val partnerId: String = "",
    val partnerCode: String = ""
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatRoom(
    val id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0
)
