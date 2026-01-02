package com.allan.attendify.data.remote

import com.allan.attendify.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // Attendance
    @GET("attendance/today")
    suspend fun getTodayAttendance(): Response<AttendanceResponse>

    @POST("attendance/check-in")
    suspend fun checkIn(@Body request: CheckInRequest): Response<AttendanceActionResponse>

    @POST("attendance/check-out")
    suspend fun checkOut(@Body request: CheckOutRequest): Response<AttendanceActionResponse>

    @GET("attendance/history")
    suspend fun getAttendanceHistory(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("start_date") startDate: String?,
        @Query("end_date") endDate: String?
    ): Response<AttendanceHistoryResponse>

    // Locations
    @GET("locations")
    suspend fun getLocations(): Response<LocationsResponse>

    @GET("locations/nearby")
    suspend fun getNearbyLocations(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("max_distance") maxDistance: Double? = null
    ): Response<LocationsResponse>

    // Upload
    @Multipart
    @POST("upload/photo")
    suspend fun uploadPhoto(
        @Part photo: MultipartBody.Part
    ): Response<PhotoUploadResponse>

    // Schedule
    @GET("schedules/today")
    suspend fun getTodaySchedule(): Response<TodayScheduleResponse>
}
