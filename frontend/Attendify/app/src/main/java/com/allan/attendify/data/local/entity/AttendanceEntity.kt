package com.allan.attendify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val locationId: String,
    val checkInTime: Long,
    val checkOutTime: Long?,
    val checkInLatitude: Double,
    val checkInLongitude: Double,
    val checkOutLatitude: Double?,
    val checkOutLongitude: Double?,
    val status: String,
    val isLate: Boolean,
    val locationName: String?,
    val locationAddress: String?,
    val isSynced: Boolean = true // To track offline records
)
