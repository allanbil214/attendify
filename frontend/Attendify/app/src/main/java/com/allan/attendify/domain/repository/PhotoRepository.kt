package com.allan.attendify.domain.repository

import com.allan.attendify.data.remote.dto.PhotoUploadResponse
import okhttp3.MultipartBody

interface PhotoRepository {
    suspend fun uploadPhoto(photo: MultipartBody.Part): Result<PhotoUploadResponse>
}
