package com.allan.attendify.domain.model

import java.util.Date

data class Attendance(
    val id: String,
    val userId: String,
    val locationId: String,
    val checkInTime: Date,
    val checkOutTime: Date? = null,
    val checkInLatitude: Double,
    val checkInLongitude: Double,
    val checkOutLatitude: Double? = null,
    val checkOutLongitude: Double? = null,
    val status: String,
    val isLate: Boolean,
    val checkInNote: String? = null,
    val checkOutNote: String? = null,
    val locationName: String? = null,
    val locationAddress: String? = null
)
