# FantaHelpFE - Future Roadmap & Steps 🚀

This document tracks upcoming features, structural refactorings, and enhancement ideas for future development phases.

---

## 🌍 1. Multi-Language & i18n Support
- **Goal**: Full internationalization for English (Default) and Italian (`it-IT`).
- **Tasks**:
  - Extract hardcoded strings across Composable UI screens into `res/values/strings.xml` and `res/values-it/strings.xml`.
  - Add language toggle in the **Settings Tab** or sync with OS system locale.
  - Localize position role names (e.g. `P` -> `GK / Portiere`, `D` -> `DEF / Difensore`, `C` -> `MID / Centrocampista`, `A` -> `ATT / Attaccante`).

---

## 📊 2. Advanced Auction Analytics & Visualizations
- **Goal**: Provide deeper visual insights into team balance and market trends.
- **Tasks**:
  - Add interactive squad balance chart (budget spent per role vs target budget allocation).
  - Price fluctuation timeline for auctioned players during active sessions.

---

## ⚡ 3. Real-Time Offline Caching & Session Recovery
- **Goal**: Ensure seamless offline resilience during fast-paced auction drafts.
- **Tasks**:
  - Cache available players list and optimal team suggestions locally using Room DB or DataStore.
  - Automatic background sync when connection is restored.

---

## 🔔 4. Live Notifications & Sound Effects
- **Goal**: Alert users during crucial auction moments.
- **Tasks**:
  - Haptic feedback and subtle audio cues when a recommended player's bid enters the "Must Buy" value threshold.
