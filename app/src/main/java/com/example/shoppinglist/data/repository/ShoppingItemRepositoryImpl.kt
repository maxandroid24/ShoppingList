package com.example.shoppinglist.data.repository

import android.content.Context
import com.example.shoppinglist.data.local.ShoppingItemDao
import com.example.shoppinglist.data.mappers.toDomain
import com.example.shoppinglist.data.mappers.toEntity
import com.example.shoppinglist.data.mappers.toMap
import com.example.shoppinglist.data.mappers.toShoppingItem
import com.example.shoppinglist.domain.models.ShoppingItem
import com.example.shoppinglist.domain.repositories.ShoppingItemRepository
import com.example.shoppinglist.domain.utils.Resource
import com.example.shoppinglist.presentation.widget.WidgetUpdateHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ShoppingItemRepositoryImpl @Inject constructor(
    private val dao: ShoppingItemDao,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ShoppingItemRepository {

    override fun getItems(groupId: String): Flow<Resource<List<ShoppingItem>>> = callbackFlow {
        trySend(Resource.Loading)
        
        val localFlow = dao.getItemsForGroup(groupId).map { entities ->
            entities.map { it.toDomain() }
        }
        
        var listener: ListenerRegistration? = null
        try {
            listener = firestore.collection("groups").document(groupId)
                .collection("items")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.data?.toShoppingItem(doc.id)
                        }
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            items.forEach { item ->
                                val localItem = dao.getItemByName(item.name, item.groupId)
                                if (localItem?.needsSync != true) {
                                    dao.insertItem(item.toEntity(needsSync = false))
                                }
                            }
                            WidgetUpdateHelper.updateWidgets(context)
                        }
                    }
                }
        } catch (e: Exception) {}
        
        val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            localFlow.collect { items ->
                trySend(Resource.Success(items))
            }
        }

        awaitClose {
            listener?.remove()
            job.cancel()
        }
    }

    override suspend fun addItem(item: ShoppingItem): Resource<Unit> {
        return try {
            val entity = item.toEntity(needsSync = true)
            dao.insertItem(entity)
            syncItem(entity)
            WidgetUpdateHelper.updateWidgets(context)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun updateItem(item: ShoppingItem): Resource<Unit> {
        return try {
            val entity = item.toEntity(needsSync = true)
            dao.updateItem(entity)
            syncItem(entity)
            WidgetUpdateHelper.updateWidgets(context)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun deleteItem(item: ShoppingItem): Resource<Unit> {
        return try {
            val entity = item.toEntity(needsSync = true, isDeleted = true)
            dao.markItemAsDeleted(entity.id)
            syncDeletedItem(entity)
            WidgetUpdateHelper.updateWidgets(context)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun syncOfflineData() {
        val unsyncedItems = dao.getUnsyncedItems()
        for (item in unsyncedItems) {
            if (item.isDeleted) {
                syncDeletedItem(item)
            } else {
                syncItem(item)
            }
        }
    }
    
    private suspend fun syncItem(entity: com.example.shoppinglist.data.local.entities.ShoppingItemEntity) {
        try {
            val item = entity.toDomain()
            firestore.collection("groups").document(item.groupId)
                .collection("items").document(item.id)
                .set(item.toMap())
                .await()
            dao.updateItem(entity.copy(needsSync = false))
        } catch (e: Exception) {}
    }
    
    private suspend fun syncDeletedItem(entity: com.example.shoppinglist.data.local.entities.ShoppingItemEntity) {
        try {
            firestore.collection("groups").document(entity.groupId)
                .collection("items").document(entity.id)
                .delete()
                .await()
            dao.deleteItemPermanently(entity.id)
        } catch (e: Exception) {}
    }
}
