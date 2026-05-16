package com.example.shoppinglist.domain.repositories

import com.example.shoppinglist.domain.models.ShoppingGroup
import com.example.shoppinglist.domain.models.User
import com.example.shoppinglist.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    fun getCurrentUserSync(): User?
    suspend fun signInAnonymously(): Resource<User>
    suspend fun logout(): Resource<Unit>
    suspend fun addGroupToUser(groupId: String): Resource<Unit>
    suspend fun removeGroupFromUser(groupId: String): Resource<Unit>
    suspend fun setActiveGroupId(groupId: String): Resource<Unit>
    suspend fun updateDisplayName(name: String): Resource<Unit>
    fun cacheGroups(groups: List<ShoppingGroup>)
}
