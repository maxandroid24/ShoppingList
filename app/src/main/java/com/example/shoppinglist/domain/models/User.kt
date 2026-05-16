package com.example.shoppinglist.domain.models

data class User(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val groupIds: List<String> = emptyList()
)
