package com.example.fantahelpfe.data

import com.example.fantahelpfe.ui.auction.AuctionSettings
import javax.inject.Inject


// Dummy implementation for now for UserPreferencesRepository
class UserPreferencesRepositoryImpl @Inject constructor() : UserPreferencesRepository {
    private var currentSettings = AuctionSettings() // In-memory for this dummy
    override suspend fun loadAuctionSettings(): AuctionSettings {
        return currentSettings
    }

    override suspend fun saveAuctionSettings(settings: AuctionSettings) {
        currentSettings = settings
    }
}