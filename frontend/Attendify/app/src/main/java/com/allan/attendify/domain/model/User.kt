package com.allan.attendify.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val organizationName: String,
    val phone: String? = null,
    val avatarUrl: String? = null
)
