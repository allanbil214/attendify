package com.allan.attendify.data.repository

import com.allan.attendify.data.local.TokenManager
import com.allan.attendify.data.local.dao.UserDao
import com.allan.attendify.data.local.entity.UserEntity
import com.allan.attendify.data.remote.ApiService
import com.allan.attendify.data.remote.dto.LoginRequest
import com.allan.attendify.data.remote.dto.LoginResponse
import com.allan.attendify.data.remote.dto.RegisterRequest
import com.allan.attendify.data.remote.dto.RegisterResponse
import com.allan.attendify.domain.model.User
import com.allan.attendify.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.data != null) {
                    // Save tokens
                    tokenManager.saveAccessToken(body.data.accessToken)
                    tokenManager.saveRefreshToken(body.data.refreshToken)

                    // Save user to DB
                    val userDto = body.data.user
                    val userEntity = UserEntity(
                        id = userDto.id,
                        email = userDto.email,
                        fullName = userDto.fullName,
                        role = userDto.role,
                        organizationName = userDto.organizationName,
                        phone = null, // Not provided in login response
                        avatarUrl = null
                    )
                    userDao.insertUser(userEntity)
                    
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.message))
                }
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(request: RegisterRequest): Result<RegisterResponse> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.message))
                }
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        tokenManager.clearTokens()
        userDao.clearUser()
    }

    override fun getCurrentUser(): Flow<User?> {
        return userDao.getUser().map { entity ->
            entity?.let {
                User(
                    id = it.id,
                    email = it.email,
                    fullName = it.fullName,
                    role = it.role,
                    organizationName = it.organizationName,
                    phone = it.phone,
                    avatarUrl = it.avatarUrl
                )
            }
        }
    }

    override fun isLoggedIn(): Boolean {
        return tokenManager.getAccessToken() != null
    }
}
