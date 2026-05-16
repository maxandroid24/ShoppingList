package com.example.shoppinglist.data.repository

import android.content.Context
import android.util.Log
import com.example.shoppinglist.domain.models.ShoppingGroup
import com.example.shoppinglist.domain.models.User
import com.example.shoppinglist.domain.repositories.AuthRepository
import com.example.shoppinglist.domain.utils.Resource
import com.example.shoppinglist.presentation.widget.WidgetUpdateHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : AuthRepository {
    
    private val TAG = "AuthRepo"
    private val prefs = context.getSharedPreferences("mock_auth", Context.MODE_PRIVATE)

    override val currentUser: Flow<User?> = callbackFlow {
        var userDocListener: ListenerRegistration? = null
        
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            Log.d(TAG, "AuthState changed: ${firebaseUser?.uid}")
            
            userDocListener?.remove()
            
            if (firebaseUser != null) {
                userDocListener = firestore.collection("users").document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Firestore snapshot error", error)
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null && snapshot.exists()) {
                            val groupIds = (snapshot.get("groupIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            val displayName = snapshot.getString("displayName") ?: "User_${firebaseUser.uid.takeLast(4)}"
                            
                            // Cache groupIds for widget access
                            prefs.edit().putString("cached_groupIds", org.json.JSONArray(groupIds).toString()).apply()
                            
                            Log.d(TAG, "User updated in Firestore. Groups: $groupIds")
                            trySend(User(firebaseUser.uid, firebaseUser.email ?: "", displayName, groupIds))
                        } else {
                            trySend(User(firebaseUser.uid, firebaseUser.email ?: "", null, emptyList()))
                        }
                    }
            } else {
                trySend(null)
            }
        }
        
        auth.addAuthStateListener(authListener)
        awaitClose { 
            auth.removeAuthStateListener(authListener)
            userDocListener?.remove()
        }
    }

    override fun getCurrentUserSync(): User? {
        val fbUser = auth.currentUser ?: return null
        val groupsJson = prefs.getString("cached_groupIds", "[]") ?: "[]"
        val groupIds = mutableListOf<String>()
        try {
            val array = org.json.JSONArray(groupsJson)
            for (i in 0 until array.length()) {
                groupIds.add(array.getString(i))
            }
        } catch (e: Exception) {}
        
        return User(fbUser.uid, fbUser.email ?: "", fbUser.displayName, groupIds)
    }

    override suspend fun signInAnonymously(): Resource<User> {
        return try {
            Log.d(TAG, "Attempting anonymous sign in...")
            val result = auth.signInAnonymously().await()
            val firebaseUser = result.user ?: throw Exception("Anonymous sign in failed: result user is null")
            
            Log.d(TAG, "Firebase Auth success, checking Firestore for UID: ${firebaseUser.uid}")
            val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
            var groupIds = emptyList<String>()
            
            if (doc.exists()) {
                groupIds = (doc.get("groupIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                Log.d(TAG, "Existing Firestore user found with groups: $groupIds")
            } else {
                Log.d(TAG, "New user, initializing Firestore document...")
                val userMap = mapOf(
                    "email" to "",
                    "displayName" to "User_${firebaseUser.uid.takeLast(4)}",
                    "groupIds" to emptyList<String>()
                )
                firestore.collection("users").document(firebaseUser.uid).set(userMap).await()
            }
            
            val user = User(firebaseUser.uid, "", "User_${firebaseUser.uid.takeLast(4)}", groupIds)
            Resource.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Cloud sign in failed", e)
            Resource.Error(e)
        }
    }

    override suspend fun logout(): Resource<Unit> {
        return try {
            auth.signOut()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun addGroupToUser(groupId: String): Resource<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
            Log.d(TAG, "Adding group $groupId to user $uid in Firestore")
            firestore.collection("users").document(uid)
                .update("groupIds", FieldValue.arrayUnion(groupId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding group to user", e)
            Resource.Error(e)
        }
    }

    override suspend fun removeGroupFromUser(groupId: String): Resource<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
            firestore.collection("users").document(uid)
                .update("groupIds", FieldValue.arrayRemove(groupId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing group from user", e)
            Resource.Error(e)
        }
    }

    override suspend fun setActiveGroupId(groupId: String): Resource<Unit> {
        prefs.edit().putString("activeGroupId", groupId).apply()
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
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
            firestore.collection("users").document(uid)
                .update("displayName", name)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating display name", e)
            Resource.Error(e)
        }
    }
}
