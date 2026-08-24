package com.example.fantahelpfe.ui.auction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fantahelpfe.data.mapper.toDomainPlayers
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.model.PlayerRole
import com.example.fantahelpfe.model.TeamPlayer
import com.example.fantahelpfe.ui.components.ErrorState
import com.example.fantahelpfe.ui.components.LoadingState
import com.example.fantahelpfe.utils.capitalizeFirstLetter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionMyTeamScreen(
    sharedViewModel: AuctionSharedViewModel
) {
    val viewModel: AuctionMyTeamViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var removeConfirmationPlayer by remember { mutableStateOf<TeamPlayer?>(null) }
    var inspectedPlayer by remember { mutableStateOf<Pair<TeamPlayer, Player?>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Track active tab within My Team: 0 = Current Roster, 1 = Optimal Suggestion
    var selectedViewMode by remember { mutableIntStateOf(0) }

    // Refresh team data whenever My Team screen enters composition
    LaunchedEffect(Unit) {
        viewModel.refreshTeam()
    }

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
                    if (inspectedPlayer?.first?.playerId == player.playerId) {
                        inspectedPlayer = null
                    }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { removeConfirmationPlayer = null }) { Text("Cancel") }
            }
        )
    }

    // Executive Player Inspector Dialog
    inspectedPlayer?.let { (teamPlayer, fullPlayer) ->
        val isAcquired = uiState.selectedTeam?.players?.any { it.playerId == teamPlayer.playerId } == true
        PlayerInspectorDialog(
            teamPlayer = teamPlayer,
            player = fullPlayer,
            isAcquired = isAcquired,
            onDismiss = { inspectedPlayer = null },
            onRemove = {
                removeConfirmationPlayer = teamPlayer
            },
            onAuctionClick = {
                fullPlayer?.let { sharedViewModel.selectPlayerForBuy(it) }
                inspectedPlayer = null
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
                // Team Selector (Horizontal Scrollable for N-Teams)
                if (uiState.teams.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(uiState.teams, key = { it.id }) { team ->
                            FilterChip(
                                selected = uiState.selectedTeam?.id == team.id,
                                onClick = { viewModel.selectTeam(team) },
                                label = {
                                    Text(
                                        team.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                uiState.selectedTeam?.let { team ->
                    // Compact Team Header Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                team.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "💰 ${team.remainingBudget} CR",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "👥 ${team.players.size} Players",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Squad Analytics Overview Banner
                    val totalExpScore = remember(team.players, uiState.allPlayersMap) {
                        team.players.mapNotNull { uiState.allPlayersMap[it.playerId]?.expectedPerformance }.sum()
                    }
                    val totalSpent = remember(team.players) {
                        team.players.sumOf { it.auctionPrice }
                    }
                    val avgRating = remember(team.players, uiState.allPlayersMap) {
                        val ratings = team.players.mapNotNull { uiState.allPlayersMap[it.playerId]?.rating }.filter { it > 0.0 }
                        if (ratings.isNotEmpty()) ratings.average() else 0.0
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("⚡ Exp. Score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    String.format("%.1f pts", totalExpScore),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💰 Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalSpent CR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("⭐️ Avg Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (avgRating > 0.0) String.format("%.2f", avgRating) else "N/A",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))

                    when (selectedViewMode) {
                        0 -> {
                            // Role-Grouped Current Roster View
                            if (team.players.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No players assigned to this team yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                val gkList = remember(team.players) { team.players.filter { it.playerRole == PlayerRole.P } }
                                val defList = remember(team.players) { team.players.filter { it.playerRole == PlayerRole.D } }
                                val midList = remember(team.players) { team.players.filter { it.playerRole == PlayerRole.C } }
                                val attList = remember(team.players) { team.players.filter { it.playerRole == PlayerRole.A } }

                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (gkList.isNotEmpty()) {
                                        item { RoleGroupHeader("🧤 Goalkeepers", gkList.size, Color(0xFFFFB74D)) }
                                        items(gkList, key = { it.playerId }) { teamPlayer ->
                                            TeamPlayerItem(
                                                teamPlayer = teamPlayer,
                                                fullPlayer = uiState.allPlayersMap[teamPlayer.playerId],
                                                onClick = {
                                                    inspectedPlayer = teamPlayer to uiState.allPlayersMap[teamPlayer.playerId]
                                                },
                                                onRemove = { removeConfirmationPlayer = teamPlayer }
                                            )
                                        }
                                    }

                                    if (defList.isNotEmpty()) {
                                        item { RoleGroupHeader("🛡️ Defenders", defList.size, Color(0xFF64B5F6)) }
                                        items(defList, key = { it.playerId }) { teamPlayer ->
                                            TeamPlayerItem(
                                                teamPlayer = teamPlayer,
                                                fullPlayer = uiState.allPlayersMap[teamPlayer.playerId],
                                                onClick = {
                                                    inspectedPlayer = teamPlayer to uiState.allPlayersMap[teamPlayer.playerId]
                                                },
                                                onRemove = { removeConfirmationPlayer = teamPlayer }
                                            )
                                        }
                                    }

                                    if (midList.isNotEmpty()) {
                                        item { RoleGroupHeader("🎯 Midfielders", midList.size, Color(0xFF81C784)) }
                                        items(midList, key = { it.playerId }) { teamPlayer ->
                                            TeamPlayerItem(
                                                teamPlayer = teamPlayer,
                                                fullPlayer = uiState.allPlayersMap[teamPlayer.playerId],
                                                onClick = {
                                                    inspectedPlayer = teamPlayer to uiState.allPlayersMap[teamPlayer.playerId]
                                                },
                                                onRemove = { removeConfirmationPlayer = teamPlayer }
                                            )
                                        }
                                    }

                                    if (attList.isNotEmpty()) {
                                        item { RoleGroupHeader("🚀 Attackers", attList.size, Color(0xE5E57373)) }
                                        items(attList, key = { it.playerId }) { teamPlayer ->
                                            TeamPlayerItem(
                                                teamPlayer = teamPlayer,
                                                fullPlayer = uiState.allPlayersMap[teamPlayer.playerId],
                                                onClick = {
                                                    inspectedPlayer = teamPlayer to uiState.allPlayersMap[teamPlayer.playerId]
                                                },
                                                onRemove = { removeConfirmationPlayer = teamPlayer }
                                            )
                                        }
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
                                            val matchingTeamPlayer = team.players.find { it.playerId == player.id }
                                            val isAcquired = matchingTeamPlayer != null
                                            SuggestedPlayerItem(
                                                player = player,
                                                isAcquired = isAcquired,
                                                onClick = {
                                                    val tp = matchingTeamPlayer ?: TeamPlayer(
                                                        playerId = player.id,
                                                        teamId = team.id,
                                                        playerName = player.name,
                                                        auctionPrice = player.expectedPrice,
                                                        playerRole = player.role
                                                    )
                                                    inspectedPlayer = tp to player
                                                }
                                            )
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
private fun RoleGroupHeader(title: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            color = color.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                "$count Slot${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun TeamPlayerItem(
    teamPlayer: TeamPlayer,
    fullPlayer: Player?,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                SuggestionRoleBadge(roleName = teamPlayer.playerRole.name)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        teamPlayer.playerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val squad = fullPlayer?.squad ?: "Roster"
                    val age = fullPlayer?.age?.takeIf { it > 0 }?.let { " · $it yrs" } ?: ""
                    Text(
                        "$squad$age",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "💰 ${teamPlayer.auctionPrice} CR",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (fullPlayer != null && fullPlayer.expectedPerformance > 0.0) {
                        Text(
                            String.format("⚡ %.1f pts", fullPlayer.expectedPerformance),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Player",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerInspectorDialog(
    teamPlayer: TeamPlayer,
    player: Player?,
    isAcquired: Boolean,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onAuctionClick: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        SuggestionRoleBadge(roleName = teamPlayer.playerRole.name)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                teamPlayer.playerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val squad = player?.squad ?: "Squad"
                            val age = player?.age?.takeIf { it > 0 }?.let { " · $it yrs" } ?: ""
                            Text(
                                "$squad$age",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                // Financial Deal Intelligence Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isAcquired) {
                                Column {
                                    Text("Paid in Auction", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${teamPlayer.auctionPrice} CR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            val expPrice = player?.expectedPrice ?: 0
                            val stdVal = player?.expectedStd ?: 0.0
                            Column(horizontalAlignment = if (isAcquired) Alignment.CenterHorizontally else Alignment.Start) {
                                Text("🤖 ML Fair Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (expPrice > 0) "$expPrice CR${if (stdVal > 0) " (±${stdVal.toInt()})" else ""}" else "N/A",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isAcquired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (player != null && player.fvm > 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📊 FVM Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${player.fvm} CR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("🏷️ List Base", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${player?.price ?: 0} CR", style = MaterialTheme.typography.titleSmall)
                            }
                        }

                        // Deal Outcome Evaluation Banner (Acquired) or Target Banner (Target)
                        if (isAcquired && player != null && player.expectedPrice > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val diff = player.expectedPrice - teamPlayer.auctionPrice
                            val (dealText, dealColor) = when {
                                diff > 0 -> "🟢 Bargain! Acquired $diff CR below ML predicted price" to Color(0xFF388E3C)
                                diff == 0 -> "🟡 Exact Fair Value Acquisition" to Color(0xFFF57F17)
                                else -> "🔴 Paid ${-diff} CR above ML fair valuation" to Color(0xFFD32F2F)
                            }
                            Surface(
                                color = dealColor.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    dealText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = dealColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else if (!isAcquired) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "🎯 Optimal Suggestion Target — Recommended for this Roster",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tactical KPI Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exp Perf
                    Column {
                        Text("⚡ Exp. Perf", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (player != null && player.expectedPerformance > 0.0) String.format("%.1f pts", player.expectedPerformance) else "N/A",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Starter Share
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏱️ Starter Share", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val reg = player?.regularness ?: 0
                        val regColor = when {
                            reg >= 80 -> Color(0xFF388E3C)
                            reg >= 50 -> Color(0xFFF57F17)
                            else -> Color(0xFFD32F2F)
                        }
                        Text(
                            "$reg%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = regColor
                        )
                    }

                    // Rating
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⭐️ Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val rating = player?.rating ?: 0.0
                        Text(
                            if (rating > 0.0) String.format("%.2f", rating) else "N/A",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Robustness
                    if (player?.integrity != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("🏥 Robustness", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${player.integrity}/5",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    player.integrity >= 4 -> Color(0xFF388E3C)
                                    player.integrity == 3 -> Color(0xFFF57F17)
                                    else -> Color(0xFFD32F2F)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAcquired) {
                        OutlinedButton(
                            onClick = {
                                onRemove()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    } else if (onAuctionClick != null) {
                        Button(
                            onClick = onAuctionClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("⚽ Auction Player")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedPlayerItem(
    player: Player,
    isAcquired: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                val age = player.age?.takeIf { it > 0 }?.let { " · $it yrs" } ?: ""
                Text(
                    "${player.squad}$age · Exp. Perf: ${String.format("%.1f", player.expectedPerformance)}",
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
