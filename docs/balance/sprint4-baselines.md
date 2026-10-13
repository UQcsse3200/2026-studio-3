# Sprint 4 — Encounter baselines (Zeyu Wang / M5)

Snapshot against `origin/feature/sprint4-cards-game` @ `bff411f3` (13 Oct 2026).

**Ownership:** Team 1 owns enemy / encounter numbers in `enemies.json`, `encounters.json`, and `EnemyScaling`. This file is a **read-only snapshot for Team 6 balance review**, not a CI lock. When Team 1 changes those values, **Zeyu (Team 6 M5)** updates this doc; do not hard-fail tests on these figures.

Player baseline from `configs/player.json`: **100 / 100 HP**, **3 energy**, opening hand **5**. Campfire rest restores **30%** max HP between fights when taken.

## Config pressure (snapshot before live targets)

Scaled with `EnemyScaling`. Armour is never scaled.

### Progression 0

| Tier | Encounter (ids) | Total enemy HP | Notable ATK | Notes |
| --- | --- | --- | --- | --- |
| Normal | `crawler_alone` — bone_crawler | 30 | 5 (+2 armour) | Low pressure opener |
| Normal | `shade_pair` — 2× lesser_shade | 48 | 6 each | Dual chip; erratic AI |
| Normal | `crawler_and_shade` | 54 | 5 / 6 | Mixed |
| Elite | `guardian_and_shade` | 96 | 10 (+5 armour) / 6 | Taunt + shield-break retaliation |
| Elite | `void_knight_alone` | 40 | **16** | Low HP, high burst — spike candidate |
| Boss | `boss_knight` — Vitium | 150 | 14 (+8 armour) | Phased AI (steel / walls / silence) |

### Progression 5 (mid-map)

| Enemy | Scaled HP | Scaled ATK | Armour |
| --- | --- | --- | --- |
| bone_crawler | 42 | 6 | 2 |
| lesser_shade | 34 | 8 | 0 |
| tomb_guardian | 108 | 14 | 5 |
| void_knight | 60 | 22 | 0 |
| boss_knight | 210 | 18 | 8 |

## Difficulty flags (for joint review — no edits made)

1. **Void Knight burst**: base ATK 16 (22 @ p5) on 40–60 HP. Unavoidable burst deaths possible if Block/Armour is thin.
2. **Boss HP pool**: 150 → 210 @ p5 with phases can prolong fights without raising threat every turn.
3. **Boss uses NORMAL scaling rates** in `EnemyScaling` (not a separate boss curve).
4. Recovery between encounters: Campfire 30% only at forced campfire height; Chance healer +20 / shrine −10 are Team 2 values.

## Live playtest log template

Fill during Yu Zhu’s playtest rounds (seeds + routes). Do not invent numbers.

| Date | Seed | Encounter | Turns | Dmg taken / maxHP | HP after | Energy notes | Tester |
| --- | --- | --- | --- | --- | --- | --- | --- |
| | | normal: | | | | | |
| | | elite: | | | | | |
| | | boss: | | | | | |
