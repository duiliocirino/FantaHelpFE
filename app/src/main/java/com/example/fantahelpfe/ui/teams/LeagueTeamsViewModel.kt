package com.example.fantahelpfe.ui.teams

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.model.Team
import com.example.fantahelpfe.data.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TeamsUiState {
    data object Loading : TeamsUiState()
    data class Success(val teams: List<Team>) : TeamsUiState()
    data class Error(val message: String) : TeamsUiState()
}

@HiltViewModel
class LeagueTeamsViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    savedStateHandle: SavedStateHandle // Hilt provides this to access navigation arguments
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeamsUiState>(TeamsUiState.Loading)
    val uiState: StateFlow<TeamsUiState> = _uiState

    private val leagueId: Int = checkNotNull(savedStateHandle["leagueId"])

    init {
        fetchTeams()
    }

    private fun fetchTeams() {
        viewModelScope.launch {
            _uiState.value = TeamsUiState.Loading
            try {
                _uiState.value = TeamsUiState.Success(dataRepository.getTeamsForLeague(leagueId))
            } catch (e: Exception) {
                _uiState.value = TeamsUiState.Error("Failed to load teams: ${e.message}")
            }
        }
    }
}