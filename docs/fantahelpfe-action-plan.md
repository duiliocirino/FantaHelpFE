# FantaHelpFE - Action Plan to Auction-Ready State

## Goal
Make the frontend fully usable for conducting a fantasy football auction. The user should be able to:
- Select a league and enter the auction
- See available players and their details
- Search, bid on, and assign players to teams
- View their team roster and manage it (add/remove players)
- See real-time budget and remaining players
- Configure auction settings (favorites, lineup, budget spread)
- Get optimal team suggestions at any point

---

## Phase 0: Housekeeping & Foundation

### 0.1 Clean up dead code
- **Remove `AuctionScreen.kt` and `AuctionViewModel.kt`** -- these are an older/simpler auction screen that is not wired into the navigation graph. They are dead code that will confuse future development.
- **Remove `PlayersScreen.kt` and `PlayersViewModel.kt`** -- not wired into navigation. The player listing functionality is already covered by the available-players endpoint used in the auction flow.
- **Delete `app/src/main/new.xml`** -- stray file with no purpose.

### 0.2 Rename `YourAppError` to `AppError`
- The class is named `YourAppError` (placeholder from AI generation). Rename to `AppError` everywhere.
- Files affected: `utils/AppError.kt`, all catch blocks in ViewModels and Repository.

### 0.3 Fix `PlayerMapper` TODOs
- `age` is hardcoded to `25`. The backend `PlayerReadDto` doesn't return `age` yet. Either remove the field from the domain model or map it properly once the backend supports it.
- `injury` is hardcoded to `3`. Same situation.
- `mate` is hardcoded to `null`. The backend Player model has a `Mate` field. Map it from the DTO.
- The backend `PlayerReadDto` does NOT include `age`, `injury`, or `mate`. These need to be added to the backend DTO or removed from the frontend domain model.
- **Decision needed**: For now, remove `age`, `injury`, and `mate` from the `Player` domain model. Add them back when the backend exposes them.

### 0.4 Fix `League` domain model drift
- Backend `League` has `GoalBonusPerRole: bool` but frontend `League` and `LeagueReadDto` do NOT include it.
- The suggestion algorithm uses `league.GoalBonusPerRole` in score calculation.
- **Action**: Add `goalBonusPerRole` to both `LeagueReadDto` and the `League` domain model. Map it in `LeagueMappers.kt`. Default to `true` in the DTO for backward compatibility.

### 0.5 Fix `Team` domain model drift
- Backend `Team` has `OwnerId` and `Owner` navigation property. Frontend `Team` does not include `ownerId`.
- For the auction flow, we need to know which team belongs to which user.
- **Action**: Add `ownerId` to both `TeamReadDto` and the `Team` domain model. Map it in `TeamMappers.kt`.

### 0.6 Fix `TeamPlayer` domain model
- The domain model has `var teamId: Int` (mutable). Should be `val`.
- Missing `playerRole` field -- currently only stores `playerName` as a string. For the "my team" screen, we need the player's role too.
- **Action**: Add `playerRole` to `TeamPlayerReadDto` (requires backend change) and domain model. Make `teamId` immutable.

### 0.7 Implement `UserPreferencesRepository` with DataStore
- Currently in-memory only. Settings are lost on app restart.
- **Action**: Add DataStore dependency, implement proper persistence for `AuctionSettings`.
- Dependencies to add: `androidx.datastore:datastore-preferences`

### 0.8 Add `baseUrl` to config
- Currently hardcoded as `http://10.0.2.2:60001/` in `AppModule.kt`.
- **Action**: Move to a `BuildConfig` field or `local.properties` so it can differ between dev/prod without code changes.

---

## Phase 1: Core API Gaps

### 1.1 Add missing API endpoints to `ApiService.kt`

**Remove player from team:**
```kotlin
@DELETE("api/teams/{teamId}/players/{playerId}")
suspend fun removePlayerFromTeam(@Path("teamId") teamId: Int, @Path("playerId") playerId: Int): TeamReadDto
```

