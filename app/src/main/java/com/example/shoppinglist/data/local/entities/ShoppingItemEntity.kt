package com.example.shoppinglist.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val quantity: Int,
    val addedByUserId: String,
    val addedByUserName: String,
    val isBought: Boolean,
    val timestamp: Long,
    val groupId: String,
    val needsSync: Boolean = false,
    val isDeleted: Boolean = false
)
