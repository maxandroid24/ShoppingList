package com.example.shoppinglist.presentation.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.domain.models.User
import com.example.shoppinglist.domain.repositories.AuthRepository
import com.example.shoppinglist.domain.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        authRepository.getCurrentUserSync()
    )

    private val _authState = MutableStateFlow<Resource<User>?>(null)
    val authState: StateFlow<Resource<User>?> = _authState.asStateFlow()

    fun signInAnonymously() {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            _authState.value = authRepository.signInAnonymously()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun setActiveGroup(groupId: String) {
        viewModelScope.launch {
            authRepository.setActiveGroupId(groupId)
        }
    }
}
