package com.example.fantahelpfe.data.remote.dto.read

import com.squareup.moshi.Json

data class UserReadDto(
    @Json(name = "id") val id: Int,
    @Json(name = "userName") val userName: String,
    @Json(name = "email") val email: String
)