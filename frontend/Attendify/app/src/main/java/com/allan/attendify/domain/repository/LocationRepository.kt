package com.allan.attendify.domain.repository

import com.allan.attendify.data.remote.dto.LocationsResponse
import com.allan.attendify.domain.model.Location

interface LocationRepository {
    suspend fun getLocations(): Result<List<Location>>
    suspend fun getNearbyLocations(latitude: Double, longitude: Double, maxDistance: Double?): Result<List<Location>>
}
