package com.allan.attendify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData?
)

data class LoginData(
    val user: UserDto,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class UserDto(
    val id: String,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    val role: String,
    @SerializedName("organization_name") val organizationName: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("full_name") val fullName: String,
    val phone: String
)

data class RegisterResponse(
    val success: Boolean,
    val message: String
)
