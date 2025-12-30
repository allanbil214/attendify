package com.allan.attendify.data.repository

import com.allan.attendify.data.remote.ApiService
import com.allan.attendify.domain.model.Location
import com.allan.attendify.domain.repository.LocationRepository
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : LocationRepository {

    override suspend fun getLocations(): Result<List<Location>> {
        return try {
            val response = apiService.getLocations()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!.data
                val locations = dtos.map { dto ->
                    Location(
                        id = dto.id,
                        name = dto.name,
                        address = dto.address,
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        radius = dto.radius
                    )
                }
                Result.success(locations)
            } else {
                Result.failure(Exception("Failed to get locations: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNearbyLocations(
        latitude: Double,
        longitude: Double,
        maxDistance: Double?
    ): Result<List<Location>> {
        return try {
            val response = apiService.getNearbyLocations(latitude, longitude, maxDistance)
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!.data
                val locations = dtos.map { dto ->
                    Location(
                        id = dto.id,
                        name = dto.name,
                        address = dto.address,
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        radius = dto.radius
                    )
                }
                Result.success(locations)
            } else {
                Result.failure(Exception("Failed to get nearby locations: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
