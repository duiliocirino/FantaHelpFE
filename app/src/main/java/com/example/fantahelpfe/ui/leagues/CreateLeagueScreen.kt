package com.example.fantahelpfe.ui.leagues

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done // For successfully created teams
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLeagueScreen(
    viewModel: CreateLeagueViewModel = hiltViewModel(),
    navController: NavHostController // Or an onLeagueSuccessfullyCreated: (League, List<Team>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- Handle Creation Status Side Effects (e.g., navigation, snackbars) ---
    // This LaunchedEffect will react to changes in creationStatus
    LaunchedEffect(uiState.creationStatus) {
        when (val status = uiState.creationStatus) {
            is CreationStatus.Success -> {
                // Example: Show a Snackbar or Toast
                Log.d("CreateLeagueScreen", "SUCCESS: League '${status.createdLeague.name}' and ${status.createdTeams.size} teams created.")
                // Optionally navigate: navController.popBackStack() or navigate to the new league's screen
                // viewModel.resetCreationStatusAndForm() // If you want to clear the form
            }
            is CreationStatus.PartiallyCreated -> {
                Log.w("CreateLeagueScreen", "PARTIAL: League '${status.createdLeague.name}' created, but ${status.failedTeamDrafts.size} teams failed.")
                // UI will show details, but you could show a Snackbar summary
            }
            is CreationStatus.Error -> {
                Log.e("CreateLeagueScreen", "ERROR: ${status.message} (Step: ${status.stepFailed})")
                // Snackbar to show the error message
            }
            else -> { /* Idle, CreatingLeague, CreatingTeams - usually handled by specific UI elements */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Create New League") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // League Name Input
            OutlinedTextField(
                value = uiState.leagueName,
                onValueChange = viewModel::onLeagueNameChanged,
                label = { Text("League Name") },
                isError = uiState.formError?.contains("League name", ignoreCase = true) == true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Initial Budget Input
            OutlinedTextField(
                value = uiState.initialBudget,
                onValueChange = viewModel::onInitialBudgetChanged,
                label = { Text("Initial Budget per Team") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.formError?.contains("budget", ignoreCase = true) == true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- Add Team Section ---
            Text("Add Teams", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.currentTeamNameInput,
                    onValueChange = viewModel::onCurrentTeamNameInputChanged,
                    label = { Text("Team Name") },
                    isError = uiState.formError?.contains("Team name", ignoreCase = true) == true,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardActions = KeyboardActions(onDone = {
                        if (uiState.currentTeamNameInput.isNotBlank()) {
                            viewModel.addTeamDraft()
                        }
                    })
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = viewModel::addTeamDraft,
                    enabled = uiState.currentTeamNameInput.isNotBlank() && uiState.creationStatus == CreationStatus.Idle
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Team")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // --- Display Added Team Drafts ---
            if (uiState.teamDrafts.isNotEmpty()) {
                Text("Teams to Add (${uiState.teamDrafts.size}):", style = MaterialTheme.typography.labelSmall)
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .padding(vertical = 4.dp)
                ) {
                    itemsIndexed(uiState.teamDrafts, key = { _, draft -> draft.name /* Assuming names are unique for drafts */ }) { _, draft ->
                        TeamDraftRow(
                            teamDraft = draft,
                            onRemove = { if (uiState.creationStatus == CreationStatus.Idle) viewModel.removeTeamDraft(draft.name) },
                            enabled = uiState.creationStatus == CreationStatus.Idle || // Allow removal if idle
                                    (uiState.creationStatus is CreationStatus.PartiallyCreated && draft.creationError != null) // Or if it failed in partial
                        )
                        Divider()
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // --- Display Form Error or Progress Messages ---
            if (uiState.formError != null) {
                Text(
                    text = uiState.formError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else if (uiState.overallProgressMessage != null && uiState.creationStatus !is CreationStatus.Idle && uiState.creationStatus !is CreationStatus.Error && uiState.creationStatus !is CreationStatus.PartiallyCreated) {
                // Show general progress if not an error or specific handled state
                Text(
                    text = uiState.overallProgressMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }


            // Display detailed messages for specific states
            when (val status = uiState.creationStatus) {
                is CreationStatus.Error -> {
                    Text(
                        "Error (${status.stepFailed}): ${status.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                is CreationStatus.PartiallyCreated -> {
                    Column {
                        Text(
                            "League '${status.createdLeague.name}' created.",
                            color = Color(0xFF388E3C), // Dark Green
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (status.failedTeamDrafts.isNotEmpty()) {
                            Text(
                                "However, ${status.failedTeamDrafts.size} team(s) could not be added. Please review the team list.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                is CreationStatus.Success -> {
                    Text(
                        "League '${status.createdLeague.name}' and all teams created successfully!",
                        color = Color(0xFF388E3C), // Dark Green
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {} // Other states handled by loading indicator or progress message
            }


            Spacer(modifier = Modifier.weight(1f)) // Push button to bottom

            // --- Create League Button ---
            val isLoading = uiState.creationStatus is CreationStatus.CreatingLeague ||
                    uiState.creationStatus is CreationStatus.CreatingTeams

            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    uiState.overallProgressMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (uiState.creationStatus is CreationStatus.CreatingTeams) {
                        val creatingTeamsStatus = uiState.creationStatus as CreationStatus.CreatingTeams
                        Text(
                            "Processing: ${creatingTeamsStatus.currentTeamProcessing ?: ""} (${creatingTeamsStatus.teamsSuccessfullyCreated}/${creatingTeamsStatus.totalTeamsToCreate})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (uiState.creationStatus is CreationStatus.PartiallyCreated) {
                            // Decide what to do for retry. Maybe a different button or logic.
                            // For now, let's assume retry means trying the whole thing again.
                            // Or you might have a viewModel.retryFailedTeams()
                            viewModel.submitLeagueAndTeams()
                        } else {
                            viewModel.submitLeagueAndTeams()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = uiState.creationStatus == CreationStatus.Idle ||
                            uiState.creationStatus is CreationStatus.Error || // Allow retry on error
                            uiState.creationStatus is CreationStatus.PartiallyCreated // Allow retry/proceed on partial
                ) {
                    Text(
                        when (uiState.creationStatus) {
                            is CreationStatus.PartiallyCreated -> "Retry Failed / Finalize" // Or more specific
                            is CreationStatus.Error -> "Retry Creation"
                            else -> "Create League and Teams"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TeamDraftRow(teamDraft: TeamDraft, onRemove: () -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            when {
                teamDraft.isBeingProcessed -> {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                teamDraft.creationError != null -> {
                    Icon(Icons.Filled.Warning, contentDescription = "Error creating team", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                teamDraft.createdTeam != null -> {
                    Icon(Icons.Filled.Done, contentDescription = "Team created", tint = Color(0xFF388E3C), modifier = Modifier.size(16.dp)) // Dark Green
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Text(
                text = teamDraft.name,
                textDecoration = if (teamDraft.creationError != null) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
        }
        if (teamDraft.creationError != null) {
            Text(
                text = teamDraft.creationError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        IconButton(onClick = onRemove, enabled = enabled) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove ${teamDraft.name}", tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
        }
    }
}