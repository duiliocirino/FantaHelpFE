package com.example.fantahelpfe.ui.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fantahelpfe.model.TeamPlayer
import com.example.fantahelpfe.ui.components.ErrorState
import com.example.fantahelpfe.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionMyTeamScreen(
    sharedViewModel: AuctionSharedViewModel
) {
    val viewModel: AuctionMyTeamViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var removeConfirmationPlayer by remember { mutableStateOf<TeamPlayer?>(null) }
    val snackbarHostState = rememberSnackbarHostState()

    // Collect snackbar messages from ViewModel
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Confirmation dialog for player removal
    removeConfirmationPlayer?.let { player ->
        AlertDialog(
            onDismissRequest = { removeConfirmationPlayer = null },
            title = { Text("Remove ${player.playerName}?") },
            text = { Text("This player will be removed from your team and returned to the available pool.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removePlayer(player.playerId)
                    removeConfirmationPlayer = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { removeConfirmationPlayer = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Team") },
                actions = {
                    IconButton(onClick = { viewModel.loadTeams() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Teams")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingState()
        } else if (uiState.error != null) {
            ErrorState(message = uiState.error!!, onRetry = { viewModel.loadTeams() })
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Team Selector
                if (uiState.teams.size > 1) {
                    Text("Select Team:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.teams.forEach { team ->
                            FilterChip(
                                selected = uiState.selectedTeam?.id == team.id,
                                onClick = { viewModel.selectTeam(team) },
                                label = { Text(team.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                uiState.selectedTeam?.let { team ->
                    // Team Header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(team.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Budget: ${team.remainingBudget} CR", style = MaterialTheme.typography.bodyLarge)
                                Text("Players: ${team.players.size}", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player List
                    Text("Roster", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(team.players) { player ->
                            PlayerItem(player = player, onRemove = { removeConfirmationPlayer = player })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Suggestion Section
                    Button(
                        onClick = { viewModel.requestSuggestion() },
                        enabled = !uiState.isLoadingSuggestion,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isLoadingSuggestion) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Get Optimal Team Suggestion")
                        }
                    }

                    uiState.suggestionResults?.let { results ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Suggestions:", style = MaterialTheme.typography.titleMedium)
                        results.firstOrNull()?.let { result ->
                            Text(
                                "Projected Total Score: ${String.format("%.1f", result.score.totalScore)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerItem(player: TeamPlayer, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(player.playerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("Price: ${player.auctionPrice}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Player", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
