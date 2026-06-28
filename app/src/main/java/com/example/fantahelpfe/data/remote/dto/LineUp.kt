package com.example.fantahelpfe.data.remote.dto

import com.squareup.moshi.Json

data class LineUp(
    @Json(name = "keepers") val keepers: Int = 1,
    @Json(name = "defenders") val defenders: Int,
    @Json(name = "midfielders") val midfielders: Int,
    @Json(name = "attackers") val attackers: Int
)