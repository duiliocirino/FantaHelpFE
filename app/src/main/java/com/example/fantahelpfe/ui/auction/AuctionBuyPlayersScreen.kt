package com.example.fantahelpfe.ui.auction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fantahelpfe.data.remote.dto.Score
import com.example.fantahelpfe.data.remote.dto.read.PlayerReadDto
import com.example.fantahelpfe.utils.capitalizeFirstLetter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionBuyPlayersScreen(
    navController: NavController,
    sharedViewModel: AuctionSharedViewModel = hiltViewModel()
) {
    val buyPlayersViewModel: AuctionBuyPlayersViewModel = hiltViewModel()
    val uiState by buyPlayersViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect snackbar messages from ViewModel
    LaunchedEffect(Unit) {
        buyPlayersViewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Reset assignment status after success
    LaunchedEffect(uiState.assignmentStatus) {
        if (uiState.assignmentStatus == ActionStatus.Success) {
            delay(2000)
            buyPlayersViewModel.resetAssignmentStatus()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
        // --- League Name (Optional) ---
        uiState.leagueDetails?.let {
            Text(
                "League: ${it.name}", // Assuming your League model has 'name'
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )
        }
        if (uiState.leagueDetails == null && !uiState.isLoadingTeams) {
            Text(
                "Failed to load league details.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // --- Player Search and Selection ---
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = buyPlayersViewModel::onSearchQueryChanged,
            label = { Text("Search Player Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, "Search Icon") },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        buyPlayersViewModel.onSearchQueryChanged("")
                        buyPlayersViewModel.clearSelectedPlayer()
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Filled.Clear, "Clear Search")
                    }
                }
            },
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        if (uiState.isSearchingPlayers) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        } else if (uiState.searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
                    .padding(vertical = 4.dp)
            ) {
                items(uiState.searchResults, key = { it.id }) { player ->
                    Text(
                        text = "${player.name} (${player.role.name.capitalizeFirstLetter()})",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                buyPlayersViewModel.onPlayerSelectedFromSearch(player)
                                focusManager.clearFocus()
                            }
                            .padding(8.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
        if (uiState.selectedPlayer == null && uiState.searchResults.isEmpty()) {
            uiState.lastAcquisition?.let { last ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Recent Acquisition",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                last.playerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Assigned to ${last.teamName} for ${last.bid} CR",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        OutlinedButton(
                            onClick = { buyPlayersViewModel.undoLastAcquisition() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Undo Acquisition",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo")
                        }
                    }
                }
            }
        }

        uiState.selectedPlayer?.let { player ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        player.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${player.squad} · ${player.role.name.capitalizeFirstLetter()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Market: ${player.price} CR", style = MaterialTheme.typography.bodySmall)
                        Text("Rating: ${String.format("%.1f", player.rating)}", style = MaterialTheme.typography.bodySmall)
                        Text("Exp. Perf: ${String.format("%.1f", player.expectedPerformance)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Bid Amount ---
        if (uiState.selectedPlayer != null) {
            // Budget display
            uiState.selectedTeamForAssignment?.let { team ->
                val budgetColor = if (team.remainingBudget < uiState.currentBidAmount)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.tertiary
                Text(
                    "Remaining budget: ${team.remainingBudget} CR",
                    style = MaterialTheme.typography.bodyMedium,
                    color = budgetColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text("Bid Amount:", style = MaterialTheme.typography.titleSmall)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { buyPlayersViewModel.adjustBid(-5) },
                    modifier = Modifier.weight(1f)
                ) { Text("-5") }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { buyPlayersViewModel.adjustBid(-1) },
                    modifier = Modifier.weight(1f)
                ) { Text("-1") }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = uiState.bidAmountInput,
                    onValueChange = buyPlayersViewModel::onBidAmountChanged,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.width(100.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { buyPlayersViewModel.adjustBid(1) },
                    modifier = Modifier.weight(1f)
                ) { Text("+1") }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { buyPlayersViewModel.adjustBid(5) },
                    modifier = Modifier.weight(1f)
                ) { Text("+5") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Team Assignment Dropdown ---
        if (uiState.selectedPlayer != null) {
            var expandedTeamDropdown by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedTeamDropdown,
                onExpandedChange = { expandedTeamDropdown = !expandedTeamDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.selectedTeamForAssignment?.name ?: "Select Team to Assign",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign to Team") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTeamDropdown)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedTeamDropdown,
                    onDismissRequest = { expandedTeamDropdown = false }
                ) {
                    if (uiState.isLoadingTeams) {
                        DropdownMenuItem(
                            text = { Text("Loading teams...") },
                            onClick = {},
                            enabled = false
                        )
                    } else if (uiState.teamsInLeague.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No teams available/configured") },
                            onClick = {},
                            enabled = false
                        )
                    }
                    uiState.teamsInLeague.forEach { team ->
                        DropdownMenuItem(
                            text = { Text(team.name) },
                            onClick = {
                                buyPlayersViewModel.onTeamSelectedForAssignment(team)
                                expandedTeamDropdown = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Convenience Score Display ---
        if (uiState.selectedPlayer != null && uiState.currentBidAmount > 0 && uiState.selectedTeamForAssignment != null) {
            when {
                uiState.isLoadingConvenienceScore -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            " Calculating score advantage...",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        TextButton(onClick = { buyPlayersViewModel.cancelSuggestion() }) {
                            Text("Cancel")
                        }
                    }
                }
                uiState.convenienceScoreError != null -> {
                    Text(
                        "Score Error: ${uiState.convenienceScoreError}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.baseOptimalScore != null && uiState.potentialOptimalScore != null -> {
                    HybridExecutiveDashboardCard(uiState = uiState)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f)) // Push button to bottom

        // --- Assign Button ---
        if (uiState.assignmentStatus == ActionStatus.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = buyPlayersViewModel::assignPlayer,
                enabled = uiState.selectedPlayer != null &&
                        uiState.selectedTeamForAssignment != null &&
                        uiState.currentBidAmount > 0 &&
                        uiState.assignmentStatus == ActionStatus.Idle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Assign Player")
            }
        }
        uiState.assignmentError?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
        if (uiState.assignmentStatus == ActionStatus.Success) {
            Text(
                "Player Assigned Successfully!",
                color = Color(0xFF4CAF50), // Green
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold
            )
        }

        // --- General Error ---
        uiState.generalError?.let {
            Text(
                "Error: $it",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
    }
}

@Composable
fun HybridExecutiveDashboardCard(
    uiState: AuctionBuyPlayersUiState
) {
    val delta = uiState.convenienceScoreDelta ?: 0.0
    val playerName = uiState.selectedPlayer?.name ?: "Player"
    var expandedDetails by remember { mutableStateOf(false) }

    val (statusTitle, statusSubtitle, statusBgColor, statusTextColor) = when {
        delta >= 3.0 -> Quadruple("🚀 MUST BUY — TOP VALUE UPGRADE", "Huge point gain for your lineup!", Color(0xFF1B5E20), Color.White)
        delta >= 1.0 -> Quadruple("👍 SOLID UPGRADE", "Clear starter quality addition", Color(0xFF2E7D32), Color.White)
        delta >= 0.0 -> Quadruple("⚠️ MARGINAL GAIN", "Minor squad depth improvement", Color(0xFFF57F17), Color.Black)
        else -> Quadruple("🛑 OVERBID / NO GAIN", "Point gain is negative or budget depleted", Color(0xFFC62828), Color.White)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            // Recommendation Status Banner
            Surface(
                color = statusBgColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(statusTitle, color = statusTextColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(statusSubtitle, color = statusTextColor.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Side-by-Side Hero Advantage Callout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Net Score Gain", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            String.format("%+.1f pts", delta),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (delta >= 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Projected Score", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                String.format("%.1f", uiState.baseOptimalScore ?: 0.0),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(" ➔ ", style = MaterialTheme.typography.titleMedium)
                            Text(
                                String.format("%.1f pts", uiState.potentialOptimalScore ?: 0.0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // Quick Roster & Financial Insights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Starter vs Bench Badge
                    Surface(
                        color = if (uiState.isStarterInSuggested) Color(0xFFFFB74D) else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = if (uiState.isStarterInSuggested) "⭐️ Starter XI" else "🪑 Bench Depth",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (uiState.isStarterInSuggested) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Displaced Player or ROI Ratio
                    if (uiState.displacedPlayerName != null) {
                        Text(
                            "Replaces: ${uiState.displacedPlayerName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (uiState.creditsPerPointRatio != null) {
                        Text(
                            String.format("Efficiency: %.1f CR/pt", uiState.creditsPerPointRatio),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Expandable Tactical Breakdown Toggle
                TextButton(
                    onClick = { expandedDetails = !expandedDetails },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (expandedDetails) "Hide Tactical Breakdown ▲" else "Show Tactical Breakdown ▼",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Expanded Tactical Breakdown Content
                androidx.compose.animation.AnimatedVisibility(visible = expandedDetails) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Score Category Breakdown Grid
                        Text("Score Breakdown Comparison:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Starters Score", style = MaterialTheme.typography.bodySmall)
                                Text("Bench Score", style = MaterialTheme.typography.bodySmall)
                                Text("Penalty Score", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("%.1f", uiState.baseScoreDetail?.starterScore ?: 0.0), style = MaterialTheme.typography.bodySmall)
                                Text(String.format("%.1f", uiState.baseScoreDetail?.benchScore ?: 0.0), style = MaterialTheme.typography.bodySmall)
                                Text(String.format("%.1f", uiState.baseScoreDetail?.penaltyScore ?: 0.0), style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("With $playerName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(String.format("%.1f", uiState.potentialScoreDetail?.starterScore ?: 0.0), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(String.format("%.1f", uiState.potentialScoreDetail?.benchScore ?: 0.0), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(String.format("%.1f", uiState.potentialScoreDetail?.penaltyScore ?: 0.0), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Projected Lineup Role Chips Preview
                        if (uiState.potentialSuggestedPlayers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Projected Lineup Players:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                uiState.potentialSuggestedPlayers.take(6).forEach { playerDto: PlayerReadDto ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SuggestionRoleBadge(roleName = playerDto.role)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                playerDto.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (playerDto.id == uiState.selectedPlayer?.id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (playerDto.id == uiState.selectedPlayer?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (playerDto.id == uiState.selectedPlayer?.id) {
                                            Text("⭐️ New", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
