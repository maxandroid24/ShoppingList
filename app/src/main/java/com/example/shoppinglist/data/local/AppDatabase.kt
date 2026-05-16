package com.example.shoppinglist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.shoppinglist.data.local.entities.ShoppingItemEntity

@Database(entities = [ShoppingItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingItemDao(): ShoppingItemDao
}
