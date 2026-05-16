package com.example.shoppinglist.data.repository

import android.util.Log
import com.example.shoppinglist.domain.models.ShoppingGroup
import com.example.shoppinglist.domain.repositories.GroupRepository
import com.example.shoppinglist.domain.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : GroupRepository {

    private val TAG = "GroupRepo"

    override suspend fun createGroup(groupName: String, ownerId: String, ownerName: String): Resource<ShoppingGroup> {
        return try {
            val groupId = UUID.randomUUID().toString()
            val inviteCode = UUID.randomUUID().toString().take(6).uppercase()
            
            Log.d(TAG, "Creating Firestore group: $groupName, Code: $inviteCode")
            
            val groupMap = mapOf(
                "id" to groupId,
                "name" to groupName,
                "ownerId" to ownerId,
                "memberIds" to listOf(ownerId),
                "memberNames" to listOf(ownerName),
                "inviteCode" to inviteCode
            )
            
            firestore.collection("groups").document(groupId).set(groupMap).await()
            
            val group = ShoppingGroup(groupId, groupName, ownerId, listOf(ownerId), listOf(ownerName), inviteCode)
            Resource.Success(group)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group", e)
            Resource.Error(e)
        }
    }

    override suspend fun joinGroup(inviteCode: String, userId: String, userName: String): Resource<ShoppingGroup> {
        val searchCode = inviteCode.trim().uppercase()
        Log.d(TAG, "Attempting to join Firestore group with code: $searchCode")
        
        return try {
            val querySnapshot = firestore.collection("groups")
                .whereEqualTo("inviteCode", searchCode)
                .get()
                .await()
                
            if (querySnapshot.isEmpty) {
                Log.e(TAG, "No Firestore group found with code: $searchCode")
                return Resource.Error(Exception("Invalid invite code: $searchCode"))
            }
            
            val doc = querySnapshot.documents.first()
            val groupId = doc.id
            Log.d(TAG, "Match found in Firestore: $groupId")
            
            val memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val memberNames = (doc.get("memberNames") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            
            // Check if name already exists in this group
            if (memberNames.any { it.equals(userName, ignoreCase = true) }) {
                // If it's the same user re-joining, we can allow it or block it. 
                // But the requirement says "shouldn't already exists in group members".
                // We should check if the UID is different.
                val existingIndex = memberNames.indexOfFirst { it.equals(userName, ignoreCase = true) }
                if (existingIndex != -1 && memberIds[existingIndex] != userId) {
                    return Resource.Error(Exception("Name '$userName' is already taken in this group"))
                }
            }
            
            if (!memberIds.contains(userId)) {
                val updatedMembers = memberIds + userId
                val updatedNames = memberNames + userName
                firestore.collection("groups").document(groupId)
                    .update(
                        "memberIds", updatedMembers,
                        "memberNames", updatedNames
                    )
                    .await()
            }
            
            val group = docToGroup(doc)
            Resource.Success(group)
        } catch (e: Exception) {
            Log.e(TAG, "Error joining group", e)
            Resource.Error(e)
        }
    }

    override suspend fun getGroupById(groupId: String): Resource<ShoppingGroup> {
        return try {
            val doc = firestore.collection("groups").document(groupId).get().await()
            if (doc.exists()) {
                Resource.Success(docToGroup(doc))
            } else {
                Resource.Error(Exception("Group not found"))
            }
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun getGroupsByIds(groupIds: List<String>): Resource<List<ShoppingGroup>> {
        return try {
            if (groupIds.isEmpty()) return Resource.Success(emptyList())
            val querySnapshot = firestore.collection("groups")
                .whereIn("id", groupIds)
                .get()
                .await()
            val groups = querySnapshot.documents.map { docToGroup(it) }
            Resource.Success(groups)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    private fun docToGroup(doc: com.google.firebase.firestore.DocumentSnapshot): ShoppingGroup {
        return ShoppingGroup(
            id = doc.id,
            name = doc.getString("name") ?: "",
            ownerId = doc.getString("ownerId") ?: "",
            memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            memberNames = (doc.get("memberNames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            inviteCode = doc.getString("inviteCode") ?: ""
        )
    }
}
