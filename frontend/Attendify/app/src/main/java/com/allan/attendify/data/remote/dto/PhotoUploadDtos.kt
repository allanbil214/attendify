package com.allan.attendify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PhotoUploadResponse(
    val success: Boolean,
    val message: String,
    val data: PhotoUploadData?
)

data class PhotoUploadData(
    val filename: String,
    val url: String,
    val size: Long
)
