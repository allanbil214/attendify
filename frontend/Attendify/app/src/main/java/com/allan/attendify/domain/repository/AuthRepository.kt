package com.allan.attendify.domain.repository

import com.allan.attendify.data.remote.dto.LoginRequest
import com.allan.attendify.data.remote.dto.LoginResponse
import com.allan.attendify.data.remote.dto.RegisterRequest
import com.allan.attendify.data.remote.dto.RegisterResponse
import com.allan.attendify.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(request: LoginRequest): Result<LoginResponse>
    suspend fun register(request: RegisterRequest): Result<RegisterResponse>
    suspend fun logout()
    fun getCurrentUser(): Flow<User?>
    fun isLoggedIn(): Boolean
}
