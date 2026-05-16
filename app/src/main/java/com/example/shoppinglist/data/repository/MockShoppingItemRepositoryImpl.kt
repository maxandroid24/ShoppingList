package com.example.shoppinglist.data.repository

import android.content.Context
import com.example.shoppinglist.data.local.ShoppingItemDao
import com.example.shoppinglist.data.mappers.toDomain
import com.example.shoppinglist.data.mappers.toEntity
import com.example.shoppinglist.domain.models.ShoppingItem
import com.example.shoppinglist.domain.repositories.ShoppingItemRepository
import com.example.shoppinglist.domain.utils.Resource
import com.example.shoppinglist.presentation.widget.WidgetUpdateHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MockShoppingItemRepositoryImpl @Inject constructor(
    private val dao: ShoppingItemDao,
    @ApplicationContext private val context: Context
) : ShoppingItemRepository {

    override fun getItems(groupId: String): Flow<Resource<List<ShoppingItem>>> {
        return dao.getItemsForGroup(groupId).map { entities ->
            Resource.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun addItem(item: ShoppingItem): Resource<Unit> {
        return try {
            dao.insertItem(item.toEntity(needsSync = false))
            // Force widget refresh
            WidgetUpdateHelper.updateWidgets(context)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun updateItem(item: ShoppingItem): Resource<Unit> {
        return try {
            dao.updateItem(item.toEntity(needsSync = false))
            WidgetUpdateHelper.updateWidgets(context)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun deleteItem(item: ShoppingItem): Resource<Unit> {
        return try {
            dao.deleteItemPermanently(item.id)
            WidgetUpdateHelper.updateWidgets(context)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    override suspend fun syncOfflineData() {
        // No-op for mock
    }
}
