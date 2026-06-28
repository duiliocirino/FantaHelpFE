package com.example.fantahelpfe.ui.leagues
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.model.League
import com.example.fantahelpfe.data.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LeaguesUiState {
    data object Loading : LeaguesUiState()
    data class Success(val leagues: List<League>) : LeaguesUiState()
    data class Error(val message: String) : LeaguesUiState()
}

@HiltViewModel
class LoadLeaguesViewModel @Inject constructor(
    private val dataRepository: DataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeaguesUiState>(LeaguesUiState.Loading)
    val uiState: StateFlow<LeaguesUiState> = _uiState

    init {
        fetchLeagues()
    }

    private fun fetchLeagues() {
        viewModelScope.launch {
            _uiState.value = LeaguesUiState.Loading
            try {
                _uiState.value = LeaguesUiState.Success(dataRepository.getAllLeagues())
            } catch (e: Exception) {
                _uiState.value = LeaguesUiState.Error("Failed to load leagues: ${e.message}")
            }
        }
    }
}