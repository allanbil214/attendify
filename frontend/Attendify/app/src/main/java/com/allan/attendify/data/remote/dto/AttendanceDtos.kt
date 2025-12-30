package com.allan.attendify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CheckInRequest(
    @SerializedName("location_id") val locationId: String,
    val latitude: Double,
    val longitude: Double,
    val note: String?,
    @SerializedName("device_info") val deviceInfo: DeviceInfo
)

data class DeviceInfo(
    val model: String,
    val os: String,
    @SerializedName("app_version") val appVersion: String
)

data class CheckOutRequest(
    @SerializedName("attendance_id") val attendanceId: String,
    val latitude: Double,
    val longitude: Double,
    val note: String?
)

data class AttendanceResponse(
    val success: Boolean,
    val data: AttendanceDto?
)

data class AttendanceActionResponse(
    val success: Boolean,
    val message: String,
    val data: AttendanceDto
)

// The structure of this response was incorrect based on the provided JSON
data class AttendanceHistoryResponse(
    val success: Boolean,
    val data: AttendanceHistoryData
)

data class AttendanceHistoryData(
    val records: List<AttendanceDto>,
    val pagination: PaginationMeta,
    val summary: SummaryMeta?
)

data class PaginationMeta(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_records") val totalRecords: Int,
    @SerializedName("per_page") val perPage: Int
)

data class SummaryMeta(
    @SerializedName("total_days") val totalDays: String,
    @SerializedName("present_days") val presentDays: String,
    @SerializedName("late_days") val lateDays: String,
    @SerializedName("average_duration") val averageDuration: String
)

data class AttendanceDto(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("location_id") val locationId: String,
    @SerializedName("check_in_time") val checkInTime: String,
    @SerializedName("check_out_time") val checkOutTime: String?,
    @SerializedName("check_in_latitude") val checkInLatitude: Double, // JSON has this as string, but keeping as Double for now assuming Gson converts
    @SerializedName("check_in_longitude") val checkInLongitude: Double,
    val status: String,
    @SerializedName("is_late") val isLate: Boolean,
    @SerializedName("location_name") val locationName: String?, // Changed from nested object to flat field
    @SerializedName("location_address") val locationAddress: String?
    // Added new fields based on JSON if needed later: check_in_note, check_out_note, device_info, etc.
)

data class LocationsResponse(
    val success: Boolean,
    val data: List<LocationDto>
)

data class LocationDto(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double
)
