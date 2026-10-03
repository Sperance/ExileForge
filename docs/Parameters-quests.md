# Параметры · quests

Точный справочник значений файла. Индексы в квадратных скобках начинаются с нуля. Отсутствующие в JSON поля получают значения модели Kotlin; этот раздел показывает именно явно заданные параметры, а не полный результат загрузки.

| Путь параметра | Значение |
| --- | --- |
| `goals[0].code` | `KILL` |
| `goals[0].category` | `COMBAT` |
| `goals[0].counter` | `KILLS` |
| `goals[0].base` | 40 |
| `goals[0].perLevel` | 0.6 |
| `goals[0].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[0].weight` | 14 |
| `goals[0].minLevel` | 1 |
| `goals[0].conditional` | Да |
| `goals[1].code` | `KILL_MAGIC` |
| `goals[1].category` | `COMBAT` |
| `goals[1].counter` | `KILLS_MAGIC` |
| `goals[1].base` | 8 |
| `goals[1].perLevel` | 0.1 |
| `goals[1].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[1].weight` | 10 |
| `goals[1].minLevel` | 1 |
| `goals[1].conditional` | Да |
| `goals[2].code` | `KILL_RARE` |
| `goals[2].category` | `COMBAT` |
| `goals[2].counter` | `KILLS_RARE` |
| `goals[2].base` | 3 |
| `goals[2].perLevel` | 0.03 |
| `goals[2].scope` | `ANY` |
| `goals[2].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[2].weight` | 8 |
| `goals[2].minLevel` | 1 |
| `goals[2].conditional` | Да |
| `goals[3].code` | `GUARDIANS` |
| `goals[3].category` | `COMBAT` |
| `goals[3].counter` | `BOSSES` |
| `goals[3].base` | 2 |
| `goals[3].perLevel` | 0.02 |
| `goals[3].scope` | `ANY` |
| `goals[3].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[3].weight` | 8 |
| `goals[3].minLevel` | 1 |
| `goals[3].conditional` | Да |
| `goals[4].code` | `RUNS` |
| `goals[4].category` | `COMBAT` |
| `goals[4].counter` | `RUNS` |
| `goals[4].base` | 3 |
| `goals[4].perLevel` | 0 |
| `goals[4].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[4].weight` | 8 |
| `goals[4].minLevel` | 1 |
| `goals[4].conditional` | Нет |
| `goals[5].code` | `CHESTS` |
| `goals[5].category` | `COMBAT` |
| `goals[5].counter` | `CHESTS` |
| `goals[5].base` | 3 |
| `goals[5].perLevel` | 0 |
| `goals[5].scope` | `ANY` |
| `goals[5].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[5].weight` | 6 |
| `goals[5].minLevel` | 1 |
| `goals[5].conditional` | Нет |
| `goals[6].code` | `CRYSTALS` |
| `goals[6].category` | `COMBAT` |
| `goals[6].counter` | `CRYSTALS` |
| `goals[6].base` | 2 |
| `goals[6].perLevel` | 0 |
| `goals[6].scope` | `ANY` |
| `goals[6].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[6].weight` | 4 |
| `goals[6].minLevel` | 10 |
| `goals[6].conditional` | Нет |
| `goals[7].code` | `ORBS` |
| `goals[7].category` | `CRAFT` |
| `goals[7].counter` | `ORBS_USED` |
| `goals[7].base` | 6 |
| `goals[7].perLevel` | 0.05 |
| `goals[7].scope` | `ANY` |
| `goals[7].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[7].weight` | 10 |
| `goals[7].minLevel` | 1 |
| `goals[7].conditional` | Нет |
| `goals[8].code` | `ESSENCES` |
| `goals[8].category` | `CRAFT` |
| `goals[8].counter` | `ESSENCES_USED` |
| `goals[8].base` | 2 |
| `goals[8].perLevel` | 0 |
| `goals[8].scope` | `ANY` |
| `goals[8].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[8].weight` | 4 |
| `goals[8].minLevel` | 10 |
| `goals[8].conditional` | Нет |
| `goals[9].code` | `CRAFT` |
| `goals[9].category` | `CRAFT` |
| `goals[9].counter` | `CRAFT_MADE` |
| `goals[9].base` | 2 |
| `goals[9].perLevel` | 0 |
| `goals[9].scope` | `ANY` |
| `goals[9].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[9].weight` | 6 |
| `goals[9].minLevel` | 1 |
| `goals[9].conditional` | Нет |
| `goals[10].code` | `WORK` |
| `goals[10].category` | `CRAFT` |
| `goals[10].counter` | `CRAFT_CYCLES` |
| `goals[10].base` | 3 |
| `goals[10].perLevel` | 0 |
| `goals[10].scope` | `ANY` |
| `goals[10].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[10].weight` | 5 |
| `goals[10].minLevel` | 1 |
| `goals[10].conditional` | Нет |
| `goals[11].code` | `SELL` |
| `goals[11].category` | `ECONOMY` |
| `goals[11].counter` | `ITEMS_SOLD` |
| `goals[11].base` | 8 |
| `goals[11].perLevel` | 0.05 |
| `goals[11].scope` | `ANY` |
| `goals[11].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[11].weight` | 8 |
| `goals[11].minLevel` | 1 |
| `goals[11].conditional` | Нет |
| `goals[12].code` | `EARN` |
| `goals[12].category` | `ECONOMY` |
| `goals[12].counter` | `GOLD_EARNED` |
| `goals[12].base` | 800 |
| `goals[12].perLevel` | 150 |
| `goals[12].scope` | `ANY` |
| `goals[12].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[12].weight` | 8 |
| `goals[12].minLevel` | 1 |
| `goals[12].conditional` | Нет |
| `goals[13].code` | `SPEND` |
| `goals[13].category` | `ECONOMY` |
| `goals[13].counter` | `GOLD_SPENT` |
| `goals[13].base` | 500 |
| `goals[13].perLevel` | 100 |
| `goals[13].scope` | `ANY` |
| `goals[13].kinds` | `DAILY`; `WEEKLY`; `CONTRACT`; `GUILD`; `GUILD_DAILY`; `GUILD_WEEKLY` |
| `goals[13].weight` | 5 |
| `goals[13].minLevel` | 1 |
| `goals[13].conditional` | Нет |
| `goals[14].code` | `AUCTION_SELL` |
| `goals[14].category` | `ECONOMY` |
| `goals[14].counter` | `AUCTION_SOLD` |
| `goals[14].base` | 1 |
| `goals[14].perLevel` | 0 |
| `goals[14].scope` | `ANY` |
| `goals[14].kinds` | `WEEKLY`; `CONTRACT`; `GUILD_WEEKLY` |
| `goals[14].weight` | 3 |
| `goals[14].minLevel` | 5 |
| `goals[14].conditional` | Нет |
| `goals[15].code` | `AUCTION_BUY` |
| `goals[15].category` | `ECONOMY` |
| `goals[15].counter` | `AUCTION_BOUGHT` |
| `goals[15].base` | 1 |
| `goals[15].perLevel` | 0 |
| `goals[15].scope` | `ANY` |
| `goals[15].kinds` | `WEEKLY`; `CONTRACT`; `GUILD_WEEKLY` |
| `goals[15].weight` | 3 |
| `goals[15].minLevel` | 5 |
| `goals[15].conditional` | Нет |
| `goals[16].code` | `LEVEL` |
| `goals[16].category` | `PROGRESS` |
| `goals[16].counter` | `LEVEL` |
| `goals[16].base` | 1 |
| `goals[16].perLevel` | 0 |
| `goals[16].scope` | `ANY` |
| `goals[16].kinds` | `WEEKLY`; `CONTRACT` |
| `goals[16].weight` | 4 |
| `goals[16].minLevel` | 1 |
| `goals[16].conditional` | Нет |
| `goals[16].max` | 2 |
| `goals[17].code` | `TREE` |
| `goals[17].category` | `PROGRESS` |
| `goals[17].counter` | `TREE` |
| `goals[17].base` | 2 |
| `goals[17].perLevel` | 0 |
| `goals[17].scope` | `ANY` |
| `goals[17].kinds` | `WEEKLY`; `CONTRACT` |
| `goals[17].weight` | 4 |
| `goals[17].minLevel` | 2 |
| `goals[17].conditional` | Нет |
| `goals[17].max` | 5 |
| `goals[18].code` | `ZONES` |
| `goals[18].category` | `PROGRESS` |
| `goals[18].counter` | `ZONES` |
| `goals[18].base` | 1 |
| `goals[18].perLevel` | 0 |
| `goals[18].scope` | `ANY` |
| `goals[18].kinds` | `WEEKLY`; `CONTRACT` |
| `goals[18].weight` | 4 |
| `goals[18].minLevel` | 1 |
| `goals[18].conditional` | Нет |
| `goals[18].max` | 3 |
| `goals[19].code` | `ATLAS` |
| `goals[19].category` | `PROGRESS` |
| `goals[19].counter` | `ATLAS` |
| `goals[19].base` | 1 |
| `goals[19].perLevel` | 0 |
| `goals[19].scope` | `ANY` |
| `goals[19].kinds` | `WEEKLY` |
| `goals[19].weight` | 2 |
| `goals[19].minLevel` | 10 |
| `goals[19].conditional` | Нет |
| `goals[19].max` | 3 |
| `rarities[0].rarity` | `COMMON` |
| `rarities[0].weight` | 60 |
| `rarities[0].target` | 1 |
| `rarities[0].reward` | 1 |
| `rarities[0].conditions` | 0 |
| `rarities[0].contractHours` | 2 |
| `rarities[0].orbs[0].code` | [Orb of Transmutation](Items-CURRENCY.md#orb_of_transmutation) |
| `rarities[0].orbs[0].amount` | 3 |
| `rarities[0].orbs[0].weight` | 3 |
| `rarities[0].orbs[1].code` | [Orb of Augmentation](Items-CURRENCY.md#orb_of_augmentation) |
| `rarities[0].orbs[1].amount` | 2 |
| `rarities[0].orbs[1].weight` | 3 |
| `rarities[0].orbs[2].code` | [Orb of Alteration](Items-CURRENCY.md#orb_of_alteration) |
| `rarities[0].orbs[2].amount` | 1 |
| `rarities[0].orbs[2].weight` | 2 |
| `rarities[0].chestChance` | 0.03 |
| `rarities[0].chests[0].code` | `CHEST_WOODEN` |
| `rarities[0].chests[0].amount` | 1 |
| `rarities[0].chests[0].weight` | 3 |
| `rarities[0].chests[1].code` | `CHEST_ARTISAN` |
| `rarities[0].chests[1].amount` | 1 |
| `rarities[0].chests[1].weight` | 2 |
| `rarities[0].chests[2].code` | `CHEST_TREASURY` |
| `rarities[0].chests[2].amount` | 1 |
| `rarities[0].chests[2].weight` | 1 |
| `rarities[1].rarity` | `MAGIC` |
| `rarities[1].weight` | 28 |
| `rarities[1].target` | 1.5 |
| `rarities[1].reward` | 2 |
| `rarities[1].conditions` | 1 |
| `rarities[1].contractHours` | 6 |
| `rarities[1].orbs[0].code` | [Orb of Alteration](Items-CURRENCY.md#orb_of_alteration) |
| `rarities[1].orbs[0].amount` | 3 |
| `rarities[1].orbs[0].weight` | 3 |
| `rarities[1].orbs[1].code` | [Orb of Chance](Items-CURRENCY.md#orb_of_chance) |
| `rarities[1].orbs[1].amount` | 2 |
| `rarities[1].orbs[1].weight` | 2 |
| `rarities[1].orbs[2].code` | [Orb of Alchemy](Items-CURRENCY.md#orb_of_alchemy) |
| `rarities[1].orbs[2].amount` | 1 |
| `rarities[1].orbs[2].weight` | 2 |
| `rarities[1].orbs[3].code` | [Orb of Scouring](Items-CURRENCY.md#orb_of_scouring) |
| `rarities[1].orbs[3].amount` | 1 |
| `rarities[1].orbs[3].weight` | 1 |
| `rarities[1].chestChance` | 0.08 |
| `rarities[1].chests[0].code` | `CHEST_WOODEN` |
| `rarities[1].chests[0].amount` | 1 |
| `rarities[1].chests[0].weight` | 3 |
| `rarities[1].chests[1].code` | `CHEST_TREASURY` |
| `rarities[1].chests[1].amount` | 1 |
| `rarities[1].chests[1].weight` | 2 |
| `rarities[1].chests[2].code` | `CHEST_ORBS` |
| `rarities[1].chests[2].amount` | 1 |
| `rarities[1].chests[2].weight` | 2 |
| `rarities[1].chests[3].code` | `CHEST_CARTOGRAPHER` |
| `rarities[1].chests[3].amount` | 1 |
| `rarities[1].chests[3].weight` | 2 |
| `rarities[1].chests[4].code` | `CHEST_ARTISAN` |
| `rarities[1].chests[4].amount` | 1 |
| `rarities[1].chests[4].weight` | 1 |
| `rarities[2].rarity` | `RARE` |
| `rarities[2].weight` | 10 |
| `rarities[2].target` | 2 |
| `rarities[2].reward` | 4 |
| `rarities[2].conditions` | 2 |
| `rarities[2].contractHours` | 12 |
| `rarities[2].orbs[0].code` | [Chaos Orb](Items-CURRENCY.md#chaos_orb) |
| `rarities[2].orbs[0].amount` | 1 |
| `rarities[2].orbs[0].weight` | 4 |
| `rarities[2].orbs[1].code` | [Regal Orb](Items-CURRENCY.md#regal_orb) |
| `rarities[2].orbs[1].amount` | 1 |
| `rarities[2].orbs[1].weight` | 3 |
| `rarities[2].orbs[2].code` | [Orb of Alchemy](Items-CURRENCY.md#orb_of_alchemy) |
| `rarities[2].orbs[2].amount` | 3 |
| `rarities[2].orbs[2].weight` | 3 |
| `rarities[2].orbs[3].code` | [Blessed Orb](Items-CURRENCY.md#blessed_orb) |
| `rarities[2].orbs[3].amount` | 1 |
| `rarities[2].orbs[3].weight` | 2 |
| `rarities[2].chestChance` | 0.18 |
| `rarities[2].chests[0].code` | `CHEST_IRONBOUND` |
| `rarities[2].chests[0].amount` | 1 |
| `rarities[2].chests[0].weight` | 3 |
| `rarities[2].chests[1].code` | `CHEST_ESSENCE` |
| `rarities[2].chests[1].amount` | 1 |
| `rarities[2].chests[1].weight` | 2 |
| `rarities[2].chests[2].code` | `CHEST_JEWELLER` |
| `rarities[2].chests[2].amount` | 1 |
| `rarities[2].chests[2].weight` | 2 |
| `rarities[2].chests[3].code` | `CHEST_SEER` |
| `rarities[2].chests[3].amount` | 1 |
| `rarities[2].chests[3].weight` | 2 |
| `rarities[2].chests[4].code` | `CHEST_BEAST` |
| `rarities[2].chests[4].amount` | 1 |
| `rarities[2].chests[4].weight` | 1 |
| `rarities[2].chests[5].code` | `CHEST_ANCIENT` |
| `rarities[2].chests[5].amount` | 1 |
| `rarities[2].chests[5].weight` | 1 |
| `rarities[3].rarity` | `UNIQUE` |
| `rarities[3].weight` | 2 |
| `rarities[3].target` | 3 |
| `rarities[3].reward` | 8 |
| `rarities[3].conditions` | 2 |
| `rarities[3].contractHours` | 24 |
| `rarities[3].orbs[0].code` | [Exalted Orb](Items-CURRENCY.md#exalted_orb) |
| `rarities[3].orbs[0].amount` | 1 |
| `rarities[3].orbs[0].weight` | 2 |
| `rarities[3].orbs[1].code` | [Divine Orb](Items-CURRENCY.md#divine_orb) |
| `rarities[3].orbs[1].amount` | 1 |
| `rarities[3].orbs[1].weight` | 3 |
| `rarities[3].orbs[2].code` | [Vaal Orb](Items-CURRENCY.md#vaal_orb) |
| `rarities[3].orbs[2].amount` | 2 |
| `rarities[3].orbs[2].weight` | 3 |
| `rarities[3].orbs[3].code` | [Chaos Orb](Items-CURRENCY.md#chaos_orb) |
| `rarities[3].orbs[3].amount` | 4 |
| `rarities[3].orbs[3].weight` | 3 |
| `rarities[3].orbs[4].code` | [Orb of Annulment](Items-CURRENCY.md#orb_of_annulment) |
| `rarities[3].orbs[4].amount` | 1 |
| `rarities[3].orbs[4].weight` | 1 |
| `rarities[3].chestChance` | 0.4 |
| `rarities[3].chests[0].code` | `CHEST_ANCIENT` |
| `rarities[3].chests[0].amount` | 1 |
| `rarities[3].chests[0].weight` | 3 |
| `rarities[3].chests[1].code` | `CHEST_WARLORD` |
| `rarities[3].chests[1].amount` | 1 |
| `rarities[3].chests[1].weight` | 2 |
| `rarities[3].chests[2].code` | `CHEST_CLASS` |
| `rarities[3].chests[2].amount` | 1 |
| `rarities[3].chests[2].weight` | 3 |
| `rarities[3].chests[3].code` | `CHEST_RELIC` |
| `rarities[3].chests[3].amount` | 1 |
| `rarities[3].chests[3].weight` | 1 |
| `kinds[0].kind` | `DAILY` |
| `kinds[0].count` | 3 |
| `kinds[0].rarities` | `COMMON`; `MAGIC`; `RARE` |
| `kinds[0].target` | 1 |
| `kinds[0].reward` | 1 |
| `kinds[0].orbs` | 1 |
| `kinds[1].kind` | `WEEKLY` |
| `kinds[1].count` | 3 |
| `kinds[1].rarities` | `MAGIC`; `RARE`; `UNIQUE` |
| `kinds[1].target` | 5 |
| `kinds[1].reward` | 4 |
| `kinds[1].orbs` | 2 |
| `kinds[2].kind` | `CONTRACT` |
| `kinds[2].count` | 0 |
| `kinds[2].rarities` | `COMMON`; `MAGIC`; `RARE`; `UNIQUE` |
| `kinds[2].target` | 1.5 |
| `kinds[2].reward` | 1.5 |
| `kinds[2].orbs` | 1 |
| `kinds[3].kind` | `STORY` |
| `kinds[3].count` | 0 |
| `kinds[3].rarities` | `COMMON`; `MAGIC`; `RARE`; `UNIQUE` |
| `kinds[3].target` | 1 |
| `kinds[3].reward` | 3 |
| `kinds[3].orbs` | 1 |
| `kinds[4].kind` | `GUILD` |
| `kinds[4].count` | 2 |
| `kinds[4].rarities` | `COMMON`; `MAGIC`; `RARE` |
| `kinds[4].target` | 1 |
| `kinds[4].reward` | 1 |
| `kinds[4].orbs` | 1 |
| `kinds[5].kind` | `GUILD_DAILY` |
| `kinds[5].count` | 2 |
| `kinds[5].rarities` | `MAGIC`; `RARE` |
| `kinds[5].target` | 0.6 |
| `kinds[5].reward` | 1.5 |
| `kinds[5].orbs` | 1 |
| `kinds[6].kind` | `GUILD_WEEKLY` |
| `kinds[6].count` | 3 |
| `kinds[6].rarities` | `MAGIC`; `RARE`; `UNIQUE` |
| `kinds[6].target` | 3 |
| `kinds[6].reward` | 4 |
| `kinds[6].orbs` | 2 |
| `reward.goldBase` | 40 |
| `reward.goldPower` | 1.3 |
| `reward.experienceShare` | 5 |
| `reward.experienceCap` | 60 |
| `board.size` | 6 |
| `board.active` | 3 |
| `board.refillHours` | 2 |
| `guild.fairShare` | 0.25 |
| `guild.experience` | 0.5 |
| `guild.sharedLevel` | 20 |
| `story[0].region` | `REGION_1` |
| `story[0].steps[0].code` | `S1_LANDING` |
| `story[0].steps[0].counter` | `CLEAR` |
| `story[0].steps[0].target` | 1 |
| `story[0].steps[0].zone` | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| `story[0].steps[0].rarity` | `COMMON` |
| `story[0].steps[1].code` | `S1_WRECKS` |
| `story[0].steps[1].counter` | `KILLS` |
| `story[0].steps[1].target` | 30 |
| `story[0].steps[1].zone` | [Кладбище кораблей](World-REGION_1.md#c1_ship_graveyard) |
| `story[0].steps[1].rarity` | `COMMON` |
| `story[0].steps[2].code` | `S1_LIGHT` |
| `story[0].steps[2].counter` | `CLEAR` |
| `story[0].steps[2].target` | 1 |
| `story[0].steps[2].zone` | [Маяк утопленников](World-REGION_1.md#c1_drowned_lighthouse) |
| `story[0].steps[2].rarity` | `MAGIC` |
| `story[0].steps[3].code` | `S1_SMUGGLERS` |
| `story[0].steps[3].counter` | `BOSSES` |
| `story[0].steps[3].target` | 1 |
| `story[0].steps[3].zone` | [Грот контрабандистов](World-REGION_1.md#c1_smugglers_grotto) |
| `story[0].steps[3].rarity` | `MAGIC` |
| `story[0].steps[4].code` | `S1_BARROW` |
| `story[0].steps[4].counter` | `CLEAR` |
| `story[0].steps[4].target` | 1 |
| `story[0].steps[4].zone` | [Курган вождя](World-REGION_1.md#c1_chieftains_barrow) |
| `story[0].steps[4].rarity` | `RARE` |
| `story[0].steps[5].code` | `S1_CRYPT` |
| `story[0].steps[5].counter` | `BOSSES` |
| `story[0].steps[5].target` | 1 |
| `story[0].steps[5].zone` | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| `story[0].steps[5].rarity` | `UNIQUE` |
| `story[1].region` | `REGION_2` |
| `story[1].steps[0].code` | `S2_VILLAGE` |
| `story[1].steps[0].counter` | `CLEAR` |
| `story[1].steps[0].target` | 1 |
| `story[1].steps[0].zone` | [Рудничный посёлок](World-REGION_2.md#c2_miners_village) |
| `story[1].steps[0].rarity` | `COMMON` |
| `story[1].steps[1].code` | `S2_MINES` |
| `story[1].steps[1].counter` | `KILLS_MAGIC` |
| `story[1].steps[1].target` | 6 |
| `story[1].steps[1].zone` | [Кристальные копи](World-REGION_2.md#c2_crystal_mines) |
| `story[1].steps[1].rarity` | `COMMON` |
| `story[1].steps[2].code` | `S2_TOOLS` |
| `story[1].steps[2].counter` | `ORBS_USED` |
| `story[1].steps[2].target` | 5 |
| `story[1].steps[2].rarity` | `MAGIC` |
| `story[1].steps[3].code` | `S2_PASS` |
| `story[1].steps[3].counter` | `CLEAR` |
| `story[1].steps[3].target` | 1 |
| `story[1].steps[3].zone` | [Замёрзший перевал](World-REGION_2.md#c2_frozen_pass) |
| `story[1].steps[3].rarity` | `MAGIC` |
| `story[1].steps[4].code` | `S2_PEAK` |
| `story[1].steps[4].counter` | `BOSSES` |
| `story[1].steps[4].target` | 1 |
| `story[1].steps[4].zone` | [Грозовой пик](World-REGION_2.md#c2_storm_peak) |
| `story[1].steps[4].rarity` | `RARE` |
| `story[1].steps[5].code` | `S2_TEMPLE` |
| `story[1].steps[5].counter` | `BOSSES` |
| `story[1].steps[5].target` | 1 |
| `story[1].steps[5].zone` | [Храм утонувших](World-REGION_2.md#c2_drowned_temple) |
| `story[1].steps[5].rarity` | `UNIQUE` |
| `story[2].region` | `REGION_3` |
| `story[2].steps[0].code` | `S3_CARAVAN` |
| `story[2].steps[0].counter` | `CLEAR` |
| `story[2].steps[0].target` | 1 |
| `story[2].steps[0].zone` | [Караванный путь](World-REGION_3.md#c3_caravan_road) |
| `story[2].steps[0].rarity` | `COMMON` |
| `story[2].steps[1].code` | `S3_TRADE` |
| `story[2].steps[1].counter` | `ITEMS_SOLD` |
| `story[2].steps[1].target` | 10 |
| `story[2].steps[1].rarity` | `COMMON` |
| `story[2].steps[2].code` | `S3_RAIDERS` |
| `story[2].steps[2].counter` | `KILLS_RARE` |
| `story[2].steps[2].target` | 3 |
| `story[2].steps[2].zone` | [Стан налётчиков](World-REGION_3.md#c3_raider_camp) |
| `story[2].steps[2].rarity` | `MAGIC` |
| `story[2].steps[3].code` | `S3_TOMBS` |
| `story[2].steps[3].counter` | `CLEAR` |
| `story[2].steps[3].target` | 1 |
| `story[2].steps[3].zone` | [Гробницы царей](World-REGION_3.md#c3_tombs_of_kings) |
| `story[2].steps[3].rarity` | `MAGIC` |
| `story[2].steps[4].code` | `S3_EMBALMER` |
| `story[2].steps[4].counter` | `BOSSES` |
| `story[2].steps[4].target` | 1 |
| `story[2].steps[4].zone` | [Зал бальзамировщиков](World-REGION_3.md#c3_embalmers_hall) |
| `story[2].steps[4].rarity` | `RARE` |
| `story[2].steps[5].code` | `S3_PYRAMID` |
| `story[2].steps[5].counter` | `BOSSES` |
| `story[2].steps[5].target` | 1 |
| `story[2].steps[5].zone` | [Пирамида Солнечного царя](World-REGION_3.md#c3_sun_kings_pyramid) |
| `story[2].steps[5].rarity` | `UNIQUE` |
| `story[3].region` | `REGION_4` |
| `story[3].steps[0].code` | `S4_EDGE` |
| `story[3].steps[0].counter` | `CLEAR` |
| `story[3].steps[0].target` | 1 |
| `story[3].steps[0].zone` | [Кромка джунглей](World-REGION_4.md#c4_jungle_edge) |
| `story[3].steps[0].rarity` | `COMMON` |
| `story[3].steps[1].code` | `S4_EXPEDITION` |
| `story[3].steps[1].counter` | `CLEAR` |
| `story[3].steps[1].target` | 1 |
| `story[3].steps[1].zone` | [Стоянка пропавшей экспедиции](World-REGION_4.md#c4_lost_expedition) |
| `story[3].steps[1].rarity` | `MAGIC` |
| `story[3].steps[2].code` | `S4_BROOD` |
| `story[3].steps[2].counter` | `KILLS` |
| `story[3].steps[2].target` | 80 |
| `story[3].steps[2].zone` | [Выводковые камеры](World-REGION_4.md#c4_brood_chambers) |
| `story[3].steps[2].rarity` | `MAGIC` |
| `story[3].steps[3].code` | `S4_CRAFT` |
| `story[3].steps[3].counter` | `CRAFT_MADE` |
| `story[3].steps[3].target` | 2 |
| `story[3].steps[3].rarity` | `MAGIC` |
| `story[3].steps[4].code` | `S4_TREE` |
| `story[3].steps[4].counter` | `BOSSES` |
| `story[3].steps[4].target` | 1 |
| `story[3].steps[4].zone` | [Древо-великан](World-REGION_4.md#c4_great_tree) |
| `story[3].steps[4].rarity` | `RARE` |
| `story[3].steps[5].code` | `S4_HIVE` |
| `story[3].steps[5].counter` | `BOSSES` |
| `story[3].steps[5].target` | 1 |
| `story[3].steps[5].zone` | [Сердце улья](World-REGION_4.md#c4_hive_heart) |
| `story[3].steps[5].rarity` | `UNIQUE` |
| `story[4].region` | `REGION_5` |
| `story[4].steps[0].code` | `S5_FORT` |
| `story[4].steps[0].counter` | `CLEAR` |
| `story[4].steps[0].target` | 1 |
| `story[4].steps[0].zone` | [Пограничный форт](World-REGION_5.md#c5_frontier_fort) |
| `story[4].steps[0].rarity` | `COMMON` |
| `story[4].steps[1].code` | `S5_GENERAL` |
| `story[4].steps[1].counter` | `BOSSES` |
| `story[4].steps[1].target` | 1 |
| `story[4].steps[1].zone` | [Пограничный форт](World-REGION_5.md#c5_frontier_fort) |
| `story[4].steps[1].rarity` | `MAGIC` |
| `story[4].steps[2].code` | `S5_LEVEL` |
| `story[4].steps[2].counter` | `LEVEL` |
| `story[4].steps[2].target` | 55 |
| `story[4].steps[2].rarity` | `MAGIC` |
| `story[4].steps[3].code` | `S5_FORGES` |
| `story[4].steps[3].counter` | `KILLS_RARE` |
| `story[4].steps[3].target` | 5 |
| `story[4].steps[3].zone` | [Военные кузни](World-REGION_5.md#c5_war_forges) |
| `story[4].steps[3].rarity` | `MAGIC` |
| `story[4].steps[4].code` | `S5_GATE` |
| `story[4].steps[4].counter` | `BOSSES` |
| `story[4].steps[4].target` | 1 |
| `story[4].steps[4].zone` | [Инфернальные врата](World-REGION_5.md#c5_infernal_gate) |
| `story[4].steps[4].rarity` | `RARE` |
| `story[4].steps[5].code` | `S5_CITADEL` |
| `story[4].steps[5].counter` | `BOSSES` |
| `story[4].steps[5].target` | 1 |
| `story[4].steps[5].zone` | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| `story[4].steps[5].rarity` | `UNIQUE` |
| `story[5].region` | `REGION_6` |
| `story[5].steps[0].code` | `S6_RIFT` |
| `story[5].steps[0].counter` | `CLEAR` |
| `story[5].steps[0].target` | 1 |
| `story[5].steps[0].zone` | [Край разлома](World-REGION_6.md#c6_rift_edge) |
| `story[5].steps[0].rarity` | `COMMON` |
| `story[5].steps[1].code` | `S6_ABBEY` |
| `story[5].steps[1].counter` | `BOSSES` |
| `story[5].steps[1].target` | 1 |
| `story[5].steps[1].zone` | [Покинутое аббатство](World-REGION_6.md#c6_forsaken_abbey) |
| `story[5].steps[1].rarity` | `MAGIC` |
| `story[5].steps[2].code` | `S6_OSSUARY` |
| `story[5].steps[2].counter` | `KILLS` |
| `story[5].steps[2].target` | 120 |
| `story[5].steps[2].zone` | [Костница](World-REGION_6.md#c6_ossuary) |
| `story[5].steps[2].rarity` | `MAGIC` |
| `story[5].steps[3].code` | `S6_STAR` |
| `story[5].steps[3].counter` | `CLEAR` |
| `story[5].steps[3].target` | 1 |
| `story[5].steps[3].zone` | [Павшая звезда](World-REGION_6.md#c6_fallen_star) |
| `story[5].steps[3].rarity` | `RARE` |
| `story[5].steps[4].code` | `S6_STAIR` |
| `story[5].steps[4].counter` | `BOSSES` |
| `story[5].steps[4].target` | 1 |
| `story[5].steps[4].zone` | [Бесконечная лестница](World-REGION_6.md#c6_endless_stair) |
| `story[5].steps[4].rarity` | `RARE` |
| `story[5].steps[5].code` | `S6_THRONE` |
| `story[5].steps[5].counter` | `BOSSES` |
| `story[5].steps[5].target` | 1 |
| `story[5].steps[5].zone` | [Трон бездны](World-REGION_6.md#c6_abyssal_throne) |
| `story[5].steps[5].rarity` | `UNIQUE` |
| `story[6].region` | `REGION_7` |
| `story[6].steps[0].code` | `S7_1` |
| `story[6].steps[0].counter` | `CLEAR` |
| `story[6].steps[0].target` | 1 |
| `story[6].steps[0].zone` | [Небесная пристань](World-REGION_7.md#c7_sky_landing) |
| `story[6].steps[0].rarity` | `COMMON` |
| `story[6].steps[1].code` | `S7_2` |
| `story[6].steps[1].counter` | `BOSSES` |
| `story[6].steps[1].target` | 1 |
| `story[6].steps[1].zone` | [Зеркальная равнина](World-REGION_7.md#c7_mirror_flats) |
| `story[6].steps[1].rarity` | `MAGIC` |
| `story[6].steps[2].code` | `S7_3` |
| `story[6].steps[2].counter` | `KILLS` |
| `story[6].steps[2].target` | 140 |
| `story[6].steps[2].zone` | [Храм ветров](World-REGION_7.md#c7_wind_temple) |
| `story[6].steps[2].rarity` | `MAGIC` |
| `story[6].steps[3].code` | `S7_4` |
| `story[6].steps[3].counter` | `CLEAR` |
| `story[6].steps[3].target` | 1 |
| `story[6].steps[3].zone` | [Облачная цитадель](World-REGION_7.md#c7_cloud_citadel) |
| `story[6].steps[3].rarity` | `RARE` |
| `story[6].steps[4].code` | `S7_5` |
| `story[6].steps[4].counter` | `BOSSES` |
| `story[6].steps[4].target` | 1 |
| `story[6].steps[4].zone` | [Око бури](World-REGION_7.md#c7_eye_of_the_storm) |
| `story[6].steps[4].rarity` | `RARE` |
| `story[6].steps[5].code` | `S7_6` |
| `story[6].steps[5].counter` | `BOSSES` |
| `story[6].steps[5].target` | 1 |
| `story[6].steps[5].zone` | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| `story[6].steps[5].rarity` | `UNIQUE` |
| `story[7].region` | `REGION_8` |
| `story[7].steps[0].code` | `S8_1` |
| `story[7].steps[0].counter` | `CLEAR` |
| `story[7].steps[0].target` | 1 |
| `story[7].steps[0].zone` | [Затонувшие врата](World-REGION_8.md#c8_sunken_gate) |
| `story[7].steps[0].rarity` | `COMMON` |
| `story[7].steps[1].code` | `S8_2` |
| `story[7].steps[1].counter` | `BOSSES` |
| `story[7].steps[1].target` | 1 |
| `story[7].steps[1].zone` | [Жемчужный риф](World-REGION_8.md#c8_pearl_reef) |
| `story[7].steps[1].rarity` | `MAGIC` |
| `story[7].steps[2].code` | `S8_3` |
| `story[7].steps[2].counter` | `KILLS` |
| `story[7].steps[2].target` | 140 |
| `story[7].steps[2].zone` | [Казармы легиона](World-REGION_8.md#c8_legion_barracks) |
| `story[7].steps[2].rarity` | `MAGIC` |
| `story[7].steps[3].code` | `S8_4` |
| `story[7].steps[3].counter` | `CLEAR` |
| `story[7].steps[3].target` | 1 |
| `story[7].steps[3].zone` | [Запечатанная цистерна](World-REGION_8.md#c8_sealed_cistern) |
| `story[7].steps[3].rarity` | `RARE` |
| `story[7].steps[4].code` | `S8_5` |
| `story[7].steps[4].counter` | `BOSSES` |
| `story[7].steps[4].target` | 1 |
| `story[7].steps[4].zone` | [Императорские термы](World-REGION_8.md#c8_imperial_baths) |
| `story[7].steps[4].rarity` | `RARE` |
| `story[7].steps[5].code` | `S8_6` |
| `story[7].steps[5].counter` | `BOSSES` |
| `story[7].steps[5].target` | 1 |
| `story[7].steps[5].zone` | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| `story[7].steps[5].rarity` | `UNIQUE` |
| `story[8].region` | `REGION_9` |
| `story[8].steps[0].code` | `S9_1` |
| `story[8].steps[0].counter` | `CLEAR` |
| `story[8].steps[0].target` | 1 |
| `story[8].steps[0].zone` | [Порог богов](World-REGION_9.md#c9_gods_threshold) |
| `story[8].steps[0].rarity` | `COMMON` |
| `story[8].steps[1].code` | `S9_2` |
| `story[8].steps[1].counter` | `BOSSES` |
| `story[8].steps[1].target` | 1 |
| `story[8].steps[1].zone` | [Мост созвездий](World-REGION_9.md#c9_constellation_bridge) |
| `story[8].steps[1].rarity` | `MAGIC` |
| `story[8].steps[2].code` | `S9_3` |
| `story[8].steps[2].counter` | `KILLS` |
| `story[8].steps[2].target` | 140 |
| `story[8].steps[2].zone` | [Солнечная кузня](World-REGION_9.md#c9_sunforge) |
| `story[8].steps[2].rarity` | `MAGIC` |
| `story[8].steps[3].code` | `S9_4` |
| `story[8].steps[3].counter` | `CLEAR` |
| `story[8].steps[3].target` | 1 |
| `story[8].steps[3].zone` | [Пустой пантеон](World-REGION_9.md#c9_hollow_pantheon) |
| `story[8].steps[3].rarity` | `RARE` |
| `story[8].steps[4].code` | `S9_5` |
| `story[8].steps[4].counter` | `BOSSES` |
| `story[8].steps[4].target` | 1 |
| `story[8].steps[4].zone` | [Дорога к трону](World-REGION_9.md#c9_throne_road) |
| `story[8].steps[4].rarity` | `RARE` |
| `story[8].steps[5].code` | `S9_6` |
| `story[8].steps[5].counter` | `BOSSES` |
| `story[8].steps[5].target` | 1 |
| `story[8].steps[5].zone` | [Последний трон](World-REGION_9.md#c9_last_throne) |
| `story[8].steps[5].rarity` | `UNIQUE` |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/quests.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
