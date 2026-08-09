# Backend Changes Required for FantaHelpFE Alignment

The frontend audit found 3 missing fields in Read DTOs that cause Moshi deserialization crashes on the FE side, plus 1 tracked feature gap.

---

## Status

| # | Issue | Status | Notes |
|---|---|---|---|
| 1 | `TeamReadDto` missing `OwnerId` | ✅ RESOLVED | BE added `OwnerId` property, mapped from `Team.OwnerId` |
| 2 | `TeamPlayerReadDto` missing `PlayerRole` | ✅ RESOLVED | BE added `PlayerRole` property, mapped from `TeamPlayer.Player.Role` |
| 3 | `LeagueReadDto` missing `GoalBonusPerRole` | ✅ RESOLVED | BE added `GoalBonusPerRole` property, mapped from `League.GoalBonusPerRole` |
| 4 & 5 | `SuggestionRequest.AuctionedPlayer` & Single-Call Contract | ⏳ IN PROGRESS | Agreed contract: `SuggestionResult` contains `score` (base optimal) and `potentialScore` (`Score?`, potential optimal). Shares heavy DP calculation, reducing CPU load by 50%. Returns `potentialScore = null` if `auctionedPlayer` is null or budget is exceeded. |

---

## Resolved Changes (Items 1–3)

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

## Impact

Items 1–3 were **hard crashes** on the FE side (Moshi `JsonDataException`) for every league/team/player fetch. Now resolved.

Items 4 & 5 are currently in progress on the BE. FE DTO (`SuggestionResult.kt`) and ViewModel (`AuctionBuyPlayersViewModel.kt`) are updated with `potentialScore: Score?` to support this unified contract natively with backward compatibility fallback.


