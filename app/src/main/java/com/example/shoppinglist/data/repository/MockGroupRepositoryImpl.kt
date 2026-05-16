package com.example.shoppinglist.data.repository

import android.content.Context
import android.util.Log
import com.example.shoppinglist.domain.models.ShoppingGroup
import com.example.shoppinglist.domain.repositories.GroupRepository
import com.example.shoppinglist.domain.utils.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

class MockGroupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GroupRepository {

    private val TAG = "MockGroupRepo"
    private val prefs = context.getSharedPreferences("mock_groups", Context.MODE_PRIVATE)

    override suspend fun createGroup(groupName: String, ownerId: String, ownerName: String): Resource<ShoppingGroup> {
        val groupId = UUID.randomUUID().toString()
        val inviteCode = UUID.randomUUID().toString().take(6).uppercase()
        val group = ShoppingGroup(groupId, groupName, ownerId, listOf(ownerId), listOf(ownerName), inviteCode)
        
        Log.d(TAG, "Creating group: $groupName, Code: $inviteCode")
        saveGroup(group)
        return Resource.Success(group)
    }

    override suspend fun joinGroup(inviteCode: String, userId: String, userName: String): Resource<ShoppingGroup> {
        val searchCode = inviteCode.trim().uppercase()
        Log.d(TAG, "Attempting to join group with code: $searchCode")
        
        val groupsJson = prefs.getString("groups", "[]") ?: "[]"
        val jsonArray = JSONArray(groupsJson)
        
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val storedCode = obj.getString("inviteCode").uppercase()
            
            if (storedCode == searchCode) {
                val memberIds = jsonArrayToStringList(obj.getJSONArray("memberIds"))
                val memberNames = jsonArrayToStringList(obj.optJSONArray("memberNames") ?: JSONArray())
                
                // Check if name already exists
                if (memberNames.any { it.equals(userName, ignoreCase = true) }) {
                    val existingIndex = memberNames.indexOfFirst { it.equals(userName, ignoreCase = true) }
                    if (existingIndex != -1 && memberIds[existingIndex] != userId) {
                        return Resource.Error(Exception("Name '$userName' is already taken in this group"))
                    }
                }
                
                if (!memberIds.contains(userId)) {
                    val updatedIds = memberIds + userId
                    val updatedNames = memberNames + userName
                    val updatedGroup = ShoppingGroup(
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("ownerId"),
                        updatedIds,
                        updatedNames,
                        inviteCode
                    )
                    saveGroup(updatedGroup)
                    return Resource.Success(updatedGroup)
                }
                
                return Resource.Success(jsonToGroup(obj))
            }
        }
        
        return Resource.Error(Exception("Invalid invite code: $searchCode"))
    }

    override suspend fun getGroupById(groupId: String): Resource<ShoppingGroup> {
        val groupsJson = prefs.getString("groups", "[]") ?: "[]"
        val jsonArray = JSONArray(groupsJson)
        
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("id") == groupId) {
                return Resource.Success(jsonToGroup(obj))
            }
        }
        return Resource.Error(Exception("Group not found"))
    }

    override suspend fun getGroupsByIds(groupIds: List<String>): Resource<List<ShoppingGroup>> {
        val groupsJson = prefs.getString("groups", "[]") ?: "[]"
        val jsonArray = JSONArray(groupsJson)
        val result = mutableListOf<ShoppingGroup>()
        
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (groupIds.contains(obj.getString("id"))) {
                result.add(jsonToGroup(obj))
            }
        }
        return Resource.Success(result)
    }

    private fun jsonToGroup(obj: JSONObject): ShoppingGroup {
        return ShoppingGroup(
            obj.getString("id"),
            obj.getString("name"),
            obj.getString("ownerId"),
            jsonArrayToStringList(obj.getJSONArray("memberIds")),
            jsonArrayToStringList(obj.optJSONArray("memberNames") ?: JSONArray()),
            obj.getString("inviteCode")
        )
    }

    private fun jsonArrayToStringList(array: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    private fun saveGroup(group: ShoppingGroup) {
        val groupsJson = prefs.getString("groups", "[]") ?: "[]"
        val jsonArray = JSONArray(groupsJson)
        val newArray = JSONArray()
        
        var found = false
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("id") == group.id) {
                newArray.put(groupToJson(group))
                found = true
            } else {
                newArray.put(obj)
            }
        }
        
        if (!found) {
            newArray.put(groupToJson(group))
        }
        
        prefs.edit().putString("groups", newArray.toString()).commit()
    }

    private fun groupToJson(group: ShoppingGroup): JSONObject {
        return JSONObject().apply {
            put("id", group.id)
            put("name", group.name)
            put("ownerId", group.ownerId)
            put("memberIds", JSONArray(group.memberIds))
            put("memberNames", JSONArray(group.memberNames))
            put("inviteCode", group.inviteCode)
        }
    }
}
