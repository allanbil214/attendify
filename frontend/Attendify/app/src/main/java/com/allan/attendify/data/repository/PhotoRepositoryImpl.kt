package com.allan.attendify.data.repository

import com.allan.attendify.data.remote.ApiService
import com.allan.attendify.data.remote.dto.PhotoUploadResponse
import com.allan.attendify.domain.repository.PhotoRepository
import okhttp3.MultipartBody
import javax.inject.Inject

class PhotoRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : PhotoRepository {
    override suspend fun uploadPhoto(photo: MultipartBody.Part): Result<PhotoUploadResponse> {
        return try {
            val response = apiService.uploadPhoto(photo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Photo upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
