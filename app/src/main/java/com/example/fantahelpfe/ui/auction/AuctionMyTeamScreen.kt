package com.example.fantahelpfe.ui.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.example.fantahelpfe.data.mapper.toDomainPlayers
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.utils.capitalizeFirstLetter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionMyTeamScreen(
    sharedViewModel: AuctionSharedViewModel
) {
    val viewModel: AuctionMyTeamViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var removeConfirmationPlayer by remember { mutableStateOf<TeamPlayer?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Track active tab within My Team: 0 = Current Roster, 1 = Optimal Suggestion
    var selectedViewMode by remember { mutableIntStateOf(0) }

    // Switch to suggestion tab when new suggestions arrive
    LaunchedEffect(uiState.suggestionResults) {
        if (uiState.suggestionResults != null) {
            selectedViewMode = 1
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
                    Spacer(modifier = Modifier.height(12.dp))
                }

                uiState.selectedTeam?.let { team ->
                    // Team Header Card
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // View Mode Switcher (Current Roster vs Optimal Suggestion)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = selectedViewMode == 0,
                            onClick = { selectedViewMode = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Current Roster (${team.players.size})")
                        }
                        SegmentedButton(
                            selected = selectedViewMode == 1,
                            onClick = { selectedViewMode = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text(
                                if (uiState.suggestionResults != null) "Optimal Suggestion ★" else "Optimal Suggestion"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (selectedViewMode) {
                        0 -> {
                            // Current Roster View
                            if (team.players.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No players assigned to this team yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(team.players) { player ->
                                        PlayerItem(player = player, onRemove = { removeConfirmationPlayer = player })
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Optimal Suggestion View
                            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                Button(
                                    onClick = { viewModel.requestSuggestion() },
                                    enabled = !uiState.isLoadingSuggestion,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isLoadingSuggestion) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                        Text(" Calculating Optimal Roster...", modifier = Modifier.padding(start = 8.dp))
                                    } else {
                                        Text(if (uiState.suggestionResults == null) "Calculate Optimal Team Suggestion" else "Recalculate Optimal Suggestion")
                                    }
                                }

                                uiState.suggestionResults?.firstOrNull()?.let { result ->
                                    val domainSuggestedPlayers = result.suggestedPlayers.toDomainPlayers()
                                    val currentRosterPlayerIds = team.players.map { it.playerId }.toSet()

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Score Breakdown Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "Projected Score: ${String.format("%.1f", result.score.totalScore)} pts",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    "Exp. Cost: ${result.totalExpectedPrice} CR",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Starters: ${String.format("%.1f", result.score.starterScore)}", style = MaterialTheme.typography.bodySmall)
                                                Text("Bench: ${String.format("%.1f", result.score.benchScore)}", style = MaterialTheme.typography.bodySmall)
                                                Text("Penalty: ${String.format("%.1f", result.score.penaltyScore)}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Suggested Roster (${domainSuggestedPlayers.size} Players)", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(domainSuggestedPlayers, key = { it.id }) { player ->
                                            val isAcquired = currentRosterPlayerIds.contains(player.id)
                                            SuggestedPlayerItem(player = player, isAcquired = isAcquired)
                                        }
                                    }
                                } ?: run {
                                    if (!uiState.isLoadingSuggestion) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Tap the button above to calculate the optimal player targets for this team.",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
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
                Text("Price: ${player.auctionPrice} CR", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Player", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun SuggestedPlayerItem(player: Player, isAcquired: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isAcquired) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        player.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SuggestionRoleBadge(roleName = player.role.name)
                }
                Text(
                    "${player.squad} · Exp. Perf: ${String.format("%.1f", player.expectedPerformance)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Exp: ${player.expectedPrice} CR",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isAcquired) {
                    Text(
                        "✓ Acquired",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF388E3C),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionRoleBadge(roleName: String) {
    val (badgeColor, textColor) = when (roleName.uppercase()) {
        "P" -> Color(0xFFFFB74D) to Color.Black // Orange for Goalkeeper
        "D" -> Color(0xFF64B5F6) to Color.Black // Blue for Defender
        "C" -> Color(0xFF81C784) to Color.Black // Green for Midfielder
        "A" -> Color(0xE5E57373) to Color.White // Red for Attacker
        else -> Color.Gray to Color.White
    }
    Surface(
        color = badgeColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = roleName.capitalizeFirstLetter(),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

