package com.example.fantahelpfe.ui.auction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.AuctionSessionManager
import com.example.fantahelpfe.data.AuctionSettings
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.data.remote.dto.BudgetAllocation
import com.example.fantahelpfe.data.remote.dto.LineUp
import com.example.fantahelpfe.model.League
import com.example.fantahelpfe.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuctionSharedViewModel @Inject constructor(
    private val auctionSessionManager: AuctionSessionManager,
    private val repository: DataRepository
) : ViewModel() {

    val settings: StateFlow<AuctionSettings> = auctionSessionManager.settings
    val currentLeagueId: StateFlow<Int?> = auctionSessionManager.currentLeagueId
    val leagueDetails: StateFlow<League?> = auctionSessionManager.leagueDetails
    val refreshEvent: SharedFlow<Unit> = auctionSessionManager.refreshEvent
    val selectedPlayerForBuy: StateFlow<Player?> = auctionSessionManager.selectedPlayerForBuy

    private val _allPlayers = MutableStateFlow<List<Player>>(emptyList())
    val allPlayers: StateFlow<List<Player>> = _allPlayers.asStateFlow()

    init {
        loadAllPlayers()
    }

    fun loadAllPlayers() {
        viewModelScope.launch {
            try {
                _allPlayers.value = repository.getAllPlayers()
            } catch (_: Exception) { }
        }
    }

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

    fun updateCreditsDistributionStrategy(multiplier: Int, budgetAllocation: BudgetAllocation) {
        auctionSessionManager.updateCreditsDistributionStrategy(multiplier, budgetAllocation)
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
