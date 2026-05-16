package com.example.shoppinglist.domain.models

data class ShoppingGroup(
    val id: String,
    val name: String,
    val ownerId: String,
    val memberIds: List<String>,
    val memberNames: List<String>,
    val inviteCode: String
)
