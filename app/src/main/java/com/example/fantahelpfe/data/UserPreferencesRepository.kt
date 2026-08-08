package com.example.fantahelpfe.data

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val auctionSettingsFlow: Flow<AuctionSettings>

    // Option 2: Simple suspend functions (as used in VM init above for simplicity)
    suspend fun loadAuctionSettings(): AuctionSettings
    suspend fun saveAuctionSettings(settings: AuctionSettings)
}