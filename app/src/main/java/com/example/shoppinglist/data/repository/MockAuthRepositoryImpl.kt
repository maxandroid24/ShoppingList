package com.example.shoppinglist.data.repository

import android.content.Context
import com.example.shoppinglist.domain.models.ShoppingGroup
import com.example.shoppinglist.domain.models.User
import com.example.shoppinglist.domain.repositories.AuthRepository
import com.example.shoppinglist.domain.utils.Resource
import com.example.shoppinglist.presentation.widget.WidgetUpdateHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.UUID
import javax.inject.Inject

class MockAuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs = context.getSharedPreferences("mock_auth", Context.MODE_PRIVATE)
    
    private val _currentUser = MutableStateFlow<User?>(getUserFromPrefs())
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    override fun getCurrentUserSync(): User? = getUserFromPrefs()

    private fun getUserFromPrefs(): User? {
        val id = prefs.getString("userId", null) ?: return null
        val displayName = prefs.getString("displayName", "User_${id.takeLast(4)}")
        val groupsJson = prefs.getString("groupIds", "[]") ?: "[]"
        val jsonArray = JSONArray(groupsJson)
        val groupIds = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            groupIds.add(jsonArray.getString(i))
        }
        return User(id, "", displayName, groupIds)
    }

    override suspend fun signInAnonymously(): Resource<User> {
        val existingUser = getUserFromPrefs()
        if (existingUser != null) {
            _currentUser.value = existingUser
            return Resource.Success(existingUser)
        }

        val id = UUID.randomUUID().toString()
        val name = "User_${id.takeLast(4)}"
        prefs.edit()
            .putString("userId", id)
            .putString("displayName", name)
            .putString("groupIds", "[]")
            .commit()
        
        val user = User(id, "", name, emptyList())
        _currentUser.value = user
        return Resource.Success(user)
    }

    override suspend fun logout(): Resource<Unit> {
        prefs.edit().clear().apply()
        _currentUser.value = null
        WidgetUpdateHelper.updateWidgets(context)
        return Resource.Success(Unit)
    }

    override suspend fun addGroupToUser(groupId: String): Resource<Unit> {
        val user = getUserFromPrefs() ?: return Resource.Error(Exception("User not found"))
        if (user.groupIds.contains(groupId)) return Resource.Success(Unit)
        
        val newGroupIds = user.groupIds + groupId
        prefs.edit().putString("groupIds", JSONArray(newGroupIds).toString()).commit()
        
        val updatedUser = user.copy(groupIds = newGroupIds)
        _currentUser.value = updatedUser
        WidgetUpdateHelper.updateWidgets(context)
        return Resource.Success(Unit)
    }

    override suspend fun removeGroupFromUser(groupId: String): Resource<Unit> {
        val user = getUserFromPrefs() ?: return Resource.Error(Exception("User not found"))
        val newGroupIds = user.groupIds.filter { it != groupId }
        prefs.edit().putString("groupIds", JSONArray(newGroupIds).toString()).commit()
        
        val updatedUser = user.copy(groupIds = newGroupIds)
        _currentUser.value = updatedUser
        
        // Clear active group if it was the one removed
        if (prefs.getString("activeGroupId", null) == groupId) {
            prefs.edit().remove("activeGroupId").commit()
        }
        
        WidgetUpdateHelper.updateWidgets(context)
        return Resource.Success(Unit)
    }

    override suspend fun setActiveGroupId(groupId: String): Resource<Unit> {
        prefs.edit().putString("activeGroupId", groupId).commit() // Use commit to ensure visibility for widget
        WidgetUpdateHelper.updateWidgets(context)
        return Resource.Success(Unit)
    }

    override fun cacheGroups(groups: List<ShoppingGroup>) {
        val json = org.json.JSONArray()
        groups.forEach { group ->
            json.put(org.json.JSONObject().apply {
                put("id", group.id)
                put("name", group.name)
                put("memberCount", group.memberIds.size)
            })
        }
        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .edit().putString("cached_groups_data", json.toString()).apply()
        WidgetUpdateHelper.updateWidgets(context)
    }

    override suspend fun updateDisplayName(name: String): Resource<Unit> {
        prefs.edit().putString("displayName", name).commit()
        val current = _currentUser.value
        if (current != null) {
            _currentUser.value = current.copy(displayName = name)
        }
        return Resource.Success(Unit)
    }
}
