package com.example.fantahelpfe.ui.auction

import com.example.fantahelpfe.data.remote.dto.LineUp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    val scrollState = rememberScrollState()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
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
            PlayerExecutiveScoutingCard(player = player)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Immediate Financial Context Badge (Option 2) ---
        if (uiState.selectedPlayer != null && uiState.selectedTeamForAssignment != null) {
            val team = uiState.selectedTeamForAssignment!!
            val remainingAfter = team.remainingBudget - uiState.currentBidAmount
            val isDeficit = remainingAfter < 0

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "💳 ${team.name} Budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${team.remainingBudget} CR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "After Bid",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$remainingAfter CR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeficit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- Bid Amount Controls ---
        if (uiState.selectedPlayer != null) {
            Text("Bid Amount:", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
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
            Spacer(modifier = Modifier.height(12.dp))
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
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- Manual Calculate Score Advantage Button ---
        if (uiState.selectedPlayer != null && uiState.currentBidAmount > 0 && uiState.selectedTeamForAssignment != null) {
            Button(
                onClick = buyPlayersViewModel::calculateOptimalSuggestion,
                enabled = !uiState.isLoadingConvenienceScore,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Calculate", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (uiState.baseOptimalScore != null)
                        "⚡ Re-Calculate Advantage (${uiState.currentBidAmount} CR)"
                    else
                        "⚡ Analyze Score Advantage (${uiState.currentBidAmount} CR)",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- Convenience Score Display ---
            when {
                uiState.isLoadingConvenienceScore -> {
                    TacticalCalculationLoadingCard(
                        playerName = uiState.selectedPlayer?.name ?: "Player",
                        onCancel = buyPlayersViewModel::cancelSuggestion
                    )
                }
                uiState.convenienceScoreError != null -> {
                    Text(
                        "Score Error: ${uiState.convenienceScoreError}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.baseOptimalScore != null && uiState.potentialOptimalScore != null -> {
                    HybridExecutiveDashboardCard(
                        uiState = uiState,
                        onSelectRosterTab = buyPlayersViewModel::setRosterBreakdownTab
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp)) // Spacing before Assign button

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
    uiState: AuctionBuyPlayersUiState,
    onSelectRosterTab: (RosterBreakdownTab) -> Unit
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
                // 3-Column Performance & Financial Metric Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Col 1: Net Gain
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Net Gain", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            String.format("%+.1f pts", delta),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (delta >= 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
                        )
                    }

                    // Col 2: Projected Score
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Projected Score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                String.format("%.1f", uiState.baseOptimalScore ?: 0.0),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(" ➔ ", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                String.format("%.1f pts", uiState.potentialOptimalScore ?: 0.0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Col 3: Rem. Budget
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Rem. Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val remainingAfter = (uiState.selectedTeamForAssignment?.remainingBudget ?: 0) - uiState.currentBidAmount
                        val isDeficit = remainingAfter < 0
                        Text(
                            "$remainingAfter CR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeficit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // Quick Roster Insights Row
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

                    if (uiState.displacedPlayerName != null) {
                        Text(
                            "Replaces: ${uiState.displacedPlayerName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
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
                if (expandedDetails) {
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
                                Text("Default", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("%.1f", uiState.baseScoreDetail?.starterScore ?: 0.0), style = MaterialTheme.typography.bodySmall)
                                Text(String.format("%.1f", uiState.baseScoreDetail?.benchScore ?: 0.0), style = MaterialTheme.typography.bodySmall)
                                Text(String.format("%.1f", uiState.baseScoreDetail?.penaltyScore ?: 0.0), style = MaterialTheme.typography.bodySmall)
                            }
                            if (uiState.hasDistinctPlanB && uiState.withoutPlayerScoreDetail != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Plan B", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(String.format("%.1f", uiState.withoutPlayerScoreDetail?.starterScore ?: 0.0), style = MaterialTheme.typography.bodySmall)
                                    Text(String.format("%.1f", uiState.withoutPlayerScoreDetail?.benchScore ?: 0.0), style = MaterialTheme.typography.bodySmall)
                                    Text(String.format("%.1f", uiState.withoutPlayerScoreDetail?.penaltyScore ?: 0.0), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("With $playerName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(String.format("%.1f", uiState.potentialScoreDetail?.starterScore ?: 0.0), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(String.format("%.1f", uiState.potentialScoreDetail?.benchScore ?: 0.0), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(String.format("%.1f", uiState.potentialScoreDetail?.penaltyScore ?: 0.0), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Roster Completion Cost Bar (Inside Tactical Breakdown)
                        uiState.totalExpectedPrice?.let { estCost ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "💰 Est. Roster Completion Cost:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "$estCost CR",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Dynamic 2/3 Tab Roster Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = uiState.selectedRosterTab == RosterBreakdownTab.Potential,
                                onClick = { onSelectRosterTab(RosterBreakdownTab.Potential) },
                                label = { Text("⚽ With $playerName", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )

                            if (uiState.hasDistinctPlanB) {
                                FilterChip(
                                    selected = uiState.selectedRosterTab == RosterBreakdownTab.DefaultBase,
                                    onClick = { onSelectRosterTab(RosterBreakdownTab.DefaultBase) },
                                    label = { Text("📊 Default Plan", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = uiState.selectedRosterTab == RosterBreakdownTab.PlanBWithout,
                                    onClick = { onSelectRosterTab(RosterBreakdownTab.PlanBWithout) },
                                    label = { Text("🚫 Plan B", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                FilterChip(
                                    selected = uiState.selectedRosterTab == RosterBreakdownTab.DefaultBase,
                                    onClick = { onSelectRosterTab(RosterBreakdownTab.DefaultBase) },
                                    label = { Text("📊 Base Team", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Roster Player Lineup Display for Selected Tab
                        val activePlayerList = when (uiState.selectedRosterTab) {
                            RosterBreakdownTab.Potential -> uiState.potentialSuggestedPlayers
                            RosterBreakdownTab.DefaultBase -> uiState.baseSuggestedPlayers
                            RosterBreakdownTab.PlanBWithout -> uiState.withoutPlayerSuggestedPlayers
                        }

                        if (activePlayerList.isNotEmpty()) {
                            val (startersList, benchList) = remember(activePlayerList, uiState.targetLineUp) {
                                partitionStartersAndBench(activePlayerList, uiState.targetLineUp)
                            }

                            if (startersList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "⚽ Projected Starting XI (${startersList.size} Starters):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    startersList.forEach { playerDto ->
                                        PlayerLineupRow(
                                            playerDto = playerDto,
                                            selectedPlayerId = uiState.selectedPlayer?.id,
                                            currentBid = uiState.currentBidAmount
                                        )
                                    }
                                }
                            }

                            if (benchList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "🪑 Projected Bench Depth (${benchList.size} Players):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    benchList.forEach { playerDto ->
                                        PlayerLineupRow(
                                            playerDto = playerDto,
                                            selectedPlayerId = uiState.selectedPlayer?.id,
                                            currentBid = uiState.currentBidAmount
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

@Composable
private fun PlayerLineupRow(
    playerDto: PlayerReadDto,
    selectedPlayerId: Int?,
    currentBid: Int
) {
    val isTarget = playerDto.id == selectedPlayerId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent,
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            SuggestionRoleBadge(roleName = playerDto.role)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        playerDto.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (playerDto.squad.isNotEmpty()) {
                        Text(
                            " (${playerDto.squad})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isTarget) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "⭐️ Target",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (playerDto.expectedPerformance > 0.0) {
                    Text(
                        String.format("⚡ Performance: %.2f pts", playerDto.expectedPerformance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (isTarget) {
                Text(
                    "$currentBid CR (Bid)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (playerDto.expectedPrice > 0) {
                    Text(
                        "Exp: ${playerDto.expectedPrice} CR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val expValue = if (playerDto.expectedPrice > 0) playerDto.expectedPrice else if (playerDto.fvm > 0) playerDto.fvm else playerDto.price
                Text(
                    "Exp: $expValue CR",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (playerDto.price > 0 && playerDto.price != expValue) {
                    Text(
                        "Base: ${playerDto.price} CR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun partitionStartersAndBench(
    players: List<PlayerReadDto>,
    lineUp: LineUp? = null
): Pair<List<PlayerReadDto>, List<PlayerReadDto>> {
    if (players.isEmpty()) return Pair(emptyList(), emptyList())

    val nGk = lineUp?.keepers ?: 1
    val nDef = lineUp?.defenders ?: 4
    val nMid = lineUp?.midfielders ?: 3
    val nAtt = lineUp?.attackers ?: 3

    val goalkeepers = players.filter { it.role.equals("P", ignoreCase = true) || it.role.startsWith("Port", ignoreCase = true) }
        .sortedByDescending { it.expectedPerformance }
    val defenders = players.filter { it.role.equals("D", ignoreCase = true) || it.role.startsWith("Dif", ignoreCase = true) }
        .sortedByDescending { it.expectedPerformance }
    val midfielders = players.filter { it.role.equals("C", ignoreCase = true) || it.role.startsWith("Centr", ignoreCase = true) }
        .sortedByDescending { it.expectedPerformance }
    val attackers = players.filter { it.role.equals("A", ignoreCase = true) || it.role.startsWith("Att", ignoreCase = true) }
        .sortedByDescending { it.expectedPerformance }

    val starterGk = goalkeepers.take(nGk)
    val starterDef = defenders.take(nDef)
    val starterMid = midfielders.take(nMid)
    val starterAtt = attackers.take(nAtt)

    val starters = (starterGk + starterDef + starterMid + starterAtt)
        .sortedBy { roleOrder(it.role) }

    val starterIds = starters.map { it.id }.toSet()
    val bench = players.filterNot { it.id in starterIds }
        .sortedBy { roleOrder(it.role) }

    return Pair(starters, bench)
}

private fun roleOrder(role: String): Int = when {
    role.equals("P", ignoreCase = true) || role.startsWith("Port", ignoreCase = true) -> 1
    role.equals("D", ignoreCase = true) || role.startsWith("Dif", ignoreCase = true) -> 2
    role.equals("C", ignoreCase = true) || role.startsWith("Centr", ignoreCase = true) -> 3
    role.equals("A", ignoreCase = true) || role.startsWith("Att", ignoreCase = true) -> 4
    else -> 5
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun TacticalCalculationLoadingCard(
    playerName: String,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "🤖 AI Tactical Engine Analyzing...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Evaluating line-up combinations and score impact for $playerName...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("Cancel Analysis", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PlayerExecutiveScoutingCard(
    player: com.example.fantahelpfe.model.Player
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Role Badge, Name, Squad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    SuggestionRoleBadge(roleName = player.role.name)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            player.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            player.squad,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: KPI Metrics Grid (3 Pill Badges)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // KPI 1: Exp. Performance
                Column {
                    Text(
                        "⚡ Exp. Perf.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format("%.1f pts", player.expectedPerformance),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // KPI 2: Starter Share / Regularity
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "⏱️ Starter Share",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val regColor = when {
                        player.regularness >= 80 -> Color(0xFF388E3C)
                        player.regularness >= 50 -> Color(0xFFF57F17)
                        else -> Color(0xFFD32F2F)
                    }
                    Surface(
                        color = regColor.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            "${player.regularness}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = regColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // KPI 3: Media Voto / Rating
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "⭐️ Rating",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (player.rating > 0.0) String.format("%.2f", player.rating) else "N/A",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Financial Valuation & Auctionable Range Benchmarks Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ML Expected Price + Price Volatility Std
                        val expPrice = if (player.expectedPrice > 0) player.expectedPrice else player.price
                        val stdVal = player.expectedStd
                        Column {
                            Text("🤖 Exp. Price Range", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$expPrice CR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (stdVal > 0.0) {
                                    Text(
                                        String.format(" (±%.0f)", stdVal),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // FVM Value
                        if (player.fvm > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📊 FVM Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${player.fvm} CR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // List Base Price
                        Column(horizontalAlignment = Alignment.End) {
                            Text("🏷️ List Base", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${player.price} CR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Auctionable Price Window Pill
                    val expPrice = if (player.expectedPrice > 0) player.expectedPrice else player.price
                    val stdInt = player.expectedStd.toInt()
                    if (stdInt > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val minRange = (expPrice - stdInt).coerceAtLeast(1)
                        val maxRange = expPrice + stdInt
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "🎯 Fair Auctionable Window: $minRange – $maxRange CR (Max Cap: $maxRange CR)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
