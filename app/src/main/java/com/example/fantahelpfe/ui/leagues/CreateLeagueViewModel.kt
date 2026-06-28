// In: com.example.fantahelpfe.ui.leagues.CreateLeagueViewModel.kt
package com.example.fantahelpfe.ui.leagues

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantahelpfe.data.DataRepository
import com.example.fantahelpfe.data.remote.dto.create.LeagueCreateDto
import com.example.fantahelpfe.data.remote.dto.create.TeamCreateDto
import com.example.fantahelpfe.model.League
import com.example.fantahelpfe.model.Team
import com.example.fantahelpfe.utils.YourAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// TeamDraft remains useful for the UI list before any API calls
data class TeamDraft(
    val name: String,
    var createdTeam: Team? = null, // Store the full Team domain model if successful
    var creationError: String? = null,
    var isBeingProcessed: Boolean = false // Generic processing flag
)

data class CreateLeagueScreenUiState(
    val leagueName: String = "",
    val initialBudget: String = "800",
    val currentTeamNameInput: String = "",
    val teamDrafts: List<TeamDraft> = emptyList(),
    val formError: String? = null,
    val creationStatus: CreationStatus = CreationStatus.Idle,
    val overallProgressMessage: String? = null
)

sealed interface CreationStatus {
    object Idle : CreationStatus
    object CreatingLeague : CreationStatus
    data class CreatingTeams(
        val leagueId: Int,
        val leagueName: String,
        val totalTeamsToCreate: Int,
        val teamsSuccessfullyCreated: Int,
        val currentTeamProcessing: String? = null // Name of the team currently being created
    ) : CreationStatus
    data class PartiallyCreated( // League created, but some teams failed
        val createdLeague: League,
        val successfullyCreatedTeams: List<Team>,
        val failedTeamDrafts: List<TeamDraft> // Drafts that couldn't be created
    ) : CreationStatus
    data class Success( // All steps completed successfully
        val createdLeague: League,
        val createdTeams: List<Team>
    ) : CreationStatus
    data class Error(val message: String, val stepFailed: String /* e.g. "league" or "team" */) : CreationStatus
}

