package com.example.shoppinglist.di

import com.example.shoppinglist.data.repository.AuthRepositoryImpl
import com.example.shoppinglist.data.repository.GroupRepositoryImpl
import com.example.shoppinglist.data.repository.ShoppingItemRepositoryImpl
import com.example.shoppinglist.domain.repositories.AuthRepository
import com.example.shoppinglist.domain.repositories.GroupRepository
import com.example.shoppinglist.domain.repositories.ShoppingItemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(
        groupRepositoryImpl: GroupRepositoryImpl
    ): GroupRepository

    @Binds
    @Singleton
    abstract fun bindShoppingItemRepository(
        shoppingItemRepositoryImpl: ShoppingItemRepositoryImpl
    ): ShoppingItemRepository
}
