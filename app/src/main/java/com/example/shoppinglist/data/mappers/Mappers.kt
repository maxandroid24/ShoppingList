package com.example.shoppinglist.data.mappers

import com.example.shoppinglist.data.local.entities.ShoppingItemEntity
import com.example.shoppinglist.domain.models.ShoppingItem

fun ShoppingItemEntity.toDomain(): ShoppingItem {
    return ShoppingItem(
        id = id,
        name = name,
        quantity = quantity,
        addedByUserId = addedByUserId,
        addedByUserName = addedByUserName,
        isBought = isBought,
        timestamp = timestamp,
        groupId = groupId
    )
}

fun ShoppingItem.toEntity(needsSync: Boolean = false, isDeleted: Boolean = false): ShoppingItemEntity {
    return ShoppingItemEntity(
        id = id.ifEmpty { java.util.UUID.randomUUID().toString() },
        name = name,
        quantity = quantity,
        addedByUserId = addedByUserId,
        addedByUserName = addedByUserName,
        isBought = isBought,
        timestamp = timestamp,
        groupId = groupId,
        needsSync = needsSync,
        isDeleted = isDeleted
    )
}

fun Map<String, Any>.toShoppingItem(id: String): ShoppingItem {
    return ShoppingItem(
        id = id,
        name = this["name"] as? String ?: "",
        quantity = (this["quantity"] as? Long)?.toInt() ?: 1,
        addedByUserId = this["addedByUserId"] as? String ?: "",
        addedByUserName = this["addedByUserName"] as? String ?: "Unknown",
        isBought = this["isBought"] as? Boolean ?: false,
        timestamp = this["timestamp"] as? Long ?: System.currentTimeMillis(),
        groupId = this["groupId"] as? String ?: ""
    )
}

fun ShoppingItem.toMap(): Map<String, Any> {
    return mapOf(
        "name" to name,
        "quantity" to quantity,
        "addedByUserId" to addedByUserId,
        "addedByUserName" to addedByUserName,
        "isBought" to isBought,
        "timestamp" to timestamp,
        "groupId" to groupId
    )
}
