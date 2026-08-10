package com.example.fantahelpfe.data.mapper

import com.example.fantahelpfe.data.remote.dto.create.PlayerCreateDto
import com.example.fantahelpfe.data.remote.dto.read.PlayerReadDto
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.model.PlayerRole
import kotlin.text.uppercase

fun PlayerReadDto.toDomain(): Player {
    val rawReg = this.regularity ?: this.regularness
    val parsedRegularness = when {
        rawReg > 0.0 && rawReg <= 1.0 -> (rawReg * 100).toInt()
        else -> rawReg.toInt()
    }

    return Player(
        id = this.id,
        name = this.name,
        squad = this.squad,
        role = when (this.role.uppercase()) {
            "P" -> PlayerRole.P
            "D" -> PlayerRole.D
            "C" -> PlayerRole.C
            "A" -> PlayerRole.A
            else -> PlayerRole.UNKNOWN
        },
        price = this.price,
        rating = this.rating,
        regularness = parsedRegularness,
        fvm = this.fvm,
        expectedPerformance = this.expectedPerformance,
        expectedStd = this.expectedStd,
        expectedPrice = this.expectedPrice,
    )
}

fun List<PlayerReadDto>.toDomainPlayers(): List<Player> = this.map { it.toDomain() }

fun Player.toCreateDto(
    // TODO: placeholder right now, but usually players are not created through the android app
    myRating: Float?, // Placeholder - needs definition
    squadNameForApi: String, // Example: map teamId to the expected "squad" string
    mateValueForApi: String, // Placeholder
    regularnessValueForApi: Int, // Placeholder
    fvmValueForApi: Int, // Placeholder
    expMfForApi: Float, // Placeholder
    expPriceForApi: Int, // Placeholder
    expStdForApi: Int // Placeholder
): PlayerCreateDto {
    val roleString = when (this.role) {
        PlayerRole.P -> "P"
        PlayerRole.D -> "D"
        PlayerRole.C -> "C"
        PlayerRole.A -> "A"
        PlayerRole.UNKNOWN -> "UNKNOWN" // Or handle as error
    }

    return PlayerCreateDto(
        // id = this.id, // IF your API expects your system's ID when creating (unusual)
        // OR if CreateDto.id refers to an external ID:
        id = this.id,
        role = roleString,
        name = this.name,
        squad = squadNameForApi, // This needs to be resolved. Is it team name? team API ID?
        price = this.price, // Or initialPrice, depending on creation logic
        myRating = myRating ?: 0f,
        mate = mateValueForApi,          // Placeholder - needs definition
        regularness = regularnessValueForApi, // Placeholder - needs definition
        fvm = fvmValueForApi,             // Placeholder - needs definition
        expMf = expMfForApi,              // Placeholder - needs definition
        expPrice = expPriceForApi,        // Placeholder - needs definition
        expStd = expStdForApi             // Placeholder - needs definition
    )
}