**Delete team:**
```kotlin
@DELETE("api/teams/{id}")
suspend fun deleteTeam(@Path("id") teamId: Int): Unit
```

**Delete league:**
```kotlin
@DELETE("api/leagues/{leagueId}")
suspend fun deleteLeague(@Path("leagueId") leagueId: Int): Unit
```

**Get all teams from league (dedicated endpoint):**
```kotlin
@GET("api/leagues/{leagueId}/teams")
suspend fun getTeamsFromLeague(@Path("leagueId") leagueId: Int): List<TeamReadDto>
```

### 1.2 Update `DataRepository` interface and implementation
- Add `removePlayerFromTeam(teamId, playerId)`
- Add `deleteTeam(teamId)`
- Add `deleteLeague(leagueId)`
- Add `getTeamsFromLeague(leagueId)` (already partially covered by `getTeamsForLeague` but that goes through `getLeagueById` -- use the dedicated endpoint for efficiency)

### 1.3 Fix `getTeamsForLeague` in repository
- Currently fetches the entire league object just to extract teams. Inefficient.
- **Action**: Use the new dedicated endpoint `GET api/leagues/{id}/teams` instead.

### 1.4 Add error interceptor to Retrofit
- Currently, HTTP errors (4xx, 5xx) are not caught and mapped to `AppError` types.
- Retrofit will throw `HttpException` on non-2xx responses.
- **Action**: Add an `ErrorHandler` in the repository that catches `HttpException` and maps it to appropriate `AppError` subclasses (NotFound, ApiError, etc.).

---

## Phase 2: Auction Flow Navigation

### 2.1 Redesign the `AuctionFlow` nested graph

Current state: Only `AuctionBuyPlayers` and `AuctionSettings` screens exist.

Target structure:
```
AuctionFlow/{leagueId} (nested graph)
├── AuctionTabBar (top-level composable with BottomAppBar/TabRow)
│   ├── AuctionBuyPlayers (tab: "Buy")
│   ├── AuctionMyTeam (tab: "My Team")
│   ├── AuctionMarket (tab: "Market")
│   └── AuctionSettings (tab: "Settings")
```

### 2.2 Add navigation routes
- `Screen.AuctionMyTeam` -- route: `"auction_my_team"`
- `Screen.AuctionMarket` -- route: `"auction_market"`

### 2.3 Create tab-based navigation inside AuctionFlow
- Instead of separate composable entries, use a single composable that hosts a `TabRow` + `Tab` content.
- The `AuctionSharedViewModel` is scoped to the entire graph and shared across all tabs.
- This avoids the complexity of passing `sharedViewModel` through multiple nav graph entries.

### 2.4 Update `AppNavigation.kt`
- Replace the current nested navigation graph with a single composable that contains the tab bar.
- The tab bar switches between: Buy Players, My Team, Market, Settings.

---

## Phase 3: AuctionBuyPlayers Improvements

### 3.1 Fix `League.getLineUp()` dummy extension
- Currently returns hardcoded values: `LineUp(keepers=1, defenders=4, midfielders=3, attackers=3)`
- **Option A**: Add `lineUp` field to the backend `League` model and `LeagueReadDto`. Store it in the database.
- **Option B**: Make it configurable in `AuctionSettings` and persist it per league.
- **Recommendation**: Option B for now (simpler, no backend change). Add a `LineUp` field to `AuctionSettings`. The user configures it in the Settings tab before starting the auction.

