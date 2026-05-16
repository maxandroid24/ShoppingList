package com.example.shoppinglist.presentation.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.domain.models.ShoppingGroup
import com.example.shoppinglist.domain.repositories.AuthRepository
import com.example.shoppinglist.domain.repositories.GroupRepository
import com.example.shoppinglist.domain.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _groupState = MutableStateFlow<Resource<ShoppingGroup>?>(null)
    val groupState: StateFlow<Resource<ShoppingGroup>?> = _groupState.asStateFlow()

    private val _userGroups = MutableStateFlow<Resource<List<ShoppingGroup>>>(Resource.Loading)
    val userGroups: StateFlow<Resource<List<ShoppingGroup>>> = _userGroups.asStateFlow()

    fun loadUserGroups(groupIds: List<String>) {
        viewModelScope.launch {
            _userGroups.value = Resource.Loading
            _userGroups.value = groupRepository.getGroupsByIds(groupIds)
        }
    }

    fun createGroup(groupName: String, userId: String, userName: String) {
        viewModelScope.launch {
            _groupState.value = Resource.Loading
            val result = groupRepository.createGroup(groupName, userId, userName)
            if (result is Resource.Success) {
                authRepository.updateDisplayName(userName)
                authRepository.addGroupToUser(result.data.id)
            }
            _groupState.value = result
        }
    }

    fun joinGroup(inviteCode: String, userId: String, userName: String) {
        viewModelScope.launch {
            _groupState.value = Resource.Loading
            val result = groupRepository.joinGroup(inviteCode, userId, userName)
            if (result is Resource.Success) {
                authRepository.updateDisplayName(userName)
                authRepository.addGroupToUser(result.data.id)
            }
            _groupState.value = result
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            authRepository.removeGroupFromUser(groupId)
        }
    }
}
