package com.example.shoppinglist.domain.repositories

import com.example.shoppinglist.domain.models.ShoppingGroup
import com.example.shoppinglist.domain.utils.Resource

interface GroupRepository {
    suspend fun createGroup(groupName: String, ownerId: String, ownerName: String): Resource<ShoppingGroup>
    suspend fun joinGroup(inviteCode: String, userId: String, userName: String): Resource<ShoppingGroup>
    suspend fun getGroupById(groupId: String): Resource<ShoppingGroup>
    suspend fun getGroupsByIds(groupIds: List<String>): Resource<List<ShoppingGroup>>
}
