# FantaHelpFE Project - Auction Feature Summary

## Task 1: Understanding the FantaHelpFE Project - Auction Feature

Let's synthesize our previous discussions to give you a clear picture of what we were working on, what got done, and what the next steps were.

---

## Project Context

- **Project Name**: FantaHelpFE (FantaHelp Front End)
- **Overall Goal**: To develop an Android application for fantasy football (or a similar fantasy sports context) to assist users, likely in managing their teams, players, and auctions.
- **Technology Stack**:
    - Jetpack Compose for UI
    - Hilt for dependency injection
    - Retrofit/OkHttp for network calls
    - Moshi for JSON serialization/deserialization
    - Kotlin Coroutines for asynchronous operations
    - Jetpack Navigation Compose for navigation

---

## What We Were Working On (The Problem Space)

The primary focus of our recent efforts was on implementing the **Auction feature**, specifically the `AuctionBuyPlayersScreen`, which involves:

1. **Player Search**: Allowing users to search for players available for auction.
2. **Bidding**: Entering and adjusting bid amounts for selected players.
3. **Team Assignment**: Assigning successfully acquired players to a user's team within a league.
4. **"Convenience Score" Calculation**: A key analytical feature. This score helps users decide if acquiring a player is a "good deal" by comparing:
    - The "optimal score" of their current team roster.
    - The "potential optimal score" of their team if they acquire the selected player at the proposed bid amount.
    - The difference (delta) between these two scores represents the "convenience score."
    - This relies on a backend `getOptimalTeam` (or `getSuggestion`) API.
5. **Auction Settings**: Managing global settings relevant to the auction, such as preferred player IDs or credit distribution multipliers, which are shared across different auction screens.

---

## What We Did (Implemented Features & Solved Problems)

Here's a breakdown of the progress made:

### Core Auction ViewModels

**`AuctionBuyPlayersViewModel.kt`**
- Manages the UI state (`AuctionBuyPlayersUiState`) for the `AuctionBuyPlayersScreen`.
- Handles:
    - Fetching league details and teams upon initialization.
    - Player search functionality with debouncing (`onSearchQueryChanged`).
    - Selecting a player (`onPlayerSelectedFromSearch`).
    - Managing bid input (`onBidAmountChanged`, `adjustBid`).
    - Selecting a team for player assignment (`onTeamSelectedForAssignment`).
    - Fetching the current roster of the selected team (`fetchCurrentRosterForSelectedTeam`).
    - The crucial `calculateConvenienceScore` logic (making two parallel API calls to `repository.getSuggestion` for base and potential optimal scores).
    - `assignPlayer` logic to commit a player to a team with a bid.

**`AuctionSharedViewModel.kt`**
- A ViewModel scoped to the entire `AuctionFlow` navigation graph.
- Holds shared auction settings (like `globalFavoritePlayerIds` and `creditsDistributionMultiplier`) that might be configured in `AuctionSettingsScreen` and consumed by `AuctionBuyPlayersViewModel` for score calculation.
- Initializes with the `leagueId` from the navigation arguments.

---

### Data Layer Integration

**DataRepository and ApiService**
- The `DataRepositoryImpl` uses `ApiService` (Retrofit) for interacting with the backend for operations like:
    - `searchPlayer`
    - `getLeagueById`
    - `getTeamsForLeague`
    - `getSuggestion`
    - `assignPlayerToTeam`

**DTOs**
- We use various Data Transfer Objects (DTOs) for API communication (e.g., `SuggestionRequest`, `AuctionedPlayerInfo`, `LineUp`).

---

### Navigation Setup

**`AppNavigation.kt`**
- Defined the main `NavHost` and a nested navigation graph called `AuctionFlow`.

**`Screen.kt`**
- A sealed class defining all navigation routes, including:
    - `Screen.AuctionFlow` (route: `"auction_flow/{leagueId}"`)
    - Children: `Screen.AuctionBuyPlayers` and `Screen.AuctionSettings`

**`LoadLeagueScreen.kt`**
- This screen lists available leagues.
- Clicking a league now navigates to the `AuctionFlow` graph, passing the `leagueId` as a navigation argument (`navController.navigate(Screen.AuctionFlow.createRoute(leagueId))`).

---

### Resolved Technical Issues

**1. Hilt UserPreferencesRepository Missing Binding**
- Initially, Hilt didn't know how to provide `UserPreferencesRepository`.
- **Fix**: Added a `@Provides` method in `AppModule.kt` for `UserPreferencesRepository` (and implicitly for `DataStore` if needed by its constructor).

**2. Navigation IllegalArgumentException (Graph not on back stack)**
- When navigating from `LoadLeagueScreen` to `AuctionFlow`, there was an error where `AuctionBuyPlayersScreen` couldn't find its parent graph's `NavBackStackEntry`.
- **Fix**:
    - Ensured `AuctionSharedViewModel` is correctly annotated with `@HiltViewModel`.
    - In `AppNavigation.kt`, within the composable blocks for `AuctionBuyPlayersScreen` and `AuctionSettingsScreen`, explicitly called `val sharedViewModel: AuctionSharedViewModel = hiltViewModel()` (without parameters).
    - Hilt's navigation integration is designed to automatically scope this ViewModel to the surrounding navigation graph (`AuctionFlow`).
    - The `leagueId` argument, defined on the `AuctionFlow` graph, is now correctly extracted from the `backStackEntry.arguments` of the child screen's composable (`AuctionBuyPlayersScreen`).
    - The `sharedViewModel` is then passed as a parameter to both screens.

**3. API Call Timeouts (getOptimalTeam)**
- The `getSuggestion` API call (for convenience score) was timing out due to long processing on the backend.
- **Fix**: Configured `OkHttpClient` in `AppModule.kt` with increased `readTimeout` (e.g., 60 seconds) before passing it to Retrofit.

**4. Coroutine Cancellation (StandaloneCoroutine was cancelled)**
- Discussed the "StandaloneCoroutine was cancelled" error occurring during score calculation.
- **Key Takeaway**: This is often a normal and desirable outcome (e.g., ViewModel cleared, or a new calculation cancels an old one).
- **Action**: Ensure UI loading states are reset gracefully (e.g., in a `finally` block) and not to treat `CancellationException` as a user-facing error.

---

## What Was the Plan / Next Steps

Based on the conversation, here's what was left to do or refine:

### Complete Auction Flow Screens
- The `AuctionFlow` nested graph only has `AuctionBuyPlayersScreen` and `AuctionSettingsScreen` implemented.
- Placeholders for other auction screens:
    - `AuctionMyTeam`
    - `AuctionMarket`
    - `AuctionLiveBids`
- **Action**: These need their composable entries defined in `AppNavigation.kt` (using `hiltViewModel()` for `AuctionSharedViewModel` and passing it).

### League.getLineUp() Implementation
- The `getLineUp()` method on your `League` domain model was a placeholder returning a dummy `LineUp`.
- **Action**: Needs to be implemented concretely based on how league rules define lineups (e.g., fetched from the API as part of league details, or based on specific league types).

### Refine Error Handling/UI Feedback
- While basic error handling is in place, reviewing user-facing error messages and ensuring consistent feedback for loading, success, and various error states would be beneficial.

### General UI/UX
- Continue building out the Compose UI for all auction-related screens.