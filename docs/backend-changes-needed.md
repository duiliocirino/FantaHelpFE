# Backend Changes Required for FantaHelpFE Alignment

The frontend audit found 3 missing fields in Read DTOs that cause Moshi deserialization crashes on the FE side, plus 1 tracked feature gap.

---

## Status

| # | Issue | Status | Notes |
|---|---|---|---|
| 1 | `TeamReadDto` missing `OwnerId` | ✅ RESOLVED | BE added `OwnerId` property, mapped from `Team.OwnerId` |
| 2 | `TeamPlayerReadDto` missing `PlayerRole` | ✅ RESOLVED | BE added `PlayerRole` property, mapped from `TeamPlayer.Player.Role` |
| 3 | `LeagueReadDto` missing `GoalBonusPerRole` | ✅ RESOLVED | BE added `GoalBonusPerRole` property, mapped from `League.GoalBonusPerRole` |
| 4 | `SuggestionRequest` missing `AuctionedPlayer` | 🔶 TRACKED | DTO field added and deserialized. Scoring logic to use it in potential score calculation is in the BE backlog. Until then, convenience delta reads zero. |
| 5 | Convenience Score Single-Call Optimization | 💡 PROPOSED | Optimize `POST /api/teams/getOptimal` when `auctionedPlayer` is present to return `result[0]` (Base) and `result[1]` (Potential) in a single call, cutting server solver calls by 50%. |

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

## Tracked & Proposed (Items 4–5)

### 4. `SuggestionRequest` — Missing `AuctionedPlayer` 🔶

**File**: `Fantahelp.API/Models/Dtos/SuggestionRequest.cs`

The FE sends `auctionedPlayer` (nullable object with `playerId` and `acquisitionPrice`) in the "potential score" convenience-score request. The BE now deserializes this field correctly, but the scoring logic to actually use it in the potential score calculation is tracked in the BE backlog.

**FE-side DTO already present** (`AuctionedPlayerInfo` with `playerId` and `acquisitionPrice`).

**Impact**: Non-blocking. Convenience delta will read zero until the BE implements the scoring logic.

---

### 5. Single-Call Optimization for Convenience Score (`POST /api/teams/getOptimal`) 💡

**Endpoint**: `POST /api/teams/getOptimal`

**Current Behavior**: To compute the "Convenience Score Delta" (Advantage of buying a player), the FE makes 2 parallel requests to `POST /api/teams/getOptimal`:
1. `baseRequest` (`auctionedPlayer = null`) -> Base Optimal Score
2. `potentialRequest` (`auctionedPlayer = { playerId, acquisitionPrice }`) -> Potential Optimal Score

**Proposed Optimization**:
When `auctionedPlayer` is present in `SuggestionRequest`, the backend should execute both calculations in a single request context (reusing team data & league rules) and return a 2-element array `[SuggestionResult]`:
- `result[0]`: **Base Optimal Suggestion** (current roster)
- `result[1]`: **Potential Optimal Suggestion** (with `auctionedPlayer` acquired)

**Impact & Benefits**:
- Cuts backend optimization solver CPU execution time by **50%**.
- Reduces mobile client HTTP roundtrips from 2 to 1.
- Eliminates duplicate database queries for league rules and available player pools.

---

## Impact

Items 1–3 were **hard crashes** on the FE side (Moshi `JsonDataException`) for every league/team/player fetch. Now resolved — the auction flow functions without deserialization errors.

Item 4 is non-blocking but makes the convenience score feature return incorrect data (delta always zero) until the BE scoring logic is implemented.

Item 5 is a recommended backend performance optimization to halve server load during live auctions.

