package com.allan.attendify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val organizationName: String,
    val phone: String?,
    val avatarUrl: String?
)
