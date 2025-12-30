package com.allan.attendify.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.allan.attendify.data.local.dao.AttendanceDao
import com.allan.attendify.data.remote.ApiService
import com.allan.attendify.data.remote.dto.CheckInRequest
import com.allan.attendify.data.remote.dto.CheckOutRequest
import com.allan.attendify.data.remote.dto.DeviceInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val attendanceDao: AttendanceDao,
    private val apiService: ApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val unsyncedRecords = attendanceDao.getUnsyncedAttendance()

            for (record in unsyncedRecords) {
                // Determine if it's a check-in or check-out based on fields
                // This is a simplification. Ideally, we store the "type" or request payload.
                // Reconstructing request from entity:
                
                if (record.checkOutTime == null && record.status == "pending_sync_in") {
                    // It was a check-in attempt
                    val request = CheckInRequest(
                        locationId = record.locationId,
                        latitude = record.checkInLatitude,
                        longitude = record.checkInLongitude,
                        note = null, // Note might need to be stored in entity if we want to sync it
                        deviceInfo = DeviceInfo("Unknown", "Android", "1.0.0") // Placeholder
                    )
                    
                    try {
                        val response = apiService.checkIn(request)
                        if (response.isSuccessful) {
                            // Update local record with server ID and synced status
                             val dto = response.body()!!.data
                             val updated = record.copy(
                                 id = dto.id, // Update with real ID
                                 isSynced = true,
                                 status = dto.status
                             )
                             attendanceDao.insertAttendance(updated)
                             // We might need to delete the old temp ID record if ID changed
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Similar logic for check-out...
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
