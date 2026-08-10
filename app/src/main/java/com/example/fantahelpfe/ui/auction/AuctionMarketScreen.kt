package com.example.fantahelpfe.ui.auction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fantahelpfe.model.Player
import com.example.fantahelpfe.utils.capitalizeFirstLetter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionMarketScreen(
    sharedViewModel: AuctionSharedViewModel
) {
    val viewModel: AuctionMarketViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val settings by sharedViewModel.settings.collectAsState()

    // Refresh available players when navigating to Market tab
    LaunchedEffect(Unit) {
        viewModel.loadAvailablePlayers()
    }

    val totalAvailable = uiState.availablePlayers.size
    val gkCount = remember(uiState.availablePlayers) { uiState.availablePlayers.count { it.role.name.equals("P", ignoreCase = true) } }
    val defCount = remember(uiState.availablePlayers) { uiState.availablePlayers.count { it.role.name.equals("D", ignoreCase = true) } }
    val midCount = remember(uiState.availablePlayers) { uiState.availablePlayers.count { it.role.name.equals("C", ignoreCase = true) } }
    val attCount = remember(uiState.availablePlayers) { uiState.availablePlayers.count { it.role.name.equals("A", ignoreCase = true) } }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Market Overview KPI Summary Header Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Market Pool 🔍",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$totalAvailable Players Available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoleCountPill("P", gkCount, Color(0xFFFFB74D), Modifier.weight(1f))
                        RoleCountPill("D", defCount, Color(0xFF64B5F6), Modifier.weight(1f))
                        RoleCountPill("C", midCount, Color(0xFF81C784), Modifier.weight(1f))
                        RoleCountPill("A", attCount, Color(0xE5E57373), Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Input Field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::search,
                label = { Text("Search player or squad...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.search("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Position & Favorites Filter Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = uiState.onlyFavorites,
                        onClick = viewModel::toggleOnlyFavorites,
                        label = { Text("★ Favorites (${settings.globalFavoritePlayerIds.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFD54F),
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                val roles = listOf(
                    "All" to totalAvailable,
                    "P" to gkCount,
                    "D" to defCount,
                    "C" to midCount,
                    "A" to attCount
                )
                items(roles) { (roleCode, count) ->
                    val isSelected = (roleCode == "All" && uiState.filterRole == null) || uiState.filterRole == roleCode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.filterByRole(if (roleCode == "All") null else roleCode) },
                        label = { Text(if (roleCode == "All") "ALL ($count)" else "$roleCode ($count)", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sort Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sort:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                )
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(SortOption.values()) { option ->
                        FilterChip(
                            selected = uiState.sortBy == option,
                            onClick = { viewModel.sortBy(option) },
                            label = { Text(option.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Filter Summary Count Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Showing ${uiState.filteredPlayers.size} player${if (uiState.filteredPlayers.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Text(
                    uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (uiState.filteredPlayers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (uiState.onlyFavorites) "No favorite players match current search."
                        else "No players match selected filters.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredPlayers, key = { it.id }) { player ->
                        val isFavorite = settings.globalFavoritePlayerIds.contains(player.id)
                        MarketPlayerItem(
                            player = player,
                            isFavorite = isFavorite,
                            onToggleFavorite = {
                                if (isFavorite) sharedViewModel.removeGlobalFavoritePlayer(player.id)
                                else sharedViewModel.addGlobalFavoritePlayer(player.id)
                            },
                            onBuyClick = {
                                sharedViewModel.selectPlayerForBuy(player)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleCountPill(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
    ) {
        Text(
            "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MarketPlayerItem(
    player: Player,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBuyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBuyClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Role Badge, Name, Squad, Favorite Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    MarketRoleBadge(roleName = player.role.name)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            player.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val ageText = player.age?.let { " · $it yrs" } ?: ""
                        Text(
                            "${player.squad}$ageText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: KPI Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exp Perf
                Column {
                    Text("⚡ Exp. Perf", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        String.format("%.1f pts", player.expectedPerformance),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Starter Share
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏱️ Starter Share", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val regColor = when {
                        player.regularness >= 80 -> Color(0xFF388E3C)
                        player.regularness >= 50 -> Color(0xFFF57F17)
                        else -> Color(0xFFD32F2F)
                    }
                    Text(
                        "${player.regularness}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = regColor
                    )
                }

                // Rating
                Column(horizontalAlignment = Alignment.End) {
                    Text("⭐️ Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (player.rating > 0.0) String.format("%.2f", player.rating) else "N/A",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Financial Valuation & Action Button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onBuyClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("⚽ Auction", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketRoleBadge(roleName: String) {
    val (badgeColor, textColor) = when (roleName.uppercase()) {
        "P" -> Color(0xFFFFB74D) to Color.Black
        "D" -> Color(0xFF64B5F6) to Color.Black
        "C" -> Color(0xFF81C784) to Color.Black
        "A" -> Color(0xE5E57373) to Color.White
        else -> Color.Gray to Color.White
    }
    Surface(
        color = badgeColor,
        shape = MaterialTheme.shapes.extraSmall
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
