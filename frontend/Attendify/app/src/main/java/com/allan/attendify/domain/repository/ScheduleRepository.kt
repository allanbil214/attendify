package com.allan.attendify.domain.repository

import com.allan.attendify.data.remote.dto.TodayScheduleResponse

interface ScheduleRepository {
    suspend fun getTodaySchedule(): Result<TodayScheduleResponse>
}
