package com.example.fantahelpfe.data.remote.dto

import com.squareup.moshi.Json

data class GoalBonusUpdateDto(
    @Json(name = "goalBonusPerRole") val goalBonusPerRole: Boolean
)
