package com.example.shoppinglist.domain.utils

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val exception: Exception, val message: String = exception.message ?: "Unknown error") : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
