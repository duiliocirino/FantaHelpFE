package com.example.fantahelpfe.data.remote.dto.create

import com.squareup.moshi.Json

data class UserCreateDto(
    @Json(name = "userName") val userName: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "isFake") val isFake: Boolean = false
)