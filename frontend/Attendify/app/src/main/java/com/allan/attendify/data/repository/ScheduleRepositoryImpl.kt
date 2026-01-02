package com.allan.attendify.data.repository

import com.allan.attendify.data.remote.ApiService
import com.allan.attendify.data.remote.dto.TodayScheduleResponse
import com.allan.attendify.domain.repository.ScheduleRepository
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ScheduleRepository {

    override suspend fun getTodaySchedule(): Result<TodayScheduleResponse> {
        return try {
            val response = apiService.getTodaySchedule()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get schedule: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
