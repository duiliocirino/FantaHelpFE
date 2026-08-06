package com.example.fantahelpfe.ui.auction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionFlowScreen(
    navController: NavController,
    sharedViewModel: AuctionSharedViewModel
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Buy", "My Team", "Market", "Settings")

    // Switch to Buy tab when a player is selected from the Market tab
    LaunchedEffect(Unit) {
        sharedViewModel.selectedPlayerForBuy.collectLatest { player ->
            if (player != null) {
                selectedTabIndex = 0
                sharedViewModel.clearSelectedPlayerForBuy()
            }
        }
    }

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Content based on selected tab
            when (selectedTabIndex) {
                0 -> AuctionBuyPlayersScreen(navController = navController, sharedViewModel = sharedViewModel)
                1 -> AuctionMyTeamScreen(sharedViewModel = sharedViewModel)
                2 -> AuctionMarketScreen(sharedViewModel = sharedViewModel)
                3 -> AuctionSettingsScreen(sharedViewModel = sharedViewModel)
            }
        }
    }
}
