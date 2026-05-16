package com.example.shoppinglist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.shoppinglist.data.local.entities.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items WHERE groupId = :groupId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getItemsForGroup(groupId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE groupId = :groupId AND isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getItemsForGroupSync(groupId: String): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItemEntity)

    @Update
    suspend fun updateItem(item: ShoppingItemEntity)

    @Query("SELECT * FROM shopping_items WHERE needsSync = 1")
    suspend fun getUnsyncedItems(): List<ShoppingItemEntity>
    
    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteItemPermanently(itemId: String)

    @Query("UPDATE shopping_items SET isDeleted = 1, needsSync = 1 WHERE id = :itemId")
    suspend fun markItemAsDeleted(itemId: String)
    
    @Query("SELECT * FROM shopping_items WHERE name = :name AND groupId = :groupId AND isDeleted = 0 LIMIT 1")
    suspend fun getItemByName(name: String, groupId: String): ShoppingItemEntity?
}