### 3.2 Improve player search
- Currently does client-side filtering of the full player list.
- For large player pools (200+), this is slow and memory-intensive.
- **Action**: Keep client-side search for now (it's fine for ~500 players). Add pagination or server-side search only if performance becomes an issue.
- **Immediate fix**: The search fetches ALL players into memory on first call. Add a null-coalescing check and handle the case where the cache hasn't been populated yet.

### 3.3 Fix convenience score calculation edge cases
- The `calculateConvenienceScore` function requires: player selected, team selected, valid bid, lineup configured.
- If any of these is missing, scores are cleared. This is correct.
- **Issue**: The `LaunchedEffect` in the screen triggers recalculation on every state change. With 6 keys, this fires very frequently.
- **Action**: Add a minimum debounce time (already 300ms, keep it). Consider using `flow.combine` + `debounce` in the ViewModel instead of `LaunchedEffect` in the composable.

### 3.4 Add budget validation
- Before assigning a player, check that the team's `remainingBudget >= bidAmount`.
- Show a clear error message if the bid exceeds remaining budget.
- **Action**: Add validation in `assignPlayer()` function.

### 3.5 Refresh team data after assignment
- After successful assignment, the team's `remainingBudget` changes.
- **Action**: After `assignPlayer` succeeds, re-fetch the selected team's data to update the budget display.

### 3.6 Show player availability status
- When searching, indicate if a player is already assigned to another team in the league.
- The `getAvailablePlayersForLeague` endpoint already filters this. Use it instead of `getAllPlayers` for search.
- **Action**: Change `searchPlayer` in the repository to use `getAvailablePlayersForLeague` + client-side filter, instead of `getAllPlayers` + client-side filter.

---

## Phase 4: AuctionMyTeam Screen (NEW)

### 4.1 Create `AuctionMyTeamViewModel`
- Loads the user's team(s) in the current league.
- Displays team roster with player details (name, role, auction price).
- Shows remaining budget.
- Supports removing players from the team.
- Supports requesting a new optimal team suggestion.

### 4.2 UI State
```kotlin
data class AuctionMyTeamUiState(
    val teams: List<Team> = emptyList(),
    val selectedTeam: Team? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val suggestionResults: List<SuggestionResult>? = null,
    val isLoadingSuggestion: Boolean = false
)
```

### 4.3 Key actions
- `loadTeams()` -- fetch teams for the current league, filter by owner
- `selectTeam(team)` -- select which team to view/manage
- `removePlayer(playerId)` -- remove player from team, update budget
- `requestSuggestion()` -- call the suggestion API with current team state
- `refreshTeam()` -- re-fetch team data

### 4.4 UI Components
- Team selector (if user owns multiple teams)
- Team header: name, remaining budget, player count
- Player list: name, role, auction price, remove button
- "Get Suggestion" button
- Suggestion results panel (if loaded)

### 4.5 Wire into navigation
- Add as a tab in the AuctionFlow tab bar.

---

## Phase 5: AuctionMarket Screen (NEW)

### 5.1 Purpose
Show all players still available for auction in the current league, sorted by various criteria (price, rating, expected performance, role).

### 5.2 Create `AuctionMarketViewModel`
```kotlin
data class AuctionMarketUiState(
    val availablePlayers: List<Player> = emptyList(),
    val filteredPlayers: List<Player> = emptyList(),
    val isLoading: Boolean = false,
    val filterRole: PlayerRole? = null,
    val sortBy: SortOption = SortOption.Price,
    val searchQuery: String = "",
    val error: String? = null
)

enum class SortOption { Price, Rating, ExpectedPerformance, Name }
```

### 5.3 Key actions
- `loadAvailablePlayers()` -- fetch from `getAvailablePlayersForLeague`
- `filterByRole(role)` -- filter by selected role
- `sortBy(option)` -- sort by selected criterion
- `search(query)` -- client-side text search
- `refresh()` -- re-fetch available players

### 5.4 UI Components
- Search bar
- Role filter chips (P, D, C, A, All)
- Sort dropdown
- Player list with: name, squad, role, price, rating
- "Buy" button on each player (navigates to Buy tab with player pre-selected)

### 5.5 Wire into navigation
- Add as a tab in the AuctionFlow tab bar.

---

## Phase 6: AuctionSettings Enhancements

### 6.1 Add LineUp configuration
- Current settings: favorite players, credits distribution multiplier.
- Add: LineUp configuration (defenders, midfielders, attackers counts).
- Keepers is always 1 (hardcoded in the algorithm).
- Show constraints: e.g., "Total starters must be 11" or whatever the league rules are.

### 6.2 Add LineUp to `AuctionSettings`
```kotlin
data class AuctionSettings(
    val globalFavoritePlayerIds: List<Int> = emptyList(),
    val creditsDistributionMultiplier: Int = 1,
    val lineUp: LineUp = LineUp(keepers = 1, defenders = 4, midfielders = 3, attackers = 3)
)
```

### 6.3 Update `AuctionSettingsScreen`
- Add LineUp section with number pickers for defenders, midfielders, attackers.
- Show current configuration summary.
- Validate that values are reasonable (e.g., defenders 3-5, midfielders 2-5, attackers 2-5).

### 6.4 Remove dummy `getLineUp()` extension
- Once LineUp is in `AuctionSettings`, remove the extension function from `AuctionBuyPlayersViewModel.kt`.
- Update all callers to get LineUp from `sharedViewModel.settings.value.lineUp`.

---

## Phase 7: Shared State & Data Consistency

### 7.1 Centralize league context in `AuctionSharedViewModel`
- Currently stores `leagueId` and `settings`.
- Add: `leagueDetails: League?`, `availablePlayersCount: Int`, `lastRefreshTime: Long?`.
- Provide methods to refresh league data and available players count.

### 7.2 Add data refresh mechanism
- After any mutation (assign player, remove player), the available players list and team data change.
- **Action**: Add a `refreshAll()` method to `AuctionSharedViewModel` that invalidates caches and triggers re-fetches.
- Call this after every successful mutation.

### 7.3 Fix in-memory cache invalidation
- `DataRepositoryImpl` caches `playerCache` and `leaguesCache` in memory.
- These are never invalidated after mutations.
- **Action**: Add a `clearCache()` method to the repository. Call it after mutations.
- Alternatively, use a more sophisticated caching strategy (e.g., timed expiry).

### 7.4 Handle team ownership
- Currently, `getTeamsForLeague` returns ALL teams in a league.
- In the auction flow, the user should only see/interact with their own team(s).
- **Action**: Filter teams by `ownerId` in the ViewModel. The `ownerId` needs to come from the user context.
- **User context**: Currently hardcoded as `ownerId = 1` in `CreateLeagueViewModel`. Need a proper user selection mechanism.

### 7.5 Add user selection to the flow
- Before entering the auction, the user should select which user account they are acting as.
- **Action**: Add a user selection screen or integrate it into the league loading screen.
- Store the selected `userId` in `UserPreferencesRepository`.
- Use it to filter teams and validate operations.

---

## Phase 8: UI/UX Polish

### 8.1 Consistent loading/error states
- All screens should have consistent loading spinners and error messages with retry buttons.
- **Action**: Create shared composables for loading and error states.

### 8.2 Snackbar feedback
- Use Snackbar for operation results (player assigned, player removed, etc.).
- **Action**: Add `SnackbarHost` to the Scaffold in each screen.

### 8.3 Budget display
- Show remaining budget prominently in the Buy Players tab.
- Show budget change after assignment (e.g., "-50 CR" badge).
- **Action**: Add budget display to `AuctionBuyPlayersScreen`.

### 8.4 Player card details
- In the Buy Players tab, show more player details: squad, rating, expected performance, expected price.
- **Action**: Expand the selected player card.

### 8.5 Confirmation dialogs
- Before removing a player from a team, show a confirmation dialog.
- Before assigning a player, show a summary dialog (player, price, team, budget impact).
- **Action**: Add `AlertDialog` for confirmations.

### 8.6 Dark theme support
- The theme file exists but dark theme may not be fully implemented.
- **Action**: Verify and test dark theme. Fix any missing color definitions.

---

## Phase 9: Error Handling & Resilience

### 9.1 Retrofit error interceptor
- Add a global error handler that catches `HttpException` and maps to `AppError`.
- Handle: 404 (NotFound), 400 (BadRequest), 500 (ServerError), network errors.

### 9.2 Timeout handling
- The `getSuggestion` endpoint can take a long time (already set to 60s).
- Show a progress indicator with an estimated time message.
- Allow the user to cancel the request.
- **Action**: Add cancellation support to the convenience score calculation.

### 9.3 Offline detection
- Detect when the device has no network connection.
- Show a clear "No internet connection" message.
- **Action**: Add `ConnectivityManager` check before API calls.

### 9.4 Graceful degradation
- If the suggestion API is unavailable, the app should still work for basic operations (buy, remove, view).
- **Action**: Make suggestion calls optional -- show a warning but don't block the flow.

---

## Phase 10: Testing

### 10.1 Unit tests for ViewModels
- Test `AuctionBuyPlayersViewModel` state transitions.
- Test `AuctionSharedViewModel` settings persistence.
- Test convenience score calculation logic.

### 10.2 Unit tests for Repository
- Mock `ApiService` and test mapping logic.
- Test cache behavior.

### 10.3 UI tests for critical flows
- Test the buy player flow: search -> select -> bid -> assign.
- Test the my team flow: view -> remove player.

---

## Execution Order

The phases are ordered by dependency. Here is the recommended execution sequence:

```
Phase 0 (Housekeeping)
    |
Phase 1 (API Gaps)
    |
Phase 2 (Navigation Redesign)
    |
Phase 3 (Buy Players Improvements)  <---- can run in parallel with Phase 6
Phase 6 (Settings Enhancements)      <---- can run in parallel with Phase 3
    |
Phase 7 (Shared State & Consistency) <---- depends on Phases 3, 4, 5
Phase 4 (My Team Screen)             <---- depends on Phase 1
Phase 5 (Market Screen)              <---- depends on Phase 1
    |
Phase 8 (UI/UX Polish)               <---- depends on all screens being implemented
    |
Phase 9 (Error Handling)             <---- can be done incrementally
    |
Phase 10 (Testing)                   <---- final phase
```

---

## Decisions

1. **Single-user tool** -- No authentication. `ownerId = 1` is acceptable for now. Skip user selection screens.
2. **LineUp per-league** -- Store LineUp configuration per league in settings, not globally.

## Tracked Backend Dependencies

- **`SuggestionRequest.auctionedPlayer` missing** (FantaHelpBE): The frontend sends `auctionedPlayer` in the convenience-score request, but the backend `SuggestionRequest` does not have this property -- it is silently dropped during deserialization. As a result, the "potential score" call returns the same value as the "base score" call, making the convenience delta always zero. Fix required in `FantaHelpBE/Fantahelp.API/Models/Dtos/SuggestionRequest.cs` and `TeamSuggestionService.cs`. Handle in the BE repo separately.

## Risks & Open Questions

1. **Budget enforcement**: The backend does NOT enforce budget constraints when adding a player to a team. The frontend must enforce this client-side.

2. **Concurrent auctions**: What happens if two users try to buy the same player? The backend has no locking mechanism. For now, assume single-user usage.

3. **Player data freshness**: The suggestion algorithm uses `ExpectedPrice` for current team players (set to `AuctionPrice`) but the standard `ExpectedPrice` for available players. This is intentional and documented in the backend code.

---

## Estimated Effort

| Phase | Complexity | Estimated Time |
|-------|-----------|----------------|
| 0 - Housekeeping | Low | 2-3 hours |
| 1 - API Gaps | Low | 1-2 hours |
| 2 - Navigation | Medium | 2-3 hours |
| 3 - Buy Players | Medium | 3-4 hours |
| 4 - My Team | High | 4-6 hours |
| 5 - Market | Medium | 3-4 hours |
| 6 - Settings | Low | 1-2 hours |
| 7 - Shared State | Medium | 3-4 hours |
| 8 - UI/UX | Medium | 3-4 hours |
| 9 - Error Handling | Medium | 2-3 hours |
| 10 - Testing | High | 4-6 hours |
| **Total** | | **~30-45 hours** |
