package com.example.shoppinglist.domain.models

data class ShoppingItem(
    val id: String = "",
    val name: String,
    val quantity: Int,
    val addedByUserId: String,
    val addedByUserName: String,
    val isBought: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val groupId: String
)