@HiltViewModel
class CreateLeagueViewModel @Inject constructor(
    private val repository: DataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateLeagueScreenUiState())
    val uiState: StateFlow<CreateLeagueScreenUiState> = _uiState.asStateFlow()

    // --- Input Handlers (mostly unchanged) ---
    fun onLeagueNameChanged(name: String) {
        _uiState.update { it.copy(leagueName = name, formError = null, overallProgressMessage = null) }
    }

    fun onInitialBudgetChanged(budget: String) {
        if (budget.all { char -> char.isDigit() }) {
            _uiState.update { it.copy(initialBudget = budget, formError = null, overallProgressMessage = null) }
        }
    }

    fun onCurrentTeamNameInputChanged(teamName: String) {
        _uiState.update { it.copy(currentTeamNameInput = teamName, formError = null, overallProgressMessage = null) }
    }

    fun addTeamDraft() {
        val currentTeamName = _uiState.value.currentTeamNameInput.trim()
        if (currentTeamName.isEmpty()) {
            _uiState.update { it.copy(formError = "Team name cannot be empty.") }
            return
        }
        if (_uiState.value.teamDrafts.any { it.name.equals(currentTeamName, ignoreCase = true) }) {
            _uiState.update { it.copy(formError = "'$currentTeamName' has already been added.") }
            return
        }
        _uiState.update { currentState ->
            currentState.copy(
                teamDrafts = currentState.teamDrafts + TeamDraft(name = currentTeamName),
                currentTeamNameInput = "",
                formError = null,
                overallProgressMessage = null
            )
        }
    }

    fun removeTeamDraft(teamNameToRemove: String) {
        _uiState.update { currentState ->
            currentState.copy(
                teamDrafts = currentState.teamDrafts.filterNot { it.name == teamNameToRemove },
                overallProgressMessage = null
            )
        }
    }

    // --- Main Creation Logic (Scenario A: League First) ---
    fun submitLeagueAndTeams() {
        val currentState = _uiState.value
        val leagueNameInput = currentState.leagueName.trim()
        val initialBudgetInput = currentState.initialBudget.toIntOrNull()

        // --- Form Validation ---
        if (leagueNameInput.isEmpty()) {
            _uiState.update { it.copy(formError = "League name cannot be empty.") }
            return
        }
        if (initialBudgetInput == null || initialBudgetInput <= 0) {
            _uiState.update { it.copy(formError = "Initial budget must be a positive number.") }
            return
        }
        if (currentState.teamDrafts.isEmpty()) {
            _uiState.update { it.copy(formError = "Please add at least one team.") }
            return
        }
        // Clear previous errors and set initial loading state for league creation
        _uiState.update {
            it.copy(
                creationStatus = CreationStatus.CreatingLeague,
                formError = null,
                overallProgressMessage = "Creating league: $leagueNameInput..."
            )
        }

        viewModelScope.launch {
            lateinit var createdLeague: League
            try {
                // --- Step 1: Create the League ---
                val leagueCreateDto = LeagueCreateDto(
                    name = leagueNameInput,
                    initialBudget = initialBudgetInput
                )
                Log.d("ViewModel", "Creating league with DTO: $leagueCreateDto")
                createdLeague = repository.createLeague(leagueCreateDto)
                Log.d("ViewModel", "League created successfully: ID ${createdLeague.id}, Name: ${createdLeague.name}")

                // --- Step 2: Create Teams and Associate with the League ---
                if (currentState.teamDrafts.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            creationStatus = CreationStatus.CreatingTeams(
                                leagueId = createdLeague.id,
                                leagueName = createdLeague.name,
                                totalTeamsToCreate = currentState.teamDrafts.size,
                                teamsSuccessfullyCreated = 0
                            ),
                            overallProgressMessage = "Adding teams to '${createdLeague.name}'..."
                        )
                    }

                    val successfullyCreatedTeams = mutableListOf<Team>()
                    val failedTeamDraftsProcessing = mutableListOf<TeamDraft>() // To store drafts that failed

                    // Update teamDrafts to reset any previous errors/status
                    var processingTeamDrafts = currentState.teamDrafts.map { it.copy(creationError = null, createdTeam = null, isBeingProcessed = false) }
                    _uiState.update { it.copy(teamDrafts = processingTeamDrafts) }


                    // Create teams one by one or concurrently (concurrently is more complex for UI updates per team)
                    // For clearer per-team UI updates, sequential might be easier to manage state for,
                    // but concurrent is faster. Let's try concurrent with individual draft updates.
                    val teamCreationJobs = processingTeamDrafts.mapIndexed { index, draft ->
                        async {
                            _uiState.update { current -> // Mark specific draft as processing
                                val updatedDrafts = current.teamDrafts.toMutableList()
                                updatedDrafts[index] = updatedDrafts[index].copy(isBeingProcessed = true, creationError = null)
                                current.copy(
                                    teamDrafts = updatedDrafts,
                                    creationStatus = CreationStatus.CreatingTeams(
                                        leagueId = createdLeague.id,
                                        leagueName = createdLeague.name,
                                        totalTeamsToCreate = currentState.teamDrafts.size,
                                        teamsSuccessfullyCreated = successfullyCreatedTeams.size, // This count might lag slightly with async
                                        currentTeamProcessing = draft.name
                                    )
                                )
                            }
                            try {
                                val teamCreateDto = TeamCreateDto(
                                    name = draft.name,
                                    initialBudget = createdLeague.initialBudget,
                                    leagueId = createdLeague.id, // CRUCIAL: Associate team with the new league
                                    ownerId = 1 // TODO: change when we fully implement users
                                )
                                Log.d("ViewModel", "Creating team '${draft.name}' for league ID ${createdLeague.id}")
                                val newTeam = repository.createTeam(teamCreateDto)
                                Log.d("ViewModel", "Team '${newTeam.name}' (ID: ${newTeam.id}) created successfully.")
                                newTeam // Return the created team
                            } catch (teamError: YourAppError) {
                                Log.e("ViewModel", "Failed to create team '${draft.name}': ${teamError.getDetailedMessage()}")
                                // Update the specific draft with its error
                                _uiState.update { current ->
                                    val updatedDrafts = current.teamDrafts.toMutableList()
                                    updatedDrafts[index] = updatedDrafts[index].copy(
                                        isBeingProcessed = false,
                                        creationError = teamError.displayMessage ?: "Failed"
                                    )
                                    current.copy(teamDrafts = updatedDrafts)
                                }
                                null // Indicate failure for this team
                            } catch (e: Exception) { // Catch any other unexpected error for this specific team
                                Log.e("ViewModel", "Unexpected error creating team '${draft.name}': ${e.message}")
                                _uiState.update { current ->
                                    val updatedDrafts = current.teamDrafts.toMutableList()
                                    updatedDrafts[index] = updatedDrafts[index].copy(
                                        isBeingProcessed = false,
                                        creationError = "Unexpected error"
                                    )
                                    current.copy(teamDrafts = updatedDrafts)
                                }
                                null
                            }
                        }
                    }

                    // Collect results
                    val teamResults = teamCreationJobs.awaitAll()
                    teamResults.forEachIndexed { index, createdTeamOrNull ->
                        if (createdTeamOrNull != null) {
                            successfullyCreatedTeams.add(createdTeamOrNull)
                            // Update the draft in UI state with the created team
                            _uiState.update { current ->
                                val updatedDrafts = current.teamDrafts.toMutableList()
                                updatedDrafts[index] = updatedDrafts[index].copy(isBeingProcessed = false, createdTeam = createdTeamOrNull)
                                current.copy(
                                    teamDrafts = updatedDrafts,
                                    creationStatus = if (current.creationStatus is CreationStatus.CreatingTeams) {
                                        (current.creationStatus as CreationStatus.CreatingTeams).copy(
                                            teamsSuccessfullyCreated = successfullyCreatedTeams.size
                                        )
                                    } else current.creationStatus
                                )
                            }
                        } else {
                            // The draft at this index failed, add it to a list of failed drafts if needed for summary
                            // The error is already set on the draft object in uiState.teamDrafts
                            failedTeamDraftsProcessing.add(processingTeamDrafts[index].copy(creationError = _uiState.value.teamDrafts[index].creationError))
                        }
                    }


                    // --- Finalize Status based on team creation outcomes ---
                    if (failedTeamDraftsProcessing.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                creationStatus = CreationStatus.Success(createdLeague, successfullyCreatedTeams),
                                overallProgressMessage = "League and all teams created successfully!"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                creationStatus = CreationStatus.PartiallyCreated(
                                    createdLeague = createdLeague,
                                    successfullyCreatedTeams = successfullyCreatedTeams,
                                    failedTeamDrafts = failedTeamDraftsProcessing // Pass the drafts that failed
                                ),
                                overallProgressMessage = "League created, but some teams failed. Review team list."
                            )
                        }
                    }

                } else { // No teams were added by the user, just the league was created
                    _uiState.update {
                        it.copy(
                            creationStatus = CreationStatus.Success(createdLeague, emptyList()),
                            overallProgressMessage = "League created (no teams were added)."
                        )
                    }
                }

            } catch (leagueError: YourAppError) {
                Log.e("ViewModel", "Failed to create league: ${leagueError.getDetailedMessage()}", leagueError)
                _uiState.update {
                    it.copy(
                        creationStatus = CreationStatus.Error(leagueError.displayMessage ?: "Failed to create league.", stepFailed = "league"),
                        overallProgressMessage = "League creation failed."
                    )
                }
            } catch (e: Exception) { // Catch any other unexpected error during league creation
                Log.e("ViewModel", "Unexpected error during league creation step: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        creationStatus = CreationStatus.Error("An unexpected error occurred during league creation.", stepFailed = "league"),
                        overallProgressMessage = "League creation failed unexpectedly."
                    )
                }
            }
        }
    }

    fun resetCreationStatusAndForm() {
        _uiState.value = CreateLeagueScreenUiState()
    }
}