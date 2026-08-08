package com.example.fantahelpfe.ui.auction

import androidx.lifecycle.ViewModel
import com.example.fantahelpfe.data.AuctionSessionManager
import com.example.fantahelpfe.data.AuctionSettings
import com.example.fantahelpfe.data.remote.dto.LineUp
import com.example.fantahelpfe.model.League
import com.example.fantahelpfe.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuctionSharedViewModel @Inject constructor(
    private val auctionSessionManager: AuctionSessionManager
) : ViewModel() {

    val settings: StateFlow<AuctionSettings> = auctionSessionManager.settings
    val currentLeagueId: StateFlow<Int?> = auctionSessionManager.currentLeagueId
    val leagueDetails: StateFlow<League?> = auctionSessionManager.leagueDetails
    val refreshEvent: SharedFlow<Unit> = auctionSessionManager.refreshEvent
    val selectedPlayerForBuy: StateFlow<Player?> = auctionSessionManager.selectedPlayerForBuy

    fun initializeForLeague(leagueId: Int) {
        auctionSessionManager.initializeForLeague(leagueId)
    }

    fun refreshLeagueDetails() {
        auctionSessionManager.refreshLeagueDetails()
    }

    fun refreshAll() {
        auctionSessionManager.refreshAll()
    }

    fun updateGlobalFavoritePlayerIds(playerIds: List<Int>) {
        auctionSessionManager.updateGlobalFavoritePlayerIds(playerIds)
    }

    fun addGlobalFavoritePlayer(playerId: Int) {
        auctionSessionManager.addGlobalFavoritePlayer(playerId)
    }

    fun removeGlobalFavoritePlayer(playerId: Int) {
        auctionSessionManager.removeGlobalFavoritePlayer(playerId)
    }

    fun updateCreditsDistributionMultiplier(multiplier: Int) {
        auctionSessionManager.updateCreditsDistributionMultiplier(multiplier)
    }

    fun updateLineUp(lineUp: LineUp) {
        auctionSessionManager.updateLineUp(lineUp)
    }

    fun selectPlayerForBuy(player: Player) {
        auctionSessionManager.selectPlayerForBuy(player)
    }

    fun clearSelectedPlayerForBuy() {
        auctionSessionManager.clearSelectedPlayerForBuy()
    }
}
