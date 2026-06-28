package com.example.fantahelpfe.model

data class League (
    val id: Int,
    val name: String,
    val initialBudget: Int,
    val teams: List<Team>
)