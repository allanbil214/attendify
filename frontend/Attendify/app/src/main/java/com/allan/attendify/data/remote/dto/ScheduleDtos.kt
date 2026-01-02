package com.allan.attendify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TodayScheduleResponse(
    val success: Boolean,
    val data: ScheduleDto?
)

data class ScheduleDto(
    @SerializedName("employee_type") val employeeType: String,
    @SerializedName("day_of_week") val dayOfWeek: Int,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?,
    @SerializedName("is_working_day") val isWorkingDay: Boolean,
    val message: String
)
