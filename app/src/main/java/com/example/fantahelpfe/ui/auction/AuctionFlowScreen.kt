package com.example.fantahelpfe.ui.auction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalPager
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberPagerState
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionFlowScreen(
    navController: NavController,
    sharedViewModel: AuctionSharedViewModel
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Buy", "My Team", "Market", "Settings")
    
    // We use a Pager for smooth transitions and to keep state of tabs
    val pagerState = rememberPagerState(pageCount = { tabs.size })

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
