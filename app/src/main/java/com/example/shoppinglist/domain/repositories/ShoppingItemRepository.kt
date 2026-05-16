package com.example.shoppinglist.domain.repositories

import com.example.shoppinglist.domain.models.ShoppingItem
import com.example.shoppinglist.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface ShoppingItemRepository {
    fun getItems(groupId: String): Flow<Resource<List<ShoppingItem>>>
    suspend fun addItem(item: ShoppingItem): Resource<Unit>
    suspend fun updateItem(item: ShoppingItem): Resource<Unit>
    suspend fun deleteItem(item: ShoppingItem): Resource<Unit>
    suspend fun syncOfflineData()
}
