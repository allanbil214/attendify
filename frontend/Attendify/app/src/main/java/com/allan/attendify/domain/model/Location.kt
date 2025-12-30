package com.allan.attendify.domain.model

data class Location(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double
)
