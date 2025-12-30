package com.allan.attendify.domain.repository

import com.allan.attendify.data.remote.dto.AttendanceActionResponse
import com.allan.attendify.data.remote.dto.AttendanceResponse
import com.allan.attendify.data.remote.dto.CheckInRequest
import com.allan.attendify.data.remote.dto.CheckOutRequest
import com.allan.attendify.domain.model.Attendance

interface AttendanceRepository {
    suspend fun getTodayAttendance(): Result<AttendanceResponse>
    suspend fun checkIn(request: CheckInRequest): Result<AttendanceActionResponse>
    suspend fun checkOut(request: CheckOutRequest): Result<AttendanceActionResponse>
    suspend fun getAttendanceHistory(page: Int, limit: Int, startDate: String?, endDate: String?): Result<List<Attendance>>
}
