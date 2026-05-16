package com.example.shoppinglist.domain.usecases

import com.example.shoppinglist.domain.models.ShoppingItem
import com.example.shoppinglist.domain.repositories.ShoppingItemRepository
import com.example.shoppinglist.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetShoppingListUseCase @Inject constructor(
    private val repository: ShoppingItemRepository
) {
    operator fun invoke(groupId: String): Flow<Resource<List<ShoppingItem>>> {
        return repository.getItems(groupId)
    }
}

class AddShoppingItemUseCase @Inject constructor(
    private val repository: ShoppingItemRepository
) {
    suspend operator fun invoke(item: ShoppingItem): Resource<Unit> {
        return repository.addItem(item)
    }
}

class UpdateItemUseCase @Inject constructor(
    private val repository: ShoppingItemRepository
) {
    suspend operator fun invoke(item: ShoppingItem): Resource<Unit> {
        return repository.updateItem(item)
    }
}

class DeleteItemUseCase @Inject constructor(
    private val repository: ShoppingItemRepository
) {
    suspend operator fun invoke(item: ShoppingItem): Resource<Unit> {
        return repository.deleteItem(item)
    }
}

class CheckDuplicateItemUseCase @Inject constructor(
    private val repository: ShoppingItemRepository
) {
    suspend operator fun invoke(name: String, currentList: List<ShoppingItem>): ShoppingItem? {
        return currentList.find { it.name.equals(name, ignoreCase = true) }
    }
}
