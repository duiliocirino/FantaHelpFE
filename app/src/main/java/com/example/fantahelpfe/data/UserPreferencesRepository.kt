package com.example.fantahelpfe.data

import com.example.fantahelpfe.ui.auction.AuctionSettings

interface UserPreferencesRepository {
    // Option 1: Expose a Flow (recommended)
    // val auctionSettingsFlow: StateFlow<AuctionSettings>

    // Option 2: Simple suspend functions (as used in VM init above for simplicity)
    suspend fun loadAuctionSettings(): AuctionSettings
    suspend fun saveAuctionSettings(settings: AuctionSettings)
}