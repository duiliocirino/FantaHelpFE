# Backend Changes Required for FantaHelpFE Alignment

The frontend audit found 3 missing fields in Read DTOs that cause Moshi deserialization crashes on the FE side, plus 1 tracked feature gap.

---

## Status

| # | Issue | Status | Notes |
|---|---|---|---|
| 1 | `TeamReadDto` missing `OwnerId` | ✅ RESOLVED | BE added `OwnerId` property, mapped from `Team.OwnerId` |
| 2 | `TeamPlayerReadDto` missing `PlayerRole` | ✅ RESOLVED | BE added `PlayerRole` property, mapped from `TeamPlayer.Player.Role` |
| 3 | `LeagueReadDto` missing `GoalBonusPerRole` | ✅ RESOLVED | BE added `GoalBonusPerRole` property, mapped from `League.GoalBonusPerRole` |
| 4 & 5 | `SuggestionRequest.AuctionedPlayer` & Single-Call Contract | ✅ RESOLVED | BE merged single-call contract: `SuggestionResult` contains `score` (base optimal) and `potentialScore` (`SuggestionResult?`, potential optimal). Shared per-role DP tables. Returns `potentialScore = null` if budget is exceeded or `auctionedPlayer` is null. Integrated on FE. |
| 6 | `GET /api/teams/{id}` Roster Inclusion | ✅ RESOLVED | BE added `.Include(t => t.TeamPlayers)` to `GET /api/teams/{id}` endpoint. Roster items map properly to `TeamReadDto.Players`. Integrated on FE. |

---

## Resolved Changes (Items 1–6)

### 1. `TeamReadDto` — Missing `OwnerId` ✅

**File**: `Fantahelp.API/Models/Dtos/Read/TeamReadDto.cs`

**Added**:
```csharp
public required int OwnerId { get; set; }
```

**Mapper updated** to include `OwnerId = team.OwnerId`.

---

### 2. `TeamPlayerReadDto` — Missing `PlayerRole` ✅

**File**: `Fantahelp.API/Models/Dtos/Read/TeamPlayerReadDto.cs`

**Added**:
```csharp
public required string PlayerRole { get; set; }
```

**Mapper updated** to include `PlayerRole = teamPlayer.Player.Role`.

---

### 3. `LeagueReadDto` — Missing `GoalBonusPerRole` ✅

**File**: `Fantahelp.API/Models/Dtos/Read/LeagueReadDto.cs`

**Added**:
```csharp
public bool GoalBonusPerRole { get; set; }
```

**Mapper updated** to include `GoalBonusPerRole = league.GoalBonusPerRole`.

---

## Action Required (Item 6) 🚨

### 6. `GET /api/teams/{id}` — Include Roster (`Players` collection)

**File**: `Fantahelp.API/Controllers/TeamsController.cs` (or `GetTeamById` handler)

**Issue**: While `POST /api/teams/getOptimal` correctly includes assigned team players when generating squad suggestions, `GET /api/teams/{id}` returns an empty `players: []` array on `TeamReadDto`.

**Fix Required**:
Ensure EF Core explicitly includes `TeamPlayers` / `Players` when retrieving the team entity:

```csharp
var team = await _context.Teams
    .Include(t => t.TeamPlayers)
    .ThenInclude(tp => tp.Player)
    .FirstOrDefaultAsync(t => t.Id == id);
```

And map `team.TeamPlayers` to `TeamReadDto.Players`.


---

## Agreed Contract (Items 4 & 5) 🤝

### 4 & 5. Single-Call Convenience Score Contract (`POST /api/teams/getOptimal`)

**Endpoint**: `POST /api/teams/getOptimal`

**Contract Specification**:
When `auctionedPlayer` is present in `SuggestionRequest`, the backend returns a **single `SuggestionResult` object** containing two score properties:

```csharp
public class SuggestionResult
{
    public List<PlayerReadDto> SuggestedPlayers { get; set; }
    public int TotalExpectedPrice { get; set; }
    public int TotalExpectedPriceStd { get; set; }
    public Score Score { get; set; } // Base optimal score (current roster only)
    public Score? PotentialScore { get; set; } // Potential optimal score (with auctioned player)
}
```

**Key Behaviors**:
1. **Performance**: Heavy per-role DP tables run **once** and are shared between `Score` and `PotentialScore`. Only the cheap final combination step runs twice, achieving the **50% CPU reduction**.
2. **`auctionedPlayer == null`**: `PotentialScore` is `null` (standard squad suggestion behavior).
3. **Budget Exceeded Edge Case**: If `acquisitionPrice > remainingBudget`, `PotentialScore` is set to `null` (signals to FE that the team cannot afford the bid).

---

## Future Enhancements / Feature Backlog 🔮

### 7. `SuggestionResult` — `RecommendedMaxBid` Break-Even Calculation

**File**: `Fantahelp.API/Models/Dtos/Read/SuggestionResult.cs` & `TeamSuggestionService.cs`

**Concept**: When `auctionedPlayer` is provided in `POST /api/teams/getOptimal`, compute the maximum acquisition price (`RecommendedMaxBid`) before the net point advantage turns neutral or negative due to remaining budget penalties.

**Proposed DTO Field**:
```csharp
public int? RecommendedMaxBid { get; set; }
```

**Behavior**:
Return the break-even maximum bid price alongside `PotentialScore`. The FE will display AI bidding advice to the user during live auctions (e.g. *"Your bid of 35 CR is 13 CR below your maximum profitable bid of 48 CR"*).


