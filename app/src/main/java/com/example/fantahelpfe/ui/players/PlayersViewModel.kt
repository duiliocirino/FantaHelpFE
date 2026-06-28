package com.example.fantahelpfe.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// A sealed class to represent the different states of our UI
sealed class PlayerUiState {
    data object Loading : PlayerUiState()
    data class Success(val players: List<Player>) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

@HiltViewModel
class PlayersViewModel @Inject constructor(
    private val dataRepository: DataRepository
) : ViewModel() {

    // Private mutable state that can be changed within the ViewModel
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    // Public immutable state that the UI can observe
    val uiState: StateFlow<PlayerUiState> = _uiState

    init {
        // Fetch the players as soon as the ViewModel is created
        fetchPlayers()
    }

    private fun fetchPlayers() {
        // viewModelScope is a coroutine scope that is automatically cancelled
        // when the ViewModel is destroyed.
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            try {
                val players = dataRepository.getAllPlayers()
                _uiState.value = PlayerUiState.Success(players)
            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error("Failed to load players: ${e.message}")
                throw e
            }
        }
    }
}