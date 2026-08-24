package com.example.fantahelpfe.model

// Represents the role of a player
enum class PlayerRole {
    P, // Portiere (P)
    D,   // Difensore (D)
    C, // Centrocampista (C)
    A,    // Attaccante (A)
    UNKNOWN
}

data class Player (
    val id: Int,
    val name: String,
    val squad: String,
    val role: PlayerRole,
    val price: Int,
    val rating: Double,
    val regularness: Int,
    val fvm: Int,
    val expectedPerformance: Double,
    val expectedStd: Double,
    val expectedPrice: Int,
    val age: Int? = null,
    val integrity: Int? = null
)
