package com.allan.attendify.data.repository

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.allan.attendify.data.local.dao.AttendanceDao
import com.allan.attendify.data.local.entity.AttendanceEntity
import com.allan.attendify.data.remote.ApiService
import com.allan.attendify.data.remote.dto.AttendanceActionResponse
import com.allan.attendify.data.remote.dto.AttendanceDto
import com.allan.attendify.data.remote.dto.AttendanceResponse
import com.allan.attendify.data.remote.dto.CheckInRequest
import com.allan.attendify.data.remote.dto.CheckOutRequest
import com.allan.attendify.data.worker.SyncWorker
import com.allan.attendify.domain.model.Attendance
import com.allan.attendify.domain.repository.AttendanceRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

class AttendanceRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val attendanceDao: AttendanceDao,
    private val workManager: WorkManager
) : AttendanceRepository {

    override suspend fun getTodayAttendance(): Result<AttendanceResponse> {
        return try {
            val response = apiService.getTodayAttendance()
            if (response.isSuccessful && response.body() != null) {
                // Cache today's attendance if needed, or just return
                Result.success(response.body()!!)
            } else {
                // If offline, maybe try to get latest from DB? 
                // For MVP, we'll return failure if we can't fetch fresh status, 
                // or rely on what we persisted last time.
                Result.failure(Exception("Failed to get attendance: ${response.code()}"))
            }
        } catch (e: Exception) {
             // In a full app, we would query the local DB for the latest record for today
            Result.failure(e)
        }
    }

    override suspend fun checkIn(request: CheckInRequest): Result<AttendanceActionResponse> {
        return try {
            val response = apiService.checkIn(request)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!.data
                saveAttendanceToDb(dto, isSynced = true)
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Check-in failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Offline Mode
            val offlineId = UUID.randomUUID().toString()
            val now = Date()
            val entity = AttendanceEntity(
                id = offlineId,
                userId = "current_user", // We might need to fetch this from prefs or DB
                locationId = request.locationId,
                checkInTime = now.time,
                checkOutTime = null,
                checkInLatitude = request.latitude,
                checkInLongitude = request.longitude,
                checkOutLatitude = null,
                checkOutLongitude = null,
                status = "pending_sync_in",
                isLate = false,
                locationName = "Offline Location",
                locationAddress = "Pending Sync",
                isSynced = false
            )
            attendanceDao.insertAttendance(entity)
            scheduleSync()
            
            // Return a fake success response for UI
            val fakeDto = AttendanceDto(
                id = offlineId,
                userId = "current_user",
                locationId = request.locationId,
                checkInTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(now),
                checkOutTime = null,
                checkInLatitude = request.latitude,
                checkInLongitude = request.longitude,
                status = "pending_sync_in",
                isLate = false,
                locationName = "Offline Location",
                locationAddress = "Pending Sync"
            )
            Result.success(AttendanceActionResponse(true, "Saved offline", fakeDto))
        }
    }

    override suspend fun checkOut(request: CheckOutRequest): Result<AttendanceActionResponse> {
        return try {
            val response = apiService.checkOut(request)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!.data
                saveAttendanceToDb(dto, isSynced = true)
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Check-out failed: ${response.code()}"))
            }
        } catch (e: Exception) {
             // Offline Mode
            // We need to find the active check-in to close it
            // For MVP, simplistic approach: just save a new record or update existing if we have ID
            // Since we might not have the server ID if it was offline check-in, this gets complex.
            // Simplified: Update the record with matching ID or create new pending sync out.
            
            // Assuming request.attendanceId matches local ID
            val now = Date()
             // Fetch existing to update?
             // attendanceDao.updateCheckout(...)
             
             // For now, just schedule sync. UI might be tricky.
             scheduleSync()
             Result.failure(Exception("Offline check-out not fully implemented in MVP but queued"))
        }
    }

    override suspend fun getAttendanceHistory(
        page: Int,
        limit: Int,
        startDate: String?,
        endDate: String?
    ): Result<List<Attendance>> {
        return try {
            val response = apiService.getAttendanceHistory(page, limit, startDate, endDate)
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!.data.records
                val domainModels = dtos.map { dto -> mapDtoToDomain(dto) }
                Result.success(domainModels)
            } else {
                Result.failure(Exception("Failed to fetch history: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Fallback to local DB could be implemented here
            Result.failure(e)
        }
    }

    private suspend fun saveAttendanceToDb(dto: AttendanceDto, isSynced: Boolean) {
        val entity = AttendanceEntity(
            id = dto.id,
            userId = dto.userId,
            locationId = dto.locationId,
            checkInTime = parseDate(dto.checkInTime),
            checkOutTime = dto.checkOutTime?.let { parseDate(it) },
            checkInLatitude = dto.checkInLatitude,
            checkInLongitude = dto.checkInLongitude,
            checkOutLatitude = null,
            checkOutLongitude = null,
            status = dto.status,
            isLate = dto.isLate,
            locationName = dto.locationName,
            locationAddress = dto.locationAddress,
            isSynced = isSynced
        )
        attendanceDao.insertAttendance(entity)
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
            
        workManager.enqueue(syncWork)
    }
    
    private fun mapDtoToDomain(dto: AttendanceDto): Attendance {
        return Attendance(
            id = dto.id,
            userId = dto.userId,
            locationId = dto.locationId,
            checkInTime = Date(parseDate(dto.checkInTime)),
            checkOutTime = dto.checkOutTime?.let { Date(parseDate(it)) },
            checkInLatitude = dto.checkInLatitude,
            checkInLongitude = dto.checkInLongitude,
            checkOutLatitude = null,
            checkOutLongitude = null,
            status = dto.status,
            isLate = dto.isLate,
            checkInNote = null,
            checkOutNote = null,
            locationName = dto.locationName,
            locationAddress = dto.locationAddress
        )
    }
    
    private fun parseDate(dateStr: String): Long {
        return try {
             // The server returns dates in UTC (indicated by 'Z' or zero offset)
             // We need to parse them as UTC so that Date() which stores time in epoch millis is correct.
             // When displayed in UI, the system's local timezone will be applied.
             
             // Check if millisecond precision exists
             val formatStr = if (dateStr.length > 19 && dateStr[19] == '.') {
                 "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
             } else {
                 "yyyy-MM-dd'T'HH:mm:ss"
             }
             
             val sdf = SimpleDateFormat(formatStr, Locale.getDefault())
             sdf.timeZone = TimeZone.getTimeZone("UTC") // Force UTC parsing
             
             sdf.parse(dateStr)?.time ?: Date().time
        } catch (e: Exception) {
            e.printStackTrace()
            Date().time
        }
    }
}
