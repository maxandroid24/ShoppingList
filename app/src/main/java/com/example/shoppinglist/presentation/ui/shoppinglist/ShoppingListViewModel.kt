package com.example.shoppinglist.presentation.ui.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.domain.models.ShoppingGroup
import com.example.shoppinglist.domain.models.ShoppingItem
import com.example.shoppinglist.domain.repositories.AuthRepository
import com.example.shoppinglist.domain.repositories.GroupRepository
import com.example.shoppinglist.domain.usecases.AddShoppingItemUseCase
import com.example.shoppinglist.domain.usecases.CheckDuplicateItemUseCase
import com.example.shoppinglist.domain.usecases.DeleteItemUseCase
import com.example.shoppinglist.domain.usecases.GetShoppingListUseCase
import com.example.shoppinglist.domain.usecases.UpdateItemUseCase
import com.example.shoppinglist.domain.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val getShoppingListUseCase: GetShoppingListUseCase,
    private val addShoppingItemUseCase: AddShoppingItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val checkDuplicateItemUseCase: CheckDuplicateItemUseCase,
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _items = MutableStateFlow<Resource<List<ShoppingItem>>>(Resource.Loading)
    val items: StateFlow<Resource<List<ShoppingItem>>> = _items.asStateFlow()

    private val _group = MutableStateFlow<Resource<ShoppingGroup>?>(null)
    val group: StateFlow<Resource<ShoppingGroup>?> = _group.asStateFlow()

    private var currentGroupId: String = ""
    private var currentUser: Pair<String, String> = Pair("", "")

    fun init(groupId: String, userId: String, userName: String) {
        currentGroupId = groupId
        currentUser = Pair(userId, userName)
        loadItems()
        loadGroup()
    }

    private fun loadItems() {
        viewModelScope.launch {
            getShoppingListUseCase(currentGroupId).collectLatest { resource ->
                _items.value = resource
            }
        }
    }

    private fun loadGroup() {
        viewModelScope.launch {
            _group.value = groupRepository.getGroupById(currentGroupId)
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            authRepository.removeGroupFromUser(currentGroupId)
        }
    }

    suspend fun checkDuplicate(name: String): ShoppingItem? {
        val currentList = (_items.value as? Resource.Success)?.data ?: emptyList()
        return checkDuplicateItemUseCase(name, currentList)
    }

    fun addItem(name: String, quantity: Int) {
        viewModelScope.launch {
            val item = ShoppingItem(
                name = name,
                quantity = quantity,
                addedByUserId = currentUser.first,
                addedByUserName = currentUser.second,
                groupId = currentGroupId
            )
            addShoppingItemUseCase(item)
        }
    }

    fun increaseQuantity(item: ShoppingItem, amount: Int) {
        viewModelScope.launch {
            val updatedItem = item.copy(quantity = item.quantity + amount)
            updateItemUseCase(updatedItem)
        }
    }

    fun toggleBoughtStatus(item: ShoppingItem) {
        viewModelScope.launch {
            val updatedItem = item.copy(isBought = !item.isBought)
            updateItemUseCase(updatedItem)
        }
    }

    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            updateItemUseCase(item)
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            deleteItemUseCase(item)
        }
    }
}
