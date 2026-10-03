# Параметры · guilds

Точный справочник значений файла. Индексы в квадратных скобках начинаются с нуля. Отсутствующие в JSON поля получают значения модели Kotlin; этот раздел показывает именно явно заданные параметры, а не полный результат загрузки.

| Путь параметра | Значение |
| --- | --- |
| `create.level` | 20 |
| `create.gold` | 10000 |
| `members.base` | 10 |
| `members.perLevel` | 5 |
| `members.max` | 30 |
| `officers` | 3 |
| `leaderIdleDays` | 14 |
| `rejoinHours` | 24 |
| `contribution.dailyPerLevel` | 2000 |
| `levels` | 0; 20000; 60000; 140000; 300000; 600000; 1100000; 1900000; 3100000; 5000000 |
| `ranks[0].code` | `NOVICE` |
| `ranks[0].from` | 0 |
| `ranks[1].code` | `BROTHER` |
| `ranks[1].from` | 5000 |
| `ranks[2].code` | `VETERAN` |
| `ranks[2].from` | 25000 |
| `ranks[3].code` | `CHAMPION` |
| `ranks[3].from` | 100000 |
| `ranks[4].code` | `LEGEND` |
| `ranks[4].from` | 400000 |
| `factions[0].code` | `WARDENS` |
| `factions[0].icon` | `guild.faction.WARDENS` |
| `factions[0].color` | `#f0a23a` |
| `factions[1].code` | `HEEDING` |
| `factions[1].icon` | `guild.faction.HEEDING` |
| `factions[1].color` | `#7cc8f0` |
| `factions[2].code` | `DEEPBOUND` |
| `factions[2].icon` | `guild.faction.DEEPBOUND` |
| `factions[2].color` | `#a36cf0` |
| `emblems` | `SWORD`; `SHIELD`; `AXE`; `HAMMER`; `CROWN`; `SKULL`; `WOLF`; `EAGLE`; `TOWER`; `FLAME`; `SUN`; `COIN` |
| `colors` | `#B23A3A`; `#D98C2B`; `#D9C23A`; `#4F9A3C`; `#2F8F8F`; `#3A64B2`; `#7A45B2`; `#8A8A8A` |
| `announcement` | 200 |
| `name` | 3; 24 |
| `tag` | 2; 4 |
| `tree.rowGate` | 3 |
| `tree.respecDays` | 7 |
| `tree.pointsPerLevel` | 1 |
| `tree.nodes[0].code` | `C_LIFE` |
| `tree.nodes[0].branch` | `COMBAT` |
| `tree.nodes[0].row` | 1 |
| `tree.nodes[0].max` | 3 |
| `tree.nodes[0].effect` | `BREW_LIFE` |
| `tree.nodes[0].perRank` | 2 |
| `tree.nodes[1].code` | `C_DAMAGE` |
| `tree.nodes[1].branch` | `COMBAT` |
| `tree.nodes[1].row` | 1 |
| `tree.nodes[1].max` | 3 |
| `tree.nodes[1].effect` | `BREW_DAMAGE` |
| `tree.nodes[1].perRank` | 2 |
| `tree.nodes[2].code` | `C_RESIST` |
| `tree.nodes[2].branch` | `COMBAT` |
| `tree.nodes[2].row` | 1 |
| `tree.nodes[2].max` | 3 |
| `tree.nodes[2].effect` | `BREW_RESIST` |
| `tree.nodes[2].perRank` | 3 |
| `tree.nodes[3].code` | `C_FURY` |
| `tree.nodes[3].branch` | `COMBAT` |
| `tree.nodes[3].row` | 2 |
| `tree.nodes[3].max` | 3 |
| `tree.nodes[3].effect` | `BREW_DAMAGE` |
| `tree.nodes[3].perRank` | 4 |
| `tree.nodes[4].code` | `C_BULWARK` |
| `tree.nodes[4].branch` | `COMBAT` |
| `tree.nodes[4].row` | 2 |
| `tree.nodes[4].max` | 3 |
| `tree.nodes[4].effect` | `BREW_LIFE` |
| `tree.nodes[4].perRank` | 4 |
| `tree.nodes[5].code` | `L_GOLD` |
| `tree.nodes[5].branch` | `LOOT` |
| `tree.nodes[5].row` | 1 |
| `tree.nodes[5].max` | 3 |
| `tree.nodes[5].effect` | [Атлас: золото](Modifiers-ATLAS.md#atlas_gold) |
| `tree.nodes[5].perRank` | 5 |
| `tree.nodes[6].code` | `L_EXPERIENCE` |
| `tree.nodes[6].branch` | `LOOT` |
| `tree.nodes[6].row` | 1 |
| `tree.nodes[6].max` | 3 |
| `tree.nodes[6].effect` | [Атлас: опыт на картах](Modifiers-ATLAS.md#atlas_experience) |
| `tree.nodes[6].perRank` | 3 |
| `tree.nodes[7].code` | `L_RARITY` |
| `tree.nodes[7].branch` | `LOOT` |
| `tree.nodes[7].row` | 1 |
| `tree.nodes[7].max` | 3 |
| `tree.nodes[7].effect` | [Атлас: редкость предметов на картах](Modifiers-ATLAS.md#atlas_rarity) |
| `tree.nodes[7].perRank` | 4 |
| `tree.nodes[8].code` | `L_QUANTITY` |
| `tree.nodes[8].branch` | `LOOT` |
| `tree.nodes[8].row` | 2 |
| `tree.nodes[8].max` | 3 |
| `tree.nodes[8].effect` | [Атлас: количество предметов на картах](Modifiers-ATLAS.md#atlas_quantity) |
| `tree.nodes[8].perRank` | 3 |
| `tree.nodes[9].code` | `L_CHESTS` |
| `tree.nodes[9].branch` | `LOOT` |
| `tree.nodes[9].row` | 2 |
| `tree.nodes[9].max` | 3 |
| `tree.nodes[9].effect` | [Атлас: добыча из сундуков](Modifiers-ATLAS.md#atlas_chest_loot) |
| `tree.nodes[9].perRank` | 10 |
| `tree.nodes[10].code` | `E_TABS` |
| `tree.nodes[10].branch` | `ECONOMY` |
| `tree.nodes[10].row` | 1 |
| `tree.nodes[10].max` | 3 |
| `tree.nodes[10].effect` | `GUILD_STASH_TABS` |
| `tree.nodes[10].perRank` | 1 |
| `tree.nodes[11].code` | `E_CRAFT` |
| `tree.nodes[11].branch` | `ECONOMY` |
| `tree.nodes[11].row` | 1 |
| `tree.nodes[11].max` | 3 |
| `tree.nodes[11].effect` | `GUILD_CRAFT_SPEED` |
| `tree.nodes[11].perRank` | 5 |
| `tree.nodes[12].code` | `E_LIMIT` |
| `tree.nodes[12].branch` | `ECONOMY` |
| `tree.nodes[12].row` | 1 |
| `tree.nodes[12].max` | 3 |
| `tree.nodes[12].effect` | `GUILD_DAILY_LIMIT` |
| `tree.nodes[12].perRank` | 10 |
| `tree.nodes[13].code` | `E_TAKES` |
| `tree.nodes[13].branch` | `ECONOMY` |
| `tree.nodes[13].row` | 2 |
| `tree.nodes[13].max` | 3 |
| `tree.nodes[13].effect` | `GUILD_TAKES` |
| `tree.nodes[13].perRank` | 2 |
| `tree.nodes[14].code` | `E_GROWTH` |
| `tree.nodes[14].branch` | `ECONOMY` |
| `tree.nodes[14].row` | 2 |
| `tree.nodes[14].max` | 3 |
| `tree.nodes[14].effect` | `GUILD_GROWTH` |
| `tree.nodes[14].perRank` | 5 |
| `stash.tabSize` | 50 |
| `stash.baseTabs` | 1 |
| `stash.takesPerDay` | 10 |
| `stash.maxTabs` | 4 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/guilds.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
