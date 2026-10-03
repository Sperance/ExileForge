# Параметры · campaign

Точный справочник значений файла. Индексы в квадратных скобках начинаются с нуля. Отсутствующие в JSON поля получают значения модели Kotlin; этот раздел показывает именно явно заданные параметры, а не полный результат загрузки.

| Путь параметра | Значение |
| --- | --- |
| `defaults.STOCK_CRITICAL_CHANCE` | 5 |
| `defaults.STOCK_CRITICAL_MULTIPLIER` | 150 |
| `growth.STOCK_HEALTH` | 1.15 |
| `growth.STOCK_ENERGY_SHIELD` | 1.15 |
| `growth.STOCK_HEALTH_REGEN` | 1.15 |
| `growth.STOCK_ATTACK_PHYSICAL` | 1.12 |
| `growth.STOCK_ATTACK_FIRE` | 1.12 |
| `growth.STOCK_ATTACK_COLD` | 1.12 |
| `growth.STOCK_ATTACK_LIGHTNING` | 1.12 |
| `growth.STOCK_ATTACK_CHAOS` | 1.12 |
| `growth.STOCK_ARMOR` | 1.12 |
| `growth.STOCK_EVASION` | 1.12 |
| `growth.STOCK_STUN_THRESHOLD` | 1.12 |
| `growth.STOCK_ATTACK_MAGICAL` | 1.12 |
| `growth.STOCK_MANA` | 1.1 |
| `growthTaper.from` | 10 |
| `growthTaper.rate` | 0.4 |
| `growthTaper.bends[0].from` | 25 |
| `growthTaper.bends[0].rate` | 0.38 |
| `growthTaper.bends[1].from` | 37 |
| `growthTaper.bends[1].rate` | 0.34 |
| `growthTaper.bends[2].from` | 49 |
| `growthTaper.bends[2].rate` | 0.3 |
| `growthTaper.bends[3].from` | 61 |
| `growthTaper.bends[3].rate` | 0.25 |
| `rarities[0].rarity` | `NORMAL` |
| `rarities[0].modifiers` | 0; 0 |
| `rarities[1].rarity` | `MAGIC` |
| `rarities[1].modifiers` | 1; 2 |
| `rarities[1].statScale` | 30 |
| `rarities[1].modifierPower` | 1.3 |
| `rarities[1].lines[0].code` | [RARITY_STOCK_HEALTH_MORE](Modifiers-RULE.md#rarity_stock_health_more) |
| `rarities[1].lines[0].values` | 40 |
| `rarities[1].quantity` | 2.5 |
| `rarities[1].rarityBonus` | 50 |
| `rarities[1].experience` | 2.2 |
| `rarities[2].rarity` | `RARE` |
| `rarities[2].modifiers` | 3; 4 |
| `rarities[2].statScale` | 70 |
| `rarities[2].modifierPower` | 1.7 |
| `rarities[2].lines[0].code` | [RARITY_STOCK_HEALTH_MORE](Modifiers-RULE.md#rarity_stock_health_more) |
| `rarities[2].lines[0].values` | 100 |
| `rarities[2].quantity` | 6 |
| `rarities[2].rarityBonus` | 150 |
| `rarities[2].experience` | 5 |
| `rarities[3].rarity` | `UNIQUE` |
| `rarities[3].modifiers` | 0; 0 |
| `rarities[3].statScale` | 120 |
| `rarities[3].modifierPower` | 2 |
| `rarities[3].lines[0].code` | [RARITY_STOCK_HEALTH_MORE](Modifiers-RULE.md#rarity_stock_health_more) |
| `rarities[3].lines[0].values` | 250 |
| `rarities[3].quantity` | 10 |
| `rarities[3].rarityBonus` | 250 |
| `rarities[3].experience` | 15 |
| `monsters[0].code` | [Утопленник](Monsters.md#drowned) |
| `monsters[0].form` | `HUMANOID` |
| `monsters[0].experience` | 20 |
| `monsters[0].loot` | [loot:T1](Tables-LOOT.md#loot-t1) |
| `monsters[0].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[0].stats.STOCK_HEALTH` | 18 |
| `monsters[0].stats.STOCK_ATTACK_PHYSICAL` | 4 |
| `monsters[0].trait` | `TRAIT_CHILLING_TOUCH` |
| `monsters[1].code` | [Береговой краб](Monsters.md#shore_crab) |
| `monsters[1].form` | `CRAB` |
| `monsters[1].experience` | 18 |
| `monsters[1].loot` | [loot:T1](Tables-LOOT.md#loot-t1) |
| `monsters[1].stats.STOCK_TAUNT` | 1 |
| `monsters[1].stats.STOCK_ATTACK_SPEED` | 1.3 |
| `monsters[1].stats.STOCK_HEALTH` | 14 |
| `monsters[1].stats.STOCK_ATTACK_PHYSICAL` | 3 |
| `monsters[1].stats.STOCK_ARMOR` | 18 |
| `monsters[1].trait` | `TRAIT_PROVOKE` |
| `monsters[2].code` | [Пещерная летучая мышь](Monsters.md#cave_bat) |
| `monsters[2].form` | `BAT` |
| `monsters[2].experience` | 14 |
| `monsters[2].loot` | [loot:T1](Tables-LOOT.md#loot-t1) |
| `monsters[2].stats.STOCK_ATTACK_SPEED` | 1.8 |
| `monsters[2].stats.STOCK_HEALTH` | 9 |
| `monsters[2].stats.STOCK_ATTACK_PHYSICAL` | 2.5 |
| `monsters[2].stats.STOCK_EVASION` | 30 |
| `monsters[2].trait` | `TRAIT_LEECHING` |
| `monsters[3].code` | [Солёный плевун](Monsters.md#brine_spitter) |
| `monsters[3].form` | `SERPENT` |
| `monsters[3].experience` | 20 |
| `monsters[3].loot` | [loot:T1](Tables-LOOT.md#loot-t1) |
| `monsters[3].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[3].stats.STOCK_HEALTH` | 15 |
| `monsters[3].stats.STOCK_ATTACK_PHYSICAL` | 2 |
| `monsters[3].stats.STOCK_ATTACK_COLD` | 2.5 |
| `monsters[3].stats.STOCK_RESIST_COLD` | 30 |
| `monsters[3].trait` | `TRAIT_SKILL_ICE_BOLT` |
| `monsters[4].code` | [Болотный зомби](Monsters.md#mire_zombie) |
| `monsters[4].form` | `HUMANOID` |
| `monsters[4].experience` | 22 |
| `monsters[4].loot` | [loot:T1](Tables-LOOT.md#loot-t1) |
| `monsters[4].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[4].stats.STOCK_HEALTH` | 26 |
| `monsters[4].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[4].stats.STOCK_STUN_THRESHOLD` | 6 |
| `monsters[4].trait` | `TRAIT_REGROWTH` |
| `monsters[5].code` | [Трясинный скрытень](Monsters.md#bog_lurker) |
| `monsters[5].form` | `SERPENT` |
| `monsters[5].experience` | 22 |
| `monsters[5].loot` | [loot:T2](Tables-LOOT.md#loot-t2) |
| `monsters[5].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[5].stats.STOCK_HEALTH` | 19 |
| `monsters[5].stats.STOCK_ATTACK_PHYSICAL` | 3 |
| `monsters[5].stats.STOCK_ATTACK_CHAOS` | 2 |
| `monsters[5].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[5].trait` | `TRAIT_SKILL_SPIT` |
| `monsters[6].code` | [Болотная пиявка](Monsters.md#swamp_leech) |
| `monsters[6].form` | `SLUG` |
| `monsters[6].experience` | 16 |
| `monsters[6].loot` | [loot:T2](Tables-LOOT.md#loot-t2) |
| `monsters[6].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[6].stats.STOCK_HEALTH` | 16 |
| `monsters[6].stats.STOCK_ATTACK_PHYSICAL` | 3 |
| `monsters[6].stats.STOCK_LEECH_ALL` | 8 |
| `monsters[6].trait` | `TRAIT_REGROWTH` |
| `monsters[7].code` | [Одичавший волк](Monsters.md#feral_wolf) |
| `monsters[7].form` | `BEAST` |
| `monsters[7].experience` | 20 |
| `monsters[7].loot` | [loot:T2](Tables-LOOT.md#loot-t2) |
| `monsters[7].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[7].stats.STOCK_HEALTH` | 15 |
| `monsters[7].stats.STOCK_ATTACK_PHYSICAL` | 4 |
| `monsters[7].stats.STOCK_EVASION` | 20 |
| `monsters[7].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[7].trait` | `TRAIT_RENDING` |
| `monsters[8].code` | [Терновая дриада](Monsters.md#thorn_dryad) |
| `monsters[8].form` | `HUMANOID` |
| `monsters[8].experience` | 22 |
| `monsters[8].loot` | [loot:T2](Tables-LOOT.md#loot-t2) |
| `monsters[8].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[8].stats.STOCK_HEALTH` | 17 |
| `monsters[8].stats.STOCK_ATTACK_PHYSICAL` | 3 |
| `monsters[8].stats.STOCK_ATTACK_CHAOS` | 2 |
| `monsters[8].stats.STOCK_HEALTH_REGEN` | 0.5 |
| `monsters[8].stats.STOCK_ATTACK_MAGICAL` | 2 |
| `monsters[8].stats.STOCK_CAST_SPEED` | 0.7 |
| `monsters[8].stats.STOCK_MANA` | 24 |
| `monsters[8].stats.STOCK_MANA_REGEN` | 1.5 |
| `monsters[8].trait` | `TRAIT_SKILL_HEAL` |
| `monsters[9].code` | [Лесной паук](Monsters.md#wood_spider) |
| `monsters[9].form` | `SPIDER` |
| `monsters[9].experience` | 20 |
| `monsters[9].loot` | [loot:T2](Tables-LOOT.md#loot-t2) |
| `monsters[9].stats.STOCK_ATTACK_SPEED` | 1.5 |
| `monsters[9].stats.STOCK_HEALTH` | 14 |
| `monsters[9].stats.STOCK_ATTACK_PHYSICAL` | 2.5 |
| `monsters[9].stats.STOCK_ATTACK_CHAOS` | 2 |
| `monsters[9].stats.STOCK_EVASION` | 25 |
| `monsters[9].trait` | `TRAIT_SKILL_WEB` |
| `monsters[10].code` | [Скелет-воин](Monsters.md#skeleton_warrior) |
| `monsters[10].form` | `UNDEAD` |
| `monsters[10].experience` | 22 |
| `monsters[10].loot` | [loot:T2](Tables-LOOT.md#loot-t2) |
| `monsters[10].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[10].stats.STOCK_HEALTH` | 18 |
| `monsters[10].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[10].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[10].stats.STOCK_ARMOR` | 15 |
| `monsters[10].stats.STOCK_RESIST_COLD` | 20 |
| `monsters[10].trait` | `TRAIT_SHIELD_WALL` |
| `monsters[11].code` | [Костяной лучник](Monsters.md#bone_archer) |
| `monsters[11].form` | `UNDEAD` |
| `monsters[11].experience` | 20 |
| `monsters[11].loot` | [loot:T2](Tables-LOOT.md#loot-t2) |
| `monsters[11].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[11].stats.STOCK_HEALTH` | 13 |
| `monsters[11].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[11].stats.STOCK_CRITICAL_CHANCE` | 12 |
| `monsters[11].trait` | `TRAIT_KEEN_EYE` |
| `monsters[12].code` | [Страж руин](Monsters.md#ruin_sentry) |
| `monsters[12].form` | `GOLEM` |
| `monsters[12].experience` | 26 |
| `monsters[12].loot` | [loot:T2](Tables-LOOT.md#loot-t2) |
| `monsters[12].stats.STOCK_TAUNT` | 1 |
| `monsters[12].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[12].stats.STOCK_HEALTH` | 28 |
| `monsters[12].stats.STOCK_ATTACK_PHYSICAL` | 6 |
| `monsters[12].stats.STOCK_ARMOR` | 40 |
| `monsters[12].stats.STOCK_STUN_THRESHOLD` | 10 |
| `monsters[12].trait` | `TRAIT_PROVOKE` |
| `monsters[13].code` | [Упырь](Monsters.md#ghoul) |
| `monsters[13].form` | `UNDEAD` |
| `monsters[13].experience` | 22 |
| `monsters[13].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[13].stats.STOCK_ATTACK_SPEED` | 1.3 |
| `monsters[13].stats.STOCK_HEALTH` | 20 |
| `monsters[13].stats.STOCK_ATTACK_PHYSICAL` | 4.5 |
| `monsters[13].stats.STOCK_LEECH_ALL` | 5 |
| `monsters[13].trait` | `TRAIT_BERSERK` |
| `monsters[14].code` | [Склепный призрак](Monsters.md#crypt_wraith) |
| `monsters[14].form` | `WRAITH` |
| `monsters[14].experience` | 24 |
| `monsters[14].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[14].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[14].stats.STOCK_HEALTH` | 10 |
| `monsters[14].stats.STOCK_ENERGY_SHIELD` | 10 |
| `monsters[14].stats.STOCK_ATTACK_COLD` | 4.5 |
| `monsters[14].stats.STOCK_EVASION` | 30 |
| `monsters[14].stats.STOCK_RESIST_COLD` | 50 |
| `monsters[14].stats.STOCK_ATTACK_MAGICAL` | 3 |
| `monsters[14].stats.STOCK_CAST_SPEED` | 0.9 |
| `monsters[14].stats.STOCK_MANA` | 30 |
| `monsters[14].stats.STOCK_MANA_REGEN` | 2 |
| `monsters[14].trait` | `TRAIT_SKILL_ICE_BOLT` |
| `monsters[15].code` | [Кристальный голем](Monsters.md#crystal_golem) |
| `monsters[15].form` | `GOLEM` |
| `monsters[15].experience` | 28 |
| `monsters[15].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[15].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[15].stats.STOCK_HEALTH` | 30 |
| `monsters[15].stats.STOCK_ATTACK_PHYSICAL` | 3 |
| `monsters[15].stats.STOCK_ATTACK_LIGHTNING` | 4 |
| `monsters[15].stats.STOCK_ARMOR` | 45 |
| `monsters[15].stats.STOCK_RESIST_LIGHTNING` | 50 |
| `monsters[15].trait` | `TRAIT_STORMCHARGED` |
| `monsters[16].code` | [Осколочный ползун](Monsters.md#shard_scuttler) |
| `monsters[16].form` | `SPIDER` |
| `monsters[16].experience` | 20 |
| `monsters[16].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[16].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[16].stats.STOCK_HEALTH` | 13 |
| `monsters[16].stats.STOCK_ATTACK_PHYSICAL` | 2 |
| `monsters[16].stats.STOCK_ATTACK_LIGHTNING` | 3 |
| `monsters[16].stats.STOCK_EVASION` | 25 |
| `monsters[16].trait` | `TRAIT_STATIC` |
| `monsters[17].code` | [Обитатель туннелей](Monsters.md#tunnel_dweller) |
| `monsters[17].form` | `HUMANOID` |
| `monsters[17].experience` | 22 |
| `monsters[17].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[17].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[17].stats.STOCK_HEALTH` | 19 |
| `monsters[17].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[17].stats.STOCK_ARMOR` | 15 |
| `monsters[17].stats.STOCK_CRITICAL_CHANCE` | 7 |
| `monsters[17].trait` | `TRAIT_KEEN_EYE` |
| `monsters[18].code` | [Угольная гончая](Monsters.md#cinder_hound) |
| `monsters[18].form` | `BEAST` |
| `monsters[18].experience` | 22 |
| `monsters[18].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[18].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[18].stats.STOCK_HEALTH` | 16 |
| `monsters[18].stats.STOCK_ATTACK_PHYSICAL` | 2.5 |
| `monsters[18].stats.STOCK_ATTACK_FIRE` | 3.5 |
| `monsters[18].stats.STOCK_RESIST_FIRE` | 50 |
| `monsters[18].trait` | `TRAIT_BURNING_BLOOD` |
| `monsters[19].code` | [Пепельный мертвец](Monsters.md#ash_revenant) |
| `monsters[19].form` | `UNDEAD` |
| `monsters[19].experience` | 24 |
| `monsters[19].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[19].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[19].stats.STOCK_HEALTH` | 21 |
| `monsters[19].stats.STOCK_ATTACK_PHYSICAL` | 4 |
| `monsters[19].stats.STOCK_ATTACK_FIRE` | 3 |
| `monsters[19].stats.STOCK_RESIST_FIRE` | 30 |
| `monsters[19].stats.STOCK_HEALTH_REGEN` | 0.6 |
| `monsters[19].trait` | `TRAIT_SMOULDER` |
| `monsters[20].code` | [Магмовый слизень](Monsters.md#magma_slug) |
| `monsters[20].form` | `SLUG` |
| `monsters[20].experience` | 24 |
| `monsters[20].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[20].stats.STOCK_ATTACK_SPEED` | 0.7 |
| `monsters[20].stats.STOCK_HEALTH` | 27 |
| `monsters[20].stats.STOCK_ATTACK_FIRE` | 5 |
| `monsters[20].stats.STOCK_ARMOR` | 30 |
| `monsters[20].stats.STOCK_RESIST_FIRE` | 75 |
| `monsters[20].trait` | `TRAIT_FIRE_BURST` |
| `monsters[21].code` | [Ледяной тролль](Monsters.md#frost_troll) |
| `monsters[21].form` | `BRUTE` |
| `monsters[21].experience` | 30 |
| `monsters[21].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[21].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[21].stats.STOCK_HEALTH` | 36 |
| `monsters[21].stats.STOCK_ATTACK_PHYSICAL` | 6 |
| `monsters[21].stats.STOCK_ATTACK_COLD` | 2 |
| `monsters[21].stats.STOCK_RESIST_COLD` | 50 |
| `monsters[21].stats.STOCK_HEALTH_REGEN` | 1 |
| `monsters[21].stats.STOCK_STUN_THRESHOLD` | 12 |
| `monsters[21].trait` | `TRAIT_REGROWTH` |
| `monsters[22].code` | [Ледяной призрак](Monsters.md#ice_wraith) |
| `monsters[22].form` | `WRAITH` |
| `monsters[22].experience` | 24 |
| `monsters[22].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[22].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[22].stats.STOCK_HEALTH` | 11 |
| `monsters[22].stats.STOCK_ENERGY_SHIELD` | 12 |
| `monsters[22].stats.STOCK_ATTACK_COLD` | 5 |
| `monsters[22].stats.STOCK_EVASION` | 30 |
| `monsters[22].stats.STOCK_RESIST_COLD` | 75 |
| `monsters[22].stats.STOCK_ATTACK_MAGICAL` | 3.5 |
| `monsters[22].stats.STOCK_CAST_SPEED` | 0.9 |
| `monsters[22].stats.STOCK_MANA` | 32 |
| `monsters[22].stats.STOCK_MANA_REGEN` | 2 |
| `monsters[22].trait` | `TRAIT_FROSTBITE` |
| `monsters[23].code` | [Снежный охотник](Monsters.md#snow_stalker) |
| `monsters[23].form` | `BEAST` |
| `monsters[23].experience` | 22 |
| `monsters[23].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[23].stats.STOCK_ATTACK_SPEED` | 1.5 |
| `monsters[23].stats.STOCK_HEALTH` | 17 |
| `monsters[23].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[23].stats.STOCK_EVASION` | 35 |
| `monsters[23].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[23].trait` | `TRAIT_FROSTBITE` |
| `monsters[24].code` | [Храмовый фанатик](Monsters.md#temple_zealot) |
| `monsters[24].form` | `HUMANOID` |
| `monsters[24].experience` | 24 |
| `monsters[24].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[24].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[24].stats.STOCK_HEALTH` | 20 |
| `monsters[24].stats.STOCK_ATTACK_PHYSICAL` | 4 |
| `monsters[24].stats.STOCK_ATTACK_FIRE` | 3 |
| `monsters[24].stats.STOCK_RESIST_FIRE` | 30 |
| `monsters[24].stats.STOCK_CRITICAL_CHANCE` | 7 |
| `monsters[24].stats.STOCK_ATTACK_MAGICAL` | 3 |
| `monsters[24].stats.STOCK_CAST_SPEED` | 0.8 |
| `monsters[24].stats.STOCK_MANA` | 30 |
| `monsters[24].stats.STOCK_MANA_REGEN` | 2 |
| `monsters[24].trait` | `TRAIT_SKILL_FIREBALL` |
| `monsters[25].code` | [Глубинный](Monsters.md#deep_one) |
| `monsters[25].form` | `SERPENT` |
| `monsters[25].experience` | 26 |
| `monsters[25].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[25].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[25].stats.STOCK_HEALTH` | 24 |
| `monsters[25].stats.STOCK_ATTACK_PHYSICAL` | 4 |
| `monsters[25].stats.STOCK_ATTACK_COLD` | 3 |
| `monsters[25].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[25].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[25].trait` | `TRAIT_SKILL_MANA_DRAIN` |
| `monsters[26].code` | [Послушник бездны](Monsters.md#abyssal_acolyte) |
| `monsters[26].form` | `WRAITH` |
| `monsters[26].experience` | 26 |
| `monsters[26].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[26].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[26].stats.STOCK_HEALTH` | 16 |
| `monsters[26].stats.STOCK_ENERGY_SHIELD` | 8 |
| `monsters[26].stats.STOCK_ATTACK_CHAOS` | 5 |
| `monsters[26].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[26].stats.STOCK_ATTACK_MAGICAL` | 4.5 |
| `monsters[26].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[26].stats.STOCK_MANA` | 40 |
| `monsters[26].stats.STOCK_MANA_REGEN` | 3 |
| `monsters[26].trait` | `TRAIT_SKILL_CHAOS_RING` |
| `monsters[27].code` | [Храмовый страж](Monsters.md#temple_guardian) |
| `monsters[27].form` | `GOLEM` |
| `monsters[27].experience` | 30 |
| `monsters[27].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[27].stats.STOCK_TAUNT` | 1 |
| `monsters[27].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[27].stats.STOCK_HEALTH` | 34 |
| `monsters[27].stats.STOCK_ATTACK_PHYSICAL` | 6 |
| `monsters[27].stats.STOCK_ARMOR` | 50 |
| `monsters[27].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[27].stats.STOCK_STUN_THRESHOLD` | 14 |
| `monsters[27].trait` | `TRAIT_PROVOKE` |
| `monsters[28].code` | [Песчаный скорпион](Monsters.md#sand_scorpion) |
| `monsters[28].form` | `SCORPION` |
| `monsters[28].experience` | 24 |
| `monsters[28].loot` | [loot:T4](Tables-LOOT.md#loot-t4) |
| `monsters[28].stats.STOCK_ATTACK_SPEED` | 1.3 |
| `monsters[28].stats.STOCK_HEALTH` | 21 |
| `monsters[28].stats.STOCK_ATTACK_PHYSICAL` | 4 |
| `monsters[28].stats.STOCK_ATTACK_CHAOS` | 2 |
| `monsters[28].stats.STOCK_ARMOR` | 25 |
| `monsters[28].stats.STOCK_RESIST_CHAOS` | 30 |
| `monsters[28].trait` | `TRAIT_IRONHIDE` |
| `monsters[29].code` | [Барханный хищник](Monsters.md#dune_stalker) |
| `monsters[29].form` | `BEAST` |
| `monsters[29].experience` | 22 |
| `monsters[29].loot` | [loot:T4](Tables-LOOT.md#loot-t4) |
| `monsters[29].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[29].stats.STOCK_HEALTH` | 18 |
| `monsters[29].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[29].stats.STOCK_EVASION` | 35 |
| `monsters[29].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[29].trait` | `TRAIT_AMBUSHER` |
| `monsters[30].code` | [Мумия](Monsters.md#mummy) |
| `monsters[30].form` | `UNDEAD` |
| `monsters[30].experience` | 26 |
| `monsters[30].loot` | [loot:T4](Tables-LOOT.md#loot-t4) |
| `monsters[30].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[30].stats.STOCK_HEALTH` | 30 |
| `monsters[30].stats.STOCK_ATTACK_PHYSICAL` | 5.5 |
| `monsters[30].stats.STOCK_ARMOR` | 20 |
| `monsters[30].stats.STOCK_RESIST_CHAOS` | 30 |
| `monsters[30].stats.STOCK_STUN_THRESHOLD` | 8 |
| `monsters[30].trait` | `TRAIT_SKILL_VULNERABILITY` |
| `monsters[31].code` | [Дух самума](Monsters.md#sandstorm_spirit) |
| `monsters[31].form` | `WRAITH` |
| `monsters[31].experience` | 25 |
| `monsters[31].loot` | [loot:T4](Tables-LOOT.md#loot-t4) |
| `monsters[31].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[31].stats.STOCK_HEALTH` | 12 |
| `monsters[31].stats.STOCK_ENERGY_SHIELD` | 12 |
| `monsters[31].stats.STOCK_ATTACK_LIGHTNING` | 5 |
| `monsters[31].stats.STOCK_EVASION` | 30 |
| `monsters[31].stats.STOCK_RESIST_LIGHTNING` | 60 |
| `monsters[31].stats.STOCK_ATTACK_MAGICAL` | 3.5 |
| `monsters[31].stats.STOCK_CAST_SPEED` | 0.9 |
| `monsters[31].stats.STOCK_MANA` | 32 |
| `monsters[31].stats.STOCK_MANA_REGEN` | 2 |
| `monsters[31].trait` | `TRAIT_SKILL_ZAP` |
| `monsters[32].code` | [Скальный стервятник](Monsters.md#cliff_vulture) |
| `monsters[32].form` | `BAT` |
| `monsters[32].experience` | 18 |
| `monsters[32].loot` | [loot:T4](Tables-LOOT.md#loot-t4) |
| `monsters[32].stats.STOCK_ATTACK_SPEED` | 1.7 |
| `monsters[32].stats.STOCK_HEALTH` | 13 |
| `monsters[32].stats.STOCK_ATTACK_PHYSICAL` | 4 |
| `monsters[32].stats.STOCK_EVASION` | 35 |
| `monsters[32].trait` | `TRAIT_RENDING` |
| `monsters[33].code` | [Налётчик каньона](Monsters.md#canyon_raider) |
| `monsters[33].form` | `HUMANOID` |
| `monsters[33].experience` | 23 |
| `monsters[33].loot` | [loot:T4](Tables-LOOT.md#loot-t4) |
| `monsters[33].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[33].stats.STOCK_HEALTH` | 19 |
| `monsters[33].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[33].stats.STOCK_CRITICAL_CHANCE` | 9 |
| `monsters[33].stats.STOCK_EVASION` | 15 |
| `monsters[33].trait` | `TRAIT_AMBUSHER` |
| `monsters[34].code` | [Песчаниковый голем](Monsters.md#sandstone_golem) |
| `monsters[34].form` | `GOLEM` |
| `monsters[34].experience` | 30 |
| `monsters[34].loot` | [loot:T4](Tables-LOOT.md#loot-t4) |
| `monsters[34].stats.STOCK_TAUNT` | 1 |
| `monsters[34].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[34].stats.STOCK_HEALTH` | 34 |
| `monsters[34].stats.STOCK_ATTACK_PHYSICAL` | 6 |
| `monsters[34].stats.STOCK_ARMOR` | 50 |
| `monsters[34].stats.STOCK_STUN_THRESHOLD` | 12 |
| `monsters[34].trait` | `TRAIT_PROVOKE` |
| `monsters[35].code` | [Изумрудная пантера](Monsters.md#emerald_panther) |
| `monsters[35].form` | `BEAST` |
| `monsters[35].experience` | 24 |
| `monsters[35].loot` | [loot:T5](Tables-LOOT.md#loot-t5) |
| `monsters[35].stats.STOCK_ATTACK_SPEED` | 1.7 |
| `monsters[35].stats.STOCK_HEALTH` | 19 |
| `monsters[35].stats.STOCK_ATTACK_PHYSICAL` | 5.5 |
| `monsters[35].stats.STOCK_EVASION` | 40 |
| `monsters[35].stats.STOCK_CRITICAL_CHANCE` | 12 |
| `monsters[35].trait` | `TRAIT_SKILL_REND` |
| `monsters[36].code` | [Споровик](Monsters.md#sporeling) |
| `monsters[36].form` | `FUNGUS` |
| `monsters[36].experience` | 24 |
| `monsters[36].loot` | [loot:T5](Tables-LOOT.md#loot-t5) |
| `monsters[36].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[36].stats.STOCK_HEALTH` | 27 |
| `monsters[36].stats.STOCK_ATTACK_CHAOS` | 4 |
| `monsters[36].stats.STOCK_RESIST_CHAOS` | 50 |
| `monsters[36].stats.STOCK_HEALTH_REGEN` | 1 |
| `monsters[36].trait` | `TRAIT_SKILL_SPIT` |
| `monsters[37].code` | [Богомол-великан](Monsters.md#giant_mantis) |
| `monsters[37].form` | `INSECT` |
| `monsters[37].experience` | 25 |
| `monsters[37].loot` | [loot:T5](Tables-LOOT.md#loot-t5) |
| `monsters[37].stats.STOCK_ATTACK_SPEED` | 1.5 |
| `monsters[37].stats.STOCK_HEALTH` | 20 |
| `monsters[37].stats.STOCK_ATTACK_PHYSICAL` | 6 |
| `monsters[37].stats.STOCK_CRITICAL_CHANCE` | 12 |
| `monsters[37].stats.STOCK_EVASION` | 25 |
| `monsters[37].trait` | `TRAIT_FRENZIED` |
| `monsters[38].code` | [Охотник с трубкой](Monsters.md#blowgun_hunter) |
| `monsters[38].form` | `HUMANOID` |
| `monsters[38].experience` | 23 |
| `monsters[38].loot` | [loot:T5](Tables-LOOT.md#loot-t5) |
| `monsters[38].stats.STOCK_ATTACK_SPEED` | 1.3 |
| `monsters[38].stats.STOCK_HEALTH` | 17 |
| `monsters[38].stats.STOCK_ATTACK_PHYSICAL` | 3 |
| `monsters[38].stats.STOCK_ATTACK_CHAOS` | 3 |
| `monsters[38].stats.STOCK_POISON_CHANCE` | 15 |
| `monsters[38].stats.STOCK_EVASION` | 20 |
| `monsters[38].trait` | `TRAIT_SKILL_SPIT` |
| `monsters[39].code` | [Трутень улья](Monsters.md#hive_drone) |
| `monsters[39].form` | `INSECT` |
| `monsters[39].experience` | 18 |
| `monsters[39].loot` | [loot:T5](Tables-LOOT.md#loot-t5) |
| `monsters[39].stats.STOCK_ATTACK_SPEED` | 1.8 |
| `monsters[39].stats.STOCK_HEALTH` | 12 |
| `monsters[39].stats.STOCK_ATTACK_PHYSICAL` | 3.5 |
| `monsters[39].stats.STOCK_EVASION` | 35 |
| `monsters[39].trait` | `TRAIT_NIMBLE` |
| `monsters[40].code` | [Солдат улья](Monsters.md#hive_soldier) |
| `monsters[40].form` | `INSECT` |
| `monsters[40].experience` | 27 |
| `monsters[40].loot` | [loot:T5](Tables-LOOT.md#loot-t5) |
| `monsters[40].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[40].stats.STOCK_HEALTH` | 27 |
| `monsters[40].stats.STOCK_ATTACK_PHYSICAL` | 6 |
| `monsters[40].stats.STOCK_ARMOR` | 35 |
| `monsters[40].stats.STOCK_BLOCK_CHANCE` | 15 |
| `monsters[40].trait` | `TRAIT_SHIELD_WALL` |
| `monsters[41].code` | [Плевун выводка](Monsters.md#brood_spitter) |
| `monsters[41].form` | `INSECT` |
| `monsters[41].experience` | 24 |
| `monsters[41].loot` | [loot:T5](Tables-LOOT.md#loot-t5) |
| `monsters[41].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[41].stats.STOCK_HEALTH` | 16 |
| `monsters[41].stats.STOCK_ATTACK_CHAOS` | 4.5 |
| `monsters[41].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[41].trait` | `TRAIT_SKILL_SPIT` |
| `monsters[42].code` | [Лавовый бес](Monsters.md#lava_imp) |
| `monsters[42].form` | `DEMON` |
| `monsters[42].experience` | 23 |
| `monsters[42].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[42].stats.STOCK_ATTACK_SPEED` | 1.5 |
| `monsters[42].stats.STOCK_HEALTH` | 17 |
| `monsters[42].stats.STOCK_ATTACK_FIRE` | 5 |
| `monsters[42].stats.STOCK_RESIST_FIRE` | 60 |
| `monsters[42].stats.STOCK_EVASION` | 20 |
| `monsters[42].trait` | `TRAIT_FIRE_BURST` |
| `monsters[43].code` | [Магмовый голем](Monsters.md#magma_golem) |
| `monsters[43].form` | `GOLEM` |
| `monsters[43].experience` | 31 |
| `monsters[43].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[43].stats.STOCK_TAUNT` | 1 |
| `monsters[43].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[43].stats.STOCK_HEALTH` | 36 |
| `monsters[43].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[43].stats.STOCK_ATTACK_FIRE` | 4 |
| `monsters[43].stats.STOCK_ARMOR` | 55 |
| `monsters[43].stats.STOCK_RESIST_FIRE` | 75 |
| `monsters[43].trait` | `TRAIT_PROVOKE` |
| `monsters[44].code` | [Огненный змей](Monsters.md#fire_drake) |
| `monsters[44].form` | `SERPENT` |
| `monsters[44].experience` | 26 |
| `monsters[44].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[44].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[44].stats.STOCK_HEALTH` | 24 |
| `monsters[44].stats.STOCK_ATTACK_PHYSICAL` | 3 |
| `monsters[44].stats.STOCK_ATTACK_FIRE` | 5 |
| `monsters[44].stats.STOCK_RESIST_FIRE` | 50 |
| `monsters[44].trait` | `TRAIT_SKILL_FLAME_BREATH` |
| `monsters[45].code` | [Инфернальный рыцарь](Monsters.md#infernal_knight) |
| `monsters[45].form` | `DEMON` |
| `monsters[45].experience` | 30 |
| `monsters[45].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[45].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[45].stats.STOCK_HEALTH` | 32 |
| `monsters[45].stats.STOCK_ATTACK_PHYSICAL` | 7 |
| `monsters[45].stats.STOCK_ATTACK_FIRE` | 3 |
| `monsters[45].stats.STOCK_ARMOR` | 45 |
| `monsters[45].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[45].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[45].trait` | `TRAIT_SKILL_WARCRY` |
| `monsters[46].code` | [Чернокнижник цитадели](Monsters.md#citadel_warlock) |
| `monsters[46].form` | `HUMANOID` |
| `monsters[46].experience` | 26 |
| `monsters[46].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[46].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[46].stats.STOCK_HEALTH` | 18 |
| `monsters[46].stats.STOCK_ATTACK_FIRE` | 3 |
| `monsters[46].stats.STOCK_ATTACK_CHAOS` | 2 |
| `monsters[46].stats.STOCK_RESIST_FIRE` | 30 |
| `monsters[46].stats.STOCK_ATTACK_MAGICAL` | 4 |
| `monsters[46].stats.STOCK_CAST_SPEED` | 0.9 |
| `monsters[46].stats.STOCK_MANA` | 36 |
| `monsters[46].stats.STOCK_MANA_REGEN` | 2.5 |
| `monsters[46].trait` | `TRAIT_SKILL_ENFEEBLE` |
| `monsters[47].code` | [Горгулья](Monsters.md#gargoyle) |
| `monsters[47].form` | `BAT` |
| `monsters[47].experience` | 24 |
| `monsters[47].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[47].stats.STOCK_ATTACK_SPEED` | 1.4 |
| `monsters[47].stats.STOCK_HEALTH` | 22 |
| `monsters[47].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[47].stats.STOCK_ARMOR` | 40 |
| `monsters[47].stats.STOCK_EVASION` | 20 |
| `monsters[47].trait` | `TRAIT_SKILL_STONESKIN` |
| `monsters[48].code` | [Порождение пустоты](Monsters.md#void_spawn) |
| `monsters[48].form` | `DEMON` |
| `monsters[48].experience` | 27 |
| `monsters[48].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[48].stats.STOCK_ATTACK_SPEED` | 1.3 |
| `monsters[48].stats.STOCK_HEALTH` | 24 |
| `monsters[48].stats.STOCK_ATTACK_PHYSICAL` | 3 |
| `monsters[48].stats.STOCK_ATTACK_CHAOS` | 6 |
| `monsters[48].stats.STOCK_RESIST_CHAOS` | 60 |
| `monsters[48].trait` | `TRAIT_VOLATILE` |
| `monsters[49].code` | [Ползун бездны](Monsters.md#abyss_crawler) |
| `monsters[49].form` | `SPIDER` |
| `monsters[49].experience` | 24 |
| `monsters[49].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[49].stats.STOCK_ATTACK_SPEED` | 1.5 |
| `monsters[49].stats.STOCK_HEALTH` | 18 |
| `monsters[49].stats.STOCK_ATTACK_PHYSICAL` | 4 |
| `monsters[49].stats.STOCK_ATTACK_CHAOS` | 3 |
| `monsters[49].stats.STOCK_EVASION` | 30 |
| `monsters[49].trait` | `TRAIT_VOIDTOUCHED` |
| `monsters[50].code` | [Тень бездны](Monsters.md#abyssal_shade) |
| `monsters[50].form` | `WRAITH` |
| `monsters[50].experience` | 27 |
| `monsters[50].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[50].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[50].stats.STOCK_HEALTH` | 14 |
| `monsters[50].stats.STOCK_ENERGY_SHIELD` | 14 |
| `monsters[50].stats.STOCK_ATTACK_COLD` | 3 |
| `monsters[50].stats.STOCK_ATTACK_CHAOS` | 3 |
| `monsters[50].stats.STOCK_RESIST_CHAOS` | 50 |
| `monsters[50].stats.STOCK_ATTACK_MAGICAL` | 4.5 |
| `monsters[50].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[50].stats.STOCK_MANA` | 40 |
| `monsters[50].stats.STOCK_MANA_REGEN` | 3 |
| `monsters[50].trait` | `TRAIT_ENERGY_WEAVE` |
| `monsters[51].code` | [Гнилошляп](Monsters.md#blightcap) |
| `monsters[51].form` | `FUNGUS` |
| `monsters[51].experience` | 26 |
| `monsters[51].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[51].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[51].stats.STOCK_HEALTH` | 30 |
| `monsters[51].stats.STOCK_ATTACK_CHAOS` | 5 |
| `monsters[51].stats.STOCK_RESIST_CHAOS` | 60 |
| `monsters[51].stats.STOCK_HEALTH_REGEN` | 1.5 |
| `monsters[51].trait` | `TRAIT_REGROWTH` |
| `monsters[52].code` | [Чумоносец](Monsters.md#plague_bearer) |
| `monsters[52].form` | `UNDEAD` |
| `monsters[52].experience` | 28 |
| `monsters[52].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[52].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[52].stats.STOCK_HEALTH` | 30 |
| `monsters[52].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[52].stats.STOCK_ATTACK_CHAOS` | 3 |
| `monsters[52].stats.STOCK_LEECH_ALL` | 4 |
| `monsters[52].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[52].trait` | `TRAIT_VOLATILE` |
| `monsters[53].code` | [Чумная оса](Monsters.md#plague_wasp) |
| `monsters[53].form` | `INSECT` |
| `monsters[53].experience` | 20 |
| `monsters[53].loot` | [loot:T6](Tables-LOOT.md#loot-t6) |
| `monsters[53].stats.STOCK_ATTACK_SPEED` | 1.9 |
| `monsters[53].stats.STOCK_HEALTH` | 13 |
| `monsters[53].stats.STOCK_ATTACK_PHYSICAL` | 3 |
| `monsters[53].stats.STOCK_ATTACK_CHAOS` | 3 |
| `monsters[53].stats.STOCK_EVASION` | 35 |
| `monsters[53].trait` | `TRAIT_VENOM_STING` |
| `monsters[54].code` | [Зовущий Прилив](Bosses.md#boss_tidecaller) |
| `monsters[54].form` | `HUMANOID` |
| `monsters[54].experience` | 60 |
| `monsters[54].loot` | [loot:BOSS_1](Tables-LOOT.md#loot-boss_1) |
| `monsters[54].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[54].stats.STOCK_HEALTH` | 36.298 |
| `monsters[54].stats.STOCK_ATTACK_PHYSICAL` | 4.533 |
| `monsters[54].stats.STOCK_ATTACK_COLD` | 3.029 |
| `monsters[54].stats.STOCK_RESIST_COLD` | 30 |
| `monsters[54].stats.STOCK_MANA` | 30 |
| `monsters[54].boss` | Да |
| `monsters[54].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[54].fixed` | [MOB_TOUGH](Modifiers-MONSTER.md#mob_tough); [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest) |
| `monsters[54].tables` | [boss:BOSS_TIDECALLER](Tables-TEMPLATE.md#boss-boss_tidecaller) |
| `monsters[54].trait` | `TRAIT_CRUSHER` |
| `monsters[55].code` | [Мать Рассола](Bosses.md#boss_brine_mother) |
| `monsters[55].form` | `CRAB` |
| `monsters[55].experience` | 60 |
| `monsters[55].loot` | [loot:BOSS_1](Tables-LOOT.md#loot-boss_1) |
| `monsters[55].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[55].stats.STOCK_HEALTH` | 27.561 |
| `monsters[55].stats.STOCK_ATTACK_PHYSICAL` | 3.338 |
| `monsters[55].stats.STOCK_ARMOR` | 55 |
| `monsters[55].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[55].stats.STOCK_STUN_THRESHOLD` | 10 |
| `monsters[55].stats.STOCK_MANA` | 30 |
| `monsters[55].boss` | Да |
| `monsters[55].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[55].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_STRONG](Modifiers-MONSTER.md#mob_strong) |
| `monsters[55].tables` | [boss:BOSS_BRINE_MOTHER](Tables-TEMPLATE.md#boss-boss_brine_mother) |
| `monsters[56].code` | [Болотная Королева](Bosses.md#boss_bog_queen) |
| `monsters[56].form` | `SLUG` |
| `monsters[56].experience` | 60 |
| `monsters[56].loot` | [loot:BOSS_1](Tables-LOOT.md#loot-boss_1) |
| `monsters[56].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[56].stats.STOCK_HEALTH` | 43.82 |
| `monsters[56].stats.STOCK_ATTACK_CHAOS` | 4.382 |
| `monsters[56].stats.STOCK_HEALTH_REGEN` | 1.881 |
| `monsters[56].stats.STOCK_RESIST_CHAOS` | 50 |
| `monsters[56].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[56].stats.STOCK_MANA` | 30 |
| `monsters[56].boss` | Да |
| `monsters[56].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[56].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_REGENERATING](Modifiers-MONSTER.md#mob_regenerating) |
| `monsters[56].tables` | [boss:BOSS_BOG_QUEEN](Tables-TEMPLATE.md#boss-boss_bog_queen) |
| `monsters[57].code` | [Вожак Стаи](Bosses.md#boss_alpha_wolf) |
| `monsters[57].form` | `BEAST` |
| `monsters[57].experience` | 60 |
| `monsters[57].loot` | [loot:BOSS_2](Tables-LOOT.md#loot-boss_2) |
| `monsters[57].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[57].stats.STOCK_HEALTH` | 5.953 |
| `monsters[57].stats.STOCK_ATTACK_PHYSICAL` | 0.925 |
| `monsters[57].stats.STOCK_EVASION` | 80 |
| `monsters[57].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[57].stats.STOCK_MOVEMENT_SPEED` | 20 |
| `monsters[57].stats.STOCK_MANA` | 30 |
| `monsters[57].boss` | Да |
| `monsters[57].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[57].fixed` | [MOB_HASTED](Modifiers-MONSTER.md#mob_hasted); [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner) |
| `monsters[57].tables` | [boss:BOSS_ALPHA_WOLF](Tables-TEMPLATE.md#boss-boss_alpha_wolf) |
| `monsters[58].code` | [Страж Руин](Bosses.md#boss_ruin_warden) |
| `monsters[58].form` | `GOLEM` |
| `monsters[58].experience` | 60 |
| `monsters[58].loot` | [loot:BOSS_2](Tables-LOOT.md#loot-boss_2) |
| `monsters[58].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[58].stats.STOCK_HEALTH` | 18.573 |
| `monsters[58].stats.STOCK_ATTACK_PHYSICAL` | 2.186 |
| `monsters[58].stats.STOCK_ARMOR` | 90 |
| `monsters[58].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[58].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[58].stats.STOCK_AVOID_STUN` | 50 |
| `monsters[58].stats.STOCK_MANA` | 30 |
| `monsters[58].boss` | Да |
| `monsters[58].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[58].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_STALWART](Modifiers-MONSTER.md#mob_stalwart) |
| `monsters[58].tables` | [boss:BOSS_RUIN_WARDEN](Tables-TEMPLATE.md#boss-boss_ruin_warden) |
| `monsters[58].trait` | `TRAIT_UNSHAKEN` |
| `monsters[59].code` | [Владыка Склепа](Bosses.md#boss_crypt_lord) |
| `monsters[59].form` | `UNDEAD` |
| `monsters[59].experience` | 60 |
| `monsters[59].loot` | [loot:BOSS_2](Tables-LOOT.md#loot-boss_2) |
| `monsters[59].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[59].stats.STOCK_HEALTH` | 3.573 |
| `monsters[59].stats.STOCK_ATTACK_PHYSICAL` | 0.448 |
| `monsters[59].stats.STOCK_ATTACK_CHAOS` | 0.279 |
| `monsters[59].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[59].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[59].stats.STOCK_MANA` | 30 |
| `monsters[59].boss` | Да |
| `monsters[59].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[59].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_VAMPIRIC](Modifiers-MONSTER.md#mob_vampiric) |
| `monsters[59].tables` | [boss:BOSS_CRYPT_LORD](Tables-TEMPLATE.md#boss-boss_crypt_lord) |
| `monsters[60].code` | [Королева Осколков](Bosses.md#boss_shard_queen) |
| `monsters[60].form` | `SPIDER` |
| `monsters[60].experience` | 60 |
| `monsters[60].loot` | [loot:BOSS_2](Tables-LOOT.md#loot-boss_2) |
| `monsters[60].stats.STOCK_ATTACK_SPEED` | 1.4 |
| `monsters[60].stats.STOCK_HEALTH` | 29.256 |
| `monsters[60].stats.STOCK_ATTACK_PHYSICAL` | 3.032 |
| `monsters[60].stats.STOCK_ATTACK_LIGHTNING` | 3.527 |
| `monsters[60].stats.STOCK_CRITICAL_CHANCE` | 12 |
| `monsters[60].stats.STOCK_SHOCK_CHANCE` | 25 |
| `monsters[60].stats.STOCK_MANA` | 30 |
| `monsters[60].boss` | Да |
| `monsters[60].skills` | [Разряд](Monster-skills.md#mob_zap); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[60].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_CRACKLING](Modifiers-MONSTER.md#mob_crackling) |
| `monsters[60].tables` | [boss:BOSS_SHARD_QUEEN](Tables-TEMPLATE.md#boss-boss_shard_queen) |
| `monsters[61].code` | [Пепельный Тиран](Bosses.md#boss_ashen_tyrant) |
| `monsters[61].form` | `BRUTE` |
| `monsters[61].experience` | 60 |
| `monsters[61].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[61].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[61].stats.STOCK_HEALTH` | 36.752 |
| `monsters[61].stats.STOCK_ATTACK_PHYSICAL` | 3.758 |
| `monsters[61].stats.STOCK_ATTACK_FIRE` | 3.758 |
| `monsters[61].stats.STOCK_RESIST_FIRE` | 60 |
| `monsters[61].stats.STOCK_IGNITE_CHANCE` | 25 |
| `monsters[61].stats.STOCK_PENETRATE_ELEMENTAL` | 10 |
| `monsters[61].stats.STOCK_MANA` | 30 |
| `monsters[61].boss` | Да |
| `monsters[61].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[61].fixed` | [MOB_BURNING](Modifiers-MONSTER.md#mob_burning); [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord) |
| `monsters[61].tables` | [boss:BOSS_ASHEN_TYRANT](Tables-TEMPLATE.md#boss-boss_ashen_tyrant) |
| `monsters[61].trait` | `TRAIT_CRUSHER` |
| `monsters[62].code` | [Ледяной Монарх](Bosses.md#boss_frost_monarch) |
| `monsters[62].form` | `WRAITH` |
| `monsters[62].experience` | 60 |
| `monsters[62].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[62].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[62].stats.STOCK_HEALTH` | 38.279 |
| `monsters[62].stats.STOCK_ENERGY_SHIELD` | 25.764 |
| `monsters[62].stats.STOCK_ATTACK_COLD` | 6.62 |
| `monsters[62].stats.STOCK_RESIST_COLD` | 70 |
| `monsters[62].stats.STOCK_ATTACK_MAGICAL` | 4.42 |
| `monsters[62].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[62].stats.STOCK_MANA` | 60 |
| `monsters[62].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[62].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[62].boss` | Да |
| `monsters[62].skills` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt) |
| `monsters[62].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_CHILLING](Modifiers-MONSTER.md#mob_chilling) |
| `monsters[62].tables` | [boss:BOSS_FROST_MONARCH](Tables-TEMPLATE.md#boss-boss_frost_monarch) |
| `monsters[62].trait` | `TRAIT_ICE_SHELL` |
| `monsters[63].code` | [Утонувший Бог](Bosses.md#boss_drowned_god) |
| `monsters[63].form` | `SERPENT` |
| `monsters[63].experience` | 60 |
| `monsters[63].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[63].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[63].stats.STOCK_HEALTH` | 6.835 |
| `monsters[63].stats.STOCK_ATTACK_PHYSICAL` | 0.708 |
| `monsters[63].stats.STOCK_ATTACK_CHAOS` | 0.495 |
| `monsters[63].stats.STOCK_ENERGY_SHIELD` | 2.134 |
| `monsters[63].stats.STOCK_RESIST_CHAOS` | 50 |
| `monsters[63].stats.STOCK_RESIST_ALL` | 20 |
| `monsters[63].stats.STOCK_REFLECT` | 10 |
| `monsters[63].stats.STOCK_MANA` | 30 |
| `monsters[63].boss` | Да |
| `monsters[63].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[63].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_WARDED](Modifiers-MONSTER.md#mob_warded) |
| `monsters[63].tables` | [boss:BOSS_DROWNED_GOD](Tables-TEMPLATE.md#boss-boss_drowned_god) |
| `monsters[64].code` | [Капитан «Скорби»](Bosses.md#boss_sorrow_captain) |
| `monsters[64].form` | `HUMANOID` |
| `monsters[64].experience` | 60 |
| `monsters[64].loot` | [loot:BOSS_1](Tables-LOOT.md#loot-boss_1) |
| `monsters[64].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[64].stats.STOCK_HEALTH` | 45.491 |
| `monsters[64].stats.STOCK_ATTACK_PHYSICAL` | 5.393 |
| `monsters[64].stats.STOCK_ATTACK_COLD` | 3.2 |
| `monsters[64].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[64].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[64].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[64].stats.STOCK_MANA` | 30 |
| `monsters[64].boss` | Да |
| `monsters[64].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[64].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_CHILLING](Modifiers-MONSTER.md#mob_chilling) |
| `monsters[64].tables` | [boss:BOSS_SORROW_CAPTAIN](Tables-TEMPLATE.md#boss-boss_sorrow_captain) |
| `monsters[64].trait` | `TRAIT_CRUSHER` |
| `monsters[65].code` | [Королева Гнёзд](Bosses.md#boss_nest_queen) |
| `monsters[65].form` | `BAT` |
| `monsters[65].experience` | 60 |
| `monsters[65].loot` | [loot:BOSS_1](Tables-LOOT.md#loot-boss_1) |
| `monsters[65].stats.STOCK_ATTACK_SPEED` | 1.8 |
| `monsters[65].stats.STOCK_HEALTH` | 22.803 |
| `monsters[65].stats.STOCK_ATTACK_PHYSICAL` | 2.797 |
| `monsters[65].stats.STOCK_EVASION` | 80 |
| `monsters[65].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[65].stats.STOCK_MANA` | 30 |
| `monsters[65].boss` | Да |
| `monsters[65].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[65].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_SWIFT](Modifiers-MONSTER.md#mob_swift) |
| `monsters[65].tables` | [boss:BOSS_NEST_QUEEN](Tables-TEMPLATE.md#boss-boss_nest_queen) |
| `monsters[66].code` | [Смотритель Маяка](Bosses.md#boss_lamp_warden) |
| `monsters[66].form` | `WRAITH` |
| `monsters[66].experience` | 60 |
| `monsters[66].loot` | [loot:BOSS_1](Tables-LOOT.md#loot-boss_1) |
| `monsters[66].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[66].stats.STOCK_HEALTH` | 12.261 |
| `monsters[66].stats.STOCK_ENERGY_SHIELD` | 7.244 |
| `monsters[66].stats.STOCK_ATTACK_LIGHTNING` | 1.67 |
| `monsters[66].stats.STOCK_RESIST_LIGHTNING` | 70 |
| `monsters[66].stats.STOCK_ATTACK_MAGICAL` | 1.307 |
| `monsters[66].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[66].stats.STOCK_MANA` | 55 |
| `monsters[66].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[66].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[66].boss` | Да |
| `monsters[66].skills` | [Разряд](Monster-skills.md#mob_zap); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[66].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_SHOCKING](Modifiers-MONSTER.md#mob_shocking) |
| `monsters[66].tables` | [boss:BOSS_LAMP_WARDEN](Tables-TEMPLATE.md#boss-boss_lamp_warden) |
| `monsters[66].trait` | `TRAIT_ICE_SHELL` |
| `monsters[67].code` | [Паромщик](Bosses.md#boss_ferryman) |
| `monsters[67].form` | `UNDEAD` |
| `monsters[67].experience` | 60 |
| `monsters[67].loot` | [loot:BOSS_1](Tables-LOOT.md#loot-boss_1) |
| `monsters[67].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[67].stats.STOCK_HEALTH` | 37.942 |
| `monsters[67].stats.STOCK_ATTACK_PHYSICAL` | 3.86 |
| `monsters[67].stats.STOCK_ATTACK_COLD` | 2.57 |
| `monsters[67].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[67].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[67].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[67].stats.STOCK_MANA` | 30 |
| `monsters[67].boss` | Да |
| `monsters[67].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[67].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_DRAINING](Modifiers-MONSTER.md#mob_draining) |
| `monsters[67].tables` | [boss:BOSS_FERRYMAN](Tables-TEMPLATE.md#boss-boss_ferryman) |
| `monsters[68].code` | [Король Контрабандистов](Bosses.md#boss_smuggler_king) |
| `monsters[68].form` | `HUMANOID` |
| `monsters[68].experience` | 60 |
| `monsters[68].loot` | [loot:BOSS_1](Tables-LOOT.md#loot-boss_1) |
| `monsters[68].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[68].stats.STOCK_HEALTH` | 41.147 |
| `monsters[68].stats.STOCK_ATTACK_PHYSICAL` | 4.677 |
| `monsters[68].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[68].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[68].stats.STOCK_MANA` | 30 |
| `monsters[68].boss` | Да |
| `monsters[68].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[68].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_EVASIVE](Modifiers-MONSTER.md#mob_evasive) |
| `monsters[68].tables` | [boss:BOSS_SMUGGLER_KING](Tables-TEMPLATE.md#boss-boss_smuggler_king) |
| `monsters[69].code` | [Трясинный Змей](Bosses.md#boss_mire_serpent) |
| `monsters[69].form` | `SERPENT` |
| `monsters[69].experience` | 60 |
| `monsters[69].loot` | [loot:BOSS_2](Tables-LOOT.md#loot-boss_2) |
| `monsters[69].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[69].stats.STOCK_HEALTH` | 37.276 |
| `monsters[69].stats.STOCK_ATTACK_PHYSICAL` | 3.032 |
| `monsters[69].stats.STOCK_ATTACK_CHAOS` | 2.661 |
| `monsters[69].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[69].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[69].stats.STOCK_MANA` | 30 |
| `monsters[69].boss` | Да |
| `monsters[69].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[69].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_TOXIC](Modifiers-MONSTER.md#mob_toxic) |
| `monsters[69].tables` | [boss:BOSS_MIRE_SERPENT](Tables-TEMPLATE.md#boss-boss_mire_serpent) |
| `monsters[70].code` | [Матёрый Волколак](Bosses.md#boss_old_werewolf) |
| `monsters[70].form` | `BEAST` |
| `monsters[70].experience` | 60 |
| `monsters[70].loot` | [loot:BOSS_2](Tables-LOOT.md#loot-boss_2) |
| `monsters[70].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[70].stats.STOCK_HEALTH` | 10.035 |
| `monsters[70].stats.STOCK_ATTACK_PHYSICAL` | 1.307 |
| `monsters[70].stats.STOCK_EVASION` | 70 |
| `monsters[70].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[70].stats.STOCK_MOVEMENT_SPEED` | 15 |
| `monsters[70].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[70].stats.STOCK_MANA` | 30 |
| `monsters[70].boss` | Да |
| `monsters[70].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[70].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_BERSERK](Modifiers-MONSTER.md#mob_berserk) |
| `monsters[70].tables` | [boss:BOSS_OLD_WEREWOLF](Tables-TEMPLATE.md#boss-boss_old_werewolf) |
| `monsters[71].code` | [Комендант Заставы](Bosses.md#boss_outpost_commander) |
| `monsters[71].form` | `UNDEAD` |
| `monsters[71].experience` | 60 |
| `monsters[71].loot` | [loot:BOSS_2](Tables-LOOT.md#loot-boss_2) |
| `monsters[71].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[71].stats.STOCK_HEALTH` | 22.889 |
| `monsters[71].stats.STOCK_ATTACK_PHYSICAL` | 2.252 |
| `monsters[71].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[71].stats.STOCK_ARMOR` | 40 |
| `monsters[71].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[71].stats.STOCK_MANA` | 30 |
| `monsters[71].boss` | Да |
| `monsters[71].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[71].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_ARMOURED](Modifiers-MONSTER.md#mob_armoured) |
| `monsters[72].code` | [Вождь Кургана](Bosses.md#boss_barrow_chieftain) |
| `monsters[72].form` | `UNDEAD` |
| `monsters[72].experience` | 60 |
| `monsters[72].loot` | [loot:BOSS_2](Tables-LOOT.md#loot-boss_2) |
| `monsters[72].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[72].stats.STOCK_HEALTH` | 1.085 |
| `monsters[72].stats.STOCK_ATTACK_PHYSICAL` | 0.107 |
| `monsters[72].stats.STOCK_ATTACK_COLD` | 0.072 |
| `monsters[72].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[72].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[72].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[72].stats.STOCK_MANA` | 30 |
| `monsters[72].boss` | Да |
| `monsters[72].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[72].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_FROSTBORN](Modifiers-MONSTER.md#mob_frostborn) |
| `monsters[72].tables` | [boss:BOSS_BARROW_CHIEFTAIN](Tables-TEMPLATE.md#boss-boss_barrow_chieftain) |
| `monsters[73].code` | [Бригадир Проходчиков](Bosses.md#boss_foreman) |
| `monsters[73].form` | `HUMANOID` |
| `monsters[73].experience` | 60 |
| `monsters[73].loot` | [loot:BOSS_2](Tables-LOOT.md#loot-boss_2) |
| `monsters[73].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[73].stats.STOCK_HEALTH` | 47.574 |
| `monsters[73].stats.STOCK_ATTACK_PHYSICAL` | 5.133 |
| `monsters[73].stats.STOCK_ATTACK_LIGHTNING` | 3.071 |
| `monsters[73].stats.STOCK_RESIST_LIGHTNING` | 40 |
| `monsters[73].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[73].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[73].stats.STOCK_MANA` | 30 |
| `monsters[73].boss` | Да |
| `monsters[73].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[73].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_CRACKLING](Modifiers-MONSTER.md#mob_crackling) |
| `monsters[73].tables` | [boss:BOSS_FOREMAN](Tables-TEMPLATE.md#boss-boss_foreman) |
| `monsters[74].code` | [Белая Рысь](Bosses.md#boss_white_lynx) |
| `monsters[74].form` | `BEAST` |
| `monsters[74].experience` | 60 |
| `monsters[74].loot` | [loot:BOSS_2](Tables-LOOT.md#loot-boss_2) |
| `monsters[74].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[74].stats.STOCK_HEALTH` | 5.124 |
| `monsters[74].stats.STOCK_ATTACK_PHYSICAL` | 0.639 |
| `monsters[74].stats.STOCK_ATTACK_COLD` | 0.306 |
| `monsters[74].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[74].stats.STOCK_EVASION` | 70 |
| `monsters[74].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[74].stats.STOCK_MOVEMENT_SPEED` | 15 |
| `monsters[74].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[74].stats.STOCK_MANA` | 30 |
| `monsters[74].boss` | Да |
| `monsters[74].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[74].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_FREEZING](Modifiers-MONSTER.md#mob_freezing) |
| `monsters[75].code` | [Рудный Голем](Bosses.md#boss_ore_golem) |
| `monsters[75].form` | `GOLEM` |
| `monsters[75].experience` | 60 |
| `monsters[75].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[75].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[75].stats.STOCK_HEALTH` | 37.02 |
| `monsters[75].stats.STOCK_ATTACK_PHYSICAL` | 3.076 |
| `monsters[75].stats.STOCK_ATTACK_LIGHTNING` | 1.409 |
| `monsters[75].stats.STOCK_RESIST_LIGHTNING` | 40 |
| `monsters[75].stats.STOCK_ARMOR` | 90 |
| `monsters[75].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[75].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[75].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[75].stats.STOCK_MANA` | 30 |
| `monsters[75].boss` | Да |
| `monsters[75].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[75].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_JUGGERNAUT](Modifiers-MONSTER.md#mob_juggernaut) |
| `monsters[75].trait` | `TRAIT_UNSHAKEN` |
| `monsters[76].code` | [Прядильщица Жеод](Bosses.md#boss_geode_spinner) |
| `monsters[76].form` | `SPIDER` |
| `monsters[76].experience` | 60 |
| `monsters[76].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[76].stats.STOCK_ATTACK_SPEED` | 1.4 |
| `monsters[76].stats.STOCK_HEALTH` | 21.457 |
| `monsters[76].stats.STOCK_ATTACK_PHYSICAL` | 1.675 |
| `monsters[76].stats.STOCK_ATTACK_LIGHTNING` | 2.142 |
| `monsters[76].stats.STOCK_RESIST_LIGHTNING` | 40 |
| `monsters[76].stats.STOCK_CRITICAL_CHANCE` | 12 |
| `monsters[76].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[76].stats.STOCK_MANA` | 30 |
| `monsters[76].boss` | Да |
| `monsters[76].skills` | [Разряд](Monster-skills.md#mob_zap) |
| `monsters[76].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_STORMBORN](Modifiers-MONSTER.md#mob_stormborn) |
| `monsters[76].tables` | [boss:BOSS_GEODE_SPINNER](Tables-TEMPLATE.md#boss-boss_geode_spinner) |
| `monsters[77].code` | [Лич Инея](Bosses.md#boss_rime_lich) |
| `monsters[77].form` | `UNDEAD` |
| `monsters[77].experience` | 60 |
| `monsters[77].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[77].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[77].stats.STOCK_HEALTH` | 3.579 |
| `monsters[77].stats.STOCK_ATTACK_PHYSICAL` | 0.333 |
| `monsters[77].stats.STOCK_ATTACK_COLD` | 0.222 |
| `monsters[77].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[77].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[77].stats.STOCK_ATTACK_MAGICAL` | 0.262 |
| `monsters[77].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[77].stats.STOCK_MANA` | 55 |
| `monsters[77].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[77].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[77].boss` | Да |
| `monsters[77].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[77].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_FROSTBORN](Modifiers-MONSTER.md#mob_frostborn) |
| `monsters[77].tables` | [boss:BOSS_RIME_LICH](Tables-TEMPLATE.md#boss-boss_rime_lich) |
| `monsters[78].code` | [Старейший Тролль](Bosses.md#boss_elder_troll) |
| `monsters[78].form` | `BRUTE` |
| `monsters[78].experience` | 60 |
| `monsters[78].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[78].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[78].stats.STOCK_HEALTH` | 18.576 |
| `monsters[78].stats.STOCK_ATTACK_PHYSICAL` | 1.466 |
| `monsters[78].stats.STOCK_ATTACK_COLD` | 0.661 |
| `monsters[78].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[78].stats.STOCK_HEALTH_REGEN` | 0.45 |
| `monsters[78].stats.STOCK_STUN_THRESHOLD` | 15 |
| `monsters[78].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[78].stats.STOCK_MANA` | 30 |
| `monsters[78].boss` | Да |
| `monsters[78].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[78].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_REGENERATING](Modifiers-MONSTER.md#mob_regenerating) |
| `monsters[78].tables` | [boss:BOSS_ELDER_TROLL](Tables-TEMPLATE.md#boss-boss_elder_troll) |
| `monsters[78].trait` | `TRAIT_CRUSHER` |
| `monsters[79].code` | [Мать Гончих](Bosses.md#boss_hound_mother) |
| `monsters[79].form` | `BEAST` |
| `monsters[79].experience` | 60 |
| `monsters[79].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[79].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[79].stats.STOCK_HEALTH` | 11.853 |
| `monsters[79].stats.STOCK_ATTACK_PHYSICAL` | 1.435 |
| `monsters[79].stats.STOCK_ATTACK_FIRE` | 0.677 |
| `monsters[79].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[79].stats.STOCK_EVASION` | 70 |
| `monsters[79].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[79].stats.STOCK_MOVEMENT_SPEED` | 15 |
| `monsters[79].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[79].stats.STOCK_MANA` | 30 |
| `monsters[79].boss` | Да |
| `monsters[79].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[79].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_FLAMING](Modifiers-MONSTER.md#mob_flaming) |
| `monsters[80].code` | [Громовержец](Bosses.md#boss_thunder_caller) |
| `monsters[80].form` | `WRAITH` |
| `monsters[80].experience` | 60 |
| `monsters[80].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[80].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[80].stats.STOCK_HEALTH` | 28.745 |
| `monsters[80].stats.STOCK_ENERGY_SHIELD` | 17.16 |
| `monsters[80].stats.STOCK_ATTACK_LIGHTNING` | 3.563 |
| `monsters[80].stats.STOCK_RESIST_LIGHTNING` | 70 |
| `monsters[80].stats.STOCK_ATTACK_MAGICAL` | 2.788 |
| `monsters[80].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[80].stats.STOCK_MANA` | 55 |
| `monsters[80].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[80].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[80].boss` | Да |
| `monsters[80].skills` | [Разряд](Monster-skills.md#mob_zap); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[80].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_STORMBORN](Modifiers-MONSTER.md#mob_stormborn) |
| `monsters[80].tables` | [boss:BOSS_THUNDER_CALLER](Tables-TEMPLATE.md#boss-boss_thunder_caller) |
| `monsters[80].trait` | `TRAIT_GROUNDED` |
| `monsters[81].code` | [Жрица Глубин](Bosses.md#boss_deep_priestess) |
| `monsters[81].form` | `HUMANOID` |
| `monsters[81].experience` | 60 |
| `monsters[81].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[81].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[81].stats.STOCK_HEALTH` | 46.646 |
| `monsters[81].stats.STOCK_ATTACK_PHYSICAL` | 4.77 |
| `monsters[81].stats.STOCK_ATTACK_COLD` | 2.848 |
| `monsters[81].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[81].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[81].stats.STOCK_ATTACK_MAGICAL` | 3.369 |
| `monsters[81].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[81].stats.STOCK_MANA` | 55 |
| `monsters[81].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[81].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[81].boss` | Да |
| `monsters[81].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[81].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_WARDED](Modifiers-MONSTER.md#mob_warded) |
| `monsters[82].code` | [Обсидиановый Колосс](Bosses.md#boss_obsidian_colossus) |
| `monsters[82].form` | `GOLEM` |
| `monsters[82].experience` | 60 |
| `monsters[82].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[82].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[82].stats.STOCK_HEALTH` | 26.593 |
| `monsters[82].stats.STOCK_ATTACK_PHYSICAL` | 2.17 |
| `monsters[82].stats.STOCK_ATTACK_FIRE` | 0.981 |
| `monsters[82].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[82].stats.STOCK_ARMOR` | 90 |
| `monsters[82].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[82].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[82].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[82].stats.STOCK_MANA` | 30 |
| `monsters[82].boss` | Да |
| `monsters[82].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[82].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_FIREPROOF](Modifiers-MONSTER.md#mob_fireproof) |
| `monsters[82].trait` | `TRAIT_UNSHAKEN` |
| `monsters[83].code` | [Шлаковый Червь](Bosses.md#boss_slag_worm) |
| `monsters[83].form` | `SLUG` |
| `monsters[83].experience` | 60 |
| `monsters[83].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[83].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[83].stats.STOCK_HEALTH` | 89.558 |
| `monsters[83].stats.STOCK_ATTACK_FIRE` | 6.592 |
| `monsters[83].stats.STOCK_RESIST_FIRE` | 70 |
| `monsters[83].stats.STOCK_HEALTH_REGEN` | 2.153 |
| `monsters[83].stats.STOCK_ARMOR` | 40 |
| `monsters[83].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[83].stats.STOCK_MANA` | 30 |
| `monsters[83].boss` | Да |
| `monsters[83].skills` | [Огненный шар](Monster-skills.md#mob_fireball) |
| `monsters[83].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_BURNING](Modifiers-MONSTER.md#mob_burning) |
| `monsters[84].code` | [Хладоклык](Bosses.md#boss_frostfang) |
| `monsters[84].form` | `SERPENT` |
| `monsters[84].experience` | 60 |
| `monsters[84].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[84].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[84].stats.STOCK_HEALTH` | 24.411 |
| `monsters[84].stats.STOCK_ATTACK_PHYSICAL` | 1.824 |
| `monsters[84].stats.STOCK_ATTACK_COLD` | 1.587 |
| `monsters[84].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[84].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[84].stats.STOCK_MANA` | 30 |
| `monsters[84].boss` | Да |
| `monsters[84].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[84].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_FREEZING](Modifiers-MONSTER.md#mob_freezing) |
| `monsters[84].tables` | [boss:BOSS_FROSTFANG](Tables-TEMPLATE.md#boss-boss_frostfang) |
| `monsters[85].code` | [Глашатай Бездны](Bosses.md#boss_abyss_herald) |
| `monsters[85].form` | `WRAITH` |
| `monsters[85].experience` | 60 |
| `monsters[85].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[85].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[85].stats.STOCK_HEALTH` | 6.278 |
| `monsters[85].stats.STOCK_ENERGY_SHIELD` | 3.769 |
| `monsters[85].stats.STOCK_ATTACK_CHAOS` | 0.769 |
| `monsters[85].stats.STOCK_RESIST_CHAOS` | 70 |
| `monsters[85].stats.STOCK_ATTACK_MAGICAL` | 0.598 |
| `monsters[85].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[85].stats.STOCK_MANA` | 55 |
| `monsters[85].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[85].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[85].boss` | Да |
| `monsters[85].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[85].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_WITHERING](Modifiers-MONSTER.md#mob_withering) |
| `monsters[85].tables` | [boss:BOSS_ABYSS_HERALD](Tables-TEMPLATE.md#boss-boss_abyss_herald) |
| `monsters[86].code` | [Кастелян Пепла](Bosses.md#boss_ash_castellan) |
| `monsters[86].form` | `UNDEAD` |
| `monsters[86].experience` | 60 |
| `monsters[86].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[86].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[86].stats.STOCK_HEALTH` | 3.556 |
| `monsters[86].stats.STOCK_ATTACK_PHYSICAL` | 0.326 |
| `monsters[86].stats.STOCK_ATTACK_FIRE` | 0.219 |
| `monsters[86].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[86].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[86].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[86].stats.STOCK_MANA` | 30 |
| `monsters[86].boss` | Да |
| `monsters[86].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[86].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_BURNING](Modifiers-MONSTER.md#mob_burning) |
| `monsters[87].code` | [Солевой скиталец](Bosses.md#boss_salt_walker) |
| `monsters[87].form` | `UNDEAD` |
| `monsters[87].experience` | 60 |
| `monsters[87].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[87].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[87].stats.STOCK_HEALTH` | 45.289 |
| `monsters[87].stats.STOCK_ATTACK_PHYSICAL` | 4.12 |
| `monsters[87].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[87].stats.STOCK_ARMOR` | 40 |
| `monsters[87].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[87].stats.STOCK_MANA` | 30 |
| `monsters[87].boss` | Да |
| `monsters[87].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[87].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_STURDY](Modifiers-MONSTER.md#mob_sturdy) |
| `monsters[88].code` | [Мясник караванов](Bosses.md#boss_caravan_butcher) |
| `monsters[88].form` | `HUMANOID` |
| `monsters[88].experience` | 60 |
| `monsters[88].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[88].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[88].stats.STOCK_HEALTH` | 24.345 |
| `monsters[88].stats.STOCK_ATTACK_PHYSICAL` | 2.456 |
| `monsters[88].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[88].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[88].stats.STOCK_MANA` | 30 |
| `monsters[88].boss` | Да |
| `monsters[88].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[88].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_VICIOUS](Modifiers-MONSTER.md#mob_vicious) |
| `monsters[88].tables` | [boss:BOSS_CARAVAN_BUTCHER](Tables-TEMPLATE.md#boss-boss_caravan_butcher) |
| `monsters[89].code` | [Тиран ущелья](Bosses.md#boss_gorge_tyrant) |
| `monsters[89].form` | `BRUTE` |
| `monsters[89].experience` | 60 |
| `monsters[89].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[89].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[89].stats.STOCK_HEALTH` | 46.557 |
| `monsters[89].stats.STOCK_ATTACK_PHYSICAL` | 3.538 |
| `monsters[89].stats.STOCK_HEALTH_REGEN` | 0.954 |
| `monsters[89].stats.STOCK_STUN_THRESHOLD` | 15 |
| `monsters[89].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[89].stats.STOCK_MANA` | 30 |
| `monsters[89].boss` | Да |
| `monsters[89].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[89].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_CRUSHING](Modifiers-MONSTER.md#mob_crushing) |
| `monsters[89].trait` | `TRAIT_CRUSHER` |
| `monsters[90].code` | [Сирена барханов](Bosses.md#boss_dune_siren) |
| `monsters[90].form` | `WRAITH` |
| `monsters[90].experience` | 60 |
| `monsters[90].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[90].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[90].stats.STOCK_HEALTH` | 22.041 |
| `monsters[90].stats.STOCK_ENERGY_SHIELD` | 13.166 |
| `monsters[90].stats.STOCK_ATTACK_LIGHTNING` | 2.66 |
| `monsters[90].stats.STOCK_RESIST_LIGHTNING` | 70 |
| `monsters[90].stats.STOCK_ATTACK_MAGICAL` | 2.06 |
| `monsters[90].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[90].stats.STOCK_MANA` | 60 |
| `monsters[90].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[90].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[90].boss` | Да |
| `monsters[90].skills` | [Разряд](Monster-skills.md#mob_zap) |
| `monsters[90].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_SHOCKING](Modifiers-MONSTER.md#mob_shocking) |
| `monsters[90].tables` | [boss:BOSS_DUNE_SIREN](Tables-TEMPLATE.md#boss-boss_dune_siren) |
| `monsters[91].code` | [Песчаный магистрат](Bosses.md#boss_sand_magistrate) |
| `monsters[91].form` | `UNDEAD` |
| `monsters[91].experience` | 60 |
| `monsters[91].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[91].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[91].stats.STOCK_HEALTH` | 30.001 |
| `monsters[91].stats.STOCK_ATTACK_PHYSICAL` | 2.732 |
| `monsters[91].stats.STOCK_ATTACK_FIRE` | 1.822 |
| `monsters[91].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[91].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[91].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[91].stats.STOCK_MANA` | 30 |
| `monsters[91].boss` | Да |
| `monsters[91].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[91].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_STALWART](Modifiers-MONSTER.md#mob_stalwart) |
| `monsters[91].tables` | [boss:BOSS_SAND_MAGISTRATE](Tables-TEMPLATE.md#boss-boss_sand_magistrate) |
| `monsters[92].code` | [Мать падали](Bosses.md#boss_carrion_mother) |
| `monsters[92].form` | `BAT` |
| `monsters[92].experience` | 60 |
| `monsters[92].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[92].stats.STOCK_ATTACK_SPEED` | 1.8 |
| `monsters[92].stats.STOCK_HEALTH` | 30.744 |
| `monsters[92].stats.STOCK_ATTACK_PHYSICAL` | 3.262 |
| `monsters[92].stats.STOCK_EVASION` | 80 |
| `monsters[92].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[92].stats.STOCK_MANA` | 30 |
| `monsters[92].boss` | Да |
| `monsters[92].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[92].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_HUNGERING](Modifiers-MONSTER.md#mob_hungering) |
| `monsters[92].tables` | [boss:BOSS_CARRION_MOTHER](Tables-TEMPLATE.md#boss-boss_carrion_mother) |
| `monsters[93].code` | [Змей оазиса](Bosses.md#boss_oasis_serpent) |
| `monsters[93].form` | `SERPENT` |
| `monsters[93].experience` | 60 |
| `monsters[93].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[93].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[93].stats.STOCK_HEALTH` | 36.03 |
| `monsters[93].stats.STOCK_ATTACK_PHYSICAL` | 2.613 |
| `monsters[93].stats.STOCK_ATTACK_CHAOS` | 2.295 |
| `monsters[93].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[93].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[93].stats.STOCK_MANA` | 30 |
| `monsters[93].boss` | Да |
| `monsters[93].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[93].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_VENOMOUS](Modifiers-MONSTER.md#mob_venomous) |
| `monsters[94].code` | [Стеклянный колосс](Bosses.md#boss_glass_colossus) |
| `monsters[94].form` | `GOLEM` |
| `monsters[94].experience` | 60 |
| `monsters[94].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[94].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[94].stats.STOCK_HEALTH` | 5.672 |
| `monsters[94].stats.STOCK_ATTACK_PHYSICAL` | 0.446 |
| `monsters[94].stats.STOCK_ATTACK_FIRE` | 0.201 |
| `monsters[94].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[94].stats.STOCK_ARMOR` | 90 |
| `monsters[94].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[94].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[94].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[94].stats.STOCK_MANA` | 30 |
| `monsters[94].boss` | Да |
| `monsters[94].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[94].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_MIRRORED](Modifiers-MONSTER.md#mob_mirrored) |
| `monsters[94].trait` | `TRAIT_UNSHAKEN` |
| `monsters[95].code` | [Хан налётчиков](Bosses.md#boss_raider_khan) |
| `monsters[95].form` | `HUMANOID` |
| `monsters[95].experience` | 60 |
| `monsters[95].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[95].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[95].stats.STOCK_HEALTH` | 44.623 |
| `monsters[95].stats.STOCK_ATTACK_PHYSICAL` | 4.462 |
| `monsters[95].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[95].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[95].stats.STOCK_MANA` | 30 |
| `monsters[95].boss` | Да |
| `monsters[95].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[95].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_MERCILESS](Modifiers-MONSTER.md#mob_merciless) |
| `monsters[95].tables` | [boss:BOSS_RAIDER_KHAN](Tables-TEMPLATE.md#boss-boss_raider_khan) |
| `monsters[96].code` | [Скорпионья матриарх](Bosses.md#boss_scorpion_matriarch) |
| `monsters[96].form` | `SCORPION` |
| `monsters[96].experience` | 60 |
| `monsters[96].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[96].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[96].stats.STOCK_HEALTH` | 35.29 |
| `monsters[96].stats.STOCK_ATTACK_PHYSICAL` | 2.915 |
| `monsters[96].stats.STOCK_ATTACK_CHAOS` | 1.853 |
| `monsters[96].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[96].stats.STOCK_ARMOR` | 70 |
| `monsters[96].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[96].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[96].stats.STOCK_MANA` | 30 |
| `monsters[96].boss` | Да |
| `monsters[96].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[96].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_TOXIC](Modifiers-MONSTER.md#mob_toxic) |
| `monsters[96].tables` | [boss:BOSS_SCORPION_MATRIARCH](Tables-TEMPLATE.md#boss-boss_scorpion_matriarch) |
| `monsters[97].code` | [Ткач миражей](Bosses.md#boss_mirage_weaver) |
| `monsters[97].form` | `WRAITH` |
| `monsters[97].experience` | 60 |
| `monsters[97].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[97].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[97].stats.STOCK_HEALTH` | 31.658 |
| `monsters[97].stats.STOCK_ENERGY_SHIELD` | 18.998 |
| `monsters[97].stats.STOCK_ATTACK_FIRE` | 3.76 |
| `monsters[97].stats.STOCK_RESIST_FIRE` | 70 |
| `monsters[97].stats.STOCK_ATTACK_MAGICAL` | 2.924 |
| `monsters[97].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[97].stats.STOCK_MANA` | 60 |
| `monsters[97].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[97].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[97].boss` | Да |
| `monsters[97].skills` | [Огненный шар](Monster-skills.md#mob_fireball); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[97].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_EVASIVE](Modifiers-MONSTER.md#mob_evasive) |
| `monsters[97].tables` | [boss:BOSS_MIRAGE_WEAVER](Tables-TEMPLATE.md#boss-boss_mirage_weaver) |
| `monsters[98].code` | [Царь-мумия](Bosses.md#boss_mummy_king) |
| `monsters[98].form` | `UNDEAD` |
| `monsters[98].experience` | 60 |
| `monsters[98].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[98].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[98].stats.STOCK_HEALTH` | 2.155 |
| `monsters[98].stats.STOCK_ATTACK_PHYSICAL` | 0.193 |
| `monsters[98].stats.STOCK_ATTACK_CHAOS` | 0.131 |
| `monsters[98].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[98].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[98].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[98].stats.STOCK_MANA` | 30 |
| `monsters[98].boss` | Да |
| `monsters[98].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[98].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_UNDYING](Modifiers-MONSTER.md#mob_undying) |
| `monsters[98].tables` | [boss:BOSS_MUMMY_KING](Tables-TEMPLATE.md#boss-boss_mummy_king) |
| `monsters[99].code` | [Страж арки](Bosses.md#boss_arch_sentinel) |
| `monsters[99].form` | `GOLEM` |
| `monsters[99].experience` | 60 |
| `monsters[99].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[99].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[99].stats.STOCK_HEALTH` | 14.497 |
| `monsters[99].stats.STOCK_ATTACK_PHYSICAL` | 1.135 |
| `monsters[99].stats.STOCK_ARMOR` | 90 |
| `monsters[99].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[99].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[99].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[99].stats.STOCK_MANA` | 30 |
| `monsters[99].boss` | Да |
| `monsters[99].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[99].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_STEADFAST](Modifiers-MONSTER.md#mob_steadfast) |
| `monsters[100].code` | [Обитатель русла](Bosses.md#boss_riverbed_lurker) |
| `monsters[100].form` | `SCORPION` |
| `monsters[100].experience` | 60 |
| `monsters[100].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[100].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[100].stats.STOCK_HEALTH` | 28.913 |
| `monsters[100].stats.STOCK_ATTACK_PHYSICAL` | 2.39 |
| `monsters[100].stats.STOCK_ARMOR` | 70 |
| `monsters[100].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[100].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[100].stats.STOCK_MANA` | 30 |
| `monsters[100].boss` | Да |
| `monsters[100].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[100].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_PIERCING](Modifiers-MONSTER.md#mob_piercing) |
| `monsters[101].code` | [Верховный бальзамировщик](Bosses.md#boss_high_embalmer) |
| `monsters[101].form` | `HUMANOID` |
| `monsters[101].experience` | 60 |
| `monsters[101].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[101].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[101].stats.STOCK_HEALTH` | 28.511 |
| `monsters[101].stats.STOCK_ATTACK_PHYSICAL` | 2.854 |
| `monsters[101].stats.STOCK_ATTACK_CHAOS` | 1.715 |
| `monsters[101].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[101].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[101].stats.STOCK_ATTACK_MAGICAL` | 1.981 |
| `monsters[101].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[101].stats.STOCK_MANA` | 60 |
| `monsters[101].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[101].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[101].boss` | Да |
| `monsters[101].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[101].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_WITHERING](Modifiers-MONSTER.md#mob_withering) |
| `monsters[101].tables` | [boss:BOSS_HIGH_EMBALMER](Tables-TEMPLATE.md#boss-boss_high_embalmer) |
| `monsters[102].code` | [Жрец Солнца](Bosses.md#boss_sun_priest) |
| `monsters[102].form` | `HUMANOID` |
| `monsters[102].experience` | 60 |
| `monsters[102].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[102].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[102].stats.STOCK_HEALTH` | 27.015 |
| `monsters[102].stats.STOCK_ATTACK_PHYSICAL` | 2.702 |
| `monsters[102].stats.STOCK_ATTACK_FIRE` | 1.623 |
| `monsters[102].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[102].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[102].stats.STOCK_ATTACK_MAGICAL` | 1.878 |
| `monsters[102].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[102].stats.STOCK_MANA` | 60 |
| `monsters[102].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[102].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[102].boss` | Да |
| `monsters[102].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[102].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_FLAMING](Modifiers-MONSTER.md#mob_flaming) |
| `monsters[102].tables` | [boss:BOSS_SUN_PRIEST](Tables-TEMPLATE.md#boss-boss_sun_priest) |
| `monsters[103].code` | [Грозовой Рух](Bosses.md#boss_storm_roc) |
| `monsters[103].form` | `BAT` |
| `monsters[103].experience` | 60 |
| `monsters[103].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[103].stats.STOCK_ATTACK_SPEED` | 1.8 |
| `monsters[103].stats.STOCK_HEALTH` | 14.916 |
| `monsters[103].stats.STOCK_ATTACK_PHYSICAL` | 1.557 |
| `monsters[103].stats.STOCK_ATTACK_LIGHTNING` | 0.927 |
| `monsters[103].stats.STOCK_RESIST_LIGHTNING` | 40 |
| `monsters[103].stats.STOCK_EVASION` | 80 |
| `monsters[103].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[103].stats.STOCK_MANA` | 30 |
| `monsters[103].boss` | Да |
| `monsters[103].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[103].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_STORMBORN](Modifiers-MONSTER.md#mob_stormborn) |
| `monsters[103].tables` | [boss:BOSS_STORM_ROC](Tables-TEMPLATE.md#boss-boss_storm_roc) |
| `monsters[104].code` | [Солнечный царь](Bosses.md#boss_sun_king) |
| `monsters[104].form` | `UNDEAD` |
| `monsters[104].experience` | 60 |
| `monsters[104].loot` | [loot:BOSS_4](Tables-LOOT.md#loot-boss_4) |
| `monsters[104].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[104].stats.STOCK_HEALTH` | 3.618 |
| `monsters[104].stats.STOCK_ATTACK_PHYSICAL` | 0.257 |
| `monsters[104].stats.STOCK_ATTACK_FIRE` | 0.172 |
| `monsters[104].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[104].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[104].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[104].stats.STOCK_RESIST_ALL` | 15 |
| `monsters[104].stats.STOCK_MANA` | 30 |
| `monsters[104].boss` | Да |
| `monsters[104].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[104].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_BURNING](Modifiers-MONSTER.md#mob_burning) |
| `monsters[104].tables` | [boss:BOSS_SUN_KING](Tables-TEMPLATE.md#boss-boss_sun_king) |
| `monsters[105].code` | [Лесной ловчий](Bosses.md#boss_jungle_stalker) |
| `monsters[105].form` | `BEAST` |
| `monsters[105].experience` | 60 |
| `monsters[105].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[105].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[105].stats.STOCK_HEALTH` | 18.274 |
| `monsters[105].stats.STOCK_ATTACK_PHYSICAL` | 2.101 |
| `monsters[105].stats.STOCK_EVASION` | 70 |
| `monsters[105].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[105].stats.STOCK_MOVEMENT_SPEED` | 15 |
| `monsters[105].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[105].stats.STOCK_MANA` | 30 |
| `monsters[105].boss` | Да |
| `monsters[105].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[105].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_SWIFT](Modifiers-MONSTER.md#mob_swift) |
| `monsters[106].code` | [Мангровая Пасть](Bosses.md#boss_mangrove_maw) |
| `monsters[106].form` | `SERPENT` |
| `monsters[106].experience` | 60 |
| `monsters[106].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[106].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[106].stats.STOCK_HEALTH` | 44.048 |
| `monsters[106].stats.STOCK_ATTACK_PHYSICAL` | 3.158 |
| `monsters[106].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[106].stats.STOCK_MANA` | 30 |
| `monsters[106].boss` | Да |
| `monsters[106].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[106].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_GORING](Modifiers-MONSTER.md#mob_goring) |
| `monsters[107].code` | [Замшелый легионер](Bosses.md#boss_moss_legionary) |
| `monsters[107].form` | `UNDEAD` |
| `monsters[107].experience` | 60 |
| `monsters[107].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[107].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[107].stats.STOCK_HEALTH` | 30.377 |
| `monsters[107].stats.STOCK_ATTACK_PHYSICAL` | 2.714 |
| `monsters[107].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[107].stats.STOCK_ARMOR` | 40 |
| `monsters[107].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[107].stats.STOCK_MANA` | 30 |
| `monsters[107].boss` | Да |
| `monsters[107].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[107].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_ARMOURED](Modifiers-MONSTER.md#mob_armoured) |
| `monsters[108].code` | [Орхидейная дриада](Bosses.md#boss_orchid_dryad) |
| `monsters[108].form` | `HUMANOID` |
| `monsters[108].experience` | 60 |
| `monsters[108].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[108].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[108].stats.STOCK_HEALTH` | 37.345 |
| `monsters[108].stats.STOCK_ATTACK_PHYSICAL` | 3.669 |
| `monsters[108].stats.STOCK_ATTACK_CHAOS` | 2.194 |
| `monsters[108].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[108].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[108].stats.STOCK_ATTACK_MAGICAL` | 2.587 |
| `monsters[108].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[108].stats.STOCK_MANA` | 60 |
| `monsters[108].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[108].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[108].boss` | Да |
| `monsters[108].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[108].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_LINGERING](Modifiers-MONSTER.md#mob_lingering) |
| `monsters[108].tables` | [boss:BOSS_ORCHID_DRYAD](Tables-TEMPLATE.md#boss-boss_orchid_dryad) |
| `monsters[109].code` | [Теневая пантера](Bosses.md#boss_shadow_panther) |
| `monsters[109].form` | `BEAST` |
| `monsters[109].experience` | 60 |
| `monsters[109].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[109].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[109].stats.STOCK_HEALTH` | 11.78 |
| `monsters[109].stats.STOCK_ATTACK_PHYSICAL` | 1.353 |
| `monsters[109].stats.STOCK_ATTACK_CHAOS` | 0.644 |
| `monsters[109].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[109].stats.STOCK_EVASION` | 70 |
| `monsters[109].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[109].stats.STOCK_MOVEMENT_SPEED` | 15 |
| `monsters[109].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[109].stats.STOCK_MANA` | 30 |
| `monsters[109].boss` | Да |
| `monsters[109].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[109].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_EVASIVE](Modifiers-MONSTER.md#mob_evasive) |
| `monsters[109].tables` | [boss:BOSS_SHADOW_PANTHER](Tables-TEMPLATE.md#boss-boss_shadow_panther) |
| `monsters[110].code` | [Осиная матрона](Bosses.md#boss_wasp_matron) |
| `monsters[110].form` | `INSECT` |
| `monsters[110].experience` | 60 |
| `monsters[110].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[110].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[110].stats.STOCK_HEALTH` | 15.755 |
| `monsters[110].stats.STOCK_ATTACK_PHYSICAL` | 1.641 |
| `monsters[110].stats.STOCK_ATTACK_CHAOS` | 1.09 |
| `monsters[110].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[110].stats.STOCK_EVASION` | 75 |
| `monsters[110].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[110].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[110].stats.STOCK_MANA` | 30 |
| `monsters[110].boss` | Да |
| `monsters[110].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[110].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_VENOMOUS](Modifiers-MONSTER.md#mob_venomous) |
| `monsters[110].tables` | [boss:BOSS_WASP_MATRON](Tables-TEMPLATE.md#boss-boss_wasp_matron) |
| `monsters[111].code` | [Лиана-душитель](Bosses.md#boss_strangler_vine) |
| `monsters[111].form` | `FUNGUS` |
| `monsters[111].experience` | 60 |
| `monsters[111].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[111].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[111].stats.STOCK_HEALTH` | 38.287 |
| `monsters[111].stats.STOCK_ATTACK_PHYSICAL` | 2.599 |
| `monsters[111].stats.STOCK_HEALTH_REGEN` | 1.026 |
| `monsters[111].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[111].stats.STOCK_MANA` | 30 |
| `monsters[111].boss` | Да |
| `monsters[111].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[111].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_SLOWING](Modifiers-MONSTER.md#mob_slowing) |
| `monsters[112].code` | [Змеиный оракул](Bosses.md#boss_serpent_oracle) |
| `monsters[112].form` | `SERPENT` |
| `monsters[112].experience` | 60 |
| `monsters[112].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[112].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[112].stats.STOCK_HEALTH` | 45.473 |
| `monsters[112].stats.STOCK_ATTACK_PHYSICAL` | 3.23 |
| `monsters[112].stats.STOCK_ATTACK_CHAOS` | 2.845 |
| `monsters[112].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[112].stats.STOCK_ATTACK_MAGICAL` | 2.845 |
| `monsters[112].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[112].stats.STOCK_MANA` | 60 |
| `monsters[112].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[112].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[112].boss` | Да |
| `monsters[112].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[112].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_TOXIC](Modifiers-MONSTER.md#mob_toxic) |
| `monsters[112].tables` | [boss:BOSS_SERPENT_ORACLE](Tables-TEMPLATE.md#boss-boss_serpent_oracle) |
| `monsters[113].code` | [Янтарный страж](Bosses.md#boss_amber_warden) |
| `monsters[113].form` | `INSECT` |
| `monsters[113].experience` | 60 |
| `monsters[113].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[113].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[113].stats.STOCK_HEALTH` | 18.91 |
| `monsters[113].stats.STOCK_ATTACK_PHYSICAL` | 1.965 |
| `monsters[113].stats.STOCK_ATTACK_LIGHTNING` | 1.303 |
| `monsters[113].stats.STOCK_RESIST_LIGHTNING` | 40 |
| `monsters[113].stats.STOCK_EVASION` | 75 |
| `monsters[113].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[113].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[113].stats.STOCK_MANA` | 30 |
| `monsters[113].boss` | Да |
| `monsters[113].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[113].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_STURDY](Modifiers-MONSTER.md#mob_sturdy) |
| `monsters[114].code` | [Ужас полога](Bosses.md#boss_canopy_horror) |
| `monsters[114].form` | `SPIDER` |
| `monsters[114].experience` | 60 |
| `monsters[114].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[114].stats.STOCK_ATTACK_SPEED` | 1.4 |
| `monsters[114].stats.STOCK_HEALTH` | 17.079 |
| `monsters[114].stats.STOCK_ATTACK_PHYSICAL` | 1.233 |
| `monsters[114].stats.STOCK_ATTACK_CHAOS` | 1.587 |
| `monsters[114].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[114].stats.STOCK_CRITICAL_CHANCE` | 12 |
| `monsters[114].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[114].stats.STOCK_MANA` | 30 |
| `monsters[114].boss` | Да |
| `monsters[114].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[114].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_BLIGHTED](Modifiers-MONSTER.md#mob_blighted) |
| `monsters[114].tables` | [boss:BOSS_CANOPY_HORROR](Tables-TEMPLATE.md#boss-boss_canopy_horror) |
| `monsters[115].code` | [Споровый владыка](Bosses.md#boss_spore_lord) |
| `monsters[115].form` | `FUNGUS` |
| `monsters[115].experience` | 60 |
| `monsters[115].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[115].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[115].stats.STOCK_HEALTH` | 17.614 |
| `monsters[115].stats.STOCK_ATTACK_CHAOS` | 1.328 |
| `monsters[115].stats.STOCK_RESIST_CHAOS` | 70 |
| `monsters[115].stats.STOCK_HEALTH_REGEN` | 0.46 |
| `monsters[115].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[115].stats.STOCK_MANA` | 30 |
| `monsters[115].boss` | Да |
| `monsters[115].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit) |
| `monsters[115].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_REGENERATING](Modifiers-MONSTER.md#mob_regenerating) |
| `monsters[115].tables` | [boss:BOSS_SPORE_LORD](Tables-TEMPLATE.md#boss-boss_spore_lord) |
| `monsters[116].code` | [Обезумевший первопроходец](Bosses.md#boss_mad_explorer) |
| `monsters[116].form` | `HUMANOID` |
| `monsters[116].experience` | 60 |
| `monsters[116].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[116].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[116].stats.STOCK_HEALTH` | 23.15 |
| `monsters[116].stats.STOCK_ATTACK_PHYSICAL` | 2.274 |
| `monsters[116].stats.STOCK_ATTACK_FIRE` | 1.349 |
| `monsters[116].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[116].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[116].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[116].stats.STOCK_MANA` | 30 |
| `monsters[116].boss` | Да |
| `monsters[116].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[116].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_BERSERK](Modifiers-MONSTER.md#mob_berserk) |
| `monsters[117].code` | [Кормилица выводка](Bosses.md#boss_brood_nurse) |
| `monsters[117].form` | `SLUG` |
| `monsters[117].experience` | 60 |
| `monsters[117].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[117].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[117].stats.STOCK_HEALTH` | 2.378 |
| `monsters[117].stats.STOCK_ATTACK_CHAOS` | 0.167 |
| `monsters[117].stats.STOCK_RESIST_CHAOS` | 70 |
| `monsters[117].stats.STOCK_HEALTH_REGEN` | 0.042 |
| `monsters[117].stats.STOCK_ARMOR` | 40 |
| `monsters[117].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[117].stats.STOCK_MANA` | 30 |
| `monsters[117].boss` | Да |
| `monsters[117].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[117].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_HUNGERING](Modifiers-MONSTER.md#mob_hungering) |
| `monsters[118].code` | [Гидра чащи](Bosses.md#boss_thicket_hydra) |
| `monsters[118].form` | `SERPENT` |
| `monsters[118].experience` | 60 |
| `monsters[118].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[118].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[118].stats.STOCK_HEALTH` | 29.185 |
| `monsters[118].stats.STOCK_ATTACK_PHYSICAL` | 2.064 |
| `monsters[118].stats.STOCK_ATTACK_COLD` | 1.821 |
| `monsters[118].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[118].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[118].stats.STOCK_MANA` | 30 |
| `monsters[118].boss` | Да |
| `monsters[118].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[118].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_CHILLING](Modifiers-MONSTER.md#mob_chilling) |
| `monsters[119].code` | [Хранитель Древа](Bosses.md#boss_tree_warden) |
| `monsters[119].form` | `GOLEM` |
| `monsters[119].experience` | 60 |
| `monsters[119].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[119].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[119].stats.STOCK_HEALTH` | 18.817 |
| `monsters[119].stats.STOCK_ATTACK_PHYSICAL` | 1.437 |
| `monsters[119].stats.STOCK_ARMOR` | 90 |
| `monsters[119].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[119].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[119].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[119].stats.STOCK_MANA` | 30 |
| `monsters[119].boss` | Да |
| `monsters[119].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[119].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_THORNY](Modifiers-MONSTER.md#mob_thorny) |
| `monsters[119].tables` | [boss:BOSS_TREE_WARDEN](Tables-TEMPLATE.md#boss-boss_tree_warden) |
| `monsters[120].code` | [Принц богомолов](Bosses.md#boss_mantis_prince) |
| `monsters[120].form` | `INSECT` |
| `monsters[120].experience` | 60 |
| `monsters[120].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[120].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[120].stats.STOCK_HEALTH` | 12.999 |
| `monsters[120].stats.STOCK_ATTACK_PHYSICAL` | 1.338 |
| `monsters[120].stats.STOCK_EVASION` | 75 |
| `monsters[120].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[120].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[120].stats.STOCK_MANA` | 30 |
| `monsters[120].boss` | Да |
| `monsters[120].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[120].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_VICIOUS](Modifiers-MONSTER.md#mob_vicious) |
| `monsters[120].tables` | [boss:BOSS_MANTIS_PRINCE](Tables-TEMPLATE.md#boss-boss_mantis_prince) |
| `monsters[121].code` | [Страж царицы](Bosses.md#boss_royal_guardian) |
| `monsters[121].form` | `INSECT` |
| `monsters[121].experience` | 60 |
| `monsters[121].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[121].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[121].stats.STOCK_HEALTH` | 12.649 |
| `monsters[121].stats.STOCK_ATTACK_PHYSICAL` | 1.301 |
| `monsters[121].stats.STOCK_ATTACK_FIRE` | 0.864 |
| `monsters[121].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[121].stats.STOCK_EVASION` | 75 |
| `monsters[121].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[121].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[121].stats.STOCK_MANA` | 30 |
| `monsters[121].boss` | Да |
| `monsters[121].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[121].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_STALWART](Modifiers-MONSTER.md#mob_stalwart) |
| `monsters[122].code` | [Царица улья](Bosses.md#boss_hive_queen) |
| `monsters[122].form` | `INSECT` |
| `monsters[122].experience` | 60 |
| `monsters[122].loot` | [loot:BOSS_5](Tables-LOOT.md#loot-boss_5) |
| `monsters[122].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[122].stats.STOCK_HEALTH` | 4.467 |
| `monsters[122].stats.STOCK_ATTACK_PHYSICAL` | 0.368 |
| `monsters[122].stats.STOCK_ATTACK_CHAOS` | 0.244 |
| `monsters[122].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[122].stats.STOCK_EVASION` | 75 |
| `monsters[122].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[122].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[122].stats.STOCK_RESIST_ALL` | 15 |
| `monsters[122].stats.STOCK_MANA` | 30 |
| `monsters[122].boss` | Да |
| `monsters[122].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[122].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_VENOMOUS](Modifiers-MONSTER.md#mob_venomous) |
| `monsters[122].tables` | [boss:BOSS_HIVE_QUEEN](Tables-TEMPLATE.md#boss-boss_hive_queen) |
| `monsters[123].code` | [Пепельный бегемот](Bosses.md#boss_cinder_behemoth) |
| `monsters[123].form` | `BRUTE` |
| `monsters[123].experience` | 60 |
| `monsters[123].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[123].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[123].stats.STOCK_HEALTH` | 17.043 |
| `monsters[123].stats.STOCK_ATTACK_PHYSICAL` | 1.251 |
| `monsters[123].stats.STOCK_ATTACK_FIRE` | 0.568 |
| `monsters[123].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[123].stats.STOCK_HEALTH_REGEN` | 0.281 |
| `monsters[123].stats.STOCK_STUN_THRESHOLD` | 15 |
| `monsters[123].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[123].stats.STOCK_MANA` | 30 |
| `monsters[123].boss` | Да |
| `monsters[123].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[123].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_FLAMING](Modifiers-MONSTER.md#mob_flaming) |
| `monsters[123].trait` | `TRAIT_CRUSHER` |
| `monsters[124].code` | [Серный король](Bosses.md#boss_sulphur_king) |
| `monsters[124].form` | `GOLEM` |
| `monsters[124].experience` | 60 |
| `monsters[124].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[124].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[124].stats.STOCK_HEALTH` | 41.241 |
| `monsters[124].stats.STOCK_ATTACK_PHYSICAL` | 3.128 |
| `monsters[124].stats.STOCK_ATTACK_CHAOS` | 1.418 |
| `monsters[124].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[124].stats.STOCK_ARMOR` | 90 |
| `monsters[124].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[124].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[124].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[124].stats.STOCK_MANA` | 30 |
| `monsters[124].boss` | Да |
| `monsters[124].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[124].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_TOXIC](Modifiers-MONSTER.md#mob_toxic) |
| `monsters[124].tables` | [boss:BOSS_SULPHUR_KING](Tables-TEMPLATE.md#boss-boss_sulphur_king) |
| `monsters[125].code` | [Генерал-предатель](Bosses.md#boss_turncoat_general) |
| `monsters[125].form` | `HUMANOID` |
| `monsters[125].experience` | 60 |
| `monsters[125].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[125].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[125].stats.STOCK_HEALTH` | 27.839 |
| `monsters[125].stats.STOCK_ATTACK_PHYSICAL` | 2.696 |
| `monsters[125].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[125].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[125].stats.STOCK_MANA` | 30 |
| `monsters[125].boss` | Да |
| `monsters[125].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[125].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_STALWART](Modifiers-MONSTER.md#mob_stalwart) |
| `monsters[125].tables` | [boss:BOSS_TURNCOAT_GENERAL](Tables-TEMPLATE.md#boss-boss_turncoat_general) |
| `monsters[126].code` | [Лавовый змей](Bosses.md#boss_lava_wyrm) |
| `monsters[126].form` | `SERPENT` |
| `monsters[126].experience` | 60 |
| `monsters[126].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[126].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[126].stats.STOCK_HEALTH` | 32.938 |
| `monsters[126].stats.STOCK_ATTACK_PHYSICAL` | 2.325 |
| `monsters[126].stats.STOCK_ATTACK_FIRE` | 2.016 |
| `monsters[126].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[126].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[126].stats.STOCK_MANA` | 30 |
| `monsters[126].boss` | Да |
| `monsters[126].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[126].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_BURNING](Modifiers-MONSTER.md#mob_burning) |
| `monsters[126].tables` | [boss:BOSS_LAVA_WYRM](Tables-TEMPLATE.md#boss-boss_lava_wyrm) |
| `monsters[127].code` | [Обсидиановая гарпия](Bosses.md#boss_obsidian_harpy) |
| `monsters[127].form` | `BAT` |
| `monsters[127].experience` | 60 |
| `monsters[127].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[127].stats.STOCK_ATTACK_SPEED` | 1.8 |
| `monsters[127].stats.STOCK_HEALTH` | 15.125 |
| `monsters[127].stats.STOCK_ATTACK_PHYSICAL` | 1.556 |
| `monsters[127].stats.STOCK_EVASION` | 80 |
| `monsters[127].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[127].stats.STOCK_MANA` | 30 |
| `monsters[127].boss` | Да |
| `monsters[127].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[127].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_PIERCING](Modifiers-MONSTER.md#mob_piercing) |
| `monsters[128].code` | [Хозяин печей](Bosses.md#boss_furnace_master) |
| `monsters[128].form` | `DEMON` |
| `monsters[128].experience` | 60 |
| `monsters[128].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[128].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[128].stats.STOCK_HEALTH` | 13.889 |
| `monsters[128].stats.STOCK_ATTACK_PHYSICAL` | 1.222 |
| `monsters[128].stats.STOCK_ATTACK_FIRE` | 0.85 |
| `monsters[128].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[128].stats.STOCK_ARMOR` | 50 |
| `monsters[128].stats.STOCK_HEALTH_REGEN` | 0.202 |
| `monsters[128].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[128].stats.STOCK_MANA` | 30 |
| `monsters[128].boss` | Да |
| `monsters[128].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[128].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_FIREPROOF](Modifiers-MONSTER.md#mob_fireproof) |
| `monsters[128].tables` | [boss:BOSS_FURNACE_MASTER](Tables-TEMPLATE.md#boss-boss_furnace_master) |
| `monsters[128].trait` | `TRAIT_GROUNDED` |
| `monsters[129].code` | [Вечный осаждающий](Bosses.md#boss_eternal_besieger) |
| `monsters[129].form` | `UNDEAD` |
| `monsters[129].experience` | 60 |
| `monsters[129].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[129].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[129].stats.STOCK_HEALTH` | 21.598 |
| `monsters[129].stats.STOCK_ATTACK_PHYSICAL` | 1.888 |
| `monsters[129].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[129].stats.STOCK_ARMOR` | 40 |
| `monsters[129].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[129].stats.STOCK_MANA` | 30 |
| `monsters[129].boss` | Да |
| `monsters[129].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[129].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_CRUSHING](Modifiers-MONSTER.md#mob_crushing) |
| `monsters[129].tables` | [boss:BOSS_ETERNAL_BESIEGER](Tables-TEMPLATE.md#boss-boss_eternal_besieger) |
| `monsters[130].code` | [Пепельная вдова](Bosses.md#boss_ash_widow) |
| `monsters[130].form` | `SPIDER` |
| `monsters[130].experience` | 60 |
| `monsters[130].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[130].stats.STOCK_ATTACK_SPEED` | 1.4 |
| `monsters[130].stats.STOCK_HEALTH` | 38.902 |
| `monsters[130].stats.STOCK_ATTACK_PHYSICAL` | 2.776 |
| `monsters[130].stats.STOCK_ATTACK_FIRE` | 3.536 |
| `monsters[130].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[130].stats.STOCK_CRITICAL_CHANCE` | 12 |
| `monsters[130].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[130].stats.STOCK_MANA` | 30 |
| `monsters[130].boss` | Да |
| `monsters[130].skills` | [Огненный шар](Monster-skills.md#mob_fireball) |
| `monsters[130].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_LINGERING](Modifiers-MONSTER.md#mob_lingering) |
| `monsters[131].code` | [Огненный перевозчик](Bosses.md#boss_magma_ferryman) |
| `monsters[131].form` | `UNDEAD` |
| `monsters[131].experience` | 60 |
| `monsters[131].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[131].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[131].stats.STOCK_HEALTH` | 15.871 |
| `monsters[131].stats.STOCK_ATTACK_PHYSICAL` | 1.374 |
| `monsters[131].stats.STOCK_ATTACK_FIRE` | 0.921 |
| `monsters[131].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[131].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[131].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[131].stats.STOCK_MANA` | 30 |
| `monsters[131].boss` | Да |
| `monsters[131].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[131].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_DRAINING](Modifiers-MONSTER.md#mob_draining) |
| `monsters[132].code` | [Страж стен](Bosses.md#boss_wall_warden) |
| `monsters[132].form` | `GOLEM` |
| `monsters[132].experience` | 60 |
| `monsters[132].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[132].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[132].stats.STOCK_HEALTH` | 39.345 |
| `monsters[132].stats.STOCK_ATTACK_PHYSICAL` | 2.97 |
| `monsters[132].stats.STOCK_ARMOR` | 90 |
| `monsters[132].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[132].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[132].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[132].stats.STOCK_MANA` | 30 |
| `monsters[132].boss` | Да |
| `monsters[132].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[132].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_ARMOURED](Modifiers-MONSTER.md#mob_armoured) |
| `monsters[133].code` | [Кипящий ужас](Bosses.md#boss_boiling_horror) |
| `monsters[133].form` | `SLUG` |
| `monsters[133].experience` | 60 |
| `monsters[133].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[133].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[133].stats.STOCK_HEALTH` | 2.113 |
| `monsters[133].stats.STOCK_ATTACK_FIRE` | 0.145 |
| `monsters[133].stats.STOCK_RESIST_FIRE` | 70 |
| `monsters[133].stats.STOCK_HEALTH_REGEN` | 0.035 |
| `monsters[133].stats.STOCK_ARMOR` | 40 |
| `monsters[133].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[133].stats.STOCK_MANA` | 30 |
| `monsters[133].boss` | Да |
| `monsters[133].skills` | [Огненный шар](Monster-skills.md#mob_fireball); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[133].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_REGENERATING](Modifiers-MONSTER.md#mob_regenerating) |
| `monsters[134].code` | [Тлеющий лич](Bosses.md#boss_smouldering_lich) |
| `monsters[134].form` | `UNDEAD` |
| `monsters[134].experience` | 60 |
| `monsters[134].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[134].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[134].stats.STOCK_HEALTH` | 15.273 |
| `monsters[134].stats.STOCK_ATTACK_PHYSICAL` | 1.329 |
| `monsters[134].stats.STOCK_ATTACK_FIRE` | 0.883 |
| `monsters[134].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[134].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[134].stats.STOCK_ATTACK_MAGICAL` | 1.033 |
| `monsters[134].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[134].stats.STOCK_MANA` | 60 |
| `monsters[134].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[134].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[134].boss` | Да |
| `monsters[134].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[134].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_ELEMENTALIST](Modifiers-MONSTER.md#mob_elementalist) |
| `monsters[134].tables` | [boss:BOSS_SMOULDERING_LICH](Tables-TEMPLATE.md#boss-boss_smouldering_lich) |
| `monsters[135].code` | [Кузнец войны](Bosses.md#boss_warsmith) |
| `monsters[135].form` | `DEMON` |
| `monsters[135].experience` | 60 |
| `monsters[135].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[135].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[135].stats.STOCK_HEALTH` | 14.682 |
| `monsters[135].stats.STOCK_ATTACK_PHYSICAL` | 1.28 |
| `monsters[135].stats.STOCK_ARMOR` | 50 |
| `monsters[135].stats.STOCK_HEALTH_REGEN` | 0.207 |
| `monsters[135].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[135].stats.STOCK_MANA` | 30 |
| `monsters[135].boss` | Да |
| `monsters[135].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[135].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_STRONG](Modifiers-MONSTER.md#mob_strong) |
| `monsters[135].tables` | [boss:BOSS_WARSMITH](Tables-TEMPLATE.md#boss-boss_warsmith) |
| `monsters[135].trait` | `TRAIT_GROUNDED` |
| `monsters[136].code` | [Горящий капитан](Bosses.md#boss_burning_captain) |
| `monsters[136].form` | `UNDEAD` |
| `monsters[136].experience` | 60 |
| `monsters[136].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[136].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[136].stats.STOCK_HEALTH` | 42.59 |
| `monsters[136].stats.STOCK_ATTACK_PHYSICAL` | 3.7 |
| `monsters[136].stats.STOCK_ATTACK_FIRE` | 2.453 |
| `monsters[136].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[136].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[136].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[136].stats.STOCK_MANA` | 30 |
| `monsters[136].boss` | Да |
| `monsters[136].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[136].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_BURNING](Modifiers-MONSTER.md#mob_burning) |
| `monsters[137].code` | [Жерловой дракон](Bosses.md#boss_vent_dragon) |
| `monsters[137].form` | `SERPENT` |
| `monsters[137].experience` | 60 |
| `monsters[137].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[137].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[137].stats.STOCK_HEALTH` | 16.256 |
| `monsters[137].stats.STOCK_ATTACK_PHYSICAL` | 1.126 |
| `monsters[137].stats.STOCK_ATTACK_FIRE` | 0.99 |
| `monsters[137].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[137].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[137].stats.STOCK_MANA` | 30 |
| `monsters[137].boss` | Да |
| `monsters[137].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[137].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_FLAMING](Modifiers-MONSTER.md#mob_flaming) |
| `monsters[137].tables` | [boss:BOSS_VENT_DRAGON](Tables-TEMPLATE.md#boss-boss_vent_dragon) |
| `monsters[138].code` | [Привратник преисподней](Bosses.md#boss_infernal_gatekeeper) |
| `monsters[138].form` | `DEMON` |
| `monsters[138].experience` | 60 |
| `monsters[138].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[138].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[138].stats.STOCK_HEALTH` | 9.734 |
| `monsters[138].stats.STOCK_ATTACK_PHYSICAL` | 0.848 |
| `monsters[138].stats.STOCK_ATTACK_FIRE` | 0.595 |
| `monsters[138].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[138].stats.STOCK_ARMOR` | 50 |
| `monsters[138].stats.STOCK_HEALTH_REGEN` | 0.131 |
| `monsters[138].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[138].stats.STOCK_MANA` | 30 |
| `monsters[138].boss` | Да |
| `monsters[138].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[138].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_WARDED](Modifiers-MONSTER.md#mob_warded) |
| `monsters[139].code` | [Сердце горы](Bosses.md#boss_mountain_heart) |
| `monsters[139].form` | `GOLEM` |
| `monsters[139].experience` | 60 |
| `monsters[139].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[139].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[139].stats.STOCK_HEALTH` | 1.424 |
| `monsters[139].stats.STOCK_ATTACK_PHYSICAL` | 0.108 |
| `monsters[139].stats.STOCK_ATTACK_FIRE` | 0.049 |
| `monsters[139].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[139].stats.STOCK_ARMOR` | 90 |
| `monsters[139].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[139].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[139].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[139].stats.STOCK_MANA` | 30 |
| `monsters[139].boss` | Да |
| `monsters[139].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[139].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_JUGGERNAUT](Modifiers-MONSTER.md#mob_juggernaut) |
| `monsters[140].code` | [Владыка Пламени](Bosses.md#boss_flame_sovereign) |
| `monsters[140].form` | `DEMON` |
| `monsters[140].experience` | 60 |
| `monsters[140].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[140].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[140].stats.STOCK_HEALTH` | 9.527 |
| `monsters[140].stats.STOCK_ATTACK_PHYSICAL` | 0.658 |
| `monsters[140].stats.STOCK_ATTACK_FIRE` | 0.465 |
| `monsters[140].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[140].stats.STOCK_ARMOR` | 50 |
| `monsters[140].stats.STOCK_HEALTH_REGEN` | 0.104 |
| `monsters[140].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[140].stats.STOCK_RESIST_ALL` | 15 |
| `monsters[140].stats.STOCK_MANA` | 30 |
| `monsters[140].boss` | Да |
| `monsters[140].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[140].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_FLAMING](Modifiers-MONSTER.md#mob_flaming) |
| `monsters[140].tables` | [boss:BOSS_FLAME_SOVEREIGN](Tables-TEMPLATE.md#boss-boss_flame_sovereign) |
| `monsters[141].code` | [Страж разлома](Bosses.md#boss_rift_watcher) |
| `monsters[141].form` | `WRAITH` |
| `monsters[141].experience` | 60 |
| `monsters[141].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[141].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[141].stats.STOCK_HEALTH` | 34.33 |
| `monsters[141].stats.STOCK_ENERGY_SHIELD` | 20.53 |
| `monsters[141].stats.STOCK_ATTACK_CHAOS` | 3.941 |
| `monsters[141].stats.STOCK_RESIST_CHAOS` | 70 |
| `monsters[141].stats.STOCK_ATTACK_MAGICAL` | 3.063 |
| `monsters[141].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[141].stats.STOCK_MANA` | 60 |
| `monsters[141].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[141].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[141].boss` | Да |
| `monsters[141].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit) |
| `monsters[141].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_WITHERING](Modifiers-MONSTER.md#mob_withering) |
| `monsters[141].tables` | [boss:BOSS_RIFT_WATCHER](Tables-TEMPLATE.md#boss-boss_rift_watcher) |
| `monsters[142].code` | [Чумной жнец](Bosses.md#boss_plague_reaper) |
| `monsters[142].form` | `UNDEAD` |
| `monsters[142].experience` | 60 |
| `monsters[142].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[142].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[142].stats.STOCK_HEALTH` | 20.93 |
| `monsters[142].stats.STOCK_ATTACK_PHYSICAL` | 1.8 |
| `monsters[142].stats.STOCK_ATTACK_CHAOS` | 1.202 |
| `monsters[142].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[142].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[142].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[142].stats.STOCK_MANA` | 30 |
| `monsters[142].boss` | Да |
| `monsters[142].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[142].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_BLIGHTED](Modifiers-MONSTER.md#mob_blighted) |
| `monsters[142].tables` | [boss:BOSS_PLAGUE_REAPER](Tables-TEMPLATE.md#boss-boss_plague_reaper) |
| `monsters[143].code` | [Падший аббат](Bosses.md#boss_fallen_abbot) |
| `monsters[143].form` | `HUMANOID` |
| `monsters[143].experience` | 60 |
| `monsters[143].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[143].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[143].stats.STOCK_HEALTH` | 24.607 |
| `monsters[143].stats.STOCK_ATTACK_PHYSICAL` | 2.353 |
| `monsters[143].stats.STOCK_ATTACK_COLD` | 1.414 |
| `monsters[143].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[143].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[143].stats.STOCK_ATTACK_MAGICAL` | 1.647 |
| `monsters[143].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[143].stats.STOCK_MANA` | 60 |
| `monsters[143].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[143].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[143].boss` | Да |
| `monsters[143].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[143].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_ARCANE](Modifiers-MONSTER.md#mob_arcane) |
| `monsters[143].tables` | [boss:BOSS_FALLEN_ABBOT](Tables-TEMPLATE.md#boss-boss_fallen_abbot) |
| `monsters[144].code` | [Шептун](Bosses.md#boss_whisperer) |
| `monsters[144].form` | `WRAITH` |
| `monsters[144].experience` | 60 |
| `monsters[144].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[144].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[144].stats.STOCK_HEALTH` | 26.015 |
| `monsters[144].stats.STOCK_ENERGY_SHIELD` | 15.51 |
| `monsters[144].stats.STOCK_ATTACK_COLD` | 2.949 |
| `monsters[144].stats.STOCK_RESIST_COLD` | 70 |
| `monsters[144].stats.STOCK_ATTACK_MAGICAL` | 2.299 |
| `monsters[144].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[144].stats.STOCK_MANA` | 60 |
| `monsters[144].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[144].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[144].boss` | Да |
| `monsters[144].skills` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt); [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[144].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_FROSTBORN](Modifiers-MONSTER.md#mob_frostborn) |
| `monsters[144].tables` | [boss:BOSS_WHISPERER](Tables-TEMPLATE.md#boss-boss_whisperer) |
| `monsters[145].code` | [Садовник гнили](Bosses.md#boss_rot_gardener) |
| `monsters[145].form` | `FUNGUS` |
| `monsters[145].experience` | 60 |
| `monsters[145].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[145].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[145].stats.STOCK_HEALTH` | 31.703 |
| `monsters[145].stats.STOCK_ATTACK_CHAOS` | 2.324 |
| `monsters[145].stats.STOCK_RESIST_CHAOS` | 70 |
| `monsters[145].stats.STOCK_HEALTH_REGEN` | 0.704 |
| `monsters[145].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[145].stats.STOCK_MANA` | 30 |
| `monsters[145].boss` | Да |
| `monsters[145].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit) |
| `monsters[145].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_REGENERATING](Modifiers-MONSTER.md#mob_regenerating) |
| `monsters[145].tables` | [boss:BOSS_ROT_GARDENER](Tables-TEMPLATE.md#boss-boss_rot_gardener) |
| `monsters[146].code` | [Чумной лекарь](Bosses.md#boss_plague_doctor) |
| `monsters[146].form` | `HUMANOID` |
| `monsters[146].experience` | 60 |
| `monsters[146].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[146].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[146].stats.STOCK_HEALTH` | 4.44 |
| `monsters[146].stats.STOCK_ATTACK_PHYSICAL` | 0.425 |
| `monsters[146].stats.STOCK_ATTACK_CHAOS` | 0.254 |
| `monsters[146].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[146].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[146].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[146].stats.STOCK_MANA` | 30 |
| `monsters[146].boss` | Да |
| `monsters[146].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[146].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_TOXIC](Modifiers-MONSTER.md#mob_toxic) |
| `monsters[146].tables` | [boss:BOSS_PLAGUE_DOCTOR](Tables-TEMPLATE.md#boss-boss_plague_doctor) |
| `monsters[147].code` | [Собиратель костей](Bosses.md#boss_bone_collector) |
| `monsters[147].form` | `UNDEAD` |
| `monsters[147].experience` | 60 |
| `monsters[147].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[147].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[147].stats.STOCK_HEALTH` | 35.465 |
| `monsters[147].stats.STOCK_ATTACK_PHYSICAL` | 3.032 |
| `monsters[147].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[147].stats.STOCK_ARMOR` | 40 |
| `monsters[147].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[147].stats.STOCK_MANA` | 30 |
| `monsters[147].boss` | Да |
| `monsters[147].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[147].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_STURDY](Modifiers-MONSTER.md#mob_sturdy) |
| `monsters[147].tables` | [boss:BOSS_BONE_COLLECTOR](Tables-TEMPLATE.md#boss-boss_bone_collector) |
| `monsters[148].code` | [Кракен пустоты](Bosses.md#boss_void_kraken) |
| `monsters[148].form` | `SERPENT` |
| `monsters[148].experience` | 60 |
| `monsters[148].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[148].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[148].stats.STOCK_HEALTH` | 46.532 |
| `monsters[148].stats.STOCK_ATTACK_PHYSICAL` | 3.235 |
| `monsters[148].stats.STOCK_ATTACK_COLD` | 2.813 |
| `monsters[148].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[148].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[148].stats.STOCK_MANA` | 30 |
| `monsters[148].boss` | Да |
| `monsters[148].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[148].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_CHILLING](Modifiers-MONSTER.md#mob_chilling) |
| `monsters[148].tables` | [boss:BOSS_VOID_KRAKEN](Tables-TEMPLATE.md#boss-boss_void_kraken) |
| `monsters[149].code` | [Споровый кардинал](Bosses.md#boss_spore_cardinal) |
| `monsters[149].form` | `FUNGUS` |
| `monsters[149].experience` | 60 |
| `monsters[149].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[149].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[149].stats.STOCK_HEALTH` | 49.631 |
| `monsters[149].stats.STOCK_ATTACK_CHAOS` | 3.603 |
| `monsters[149].stats.STOCK_RESIST_CHAOS` | 70 |
| `monsters[149].stats.STOCK_HEALTH_REGEN` | 1.084 |
| `monsters[149].stats.STOCK_ATTACK_MAGICAL` | 2.519 |
| `monsters[149].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[149].stats.STOCK_MANA` | 60 |
| `monsters[149].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[149].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[149].boss` | Да |
| `monsters[149].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[149].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_LINGERING](Modifiers-MONSTER.md#mob_lingering) |
| `monsters[149].tables` | [boss:BOSS_SPORE_CARDINAL](Tables-TEMPLATE.md#boss-boss_spore_cardinal) |
| `monsters[150].code` | [Звёздное отродье](Bosses.md#boss_star_spawn) |
| `monsters[150].form` | `DEMON` |
| `monsters[150].experience` | 60 |
| `monsters[150].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[150].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[150].stats.STOCK_HEALTH` | 20.711 |
| `monsters[150].stats.STOCK_ATTACK_PHYSICAL` | 1.791 |
| `monsters[150].stats.STOCK_ATTACK_LIGHTNING` | 1.252 |
| `monsters[150].stats.STOCK_RESIST_LIGHTNING` | 40 |
| `monsters[150].stats.STOCK_ARMOR` | 50 |
| `monsters[150].stats.STOCK_HEALTH_REGEN` | 0.27 |
| `monsters[150].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[150].stats.STOCK_MANA` | 30 |
| `monsters[150].boss` | Да |
| `monsters[150].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[150].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_STORMBORN](Modifiers-MONSTER.md#mob_stormborn) |
| `monsters[150].tables` | [boss:BOSS_STAR_SPAWN](Tables-TEMPLATE.md#boss-boss_star_spawn) |
| `monsters[151].code` | [Червь катакомб](Bosses.md#boss_catacomb_worm) |
| `monsters[151].form` | `SLUG` |
| `monsters[151].experience` | 60 |
| `monsters[151].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[151].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[151].stats.STOCK_HEALTH` | 43.495 |
| `monsters[151].stats.STOCK_ATTACK_PHYSICAL` | 2.951 |
| `monsters[151].stats.STOCK_HEALTH_REGEN` | 0.654 |
| `monsters[151].stats.STOCK_ARMOR` | 40 |
| `monsters[151].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[151].stats.STOCK_MANA` | 30 |
| `monsters[151].boss` | Да |
| `monsters[151].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[151].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_HUNGERING](Modifiers-MONSTER.md#mob_hungering) |
| `monsters[151].tables` | [boss:BOSS_CATACOMB_WORM](Tables-TEMPLATE.md#boss-boss_catacomb_worm) |
| `monsters[152].code` | [Иссохший великан](Bosses.md#boss_withered_giant) |
| `monsters[152].form` | `BRUTE` |
| `monsters[152].experience` | 60 |
| `monsters[152].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[152].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[152].stats.STOCK_HEALTH` | 52.935 |
| `monsters[152].stats.STOCK_ATTACK_PHYSICAL` | 3.801 |
| `monsters[152].stats.STOCK_ATTACK_CHAOS` | 1.722 |
| `monsters[152].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[152].stats.STOCK_HEALTH_REGEN` | 0.77 |
| `monsters[152].stats.STOCK_STUN_THRESHOLD` | 15 |
| `monsters[152].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[152].stats.STOCK_MANA` | 30 |
| `monsters[152].boss` | Да |
| `monsters[152].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[152].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_WITHERING](Modifiers-MONSTER.md#mob_withering) |
| `monsters[152].tables` | [boss:BOSS_WITHERED_GIANT](Tables-TEMPLATE.md#boss-boss_withered_giant) |
| `monsters[153].code` | [Жрица затмения](Bosses.md#boss_eclipse_priestess) |
| `monsters[153].form` | `HUMANOID` |
| `monsters[153].experience` | 60 |
| `monsters[153].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[153].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[153].stats.STOCK_HEALTH` | 24.341 |
| `monsters[153].stats.STOCK_ATTACK_PHYSICAL` | 2.313 |
| `monsters[153].stats.STOCK_ATTACK_LIGHTNING` | 1.387 |
| `monsters[153].stats.STOCK_RESIST_LIGHTNING` | 40 |
| `monsters[153].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[153].stats.STOCK_ATTACK_MAGICAL` | 1.609 |
| `monsters[153].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[153].stats.STOCK_MANA` | 60 |
| `monsters[153].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[153].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[153].boss` | Да |
| `monsters[153].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[153].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_SHOCKING](Modifiers-MONSTER.md#mob_shocking) |
| `monsters[153].tables` | [boss:BOSS_ECLIPSE_PRIESTESS](Tables-TEMPLATE.md#boss-boss_eclipse_priestess) |
| `monsters[154].code` | [Безглазый](Bosses.md#boss_eyeless_one) |
| `monsters[154].form` | `SPIDER` |
| `monsters[154].experience` | 60 |
| `monsters[154].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[154].stats.STOCK_ATTACK_SPEED` | 1.4 |
| `monsters[154].stats.STOCK_HEALTH` | 15.442 |
| `monsters[154].stats.STOCK_ATTACK_PHYSICAL` | 1.074 |
| `monsters[154].stats.STOCK_ATTACK_CHAOS` | 1.382 |
| `monsters[154].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[154].stats.STOCK_CRITICAL_CHANCE` | 12 |
| `monsters[154].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[154].stats.STOCK_MANA` | 30 |
| `monsters[154].boss` | Да |
| `monsters[154].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[154].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_VENOMOUS](Modifiers-MONSTER.md#mob_venomous) |
| `monsters[154].tables` | [boss:BOSS_EYELESS_ONE](Tables-TEMPLATE.md#boss-boss_eyeless_one) |
| `monsters[155].code` | [Мать гнили](Bosses.md#boss_mother_blight) |
| `monsters[155].form` | `FUNGUS` |
| `monsters[155].experience` | 60 |
| `monsters[155].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[155].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[155].stats.STOCK_HEALTH` | 4.746 |
| `monsters[155].stats.STOCK_ATTACK_CHAOS` | 0.343 |
| `monsters[155].stats.STOCK_RESIST_CHAOS` | 70 |
| `monsters[155].stats.STOCK_HEALTH_REGEN` | 0.102 |
| `monsters[155].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[155].stats.STOCK_MANA` | 30 |
| `monsters[155].boss` | Да |
| `monsters[155].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[155].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_BLIGHTED](Modifiers-MONSTER.md#mob_blighted) |
| `monsters[155].tables` | [boss:BOSS_MOTHER_BLIGHT](Tables-TEMPLATE.md#boss-boss_mother_blight) |
| `monsters[156].code` | [Ткач завесы](Bosses.md#boss_veil_weaver) |
| `monsters[156].form` | `WRAITH` |
| `monsters[156].experience` | 60 |
| `monsters[156].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[156].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[156].stats.STOCK_HEALTH` | 13.395 |
| `monsters[156].stats.STOCK_ENERGY_SHIELD` | 8.062 |
| `monsters[156].stats.STOCK_ATTACK_COLD` | 1.524 |
| `monsters[156].stats.STOCK_RESIST_COLD` | 70 |
| `monsters[156].stats.STOCK_ATTACK_MAGICAL` | 1.174 |
| `monsters[156].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[156].stats.STOCK_MANA` | 60 |
| `monsters[156].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[156].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[156].boss` | Да |
| `monsters[156].skills` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[156].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_MIRRORED](Modifiers-MONSTER.md#mob_mirrored) |
| `monsters[156].tables` | [boss:BOSS_VEIL_WEAVER](Tables-TEMPLATE.md#boss-boss_veil_weaver) |
| `monsters[157].code` | [Вечный привратник](Bosses.md#boss_endless_warden) |
| `monsters[157].form` | `DEMON` |
| `monsters[157].experience` | 60 |
| `monsters[157].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[157].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[157].stats.STOCK_HEALTH` | 32.929 |
| `monsters[157].stats.STOCK_ATTACK_PHYSICAL` | 2.812 |
| `monsters[157].stats.STOCK_ARMOR` | 50 |
| `monsters[157].stats.STOCK_HEALTH_REGEN` | 0.415 |
| `monsters[157].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[157].stats.STOCK_MANA` | 30 |
| `monsters[157].boss` | Да |
| `monsters[157].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[157].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_RELENTLESS](Modifiers-MONSTER.md#mob_relentless) |
| `monsters[157].tables` | [boss:BOSS_ENDLESS_WARDEN](Tables-TEMPLATE.md#boss-boss_endless_warden) |
| `monsters[158].code` | [Король бездны](Bosses.md#boss_abyss_king) |
| `monsters[158].form` | `DEMON` |
| `monsters[158].experience` | 60 |
| `monsters[158].loot` | [loot:BOSS_6](Tables-LOOT.md#loot-boss_6) |
| `monsters[158].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[158].stats.STOCK_HEALTH` | 4.837 |
| `monsters[158].stats.STOCK_ATTACK_PHYSICAL` | 0.332 |
| `monsters[158].stats.STOCK_ATTACK_CHAOS` | 0.232 |
| `monsters[158].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[158].stats.STOCK_ARMOR` | 50 |
| `monsters[158].stats.STOCK_HEALTH_REGEN` | 0.048 |
| `monsters[158].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[158].stats.STOCK_RESIST_ALL` | 15 |
| `monsters[158].stats.STOCK_MANA` | 30 |
| `monsters[158].boss` | Да |
| `monsters[158].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[158].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_WITHERING](Modifiers-MONSTER.md#mob_withering) |
| `monsters[158].tables` | [boss:BOSS_ABYSS_KING](Tables-TEMPLATE.md#boss-boss_abyss_king) |
| `monsters[159].code` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| `monsters[159].form` | `WRAITH` |
| `monsters[159].experience` | 80 |
| `monsters[159].loot` | [loot:CORRUPT_1](Tables-LOOT.md#loot-corrupt_1) |
| `monsters[159].stats.STOCK_ATTACK_SPEED` | 1.3 |
| `monsters[159].stats.STOCK_HEALTH` | 55 |
| `monsters[159].stats.STOCK_ATTACK_CHAOS` | 8 |
| `monsters[159].stats.STOCK_RESIST_CHAOS` | 35 |
| `monsters[159].corrupted` | Да |
| `monsters[159].fixed` | [MOB_VAMPIRIC](Modifiers-MONSTER.md#mob_vampiric); [MOB_DEADLY](Modifiers-MONSTER.md#mob_deadly) |
| `monsters[160].code` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `monsters[160].form` | `WRAITH` |
| `monsters[160].experience` | 110 |
| `monsters[160].loot` | [loot:CORRUPT_2](Tables-LOOT.md#loot-corrupt_2) |
| `monsters[160].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[160].stats.STOCK_HEALTH` | 80 |
| `monsters[160].stats.STOCK_ATTACK_CHAOS` | 10 |
| `monsters[160].stats.STOCK_ENERGY_SHIELD` | 25 |
| `monsters[160].stats.STOCK_RESIST_CHAOS` | 45 |
| `monsters[160].corrupted` | Да |
| `monsters[160].fixed` | [MOB_ARMOURED](Modifiers-MONSTER.md#mob_armoured); [MOB_BERSERK](Modifiers-MONSTER.md#mob_berserk) |
| `monsters[161].code` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `monsters[161].form` | `WRAITH` |
| `monsters[161].experience` | 150 |
| `monsters[161].loot` | [loot:CORRUPT_3](Tables-LOOT.md#loot-corrupt_3) |
| `monsters[161].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[161].stats.STOCK_HEALTH` | 110 |
| `monsters[161].stats.STOCK_ATTACK_CHAOS` | 14 |
| `monsters[161].stats.STOCK_ENERGY_SHIELD` | 40 |
| `monsters[161].stats.STOCK_RESIST_CHAOS` | 60 |
| `monsters[161].corrupted` | Да |
| `monsters[161].fixed` | [MOB_SHIELDED](Modifiers-MONSTER.md#mob_shielded); [MOB_ELEMENTALIST](Modifiers-MONSTER.md#mob_elementalist) |
| `monsters[162].code` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `monsters[162].form` | `WRAITH` |
| `monsters[162].experience` | 190 |
| `monsters[162].loot` | [loot:CORRUPT_4](Tables-LOOT.md#loot-corrupt_4) |
| `monsters[162].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[162].stats.STOCK_HEALTH` | 135 |
| `monsters[162].stats.STOCK_ATTACK_CHAOS` | 16 |
| `monsters[162].stats.STOCK_ENERGY_SHIELD` | 50 |
| `monsters[162].stats.STOCK_RESIST_CHAOS` | 65 |
| `monsters[162].corrupted` | Да |
| `monsters[162].fixed` | [MOB_VAMPIRIC](Modifiers-MONSTER.md#mob_vampiric); [MOB_MERCILESS](Modifiers-MONSTER.md#mob_merciless) |
| `monsters[163].code` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `monsters[163].form` | `WRAITH` |
| `monsters[163].experience` | 230 |
| `monsters[163].loot` | [loot:CORRUPT_5](Tables-LOOT.md#loot-corrupt_5) |
| `monsters[163].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[163].stats.STOCK_HEALTH` | 160 |
| `monsters[163].stats.STOCK_ATTACK_CHAOS` | 18 |
| `monsters[163].stats.STOCK_ENERGY_SHIELD` | 60 |
| `monsters[163].stats.STOCK_RESIST_CHAOS` | 70 |
| `monsters[163].corrupted` | Да |
| `monsters[163].fixed` | [MOB_WARDED](Modifiers-MONSTER.md#mob_warded); [MOB_HUNGERING](Modifiers-MONSTER.md#mob_hungering) |
| `monsters[164].code` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `monsters[164].form` | `WRAITH` |
| `monsters[164].experience` | 270 |
| `monsters[164].loot` | [loot:CORRUPT_6](Tables-LOOT.md#loot-corrupt_6) |
| `monsters[164].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[164].stats.STOCK_HEALTH` | 185 |
| `monsters[164].stats.STOCK_ATTACK_CHAOS` | 20 |
| `monsters[164].stats.STOCK_ENERGY_SHIELD` | 70 |
| `monsters[164].stats.STOCK_RESIST_CHAOS` | 75 |
| `monsters[164].corrupted` | Да |
| `monsters[164].fixed` | [MOB_SHIELDED](Modifiers-MONSTER.md#mob_shielded); [MOB_WITHERING](Modifiers-MONSTER.md#mob_withering) |
| `monsters[165].code` | [Отродье Бездны](Monsters.md#chasm_spawn) |
| `monsters[165].form` | `DEMON` |
| `monsters[165].experience` | 24 |
| `monsters[165].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[165].stats.STOCK_ATTACK_SPEED` | 1.4 |
| `monsters[165].stats.STOCK_HEALTH` | 20 |
| `monsters[165].stats.STOCK_ATTACK_PHYSICAL` | 2.5 |
| `monsters[165].stats.STOCK_ATTACK_CHAOS` | 4 |
| `monsters[165].stats.STOCK_RESIST_CHAOS` | 50 |
| `monsters[165].trait` | `TRAIT_BERSERK` |
| `monsters[166].code` | [Пиявка глубин](Monsters.md#chasm_leech) |
| `monsters[166].form` | `SLUG` |
| `monsters[166].experience` | 22 |
| `monsters[166].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[166].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[166].stats.STOCK_HEALTH` | 26 |
| `monsters[166].stats.STOCK_ATTACK_PHYSICAL` | 3.5 |
| `monsters[166].stats.STOCK_ATTACK_CHAOS` | 1.5 |
| `monsters[166].stats.STOCK_LEECH_ALL` | 10 |
| `monsters[166].stats.STOCK_HEALTH_ON_HIT` | 3 |
| `monsters[166].trait` | `TRAIT_VOLATILE` |
| `monsters[167].code` | [Шептун Бездны](Monsters.md#chasm_whisperer) |
| `monsters[167].form` | `WRAITH` |
| `monsters[167].experience` | 26 |
| `monsters[167].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[167].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[167].stats.STOCK_HEALTH` | 16 |
| `monsters[167].stats.STOCK_ENERGY_SHIELD` | 10 |
| `monsters[167].stats.STOCK_ATTACK_CHAOS` | 3 |
| `monsters[167].stats.STOCK_RESIST_CHAOS` | 60 |
| `monsters[167].stats.STOCK_ATTACK_MAGICAL` | 4.5 |
| `monsters[167].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[167].stats.STOCK_MANA` | 40 |
| `monsters[167].stats.STOCK_MANA_REGEN` | 3 |
| `monsters[167].skills` | [Слабость](Monster-skills.md#mob_enfeeble); [Кольцо хаоса](Monster-skills.md#mob_chaos_ring) |
| `monsters[167].trait` | `TRAIT_SKILL_MANA_DRAIN` |
| `monsters[168].code` | [Ловец из пропасти](Monsters.md#chasm_stalker) |
| `monsters[168].form` | `SPIDER` |
| `monsters[168].experience` | 24 |
| `monsters[168].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[168].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[168].stats.STOCK_HEALTH` | 18 |
| `monsters[168].stats.STOCK_ATTACK_PHYSICAL` | 3 |
| `monsters[168].stats.STOCK_ATTACK_CHAOS` | 2 |
| `monsters[168].stats.STOCK_EVASION` | 40 |
| `monsters[168].stats.STOCK_POISON_CHANCE` | 25 |
| `monsters[168].trait` | `TRAIT_AMBUSHER` |
| `monsters[169].code` | [Проклинатель пустоты](Monsters.md#chasm_hexer) |
| `monsters[169].form` | `HUMANOID` |
| `monsters[169].experience` | 26 |
| `monsters[169].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[169].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[169].stats.STOCK_HEALTH` | 18 |
| `monsters[169].stats.STOCK_ATTACK_CHAOS` | 2.5 |
| `monsters[169].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[169].stats.STOCK_ATTACK_MAGICAL` | 4 |
| `monsters[169].stats.STOCK_CAST_SPEED` | 0.9 |
| `monsters[169].stats.STOCK_MANA` | 40 |
| `monsters[169].stats.STOCK_MANA_REGEN` | 3 |
| `monsters[169].skills` | [Уязвимость](Monster-skills.md#mob_vulnerability); [Ядовитый плевок](Monster-skills.md#mob_spit) |
| `monsters[169].trait` | `TRAIT_DEATH_PRAYER` |
| `monsters[170].code` | [Громила Бездны](Monsters.md#chasm_brute) |
| `monsters[170].form` | `BRUTE` |
| `monsters[170].experience` | 28 |
| `monsters[170].loot` | [loot:T3](Tables-LOOT.md#loot-t3) |
| `monsters[170].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[170].stats.STOCK_HEALTH` | 40 |
| `monsters[170].stats.STOCK_ATTACK_PHYSICAL` | 5 |
| `monsters[170].stats.STOCK_ATTACK_CHAOS` | 2 |
| `monsters[170].stats.STOCK_ARMOR` | 40 |
| `monsters[170].stats.STOCK_MANA` | 30 |
| `monsters[170].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[170].trait` | `TRAIT_LAST_STAND` |
| `monsters[171].code` | [Хозяин ямы](Monsters.md#chasm_pit_master) |
| `monsters[171].form` | `BRUTE` |
| `monsters[171].experience` | 60 |
| `monsters[171].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[171].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[171].stats.STOCK_HEALTH` | 110 |
| `monsters[171].stats.STOCK_ATTACK_PHYSICAL` | 9 |
| `monsters[171].stats.STOCK_ATTACK_CHAOS` | 5 |
| `monsters[171].stats.STOCK_ARMOR` | 60 |
| `monsters[171].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[171].stats.STOCK_MANA` | 30 |
| `monsters[171].boss` | Да |
| `monsters[171].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[171].fixed` | [ABYSS_MOB_BLOODTHIRSTY](Modifiers-MONSTER.md#abyss_mob_bloodthirsty) |
| `monsters[172].code` | [Королева шёпотов](Monsters.md#chasm_whisper_queen) |
| `monsters[172].form` | `WRAITH` |
| `monsters[172].experience` | 60 |
| `monsters[172].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[172].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[172].stats.STOCK_HEALTH` | 90 |
| `monsters[172].stats.STOCK_ENERGY_SHIELD` | 60 |
| `monsters[172].stats.STOCK_ATTACK_CHAOS` | 9 |
| `monsters[172].stats.STOCK_RESIST_CHAOS` | 60 |
| `monsters[172].stats.STOCK_ATTACK_MAGICAL` | 8 |
| `monsters[172].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[172].stats.STOCK_MANA` | 60 |
| `monsters[172].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[172].boss` | Да |
| `monsters[172].skills` | [Кольцо хаоса](Monster-skills.md#mob_chaos_ring); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[172].fixed` | [ABYSS_MOB_WHISPERING](Modifiers-MONSTER.md#abyss_mob_whispering); [ABYSS_MOB_VOIDTOUCHED](Modifiers-MONSTER.md#abyss_mob_voidtouched) |
| `monsters[173].code` | [Пожиратель глубин](Monsters.md#chasm_devourer) |
| `monsters[173].form` | `DEMON` |
| `monsters[173].experience` | 60 |
| `monsters[173].loot` | [loot:BOSS_3](Tables-LOOT.md#loot-boss_3) |
| `monsters[173].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[173].stats.STOCK_HEALTH` | 170 |
| `monsters[173].stats.STOCK_ATTACK_PHYSICAL` | 10 |
| `monsters[173].stats.STOCK_ATTACK_CHAOS` | 11 |
| `monsters[173].stats.STOCK_RESIST_CHAOS` | 70 |
| `monsters[173].stats.STOCK_ARMOR` | 60 |
| `monsters[173].stats.STOCK_HEALTH_REGEN` | 3 |
| `monsters[173].stats.STOCK_MANA` | 50 |
| `monsters[173].boss` | Да |
| `monsters[173].skills` | [Кольцо хаоса](Monster-skills.md#mob_chaos_ring); [Уязвимость](Monster-skills.md#mob_vulnerability); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[173].fixed` | [ABYSS_MOB_ENTROPIC](Modifiers-MONSTER.md#abyss_mob_entropic); [ABYSS_MOB_UMBRAL](Modifiers-MONSTER.md#abyss_mob_umbral) |
| `monsters[174].code` | [Грозовая гарпия](Monsters.md#storm_harpy) |
| `monsters[174].form` | `BAT` |
| `monsters[174].experience` | 28 |
| `monsters[174].loot` | [loot:T7](Tables-LOOT.md#loot-t7) |
| `monsters[174].stats.STOCK_ATTACK_SPEED` | 1.8 |
| `monsters[174].stats.STOCK_HEALTH` | 15.45 |
| `monsters[174].stats.STOCK_ATTACK_PHYSICAL` | 3.09 |
| `monsters[174].stats.STOCK_ATTACK_LIGHTNING` | 4.12 |
| `monsters[174].stats.STOCK_EVASION` | 40 |
| `monsters[174].trait` | `TRAIT_STATIC` |
| `monsters[175].code` | [Стеклянный ловчий](Monsters.md#glass_stalker) |
| `monsters[175].form` | `CRYSTAL` |
| `monsters[175].experience` | 30 |
| `monsters[175].loot` | [loot:T7](Tables-LOOT.md#loot-t7) |
| `monsters[175].stats.STOCK_ATTACK_SPEED` | 1.4 |
| `monsters[175].stats.STOCK_HEALTH` | 22.66 |
| `monsters[175].stats.STOCK_ATTACK_PHYSICAL` | 6.18 |
| `monsters[175].stats.STOCK_ATTACK_COLD` | 3.09 |
| `monsters[175].stats.STOCK_ARMOR` | 40 |
| `monsters[175].trait` | `TRAIT_KEEN_EYE` |
| `monsters[176].code` | [Небесный часовой](Monsters.md#sky_sentinel) |
| `monsters[176].form` | `CONSTRUCT` |
| `monsters[176].experience` | 29 |
| `monsters[176].loot` | [loot:T7](Tables-LOOT.md#loot-t7) |
| `monsters[176].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[176].stats.STOCK_HEALTH` | 35.02 |
| `monsters[176].stats.STOCK_ATTACK_PHYSICAL` | 7.21 |
| `monsters[176].stats.STOCK_ARMOR` | 80 |
| `monsters[176].stats.STOCK_RESIST_ALL` | 20 |
| `monsters[176].trait` | `TRAIT_WARDED` |
| `monsters[177].code` | [Громовой дрейк](Monsters.md#thunder_drake) |
| `monsters[177].form` | `DRAGON` |
| `monsters[177].experience` | 27 |
| `monsters[177].loot` | [loot:T7](Tables-LOOT.md#loot-t7) |
| `monsters[177].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[177].stats.STOCK_HEALTH` | 28.84 |
| `monsters[177].stats.STOCK_ATTACK_LIGHTNING` | 8.24 |
| `monsters[177].stats.STOCK_RESIST_LIGHTNING` | 60 |
| `monsters[177].trait` | `TRAIT_SKILL_THUNDERCLAP` |
| `monsters[178].code` | [Осколочный голем](Monsters.md#shard_golem) |
| `monsters[178].form` | `GOLEM` |
| `monsters[178].experience` | 27 |
| `monsters[178].loot` | [loot:T7](Tables-LOOT.md#loot-t7) |
| `monsters[178].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[178].stats.STOCK_HEALTH` | 37.08 |
| `monsters[178].stats.STOCK_ATTACK_PHYSICAL` | 8.24 |
| `monsters[178].stats.STOCK_ARMOR` | 60 |
| `monsters[178].trait` | `TRAIT_SKILL_SLAM` |
| `monsters[179].code` | [Падший серафим](Monsters.md#fallen_seraph) |
| `monsters[179].form` | `ANGEL` |
| `monsters[179].experience` | 27 |
| `monsters[179].loot` | [loot:T7](Tables-LOOT.md#loot-t7) |
| `monsters[179].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[179].stats.STOCK_HEALTH` | 16.48 |
| `monsters[179].stats.STOCK_ENERGY_SHIELD` | 16 |
| `monsters[179].stats.STOCK_ATTACK_LIGHTNING` | 3.09 |
| `monsters[179].stats.STOCK_ATTACK_MAGICAL` | 5.15 |
| `monsters[179].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[179].stats.STOCK_MANA` | 45 |
| `monsters[179].stats.STOCK_MANA_REGEN` | 3 |
| `monsters[179].trait` | `TRAIT_SKILL_ZAP` |
| `monsters[180].code` | [Зефирный призрак](Monsters.md#zephyr_wraith) |
| `monsters[180].form` | `WRAITH` |
| `monsters[180].experience` | 27 |
| `monsters[180].loot` | [loot:T7](Tables-LOOT.md#loot-t7) |
| `monsters[180].stats.STOCK_ATTACK_SPEED` | 1.3 |
| `monsters[180].stats.STOCK_HEALTH` | 14.42 |
| `monsters[180].stats.STOCK_ENERGY_SHIELD` | 14 |
| `monsters[180].stats.STOCK_ATTACK_COLD` | 4.12 |
| `monsters[180].stats.STOCK_ATTACK_MAGICAL` | 4.63 |
| `monsters[180].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[180].stats.STOCK_MANA` | 40 |
| `monsters[180].stats.STOCK_MANA_REGEN` | 3 |
| `monsters[180].trait` | `TRAIT_NIMBLE` |
| `monsters[181].code` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `monsters[181].form` | `ANGEL` |
| `monsters[181].experience` | 290 |
| `monsters[181].loot` | [loot:CORRUPT_7](Tables-LOOT.md#loot-corrupt_7) |
| `monsters[181].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[181].stats.STOCK_HEALTH` | 200 |
| `monsters[181].stats.STOCK_ATTACK_LIGHTNING` | 22 |
| `monsters[181].stats.STOCK_ENERGY_SHIELD` | 80 |
| `monsters[181].stats.STOCK_RESIST_LIGHTNING` | 75 |
| `monsters[181].corrupted` | Да |
| `monsters[181].fixed` | [MOB_SHOCKING](Modifiers-MONSTER.md#mob_shocking); [MOB_WARDED](Modifiers-MONSTER.md#mob_warded) |
| `monsters[181].trait` | `TRAIT_GROUNDED` |
| `monsters[182].code` | [Вестник ветров](Bosses.md#boss_wind_herald) |
| `monsters[182].form` | `ANGEL` |
| `monsters[182].experience` | 60 |
| `monsters[182].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[182].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[182].stats.STOCK_HEALTH` | 18.147 |
| `monsters[182].stats.STOCK_ENERGY_SHIELD` | 12.214 |
| `monsters[182].stats.STOCK_ATTACK_COLD` | 3.138 |
| `monsters[182].stats.STOCK_RESIST_COLD` | 70 |
| `monsters[182].stats.STOCK_ATTACK_MAGICAL` | 2.095 |
| `monsters[182].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[182].stats.STOCK_MANA` | 60 |
| `monsters[182].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[182].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[182].boss` | Да |
| `monsters[182].skills` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt) |
| `monsters[182].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_CHILLING](Modifiers-MONSTER.md#mob_chilling) |
| `monsters[182].tables` | [boss:BOSS_WIND_HERALD](Tables-TEMPLATE.md#boss-boss_wind_herald) |
| `monsters[182].trait` | `TRAIT_GROUNDED` |
| `monsters[183].code` | [Стеклянная праматерь](Bosses.md#boss_glass_matriarch) |
| `monsters[183].form` | `CRYSTAL` |
| `monsters[183].experience` | 60 |
| `monsters[183].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[183].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[183].stats.STOCK_HEALTH` | 30.361 |
| `monsters[183].stats.STOCK_ATTACK_PHYSICAL` | 3.676 |
| `monsters[183].stats.STOCK_ATTACK_FIRE` | 1.734 |
| `monsters[183].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[183].stats.STOCK_EVASION` | 70 |
| `monsters[183].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[183].stats.STOCK_MOVEMENT_SPEED` | 15 |
| `monsters[183].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[183].stats.STOCK_MANA` | 30 |
| `monsters[183].boss` | Да |
| `monsters[183].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[183].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_FLAMING](Modifiers-MONSTER.md#mob_flaming) |
| `monsters[183].tables` | [boss:BOSS_GLASS_MATRIARCH](Tables-TEMPLATE.md#boss-boss_glass_matriarch) |
| `monsters[183].trait` | `TRAIT_ICE_SHELL` |
| `monsters[184].code` | [Громовой змей](Bosses.md#boss_thunder_wyrm) |
| `monsters[184].form` | `DRAGON` |
| `monsters[184].experience` | 60 |
| `monsters[184].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[184].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[184].stats.STOCK_HEALTH` | 30.361 |
| `monsters[184].stats.STOCK_ATTACK_PHYSICAL` | 2.647 |
| `monsters[184].stats.STOCK_ARMOR` | 50 |
| `monsters[184].stats.STOCK_HEALTH_REGEN` | 0.428 |
| `monsters[184].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[184].stats.STOCK_MANA` | 30 |
| `monsters[184].boss` | Да |
| `monsters[184].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[184].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_STRONG](Modifiers-MONSTER.md#mob_strong) |
| `monsters[184].tables` | [boss:BOSS_THUNDER_WYRM](Tables-TEMPLATE.md#boss-boss_thunder_wyrm) |
| `monsters[185].code` | [Хранитель моста](Bosses.md#boss_bridge_keeper) |
| `monsters[185].form` | `CONSTRUCT` |
| `monsters[185].experience` | 60 |
| `monsters[185].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[185].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[185].stats.STOCK_HEALTH` | 30.361 |
| `monsters[185].stats.STOCK_ATTACK_PHYSICAL` | 2.377 |
| `monsters[185].stats.STOCK_ARMOR` | 90 |
| `monsters[185].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[185].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[185].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[185].stats.STOCK_MANA` | 30 |
| `monsters[185].boss` | Да |
| `monsters[185].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[185].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_STEADFAST](Modifiers-MONSTER.md#mob_steadfast) |
| `monsters[185].tables` | [boss:BOSS_BRIDGE_KEEPER](Tables-TEMPLATE.md#boss-boss_bridge_keeper) |
| `monsters[185].trait` | `TRAIT_UNSHAKEN` |
| `monsters[186].code` | [Зеркальный рыцарь](Bosses.md#boss_mirror_knight) |
| `monsters[186].form` | `HUMANOID` |
| `monsters[186].experience` | 60 |
| `monsters[186].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[186].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[186].stats.STOCK_HEALTH` | 30.361 |
| `monsters[186].stats.STOCK_ATTACK_PHYSICAL` | 3.451 |
| `monsters[186].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[186].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[186].stats.STOCK_MANA` | 30 |
| `monsters[186].boss` | Да |
| `monsters[186].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[186].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_EVASIVE](Modifiers-MONSTER.md#mob_evasive) |
| `monsters[186].tables` | [boss:BOSS_MIRROR_KNIGHT](Tables-TEMPLATE.md#boss-boss_mirror_knight) |
| `monsters[187].code` | [Тиран шпиля](Bosses.md#boss_spire_tyrant) |
| `monsters[187].form` | `GOLEM` |
| `monsters[187].experience` | 60 |
| `monsters[187].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[187].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[187].stats.STOCK_HEALTH` | 30.361 |
| `monsters[187].stats.STOCK_ATTACK_PHYSICAL` | 2.303 |
| `monsters[187].stats.STOCK_ATTACK_FIRE` | 1.045 |
| `monsters[187].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[187].stats.STOCK_ARMOR` | 90 |
| `monsters[187].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[187].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[187].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[187].stats.STOCK_MANA` | 30 |
| `monsters[187].boss` | Да |
| `monsters[187].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[187].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_JUGGERNAUT](Modifiers-MONSTER.md#mob_juggernaut) |
| `monsters[188].code` | [Страж колоколов](Bosses.md#boss_bell_warden) |
| `monsters[188].form` | `CONSTRUCT` |
| `monsters[188].experience` | 60 |
| `monsters[188].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[188].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[188].stats.STOCK_HEALTH` | 30.361 |
| `monsters[188].stats.STOCK_ATTACK_PHYSICAL` | 2.477 |
| `monsters[188].stats.STOCK_ATTACK_FIRE` | 1.12 |
| `monsters[188].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[188].stats.STOCK_ARMOR` | 90 |
| `monsters[188].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[188].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[188].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[188].stats.STOCK_MANA` | 30 |
| `monsters[188].boss` | Да |
| `monsters[188].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[188].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_FIREPROOF](Modifiers-MONSTER.md#mob_fireproof) |
| `monsters[188].trait` | `TRAIT_UNSHAKEN` |
| `monsters[189].code` | [Коронованный осколок](Bosses.md#boss_crown_shard) |
| `monsters[189].form` | `CRYSTAL` |
| `monsters[189].experience` | 60 |
| `monsters[189].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[189].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[189].stats.STOCK_HEALTH` | 30.361 |
| `monsters[189].stats.STOCK_ATTACK_PHYSICAL` | 3.491 |
| `monsters[189].stats.STOCK_EVASION` | 70 |
| `monsters[189].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[189].stats.STOCK_MOVEMENT_SPEED` | 15 |
| `monsters[189].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[189].stats.STOCK_MANA` | 30 |
| `monsters[189].boss` | Да |
| `monsters[189].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[189].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_SWIFT](Modifiers-MONSTER.md#mob_swift) |
| `monsters[189].trait` | `TRAIT_ICE_SHELL` |
| `monsters[190].code` | [Облачный маршал](Bosses.md#boss_cloud_marshal) |
| `monsters[190].form` | `ANGEL` |
| `monsters[190].experience` | 60 |
| `monsters[190].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[190].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[190].stats.STOCK_HEALTH` | 19.085 |
| `monsters[190].stats.STOCK_ENERGY_SHIELD` | 11.276 |
| `monsters[190].stats.STOCK_ATTACK_LIGHTNING` | 2.6 |
| `monsters[190].stats.STOCK_RESIST_LIGHTNING` | 70 |
| `monsters[190].stats.STOCK_ATTACK_MAGICAL` | 2.034 |
| `monsters[190].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[190].stats.STOCK_MANA` | 55 |
| `monsters[190].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[190].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[190].boss` | Да |
| `monsters[190].skills` | [Разряд](Monster-skills.md#mob_zap); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[190].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_SHOCKING](Modifiers-MONSTER.md#mob_shocking) |
| `monsters[190].trait` | `TRAIT_GROUNDED` |
| `monsters[191].code` | [Титан бури](Bosses.md#boss_storm_titan) |
| `monsters[191].form` | `BRUTE` |
| `monsters[191].experience` | 60 |
| `monsters[191].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[191].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[191].stats.STOCK_HEALTH` | 30.361 |
| `monsters[191].stats.STOCK_ATTACK_PHYSICAL` | 3.105 |
| `monsters[191].stats.STOCK_ATTACK_FIRE` | 3.105 |
| `monsters[191].stats.STOCK_RESIST_FIRE` | 60 |
| `monsters[191].stats.STOCK_IGNITE_CHANCE` | 25 |
| `monsters[191].stats.STOCK_PENETRATE_ELEMENTAL` | 10 |
| `monsters[191].stats.STOCK_MANA` | 30 |
| `monsters[191].boss` | Да |
| `monsters[191].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[191].fixed` | [MOB_BURNING](Modifiers-MONSTER.md#mob_burning); [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord) |
| `monsters[192].code` | [Призменный охотник](Bosses.md#boss_prism_hunter) |
| `monsters[192].form` | `CRYSTAL` |
| `monsters[192].experience` | 60 |
| `monsters[192].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[192].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[192].stats.STOCK_HEALTH` | 30.361 |
| `monsters[192].stats.STOCK_ATTACK_PHYSICAL` | 4.718 |
| `monsters[192].stats.STOCK_EVASION` | 80 |
| `monsters[192].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[192].stats.STOCK_MOVEMENT_SPEED` | 20 |
| `monsters[192].stats.STOCK_MANA` | 30 |
| `monsters[192].boss` | Да |
| `monsters[192].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[192].fixed` | [MOB_HASTED](Modifiers-MONSTER.md#mob_hasted); [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner) |
| `monsters[192].tables` | [boss:BOSS_PRISM_HUNTER](Tables-TEMPLATE.md#boss-boss_prism_hunter) |
| `monsters[192].trait` | `TRAIT_ICE_SHELL` |
| `monsters[193].code` | [Машина планетария](Bosses.md#boss_orrery_engine) |
| `monsters[193].form` | `CONSTRUCT` |
| `monsters[193].experience` | 60 |
| `monsters[193].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[193].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[193].stats.STOCK_HEALTH` | 30.361 |
| `monsters[193].stats.STOCK_ATTACK_PHYSICAL` | 2.303 |
| `monsters[193].stats.STOCK_ATTACK_FIRE` | 1.045 |
| `monsters[193].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[193].stats.STOCK_ARMOR` | 90 |
| `monsters[193].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[193].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[193].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[193].stats.STOCK_MANA` | 30 |
| `monsters[193].boss` | Да |
| `monsters[193].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[193].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_JUGGERNAUT](Modifiers-MONSTER.md#mob_juggernaut) |
| `monsters[193].tables` | [boss:BOSS_ORRERY_ENGINE](Tables-TEMPLATE.md#boss-boss_orrery_engine) |
| `monsters[193].trait` | `TRAIT_UNSHAKEN` |
| `monsters[194].code` | [Недвижное око](Bosses.md#boss_still_eye) |
| `monsters[194].form` | `WRAITH` |
| `monsters[194].experience` | 60 |
| `monsters[194].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[194].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[194].stats.STOCK_HEALTH` | 19.085 |
| `monsters[194].stats.STOCK_ENERGY_SHIELD` | 11.276 |
| `monsters[194].stats.STOCK_ATTACK_LIGHTNING` | 2.6 |
| `monsters[194].stats.STOCK_RESIST_LIGHTNING` | 70 |
| `monsters[194].stats.STOCK_ATTACK_MAGICAL` | 2.034 |
| `monsters[194].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[194].stats.STOCK_MANA` | 55 |
| `monsters[194].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[194].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[194].boss` | Да |
| `monsters[194].skills` | [Разряд](Monster-skills.md#mob_zap); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[194].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_SHOCKING](Modifiers-MONSTER.md#mob_shocking) |
| `monsters[194].tables` | [boss:BOSS_STILL_EYE](Tables-TEMPLATE.md#boss-boss_still_eye) |
| `monsters[195].code` | [Стеклянный хор](Bosses.md#boss_glass_choir) |
| `monsters[195].form` | `WRAITH` |
| `monsters[195].experience` | 60 |
| `monsters[195].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[195].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[195].stats.STOCK_HEALTH` | 18.147 |
| `monsters[195].stats.STOCK_ENERGY_SHIELD` | 12.214 |
| `monsters[195].stats.STOCK_ATTACK_COLD` | 3.138 |
| `monsters[195].stats.STOCK_RESIST_COLD` | 70 |
| `monsters[195].stats.STOCK_ATTACK_MAGICAL` | 2.095 |
| `monsters[195].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[195].stats.STOCK_MANA` | 60 |
| `monsters[195].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[195].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[195].boss` | Да |
| `monsters[195].skills` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt) |
| `monsters[195].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_CHILLING](Modifiers-MONSTER.md#mob_chilling) |
| `monsters[196].code` | [Владыка разбитого неба](Bosses.md#boss_sky_sovereign) |
| `monsters[196].form` | `DRAGON` |
| `monsters[196].experience` | 60 |
| `monsters[196].loot` | [loot:BOSS_7](Tables-LOOT.md#loot-boss_7) |
| `monsters[196].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[196].stats.STOCK_HEALTH` | 34.916 |
| `monsters[196].stats.STOCK_ATTACK_PHYSICAL` | 2.397 |
| `monsters[196].stats.STOCK_ATTACK_CHAOS` | 1.675 |
| `monsters[196].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[196].stats.STOCK_ARMOR` | 50 |
| `monsters[196].stats.STOCK_HEALTH_REGEN` | 0.346 |
| `monsters[196].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[196].stats.STOCK_RESIST_ALL` | 15 |
| `monsters[196].stats.STOCK_MANA` | 30 |
| `monsters[196].boss` | Да |
| `monsters[196].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[196].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread) |
| `monsters[196].tables` | [boss:BOSS_SKY_SOVEREIGN](Tables-TEMPLATE.md#boss-boss_sky_sovereign) |
| `monsters[197].code` | [Утопленник-легионер](Monsters.md#drowned_legionary) |
| `monsters[197].form` | `UNDEAD` |
| `monsters[197].experience` | 30 |
| `monsters[197].loot` | [loot:T8](Tables-LOOT.md#loot-t8) |
| `monsters[197].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[197].stats.STOCK_HEALTH` | 32.1 |
| `monsters[197].stats.STOCK_ATTACK_PHYSICAL` | 7.49 |
| `monsters[197].stats.STOCK_ARMOR` | 60 |
| `monsters[197].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[197].trait` | `TRAIT_SKILL_BONE_ARMOUR` |
| `monsters[198].code` | [Коралловый исполин](Monsters.md#coral_behemoth) |
| `monsters[198].form` | `CRAB` |
| `monsters[198].experience` | 32 |
| `monsters[198].loot` | [loot:T8](Tables-LOOT.md#loot-t8) |
| `monsters[198].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[198].stats.STOCK_HEALTH` | 40.66 |
| `monsters[198].stats.STOCK_ATTACK_PHYSICAL` | 8.56 |
| `monsters[198].stats.STOCK_ARMOR` | 90 |
| `monsters[198].trait` | `TRAIT_THORNS` |
| `monsters[199].code` | [Бездонный угорь](Monsters.md#abyssal_eel) |
| `monsters[199].form` | `SERPENT` |
| `monsters[199].experience` | 29 |
| `monsters[199].loot` | [loot:T8](Tables-LOOT.md#loot-t8) |
| `monsters[199].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[199].stats.STOCK_HEALTH` | 21.4 |
| `monsters[199].stats.STOCK_ATTACK_LIGHTNING` | 6.42 |
| `monsters[199].stats.STOCK_EVASION` | 40 |
| `monsters[199].trait` | `TRAIT_STORM_BURST` |
| `monsters[200].code` | [Приливный конструкт](Monsters.md#tide_construct) |
| `monsters[200].form` | `CONSTRUCT` |
| `monsters[200].experience` | 32 |
| `monsters[200].loot` | [loot:T8](Tables-LOOT.md#loot-t8) |
| `monsters[200].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[200].stats.STOCK_HEALTH` | 38.52 |
| `monsters[200].stats.STOCK_ATTACK_COLD` | 7.49 |
| `monsters[200].stats.STOCK_ARMOR` | 70 |
| `monsters[200].stats.STOCK_RESIST_COLD` | 60 |
| `monsters[200].trait` | `TRAIT_SKILL_FROST_BREATH` |
| `monsters[201].code` | [Жемчужная сирена](Monsters.md#pearl_siren) |
| `monsters[201].form` | `ANGEL` |
| `monsters[201].experience` | 29 |
| `monsters[201].loot` | [loot:T8](Tables-LOOT.md#loot-t8) |
| `monsters[201].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[201].stats.STOCK_HEALTH` | 17.12 |
| `monsters[201].stats.STOCK_ENERGY_SHIELD` | 18 |
| `monsters[201].stats.STOCK_ATTACK_COLD` | 3.21 |
| `monsters[201].stats.STOCK_ATTACK_MAGICAL` | 5.89 |
| `monsters[201].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[201].stats.STOCK_MANA` | 45 |
| `monsters[201].stats.STOCK_MANA_REGEN` | 3 |
| `monsters[201].trait` | `TRAIT_SKILL_ENFEEBLE` |
| `monsters[202].code` | [Глубинный скрытень](Monsters.md#deep_lurker) |
| `monsters[202].form` | `SLUG` |
| `monsters[202].experience` | 29 |
| `monsters[202].loot` | [loot:T8](Tables-LOOT.md#loot-t8) |
| `monsters[202].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[202].stats.STOCK_HEALTH` | 36.38 |
| `monsters[202].stats.STOCK_ATTACK_CHAOS` | 6.42 |
| `monsters[202].stats.STOCK_RESIST_CHAOS` | 50 |
| `monsters[202].stats.STOCK_HEALTH_REGEN` | 1.5 |
| `monsters[202].trait` | `TRAIT_SKILL_CHAOS_RING` |
| `monsters[203].code` | [Солёный дрейк](Monsters.md#brine_drake) |
| `monsters[203].form` | `DRAGON` |
| `monsters[203].experience` | 32 |
| `monsters[203].loot` | [loot:T8](Tables-LOOT.md#loot-t8) |
| `monsters[203].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[203].stats.STOCK_HEALTH` | 32.1 |
| `monsters[203].stats.STOCK_ATTACK_COLD` | 8.56 |
| `monsters[203].stats.STOCK_RESIST_COLD` | 60 |
| `monsters[203].trait` | `TRAIT_SKILL_FROST_BREATH` |
| `monsters[204].code` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `monsters[204].form` | `SERPENT` |
| `monsters[204].experience` | 310 |
| `monsters[204].loot` | [loot:CORRUPT_8](Tables-LOOT.md#loot-corrupt_8) |
| `monsters[204].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[204].stats.STOCK_HEALTH` | 230 |
| `monsters[204].stats.STOCK_ATTACK_COLD` | 24 |
| `monsters[204].stats.STOCK_ARMOR` | 120 |
| `monsters[204].stats.STOCK_RESIST_COLD` | 75 |
| `monsters[204].corrupted` | Да |
| `monsters[204].fixed` | [MOB_FREEZING](Modifiers-MONSTER.md#mob_freezing); [MOB_STURDY](Modifiers-MONSTER.md#mob_sturdy) |
| `monsters[205].code` | [Претор врат](Bosses.md#boss_gate_praetor) |
| `monsters[205].form` | `UNDEAD` |
| `monsters[205].experience` | 60 |
| `monsters[205].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[205].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[205].stats.STOCK_HEALTH` | 31.54 |
| `monsters[205].stats.STOCK_ATTACK_PHYSICAL` | 3.209 |
| `monsters[205].stats.STOCK_ATTACK_COLD` | 2.136 |
| `monsters[205].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[205].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[205].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[205].stats.STOCK_MANA` | 30 |
| `monsters[205].boss` | Да |
| `monsters[205].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[205].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_DRAINING](Modifiers-MONSTER.md#mob_draining) |
| `monsters[205].tables` | [boss:BOSS_GATE_PRAETOR](Tables-TEMPLATE.md#boss-boss_gate_praetor) |
| `monsters[206].code` | [Коралловая королева](Bosses.md#boss_coral_queen) |
| `monsters[206].form` | `CRAB` |
| `monsters[206].experience` | 60 |
| `monsters[206].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[206].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[206].stats.STOCK_HEALTH` | 31.54 |
| `monsters[206].stats.STOCK_ATTACK_PHYSICAL` | 3.82 |
| `monsters[206].stats.STOCK_ARMOR` | 55 |
| `monsters[206].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[206].stats.STOCK_STUN_THRESHOLD` | 10 |
| `monsters[206].stats.STOCK_MANA` | 30 |
| `monsters[206].boss` | Да |
| `monsters[206].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[206].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_STRONG](Modifiers-MONSTER.md#mob_strong) |
| `monsters[206].tables` | [boss:BOSS_CORAL_QUEEN](Tables-TEMPLATE.md#boss-boss_coral_queen) |
| `monsters[207].code` | [Страж хранилищ](Bosses.md#boss_vault_warden) |
| `monsters[207].form` | `CONSTRUCT` |
| `monsters[207].experience` | 60 |
| `monsters[207].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[207].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[207].stats.STOCK_HEALTH` | 31.54 |
| `monsters[207].stats.STOCK_ATTACK_PHYSICAL` | 2.392 |
| `monsters[207].stats.STOCK_ATTACK_FIRE` | 1.085 |
| `monsters[207].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[207].stats.STOCK_ARMOR` | 90 |
| `monsters[207].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[207].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[207].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[207].stats.STOCK_MANA` | 30 |
| `monsters[207].boss` | Да |
| `monsters[207].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[207].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [MOB_JUGGERNAUT](Modifiers-MONSTER.md#mob_juggernaut) |
| `monsters[207].tables` | [boss:BOSS_VAULT_WARDEN](Tables-TEMPLATE.md#boss-boss_vault_warden) |
| `monsters[208].code` | [Утонувший оратор](Bosses.md#boss_drowned_orator) |
| `monsters[208].form` | `UNDEAD` |
| `monsters[208].experience` | 60 |
| `monsters[208].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[208].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[208].stats.STOCK_HEALTH` | 31.54 |
| `monsters[208].stats.STOCK_ATTACK_PHYSICAL` | 2.757 |
| `monsters[208].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[208].stats.STOCK_ARMOR` | 40 |
| `monsters[208].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[208].stats.STOCK_MANA` | 30 |
| `monsters[208].boss` | Да |
| `monsters[208].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[208].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_CRUSHING](Modifiers-MONSTER.md#mob_crushing) |
| `monsters[208].tables` | [boss:BOSS_DROWNED_ORATOR](Tables-TEMPLATE.md#boss-boss_drowned_orator) |
| `monsters[209].code` | [Пожиратель жемчуга](Bosses.md#boss_pearl_eater) |
| `monsters[209].form` | `SERPENT` |
| `monsters[209].experience` | 60 |
| `monsters[209].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[209].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[209].stats.STOCK_HEALTH` | 31.54 |
| `monsters[209].stats.STOCK_ATTACK_PHYSICAL` | 2.565 |
| `monsters[209].stats.STOCK_ATTACK_CHAOS` | 2.252 |
| `monsters[209].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[209].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[209].stats.STOCK_MANA` | 30 |
| `monsters[209].boss` | Да |
| `monsters[209].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[209].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_TOXIC](Modifiers-MONSTER.md#mob_toxic) |
| `monsters[209].tables` | [boss:BOSS_PEARL_EATER](Tables-TEMPLATE.md#boss-boss_pearl_eater) |
| `monsters[210].code` | [Ужас акведука](Bosses.md#boss_aqueduct_horror) |
| `monsters[210].form` | `SLUG` |
| `monsters[210].experience` | 60 |
| `monsters[210].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[210].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[210].stats.STOCK_HEALTH` | 31.54 |
| `monsters[210].stats.STOCK_ATTACK_PHYSICAL` | 2.14 |
| `monsters[210].stats.STOCK_HEALTH_REGEN` | 0.474 |
| `monsters[210].stats.STOCK_ARMOR` | 40 |
| `monsters[210].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[210].stats.STOCK_MANA` | 30 |
| `monsters[210].boss` | Да |
| `monsters[210].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[210].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_HUNGERING](Modifiers-MONSTER.md#mob_hungering) |
| `monsters[211].code` | [Трибун легиона](Bosses.md#boss_legion_tribune) |
| `monsters[211].form` | `HUMANOID` |
| `monsters[211].experience` | 60 |
| `monsters[211].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[211].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[211].stats.STOCK_HEALTH` | 31.54 |
| `monsters[211].stats.STOCK_ATTACK_PHYSICAL` | 2.997 |
| `monsters[211].stats.STOCK_ATTACK_LIGHTNING` | 1.797 |
| `monsters[211].stats.STOCK_RESIST_LIGHTNING` | 40 |
| `monsters[211].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[211].stats.STOCK_ATTACK_MAGICAL` | 2.085 |
| `monsters[211].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[211].stats.STOCK_MANA` | 60 |
| `monsters[211].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[211].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[211].boss` | Да |
| `monsters[211].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[211].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_SHOCKING](Modifiers-MONSTER.md#mob_shocking) |
| `monsters[211].tables` | [boss:BOSS_LEGION_TRIBUNE](Tables-TEMPLATE.md#boss-boss_legion_tribune) |
| `monsters[212].code` | [Жрец водорослей](Bosses.md#boss_kelp_priest) |
| `monsters[212].form` | `ANGEL` |
| `monsters[212].experience` | 60 |
| `monsters[212].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[212].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[212].stats.STOCK_HEALTH` | 19.708 |
| `monsters[212].stats.STOCK_ENERGY_SHIELD` | 11.832 |
| `monsters[212].stats.STOCK_ATTACK_CHAOS` | 2.414 |
| `monsters[212].stats.STOCK_RESIST_CHAOS` | 70 |
| `monsters[212].stats.STOCK_ATTACK_MAGICAL` | 1.877 |
| `monsters[212].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[212].stats.STOCK_MANA` | 55 |
| `monsters[212].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[212].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[212].boss` | Да |
| `monsters[212].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit); [Исцеление](Monster-skills.md#mob_heal) |
| `monsters[212].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_WITHERING](Modifiers-MONSTER.md#mob_withering) |
| `monsters[212].tables` | [boss:BOSS_KELP_PRIEST](Tables-TEMPLATE.md#boss-boss_kelp_priest) |
| `monsters[213].code` | [То, что в цистерне](Bosses.md#boss_cistern_thing) |
| `monsters[213].form` | `DEMON` |
| `monsters[213].experience` | 60 |
| `monsters[213].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[213].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[213].stats.STOCK_HEALTH` | 31.54 |
| `monsters[213].stats.STOCK_ATTACK_PHYSICAL` | 2.748 |
| `monsters[213].stats.STOCK_ATTACK_FIRE` | 1.928 |
| `monsters[213].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[213].stats.STOCK_ARMOR` | 50 |
| `monsters[213].stats.STOCK_HEALTH_REGEN` | 0.424 |
| `monsters[213].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[213].stats.STOCK_MANA` | 30 |
| `monsters[213].boss` | Да |
| `monsters[213].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[213].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_WARDED](Modifiers-MONSTER.md#mob_warded) |
| `monsters[214].code` | [Рыночный барон](Bosses.md#boss_market_baron) |
| `monsters[214].form` | `BRUTE` |
| `monsters[214].experience` | 60 |
| `monsters[214].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[214].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[214].stats.STOCK_HEALTH` | 31.54 |
| `monsters[214].stats.STOCK_ATTACK_PHYSICAL` | 2.265 |
| `monsters[214].stats.STOCK_ATTACK_CHAOS` | 1.026 |
| `monsters[214].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[214].stats.STOCK_HEALTH_REGEN` | 0.459 |
| `monsters[214].stats.STOCK_STUN_THRESHOLD` | 15 |
| `monsters[214].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[214].stats.STOCK_MANA` | 30 |
| `monsters[214].boss` | Да |
| `monsters[214].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[214].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_WITHERING](Modifiers-MONSTER.md#mob_withering) |
| `monsters[215].code` | [Пасть атолла](Bosses.md#boss_atoll_maw) |
| `monsters[215].form` | `SERPENT` |
| `monsters[215].experience` | 60 |
| `monsters[215].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[215].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[215].stats.STOCK_HEALTH` | 31.54 |
| `monsters[215].stats.STOCK_ATTACK_PHYSICAL` | 2.193 |
| `monsters[215].stats.STOCK_ATTACK_COLD` | 1.907 |
| `monsters[215].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[215].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[215].stats.STOCK_MANA` | 30 |
| `monsters[215].boss` | Да |
| `monsters[215].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[215].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_CHILLING](Modifiers-MONSTER.md#mob_chilling) |
| `monsters[216].code` | [Инженер приливов](Bosses.md#boss_tide_engineer) |
| `monsters[216].form` | `CONSTRUCT` |
| `monsters[216].experience` | 60 |
| `monsters[216].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[216].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[216].stats.STOCK_HEALTH` | 31.54 |
| `monsters[216].stats.STOCK_ATTACK_PHYSICAL` | 2.621 |
| `monsters[216].stats.STOCK_ATTACK_LIGHTNING` | 1.2 |
| `monsters[216].stats.STOCK_RESIST_LIGHTNING` | 40 |
| `monsters[216].stats.STOCK_ARMOR` | 90 |
| `monsters[216].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[216].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[216].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[216].stats.STOCK_MANA` | 30 |
| `monsters[216].boss` | Да |
| `monsters[216].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[216].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_JUGGERNAUT](Modifiers-MONSTER.md#mob_juggernaut) |
| `monsters[217].code` | [Хозяйка терм](Bosses.md#boss_bath_matron) |
| `monsters[217].form` | `WRAITH` |
| `monsters[217].experience` | 60 |
| `monsters[217].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[217].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[217].stats.STOCK_HEALTH` | 18.852 |
| `monsters[217].stats.STOCK_ENERGY_SHIELD` | 12.688 |
| `monsters[217].stats.STOCK_ATTACK_COLD` | 3.26 |
| `monsters[217].stats.STOCK_RESIST_COLD` | 70 |
| `monsters[217].stats.STOCK_ATTACK_MAGICAL` | 2.177 |
| `monsters[217].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[217].stats.STOCK_MANA` | 60 |
| `monsters[217].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[217].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[217].boss` | Да |
| `monsters[217].skills` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt) |
| `monsters[217].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_CHILLING](Modifiers-MONSTER.md#mob_chilling) |
| `monsters[218].code` | [Дрейк впадины](Bosses.md#boss_trench_drake) |
| `monsters[218].form` | `DRAGON` |
| `monsters[218].experience` | 60 |
| `monsters[218].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[218].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[218].stats.STOCK_HEALTH` | 31.54 |
| `monsters[218].stats.STOCK_ATTACK_PHYSICAL` | 2.727 |
| `monsters[218].stats.STOCK_ATTACK_LIGHTNING` | 1.907 |
| `monsters[218].stats.STOCK_RESIST_LIGHTNING` | 40 |
| `monsters[218].stats.STOCK_ARMOR` | 50 |
| `monsters[218].stats.STOCK_HEALTH_REGEN` | 0.411 |
| `monsters[218].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[218].stats.STOCK_MANA` | 30 |
| `monsters[218].boss` | Да |
| `monsters[218].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[218].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_STORMBORN](Modifiers-MONSTER.md#mob_stormborn) |
| `monsters[218].tables` | [boss:BOSS_TRENCH_DRAKE](Tables-TEMPLATE.md#boss-boss_trench_drake) |
| `monsters[219].code` | [Утонувший император](Bosses.md#boss_drowned_emperor) |
| `monsters[219].form` | `UNDEAD` |
| `monsters[219].experience` | 60 |
| `monsters[219].loot` | [loot:BOSS_8](Tables-LOOT.md#loot-boss_8) |
| `monsters[219].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[219].stats.STOCK_HEALTH` | 36.271 |
| `monsters[219].stats.STOCK_ATTACK_PHYSICAL` | 2.576 |
| `monsters[219].stats.STOCK_ATTACK_FIRE` | 1.724 |
| `monsters[219].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[219].stats.STOCK_LEECH_ALL` | 3 |
| `monsters[219].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[219].stats.STOCK_RESIST_ALL` | 15 |
| `monsters[219].stats.STOCK_MANA` | 30 |
| `monsters[219].boss` | Да |
| `monsters[219].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[219].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord) |
| `monsters[219].tables` | [boss:BOSS_DROWNED_EMPEROR](Tables-TEMPLATE.md#boss-boss_drowned_emperor) |
| `monsters[220].code` | [Божественный страж](Monsters.md#divine_warden) |
| `monsters[220].form` | `CONSTRUCT` |
| `monsters[220].experience` | 31 |
| `monsters[220].loot` | [loot:T9](Tables-LOOT.md#loot-t9) |
| `monsters[220].stats.STOCK_ATTACK_SPEED` | 0.9 |
| `monsters[220].stats.STOCK_HEALTH` | 42.18 |
| `monsters[220].stats.STOCK_ATTACK_PHYSICAL` | 6.66 |
| `monsters[220].stats.STOCK_ATTACK_FIRE` | 3.33 |
| `monsters[220].stats.STOCK_ARMOR` | 90 |
| `monsters[220].stats.STOCK_RESIST_ALL` | 25 |
| `monsters[220].trait` | `TRAIT_LAST_STAND` |
| `monsters[221].code` | [Астральный зверь](Monsters.md#astral_beast) |
| `monsters[221].form` | `CRYSTAL` |
| `monsters[221].experience` | 31 |
| `monsters[221].loot` | [loot:T9](Tables-LOOT.md#loot-t9) |
| `monsters[221].stats.STOCK_ATTACK_SPEED` | 1.5 |
| `monsters[221].stats.STOCK_HEALTH` | 26.64 |
| `monsters[221].stats.STOCK_ATTACK_PHYSICAL` | 5.55 |
| `monsters[221].stats.STOCK_ATTACK_LIGHTNING` | 4.44 |
| `monsters[221].stats.STOCK_EVASION` | 50 |
| `monsters[221].trait` | `TRAIT_SKILL_THUNDERCLAP` |
| `monsters[222].code` | [Клятвенный фанатик](Monsters.md#godsworn_zealot) |
| `monsters[222].form` | `HUMANOID` |
| `monsters[222].experience` | 33 |
| `monsters[222].loot` | [loot:T9](Tables-LOOT.md#loot-t9) |
| `monsters[222].stats.STOCK_ATTACK_SPEED` | 1.3 |
| `monsters[222].stats.STOCK_HEALTH` | 31.08 |
| `monsters[222].stats.STOCK_ATTACK_PHYSICAL` | 6.66 |
| `monsters[222].stats.STOCK_ATTACK_FIRE` | 4.44 |
| `monsters[222].stats.STOCK_BLOCK_CHANCE` | 20 |
| `monsters[222].trait` | `TRAIT_HOWL` |
| `monsters[223].code` | [Тень забвения](Monsters.md#oblivion_shade) |
| `monsters[223].form` | `WRAITH` |
| `monsters[223].experience` | 34 |
| `monsters[223].loot` | [loot:T9](Tables-LOOT.md#loot-t9) |
| `monsters[223].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[223].stats.STOCK_HEALTH` | 17.76 |
| `monsters[223].stats.STOCK_ENERGY_SHIELD` | 18 |
| `monsters[223].stats.STOCK_ATTACK_CHAOS` | 4.44 |
| `monsters[223].stats.STOCK_ATTACK_MAGICAL` | 6.11 |
| `monsters[223].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[223].stats.STOCK_MANA` | 45 |
| `monsters[223].stats.STOCK_MANA_REGEN` | 3 |
| `monsters[223].stats.STOCK_RESIST_CHAOS` | 60 |
| `monsters[223].trait` | `TRAIT_PENETRATING` |
| `monsters[224].code` | [Звёздный дракон](Monsters.md#star_dragon) |
| `monsters[224].form` | `DRAGON` |
| `monsters[224].experience` | 32 |
| `monsters[224].loot` | [loot:T9](Tables-LOOT.md#loot-t9) |
| `monsters[224].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[224].stats.STOCK_HEALTH` | 37.74 |
| `monsters[224].stats.STOCK_ATTACK_FIRE` | 5.55 |
| `monsters[224].stats.STOCK_ATTACK_LIGHTNING` | 5.55 |
| `monsters[224].stats.STOCK_RESIST_ALL` | 20 |
| `monsters[224].trait` | `TRAIT_PENETRATING` |
| `monsters[225].code` | [Падший архонт](Monsters.md#fallen_archon) |
| `monsters[225].form` | `ANGEL` |
| `monsters[225].experience` | 33 |
| `monsters[225].loot` | [loot:T9](Tables-LOOT.md#loot-t9) |
| `monsters[225].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[225].stats.STOCK_HEALTH` | 19.98 |
| `monsters[225].stats.STOCK_ENERGY_SHIELD` | 20 |
| `monsters[225].stats.STOCK_ATTACK_FIRE` | 3.33 |
| `monsters[225].stats.STOCK_ATTACK_MAGICAL` | 6.66 |
| `monsters[225].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[225].stats.STOCK_MANA` | 50 |
| `monsters[225].stats.STOCK_MANA_REGEN` | 3 |
| `monsters[225].trait` | `TRAIT_SKILL_FIREBALL` |
| `monsters[226].code` | [Титан пустоты](Monsters.md#void_titan) |
| `monsters[226].form` | `BRUTE` |
| `monsters[226].experience` | 31 |
| `monsters[226].loot` | [loot:T9](Tables-LOOT.md#loot-t9) |
| `monsters[226].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[226].stats.STOCK_HEALTH` | 46.62 |
| `monsters[226].stats.STOCK_ATTACK_PHYSICAL` | 7.77 |
| `monsters[226].stats.STOCK_ATTACK_CHAOS` | 4.44 |
| `monsters[226].stats.STOCK_RESIST_CHAOS` | 60 |
| `monsters[226].trait` | `TRAIT_SKILL_SLAM` |
| `monsters[227].code` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `monsters[227].form` | `CONSTRUCT` |
| `monsters[227].experience` | 330 |
| `monsters[227].loot` | [loot:CORRUPT_9](Tables-LOOT.md#loot-corrupt_9) |
| `monsters[227].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[227].stats.STOCK_HEALTH` | 260 |
| `monsters[227].stats.STOCK_ATTACK_FIRE` | 16 |
| `monsters[227].stats.STOCK_ATTACK_CHAOS` | 12 |
| `monsters[227].stats.STOCK_ENERGY_SHIELD` | 90 |
| `monsters[227].stats.STOCK_RESIST_ALL` | 30 |
| `monsters[227].corrupted` | Да |
| `monsters[227].fixed` | [MOB_BURNING](Modifiers-MONSTER.md#mob_burning); [MOB_UNDYING](Modifiers-MONSTER.md#mob_undying) |
| `monsters[228].code` | [Страж порога](Bosses.md#boss_threshold_guardian) |
| `monsters[228].form` | `CONSTRUCT` |
| `monsters[228].experience` | 60 |
| `monsters[228].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[228].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[228].stats.STOCK_HEALTH` | 32.719 |
| `monsters[228].stats.STOCK_ATTACK_PHYSICAL` | 2.482 |
| `monsters[228].stats.STOCK_ATTACK_CHAOS` | 1.125 |
| `monsters[228].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[228].stats.STOCK_ARMOR` | 90 |
| `monsters[228].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[228].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[228].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[228].stats.STOCK_MANA` | 30 |
| `monsters[228].boss` | Да |
| `monsters[228].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[228].fixed` | [BOSS_PLAGUEBEARER](Modifiers-MONSTER.md#boss_plaguebearer); [MOB_TOXIC](Modifiers-MONSTER.md#mob_toxic) |
| `monsters[228].tables` | [boss:BOSS_THRESHOLD_GUARDIAN](Tables-TEMPLATE.md#boss-boss_threshold_guardian) |
| `monsters[229].code` | [Звёздный прилив](Bosses.md#boss_star_tide) |
| `monsters[229].form` | `CRYSTAL` |
| `monsters[229].experience` | 60 |
| `monsters[229].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[229].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[229].stats.STOCK_HEALTH` | 32.719 |
| `monsters[229].stats.STOCK_ATTACK_PHYSICAL` | 4.262 |
| `monsters[229].stats.STOCK_EVASION` | 70 |
| `monsters[229].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[229].stats.STOCK_MOVEMENT_SPEED` | 15 |
| `monsters[229].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[229].stats.STOCK_MANA` | 30 |
| `monsters[229].boss` | Да |
| `monsters[229].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[229].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_BERSERK](Modifiers-MONSTER.md#mob_berserk) |
| `monsters[230].code` | [Забвение](Bosses.md#boss_forgetting) |
| `monsters[230].form` | `WRAITH` |
| `monsters[230].experience` | 60 |
| `monsters[230].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[230].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[230].stats.STOCK_HEALTH` | 20.426 |
| `monsters[230].stats.STOCK_ENERGY_SHIELD` | 12.294 |
| `monsters[230].stats.STOCK_ATTACK_COLD` | 2.324 |
| `monsters[230].stats.STOCK_RESIST_COLD` | 70 |
| `monsters[230].stats.STOCK_ATTACK_MAGICAL` | 1.79 |
| `monsters[230].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[230].stats.STOCK_MANA` | 60 |
| `monsters[230].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[230].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[230].boss` | Да |
| `monsters[230].skills` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[230].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_MIRRORED](Modifiers-MONSTER.md#mob_mirrored) |
| `monsters[231].code` | [Судья душ](Bosses.md#boss_judge_of_souls) |
| `monsters[231].form` | `ANGEL` |
| `monsters[231].experience` | 60 |
| `monsters[231].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[231].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[231].stats.STOCK_HEALTH` | 19.557 |
| `monsters[231].stats.STOCK_ENERGY_SHIELD` | 13.163 |
| `monsters[231].stats.STOCK_ATTACK_COLD` | 3.382 |
| `monsters[231].stats.STOCK_RESIST_COLD` | 70 |
| `monsters[231].stats.STOCK_ATTACK_MAGICAL` | 2.258 |
| `monsters[231].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[231].stats.STOCK_MANA` | 60 |
| `monsters[231].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[231].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[231].boss` | Да |
| `monsters[231].skills` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt) |
| `monsters[231].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_CHILLING](Modifiers-MONSTER.md#mob_chilling) |
| `monsters[231].tables` | [boss:BOSS_JUDGE_OF_SOULS](Tables-TEMPLATE.md#boss-boss_judge_of_souls) |
| `monsters[232].code` | [Зверь созвездий](Bosses.md#boss_constellation_beast) |
| `monsters[232].form` | `CRYSTAL` |
| `monsters[232].experience` | 60 |
| `monsters[232].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[232].stats.STOCK_ATTACK_SPEED` | 1.6 |
| `monsters[232].stats.STOCK_HEALTH` | 32.719 |
| `monsters[232].stats.STOCK_ATTACK_PHYSICAL` | 4.262 |
| `monsters[232].stats.STOCK_EVASION` | 70 |
| `monsters[232].stats.STOCK_CRITICAL_CHANCE` | 10 |
| `monsters[232].stats.STOCK_MOVEMENT_SPEED` | 15 |
| `monsters[232].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[232].stats.STOCK_MANA` | 30 |
| `monsters[232].boss` | Да |
| `monsters[232].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsters[232].fixed` | [BOSS_EXECUTIONER](Modifiers-MONSTER.md#boss_executioner); [MOB_BERSERK](Modifiers-MONSTER.md#mob_berserk) |
| `monsters[233].code` | [Последний архивариус](Bosses.md#boss_archivist) |
| `monsters[233].form` | `WRAITH` |
| `monsters[233].experience` | 60 |
| `monsters[233].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[233].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[233].stats.STOCK_HEALTH` | 20.568 |
| `monsters[233].stats.STOCK_ENERGY_SHIELD` | 12.152 |
| `monsters[233].stats.STOCK_ATTACK_LIGHTNING` | 2.801 |
| `monsters[233].stats.STOCK_RESIST_LIGHTNING` | 70 |
| `monsters[233].stats.STOCK_ATTACK_MAGICAL` | 2.192 |
| `monsters[233].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[233].stats.STOCK_MANA` | 55 |
| `monsters[233].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[233].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[233].boss` | Да |
| `monsters[233].skills` | [Разряд](Monster-skills.md#mob_zap); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[233].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_SHOCKING](Modifiers-MONSTER.md#mob_shocking) |
| `monsters[234].code` | [Солнечный кузнец](Bosses.md#boss_sun_smith) |
| `monsters[234].form` | `CONSTRUCT` |
| `monsters[234].experience` | 60 |
| `monsters[234].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[234].stats.STOCK_ATTACK_SPEED` | 0.8 |
| `monsters[234].stats.STOCK_HEALTH` | 32.719 |
| `monsters[234].stats.STOCK_ATTACK_PHYSICAL` | 2.67 |
| `monsters[234].stats.STOCK_ATTACK_FIRE` | 1.207 |
| `monsters[234].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[234].stats.STOCK_ARMOR` | 90 |
| `monsters[234].stats.STOCK_BLOCK_CHANCE` | 25 |
| `monsters[234].stats.STOCK_STUN_THRESHOLD` | 22 |
| `monsters[234].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[234].stats.STOCK_MANA` | 30 |
| `monsters[234].boss` | Да |
| `monsters[234].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsters[234].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_FIREPROOF](Modifiers-MONSTER.md#mob_fireproof) |
| `monsters[234].tables` | [boss:BOSS_SUN_SMITH](Tables-TEMPLATE.md#boss-boss_sun_smith) |
| `monsters[235].code` | [Хранитель сада](Bosses.md#boss_orchard_keeper) |
| `monsters[235].form` | `ANGEL` |
| `monsters[235].experience` | 60 |
| `monsters[235].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[235].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[235].stats.STOCK_HEALTH` | 19.557 |
| `monsters[235].stats.STOCK_ENERGY_SHIELD` | 13.163 |
| `monsters[235].stats.STOCK_ATTACK_COLD` | 3.382 |
| `monsters[235].stats.STOCK_RESIST_COLD` | 70 |
| `monsters[235].stats.STOCK_ATTACK_MAGICAL` | 2.258 |
| `monsters[235].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[235].stats.STOCK_MANA` | 60 |
| `monsters[235].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[235].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[235].boss` | Да |
| `monsters[235].skills` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt) |
| `monsters[235].fixed` | [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [MOB_CHILLING](Modifiers-MONSTER.md#mob_chilling) |
| `monsters[236].code` | [Пустой бог](Bosses.md#boss_hollow_god) |
| `monsters[236].form` | `DEMON` |
| `monsters[236].experience` | 60 |
| `monsters[236].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[236].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[236].stats.STOCK_HEALTH` | 32.719 |
| `monsters[236].stats.STOCK_ATTACK_PHYSICAL` | 2.794 |
| `monsters[236].stats.STOCK_ARMOR` | 50 |
| `monsters[236].stats.STOCK_HEALTH_REGEN` | 0.412 |
| `monsters[236].stats.STOCK_BLEED_CHANCE` | 20 |
| `monsters[236].stats.STOCK_MANA` | 30 |
| `monsters[236].boss` | Да |
| `monsters[236].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsters[236].fixed` | [BOSS_WARLORD](Modifiers-MONSTER.md#boss_warlord); [MOB_RELENTLESS](Modifiers-MONSTER.md#mob_relentless) |
| `monsters[236].tables` | [boss:BOSS_HOLLOW_GOD](Tables-TEMPLATE.md#boss-boss_hollow_god) |
| `monsters[237].code` | [Аватар войны](Bosses.md#boss_war_avatar) |
| `monsters[237].form` | `HUMANOID` |
| `monsters[237].experience` | 60 |
| `monsters[237].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[237].stats.STOCK_ATTACK_SPEED` | 1.1 |
| `monsters[237].stats.STOCK_HEALTH` | 32.719 |
| `monsters[237].stats.STOCK_ATTACK_PHYSICAL` | 3.129 |
| `monsters[237].stats.STOCK_ATTACK_COLD` | 1.88 |
| `monsters[237].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[237].stats.STOCK_CRITICAL_CHANCE` | 8 |
| `monsters[237].stats.STOCK_ATTACK_MAGICAL` | 2.19 |
| `monsters[237].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[237].stats.STOCK_MANA` | 60 |
| `monsters[237].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[237].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[237].boss` | Да |
| `monsters[237].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[237].fixed` | [BOSS_MIRROR_SHELL](Modifiers-MONSTER.md#boss_mirror_shell); [MOB_ARCANE](Modifiers-MONSTER.md#mob_arcane) |
| `monsters[237].tables` | [boss:BOSS_WAR_AVATAR](Tables-TEMPLATE.md#boss-boss_war_avatar) |
| `monsters[238].code` | [Мать пустоты](Bosses.md#boss_void_mother) |
| `monsters[238].form` | `SPIDER` |
| `monsters[238].experience` | 60 |
| `monsters[238].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[238].stats.STOCK_ATTACK_SPEED` | 1.4 |
| `monsters[238].stats.STOCK_HEALTH` | 32.719 |
| `monsters[238].stats.STOCK_ATTACK_PHYSICAL` | 2.362 |
| `monsters[238].stats.STOCK_ATTACK_CHAOS` | 3.04 |
| `monsters[238].stats.STOCK_RESIST_CHAOS` | 40 |
| `monsters[238].stats.STOCK_CRITICAL_CHANCE` | 12 |
| `monsters[238].stats.STOCK_POISON_CHANCE` | 20 |
| `monsters[238].stats.STOCK_MANA` | 30 |
| `monsters[238].boss` | Да |
| `monsters[238].skills` | [Ядовитый плевок](Monster-skills.md#mob_spit); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[238].fixed` | [BOSS_DEVASTATING](Modifiers-MONSTER.md#boss_devastating); [MOB_BLIGHTED](Modifiers-MONSTER.md#mob_blighted) |
| `monsters[238].tables` | [boss:BOSS_VOID_MOTHER](Tables-TEMPLATE.md#boss-boss_void_mother) |
| `monsters[239].code` | [Звездочёт](Bosses.md#boss_stargazer) |
| `monsters[239].form` | `DRAGON` |
| `monsters[239].experience` | 60 |
| `monsters[239].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[239].stats.STOCK_ATTACK_SPEED` | 1.15 |
| `monsters[239].stats.STOCK_HEALTH` | 32.719 |
| `monsters[239].stats.STOCK_ATTACK_PHYSICAL` | 2.879 |
| `monsters[239].stats.STOCK_ATTACK_FIRE` | 2.002 |
| `monsters[239].stats.STOCK_RESIST_FIRE` | 40 |
| `monsters[239].stats.STOCK_ARMOR` | 50 |
| `monsters[239].stats.STOCK_HEALTH_REGEN` | 0.476 |
| `monsters[239].stats.STOCK_IGNITE_CHANCE` | 20 |
| `monsters[239].stats.STOCK_MANA` | 30 |
| `monsters[239].boss` | Да |
| `monsters[239].skills` | [Удар по земле](Monster-skills.md#mob_slam); [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsters[239].fixed` | [BOSS_SIEGE](Modifiers-MONSTER.md#boss_siege); [MOB_FIREPROOF](Modifiers-MONSTER.md#mob_fireproof) |
| `monsters[239].tables` | [boss:BOSS_STARGAZER](Tables-TEMPLATE.md#boss-boss_stargazer) |
| `monsters[240].code` | [Собиратель корон](Bosses.md#boss_crown_collector) |
| `monsters[240].form` | `BRUTE` |
| `monsters[240].experience` | 60 |
| `monsters[240].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[240].stats.STOCK_ATTACK_SPEED` | 1 |
| `monsters[240].stats.STOCK_HEALTH` | 32.719 |
| `monsters[240].stats.STOCK_ATTACK_PHYSICAL` | 2.582 |
| `monsters[240].stats.STOCK_ATTACK_COLD` | 1.164 |
| `monsters[240].stats.STOCK_RESIST_COLD` | 40 |
| `monsters[240].stats.STOCK_HEALTH_REGEN` | 0.793 |
| `monsters[240].stats.STOCK_STUN_THRESHOLD` | 15 |
| `monsters[240].stats.STOCK_FREEZE_CHANCE` | 20 |
| `monsters[240].stats.STOCK_MANA` | 30 |
| `monsters[240].boss` | Да |
| `monsters[240].skills` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsters[240].fixed` | [BOSS_COLOSSAL](Modifiers-MONSTER.md#boss_colossal); [MOB_REGENERATING](Modifiers-MONSTER.md#mob_regenerating) |
| `monsters[240].tables` | [boss:BOSS_CROWN_COLLECTOR](Tables-TEMPLATE.md#boss-boss_crown_collector) |
| `monsters[241].code` | [Странник ничто](Bosses.md#boss_nothing_walker) |
| `monsters[241].form` | `WRAITH` |
| `monsters[241].experience` | 60 |
| `monsters[241].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[241].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[241].stats.STOCK_HEALTH` | 20.488 |
| `monsters[241].stats.STOCK_ENERGY_SHIELD` | 12.231 |
| `monsters[241].stats.STOCK_ATTACK_LIGHTNING` | 2.54 |
| `monsters[241].stats.STOCK_RESIST_LIGHTNING` | 70 |
| `monsters[241].stats.STOCK_ATTACK_MAGICAL` | 1.987 |
| `monsters[241].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[241].stats.STOCK_MANA` | 55 |
| `monsters[241].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[241].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[241].boss` | Да |
| `monsters[241].skills` | [Разряд](Monster-skills.md#mob_zap); [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsters[241].fixed` | [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest); [MOB_STORMBORN](Modifiers-MONSTER.md#mob_stormborn) |
| `monsters[241].tables` | [boss:BOSS_NOTHING_WALKER](Tables-TEMPLATE.md#boss-boss_nothing_walker) |
| `monsters[242].code` | [Последний бог](Bosses.md#boss_last_god) |
| `monsters[242].form` | `ANGEL` |
| `monsters[242].experience` | 60 |
| `monsters[242].loot` | [loot:BOSS_9](Tables-LOOT.md#loot-boss_9) |
| `monsters[242].stats.STOCK_ATTACK_SPEED` | 1.2 |
| `monsters[242].stats.STOCK_HEALTH` | 23.556 |
| `monsters[242].stats.STOCK_ENERGY_SHIELD` | 14.071 |
| `monsters[242].stats.STOCK_ATTACK_LIGHTNING` | 2.843 |
| `monsters[242].stats.STOCK_RESIST_LIGHTNING` | 70 |
| `monsters[242].stats.STOCK_ATTACK_MAGICAL` | 2.202 |
| `monsters[242].stats.STOCK_CAST_SPEED` | 1 |
| `monsters[242].stats.STOCK_MANA` | 60 |
| `monsters[242].stats.STOCK_MANA_REGEN` | 4 |
| `monsters[242].stats.STOCK_SHOCK_CHANCE` | 20 |
| `monsters[242].boss` | Да |
| `monsters[242].skills` | [Разряд](Monster-skills.md#mob_zap) |
| `monsters[242].fixed` | [BOSS_IMMORTAL](Modifiers-MONSTER.md#boss_immortal); [BOSS_DREAD](Modifiers-MONSTER.md#boss_dread); [BOSS_TEMPEST](Modifiers-MONSTER.md#boss_tempest) |
| `monsters[242].tables` | [boss:BOSS_LAST_GOD](Tables-TEMPLATE.md#boss-boss_last_god) |
| `world.width` | 1100 |
| `world.height` | 8280 |
| `regions[0].code` | `REGION_1` |
| `regions[0].label.x` | 850 |
| `regions[0].label.y` | 55 |
| `regions[0].zones[0].code` | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| `regions[0].zones[0].biome` | `SHORE` |
| `regions[0].zones[0].level` | 1 |
| `regions[0].zones[0].x` | 470 |
| `regions[0].zones[0].y` | 100 |
| `regions[0].zones[0].monsters` | [Утопленник](Monsters.md#drowned); [Береговой краб](Monsters.md#shore_crab) |
| `regions[0].zones[0].count` | 37; 51 |
| `regions[0].zones[0].light` | 1.2 |
| `regions[0].zones[0].chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| `regions[0].zones[0].boss` | [Зовущий Прилив](Bosses.md#boss_tidecaller) |
| `regions[0].zones[0].corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| `regions[0].zones[0].size` | 72 |
| `regions[0].zones[0].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[1].code` | [Кладбище кораблей](World-REGION_1.md#c1_ship_graveyard) |
| `regions[0].zones[1].biome` | `SHORE` |
| `regions[0].zones[1].level` | 3 |
| `regions[0].zones[1].x` | 250 |
| `regions[0].zones[1].y` | 245 |
| `regions[0].zones[1].from` | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| `regions[0].zones[1].monsters` | [Утопленник](Monsters.md#drowned); [Береговой краб](Monsters.md#shore_crab); [Солёный плевун](Monsters.md#brine_spitter) |
| `regions[0].zones[1].count` | 37; 51 |
| `regions[0].zones[1].light` | 1.1 |
| `regions[0].zones[1].chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| `regions[0].zones[1].boss` | [Капитан «Скорби»](Bosses.md#boss_sorrow_captain) |
| `regions[0].zones[1].corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| `regions[0].zones[1].size` | 72 |
| `regions[0].zones[1].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[2].code` | [Солёные пещеры](World-REGION_1.md#c1_brine_caves) |
| `regions[0].zones[2].biome` | `CAVE` |
| `regions[0].zones[2].level` | 3 |
| `regions[0].zones[2].x` | 470 |
| `regions[0].zones[2].y` | 240 |
| `regions[0].zones[2].from` | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| `regions[0].zones[2].monsters` | [Береговой краб](Monsters.md#shore_crab); [Пещерная летучая мышь](Monsters.md#cave_bat); [Солёный плевун](Monsters.md#brine_spitter) |
| `regions[0].zones[2].count` | 37; 51 |
| `regions[0].zones[2].light` | 0.8 |
| `regions[0].zones[2].chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| `regions[0].zones[2].boss` | [Мать Рассола](Bosses.md#boss_brine_mother) |
| `regions[0].zones[2].corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| `regions[0].zones[2].size` | 72 |
| `regions[0].zones[2].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[3].code` | [Птичьи утёсы](World-REGION_1.md#c1_gull_cliffs) |
| `regions[0].zones[3].biome` | `SHORE` |
| `regions[0].zones[3].level` | 3 |
| `regions[0].zones[3].x` | 690 |
| `regions[0].zones[3].y` | 255 |
| `regions[0].zones[3].from` | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| `regions[0].zones[3].monsters` | [Пещерная летучая мышь](Monsters.md#cave_bat); [Береговой краб](Monsters.md#shore_crab); [Утопленник](Monsters.md#drowned) |
| `regions[0].zones[3].count` | 37; 51 |
| `regions[0].zones[3].light` | 1.3 |
| `regions[0].zones[3].chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| `regions[0].zones[3].boss` | [Королева Гнёзд](Bosses.md#boss_nest_queen) |
| `regions[0].zones[3].corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| `regions[0].zones[3].size` | 72 |
| `regions[0].zones[3].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[4].code` | [Маяк утопленников](World-REGION_1.md#c1_drowned_lighthouse) |
| `regions[0].zones[4].biome` | `SHORE` |
| `regions[0].zones[4].level` | 5 |
| `regions[0].zones[4].x` | 200 |
| `regions[0].zones[4].y` | 390 |
| `regions[0].zones[4].from` | [Кладбище кораблей](World-REGION_1.md#c1_ship_graveyard) |
| `regions[0].zones[4].monsters` | [Утопленник](Monsters.md#drowned); [Береговой краб](Monsters.md#shore_crab); [Солёный плевун](Monsters.md#brine_spitter) |
| `regions[0].zones[4].count` | 44; 57 |
| `regions[0].zones[4].light` | 1 |
| `regions[0].zones[4].chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| `regions[0].zones[4].boss` | [Смотритель Маяка](Bosses.md#boss_lamp_warden) |
| `regions[0].zones[4].corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| `regions[0].zones[4].size` | 72 |
| `regions[0].zones[4].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[5].code` | [Гнилая топь](World-REGION_1.md#c1_rotting_mire) |
| `regions[0].zones[5].biome` | `MIRE` |
| `regions[0].zones[5].level` | 5 |
| `regions[0].zones[5].x` | 380 |
| `regions[0].zones[5].y` | 385 |
| `regions[0].zones[5].from` | [Кладбище кораблей](World-REGION_1.md#c1_ship_graveyard); [Солёные пещеры](World-REGION_1.md#c1_brine_caves) |
| `regions[0].zones[5].monsters` | [Болотный зомби](Monsters.md#mire_zombie); [Трясинный скрытень](Monsters.md#bog_lurker); [Болотная пиявка](Monsters.md#swamp_leech) |
| `regions[0].zones[5].count` | 44; 57 |
| `regions[0].zones[5].light` | 0.9 |
| `regions[0].zones[5].chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| `regions[0].zones[5].boss` | [Болотная Королева](Bosses.md#boss_bog_queen) |
| `regions[0].zones[5].corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| `regions[0].zones[5].size` | 72 |
| `regions[0].zones[5].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[6].code` | [Туманная переправа](World-REGION_1.md#c1_misty_ferry) |
| `regions[0].zones[6].biome` | `MIRE` |
| `regions[0].zones[6].level` | 5 |
| `regions[0].zones[6].x` | 570 |
| `regions[0].zones[6].y` | 395 |
| `regions[0].zones[6].from` | [Солёные пещеры](World-REGION_1.md#c1_brine_caves); [Птичьи утёсы](World-REGION_1.md#c1_gull_cliffs) |
| `regions[0].zones[6].monsters` | [Болотный зомби](Monsters.md#mire_zombie); [Утопленник](Monsters.md#drowned); [Болотная пиявка](Monsters.md#swamp_leech) |
| `regions[0].zones[6].count` | 44; 57 |
| `regions[0].zones[6].light` | 0.8 |
| `regions[0].zones[6].chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| `regions[0].zones[6].boss` | [Паромщик](Bosses.md#boss_ferryman) |
| `regions[0].zones[6].corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| `regions[0].zones[6].size` | 72 |
| `regions[0].zones[6].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[7].code` | [Грот контрабандистов](World-REGION_1.md#c1_smugglers_grotto) |
| `regions[0].zones[7].biome` | `CAVE` |
| `regions[0].zones[7].level` | 5 |
| `regions[0].zones[7].x` | 790 |
| `regions[0].zones[7].y` | 405 |
| `regions[0].zones[7].from` | [Птичьи утёсы](World-REGION_1.md#c1_gull_cliffs) |
| `regions[0].zones[7].monsters` | [Береговой краб](Monsters.md#shore_crab); [Пещерная летучая мышь](Monsters.md#cave_bat); [Утопленник](Monsters.md#drowned) |
| `regions[0].zones[7].count` | 44; 57 |
| `regions[0].zones[7].light` | 0.75 |
| `regions[0].zones[7].chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| `regions[0].zones[7].boss` | [Король Контрабандистов](Bosses.md#boss_smuggler_king) |
| `regions[0].zones[7].corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| `regions[0].zones[7].size` | 72 |
| `regions[0].zones[7].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[8].code` | [Шепчущий лес](World-REGION_1.md#c1_whispering_wood) |
| `regions[0].zones[8].biome` | `FOREST` |
| `regions[0].zones[8].level` | 7 |
| `regions[0].zones[8].x` | 290 |
| `regions[0].zones[8].y` | 535 |
| `regions[0].zones[8].from` | [Маяк утопленников](World-REGION_1.md#c1_drowned_lighthouse); [Гнилая топь](World-REGION_1.md#c1_rotting_mire) |
| `regions[0].zones[8].monsters` | [Одичавший волк](Monsters.md#feral_wolf); [Терновая дриада](Monsters.md#thorn_dryad); [Лесной паук](Monsters.md#wood_spider) |
| `regions[0].zones[8].count` | 44; 57 |
| `regions[0].zones[8].light` | 1 |
| `regions[0].zones[8].chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| `regions[0].zones[8].boss` | [Вожак Стаи](Bosses.md#boss_alpha_wolf) |
| `regions[0].zones[8].corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `regions[0].zones[8].size` | 72 |
| `regions[0].zones[8].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[9].code` | [Чёрная гать](World-REGION_1.md#c1_black_causeway) |
| `regions[0].zones[9].biome` | `MIRE` |
| `regions[0].zones[9].level` | 7 |
| `regions[0].zones[9].x` | 490 |
| `regions[0].zones[9].y` | 530 |
| `regions[0].zones[9].from` | [Гнилая топь](World-REGION_1.md#c1_rotting_mire); [Туманная переправа](World-REGION_1.md#c1_misty_ferry) |
| `regions[0].zones[9].monsters` | [Трясинный скрытень](Monsters.md#bog_lurker); [Болотная пиявка](Monsters.md#swamp_leech); [Болотный зомби](Monsters.md#mire_zombie) |
| `regions[0].zones[9].count` | 44; 57 |
| `regions[0].zones[9].light` | 0.85 |
| `regions[0].zones[9].chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| `regions[0].zones[9].boss` | [Трясинный Змей](Bosses.md#boss_mire_serpent) |
| `regions[0].zones[9].corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `regions[0].zones[9].size` | 72 |
| `regions[0].zones[9].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[10].code` | [Волчья чаща](World-REGION_1.md#c1_wolf_thicket) |
| `regions[0].zones[10].biome` | `FOREST` |
| `regions[0].zones[10].level` | 7 |
| `regions[0].zones[10].x` | 700 |
| `regions[0].zones[10].y` | 545 |
| `regions[0].zones[10].from` | [Туманная переправа](World-REGION_1.md#c1_misty_ferry) |
| `regions[0].zones[10].monsters` | [Одичавший волк](Monsters.md#feral_wolf); [Лесной паук](Monsters.md#wood_spider) |
| `regions[0].zones[10].count` | 44; 57 |
| `regions[0].zones[10].light` | 0.95 |
| `regions[0].zones[10].chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| `regions[0].zones[10].boss` | [Матёрый Волколак](Bosses.md#boss_old_werewolf) |
| `regions[0].zones[10].corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `regions[0].zones[10].size` | 72 |
| `regions[0].zones[10].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[11].code` | [Павшие руины](World-REGION_1.md#c1_fallen_ruins) |
| `regions[0].zones[11].biome` | `RUINS` |
| `regions[0].zones[11].level` | 9 |
| `regions[0].zones[11].x` | 350 |
| `regions[0].zones[11].y` | 680 |
| `regions[0].zones[11].from` | [Шепчущий лес](World-REGION_1.md#c1_whispering_wood); [Чёрная гать](World-REGION_1.md#c1_black_causeway) |
| `regions[0].zones[11].monsters` | [Скелет-воин](Monsters.md#skeleton_warrior); [Костяной лучник](Monsters.md#bone_archer); [Страж руин](Monsters.md#ruin_sentry) |
| `regions[0].zones[11].count` | 44; 57 |
| `regions[0].zones[11].light` | 1 |
| `regions[0].zones[11].chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| `regions[0].zones[11].boss` | [Страж Руин](Bosses.md#boss_ruin_warden) |
| `regions[0].zones[11].corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `regions[0].zones[11].size` | 72 |
| `regions[0].zones[11].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[12].code` | [Старая застава](World-REGION_1.md#c1_old_outpost) |
| `regions[0].zones[12].biome` | `RUINS` |
| `regions[0].zones[12].level` | 9 |
| `regions[0].zones[12].x` | 560 |
| `regions[0].zones[12].y` | 675 |
| `regions[0].zones[12].from` | [Чёрная гать](World-REGION_1.md#c1_black_causeway); [Волчья чаща](World-REGION_1.md#c1_wolf_thicket) |
| `regions[0].zones[12].monsters` | [Скелет-воин](Monsters.md#skeleton_warrior); [Костяной лучник](Monsters.md#bone_archer); [Одичавший волк](Monsters.md#feral_wolf) |
| `regions[0].zones[12].count` | 44; 57 |
| `regions[0].zones[12].light` | 1 |
| `regions[0].zones[12].chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| `regions[0].zones[12].boss` | [Комендант Заставы](Bosses.md#boss_outpost_commander) |
| `regions[0].zones[12].corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `regions[0].zones[12].size` | 72 |
| `regions[0].zones[12].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[13].code` | [Курган вождя](World-REGION_1.md#c1_chieftains_barrow) |
| `regions[0].zones[13].biome` | `CRYPT` |
| `regions[0].zones[13].level` | 9 |
| `regions[0].zones[13].x` | 790 |
| `regions[0].zones[13].y` | 690 |
| `regions[0].zones[13].from` | [Волчья чаща](World-REGION_1.md#c1_wolf_thicket) |
| `regions[0].zones[13].monsters` | [Упырь](Monsters.md#ghoul); [Скелет-воин](Monsters.md#skeleton_warrior); [Костяной лучник](Monsters.md#bone_archer) |
| `regions[0].zones[13].count` | 44; 57 |
| `regions[0].zones[13].light` | 0.7 |
| `regions[0].zones[13].chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| `regions[0].zones[13].boss` | [Вождь Кургана](Bosses.md#boss_barrow_chieftain) |
| `regions[0].zones[13].corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `regions[0].zones[13].size` | 72 |
| `regions[0].zones[13].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[0].zones[14].code` | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| `regions[0].zones[14].biome` | `CRYPT` |
| `regions[0].zones[14].level` | 11 |
| `regions[0].zones[14].x` | 470 |
| `regions[0].zones[14].y` | 825 |
| `regions[0].zones[14].from` | [Павшие руины](World-REGION_1.md#c1_fallen_ruins); [Старая застава](World-REGION_1.md#c1_old_outpost) |
| `regions[0].zones[14].finale` | Да |
| `regions[0].zones[14].monsters` | [Упырь](Monsters.md#ghoul); [Скелет-воин](Monsters.md#skeleton_warrior); [Склепный призрак](Monsters.md#crypt_wraith); [Костяной лучник](Monsters.md#bone_archer) |
| `regions[0].zones[14].count` | 44; 57 |
| `regions[0].zones[14].light` | 0.7 |
| `regions[0].zones[14].chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| `regions[0].zones[14].boss` | [Владыка Склепа](Bosses.md#boss_crypt_lord) |
| `regions[0].zones[14].corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `regions[0].zones[14].size` | 72 |
| `regions[0].zones[14].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].code` | `REGION_2` |
| `regions[1].label.x` | 880 |
| `regions[1].label.y` | 935 |
| `regions[1].zones[0].code` | [Рудничный посёлок](World-REGION_2.md#c2_miners_village) |
| `regions[1].zones[0].biome` | `MINES` |
| `regions[1].zones[0].level` | 13 |
| `regions[1].zones[0].x` | 300 |
| `regions[1].zones[0].y` | 1000 |
| `regions[1].zones[0].from` | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| `regions[1].zones[0].monsters` | [Обитатель туннелей](Monsters.md#tunnel_dweller); [Осколочный ползун](Monsters.md#shard_scuttler); [Пещерная летучая мышь](Monsters.md#cave_bat) |
| `regions[1].zones[0].count` | 51; 66 |
| `regions[1].zones[0].light` | 0.85 |
| `regions[1].zones[0].chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| `regions[1].zones[0].boss` | [Бригадир Проходчиков](Bosses.md#boss_foreman) |
| `regions[1].zones[0].corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `regions[1].zones[0].size` | 72 |
| `regions[1].zones[0].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[1].code` | [Кристальные копи](World-REGION_2.md#c2_crystal_mines) |
| `regions[1].zones[1].biome` | `MINES` |
| `regions[1].zones[1].level` | 13 |
| `regions[1].zones[1].x` | 530 |
| `regions[1].zones[1].y` | 990 |
| `regions[1].zones[1].from` | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| `regions[1].zones[1].monsters` | [Кристальный голем](Monsters.md#crystal_golem); [Осколочный ползун](Monsters.md#shard_scuttler); [Обитатель туннелей](Monsters.md#tunnel_dweller) |
| `regions[1].zones[1].count` | 51; 66 |
| `regions[1].zones[1].light` | 0.75 |
| `regions[1].zones[1].chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| `regions[1].zones[1].boss` | [Королева Осколков](Bosses.md#boss_shard_queen) |
| `regions[1].zones[1].corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `regions[1].zones[1].size` | 72 |
| `regions[1].zones[1].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[2].code` | [Предгорный тракт](World-REGION_2.md#c2_foothill_road) |
| `regions[1].zones[2].biome` | `FROST` |
| `regions[1].zones[2].level` | 13 |
| `regions[1].zones[2].x` | 760 |
| `regions[1].zones[2].y` | 1005 |
| `regions[1].zones[2].from` | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| `regions[1].zones[2].monsters` | [Снежный охотник](Monsters.md#snow_stalker); [Одичавший волк](Monsters.md#feral_wolf); [Обитатель туннелей](Monsters.md#tunnel_dweller) |
| `regions[1].zones[2].count` | 51; 66 |
| `regions[1].zones[2].light` | 1.1 |
| `regions[1].zones[2].chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| `regions[1].zones[2].boss` | [Белая Рысь](Bosses.md#boss_white_lynx) |
| `regions[1].zones[2].corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| `regions[1].zones[2].size` | 72 |
| `regions[1].zones[2].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[3].code` | [Глубинные штольни](World-REGION_2.md#c2_deep_adits) |
| `regions[1].zones[3].biome` | `MINES` |
| `regions[1].zones[3].level` | 15 |
| `regions[1].zones[3].x` | 230 |
| `regions[1].zones[3].y` | 1145 |
| `regions[1].zones[3].from` | [Рудничный посёлок](World-REGION_2.md#c2_miners_village) |
| `regions[1].zones[3].monsters` | [Кристальный голем](Monsters.md#crystal_golem); [Обитатель туннелей](Monsters.md#tunnel_dweller); [Осколочный ползун](Monsters.md#shard_scuttler) |
| `regions[1].zones[3].count` | 51; 66 |
| `regions[1].zones[3].light` | 0.7 |
| `regions[1].zones[3].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[3].boss` | [Рудный Голем](Bosses.md#boss_ore_golem) |
| `regions[1].zones[3].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[3].size` | 72 |
| `regions[1].zones[3].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[4].code` | [Жеодовая лощина](World-REGION_2.md#c2_geode_hollow) |
| `regions[1].zones[4].biome` | `MINES` |
| `regions[1].zones[4].level` | 15 |
| `regions[1].zones[4].x` | 420 |
| `regions[1].zones[4].y` | 1150 |
| `regions[1].zones[4].from` | [Рудничный посёлок](World-REGION_2.md#c2_miners_village); [Кристальные копи](World-REGION_2.md#c2_crystal_mines) |
| `regions[1].zones[4].monsters` | [Осколочный ползун](Monsters.md#shard_scuttler); [Кристальный голем](Monsters.md#crystal_golem); [Пещерная летучая мышь](Monsters.md#cave_bat) |
| `regions[1].zones[4].count` | 51; 66 |
| `regions[1].zones[4].light` | 0.8 |
| `regions[1].zones[4].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[4].boss` | [Прядильщица Жеод](Bosses.md#boss_geode_spinner) |
| `regions[1].zones[4].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[4].size` | 72 |
| `regions[1].zones[4].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[5].code` | [Замёрзший перевал](World-REGION_2.md#c2_frozen_pass) |
| `regions[1].zones[5].biome` | `FROST` |
| `regions[1].zones[5].level` | 15 |
| `regions[1].zones[5].x` | 620 |
| `regions[1].zones[5].y` | 1140 |
| `regions[1].zones[5].from` | [Кристальные копи](World-REGION_2.md#c2_crystal_mines); [Предгорный тракт](World-REGION_2.md#c2_foothill_road) |
| `regions[1].zones[5].monsters` | [Ледяной тролль](Monsters.md#frost_troll); [Ледяной призрак](Monsters.md#ice_wraith); [Снежный охотник](Monsters.md#snow_stalker) |
| `regions[1].zones[5].count` | 51; 66 |
| `regions[1].zones[5].light` | 1.1 |
| `regions[1].zones[5].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[5].boss` | [Ледяной Монарх](Bosses.md#boss_frost_monarch) |
| `regions[1].zones[5].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[5].size` | 72 |
| `regions[1].zones[5].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[6].code` | [Пепельная пустошь](World-REGION_2.md#c2_ashen_wastes) |
| `regions[1].zones[6].biome` | `ASH` |
| `regions[1].zones[6].level` | 15 |
| `regions[1].zones[6].x` | 840 |
| `regions[1].zones[6].y` | 1155 |
| `regions[1].zones[6].from` | [Предгорный тракт](World-REGION_2.md#c2_foothill_road) |
| `regions[1].zones[6].monsters` | [Угольная гончая](Monsters.md#cinder_hound); [Пепельный мертвец](Monsters.md#ash_revenant); [Магмовый слизень](Monsters.md#magma_slug) |
| `regions[1].zones[6].count` | 51; 66 |
| `regions[1].zones[6].light` | 0.9 |
| `regions[1].zones[6].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[6].boss` | [Пепельный Тиран](Bosses.md#boss_ashen_tyrant) |
| `regions[1].zones[6].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[6].size` | 72 |
| `regions[1].zones[6].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[7].code` | [Ледниковые гробницы](World-REGION_2.md#c2_glacier_tombs) |
| `regions[1].zones[7].biome` | `FROST` |
| `regions[1].zones[7].level` | 17 |
| `regions[1].zones[7].x` | 310 |
| `regions[1].zones[7].y` | 1290 |
| `regions[1].zones[7].from` | [Глубинные штольни](World-REGION_2.md#c2_deep_adits) |
| `regions[1].zones[7].monsters` | [Ледяной призрак](Monsters.md#ice_wraith); [Упырь](Monsters.md#ghoul); [Снежный охотник](Monsters.md#snow_stalker) |
| `regions[1].zones[7].count` | 51; 66 |
| `regions[1].zones[7].light` | 0.8 |
| `regions[1].zones[7].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[7].boss` | [Лич Инея](Bosses.md#boss_rime_lich) |
| `regions[1].zones[7].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[7].size` | 72 |
| `regions[1].zones[7].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[8].code` | [Тролльи пещеры](World-REGION_2.md#c2_troll_caves) |
| `regions[1].zones[8].biome` | `FROST` |
| `regions[1].zones[8].level` | 17 |
| `regions[1].zones[8].x` | 540 |
| `regions[1].zones[8].y` | 1285 |
| `regions[1].zones[8].from` | [Глубинные штольни](World-REGION_2.md#c2_deep_adits); [Замёрзший перевал](World-REGION_2.md#c2_frozen_pass) |
| `regions[1].zones[8].monsters` | [Ледяной тролль](Monsters.md#frost_troll); [Снежный охотник](Monsters.md#snow_stalker); [Пещерная летучая мышь](Monsters.md#cave_bat) |
| `regions[1].zones[8].count` | 51; 66 |
| `regions[1].zones[8].light` | 0.85 |
| `regions[1].zones[8].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[8].boss` | [Старейший Тролль](Bosses.md#boss_elder_troll) |
| `regions[1].zones[8].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[8].size` | 72 |
| `regions[1].zones[8].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[9].code` | [Тлеющие поля](World-REGION_2.md#c2_ember_fields) |
| `regions[1].zones[9].biome` | `ASH` |
| `regions[1].zones[9].level` | 17 |
| `regions[1].zones[9].x` | 770 |
| `regions[1].zones[9].y` | 1295 |
| `regions[1].zones[9].from` | [Замёрзший перевал](World-REGION_2.md#c2_frozen_pass); [Пепельная пустошь](World-REGION_2.md#c2_ashen_wastes) |
| `regions[1].zones[9].monsters` | [Угольная гончая](Monsters.md#cinder_hound); [Магмовый слизень](Monsters.md#magma_slug); [Пепельный мертвец](Monsters.md#ash_revenant) |
| `regions[1].zones[9].count` | 51; 66 |
| `regions[1].zones[9].light` | 0.95 |
| `regions[1].zones[9].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[9].boss` | [Мать Гончих](Bosses.md#boss_hound_mother) |
| `regions[1].zones[9].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[9].size` | 72 |
| `regions[1].zones[9].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[10].code` | [Грозовой пик](World-REGION_2.md#c2_storm_peak) |
| `regions[1].zones[10].biome` | `FROST` |
| `regions[1].zones[10].level` | 19 |
| `regions[1].zones[10].x` | 230 |
| `regions[1].zones[10].y` | 1435 |
| `regions[1].zones[10].from` | [Ледниковые гробницы](World-REGION_2.md#c2_glacier_tombs) |
| `regions[1].zones[10].monsters` | [Ледяной призрак](Monsters.md#ice_wraith); [Снежный охотник](Monsters.md#snow_stalker); [Кристальный голем](Monsters.md#crystal_golem) |
| `regions[1].zones[10].count` | 51; 66 |
| `regions[1].zones[10].light` | 1.2 |
| `regions[1].zones[10].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[10].boss` | [Громовержец](Bosses.md#boss_thunder_caller) |
| `regions[1].zones[10].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[10].size` | 72 |
| `regions[1].zones[10].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[11].code` | [Затонувшее святилище](World-REGION_2.md#c2_sunken_shrine) |
| `regions[1].zones[11].biome` | `TEMPLE` |
| `regions[1].zones[11].level` | 19 |
| `regions[1].zones[11].x` | 430 |
| `regions[1].zones[11].y` | 1440 |
| `regions[1].zones[11].from` | [Ледниковые гробницы](World-REGION_2.md#c2_glacier_tombs); [Тролльи пещеры](World-REGION_2.md#c2_troll_caves) |
| `regions[1].zones[11].monsters` | [Глубинный](Monsters.md#deep_one); [Храмовый фанатик](Monsters.md#temple_zealot); [Храмовый страж](Monsters.md#temple_guardian) |
| `regions[1].zones[11].count` | 51; 66 |
| `regions[1].zones[11].light` | 0.85 |
| `regions[1].zones[11].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[11].boss` | [Жрица Глубин](Bosses.md#boss_deep_priestess) |
| `regions[1].zones[11].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[11].size` | 72 |
| `regions[1].zones[11].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[12].code` | [Обсидиановый карьер](World-REGION_2.md#c2_obsidian_quarry) |
| `regions[1].zones[12].biome` | `ASH` |
| `regions[1].zones[12].level` | 19 |
| `regions[1].zones[12].x` | 640 |
| `regions[1].zones[12].y` | 1430 |
| `regions[1].zones[12].from` | [Тролльи пещеры](World-REGION_2.md#c2_troll_caves); [Тлеющие поля](World-REGION_2.md#c2_ember_fields) |
| `regions[1].zones[12].monsters` | [Магмовый слизень](Monsters.md#magma_slug); [Кристальный голем](Monsters.md#crystal_golem); [Пепельный мертвец](Monsters.md#ash_revenant) |
| `regions[1].zones[12].count` | 51; 66 |
| `regions[1].zones[12].light` | 0.9 |
| `regions[1].zones[12].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[12].boss` | [Обсидиановый Колосс](Bosses.md#boss_obsidian_colossus) |
| `regions[1].zones[12].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[12].size` | 72 |
| `regions[1].zones[12].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[13].code` | [Шлаковые ямы](World-REGION_2.md#c2_slag_pits) |
| `regions[1].zones[13].biome` | `ASH` |
| `regions[1].zones[13].level` | 19 |
| `regions[1].zones[13].x` | 850 |
| `regions[1].zones[13].y` | 1445 |
| `regions[1].zones[13].from` | [Тлеющие поля](World-REGION_2.md#c2_ember_fields) |
| `regions[1].zones[13].monsters` | [Магмовый слизень](Monsters.md#magma_slug); [Угольная гончая](Monsters.md#cinder_hound) |
| `regions[1].zones[13].count` | 51; 66 |
| `regions[1].zones[13].light` | 0.95 |
| `regions[1].zones[13].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[13].boss` | [Шлаковый Червь](Bosses.md#boss_slag_worm) |
| `regions[1].zones[13].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[13].size` | 72 |
| `regions[1].zones[13].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[14].code` | [Ледник Хладоклыка](World-REGION_2.md#c2_frostfang_glacier) |
| `regions[1].zones[14].biome` | `FROST` |
| `regions[1].zones[14].level` | 21 |
| `regions[1].zones[14].x` | 310 |
| `regions[1].zones[14].y` | 1580 |
| `regions[1].zones[14].from` | [Грозовой пик](World-REGION_2.md#c2_storm_peak) |
| `regions[1].zones[14].monsters` | [Ледяной тролль](Monsters.md#frost_troll); [Ледяной призрак](Monsters.md#ice_wraith); [Снежный охотник](Monsters.md#snow_stalker) |
| `regions[1].zones[14].count` | 51; 66 |
| `regions[1].zones[14].light` | 1.15 |
| `regions[1].zones[14].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[14].boss` | [Хладоклык](Bosses.md#boss_frostfang) |
| `regions[1].zones[14].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[14].size` | 72 |
| `regions[1].zones[14].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[15].code` | [Лестница в бездну](World-REGION_2.md#c2_abyssal_stair) |
| `regions[1].zones[15].biome` | `TEMPLE` |
| `regions[1].zones[15].level` | 21 |
| `regions[1].zones[15].x` | 530 |
| `regions[1].zones[15].y` | 1575 |
| `regions[1].zones[15].from` | [Затонувшее святилище](World-REGION_2.md#c2_sunken_shrine); [Обсидиановый карьер](World-REGION_2.md#c2_obsidian_quarry) |
| `regions[1].zones[15].monsters` | [Послушник бездны](Monsters.md#abyssal_acolyte); [Глубинный](Monsters.md#deep_one); [Храмовый фанатик](Monsters.md#temple_zealot) |
| `regions[1].zones[15].count` | 51; 66 |
| `regions[1].zones[15].light` | 0.75 |
| `regions[1].zones[15].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[15].boss` | [Глашатай Бездны](Bosses.md#boss_abyss_herald) |
| `regions[1].zones[15].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[15].size` | 72 |
| `regions[1].zones[15].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[16].code` | [Тлеющая цитадель](World-REGION_2.md#c2_cinder_citadel) |
| `regions[1].zones[16].biome` | `ASH` |
| `regions[1].zones[16].level` | 21 |
| `regions[1].zones[16].x` | 750 |
| `regions[1].zones[16].y` | 1585 |
| `regions[1].zones[16].from` | [Обсидиановый карьер](World-REGION_2.md#c2_obsidian_quarry) |
| `regions[1].zones[16].monsters` | [Пепельный мертвец](Monsters.md#ash_revenant); [Угольная гончая](Monsters.md#cinder_hound); [Храмовый страж](Monsters.md#temple_guardian) |
| `regions[1].zones[16].count` | 51; 66 |
| `regions[1].zones[16].light` | 0.85 |
| `regions[1].zones[16].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[16].boss` | [Кастелян Пепла](Bosses.md#boss_ash_castellan) |
| `regions[1].zones[16].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[16].size` | 72 |
| `regions[1].zones[16].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[1].zones[17].code` | [Храм утонувших](World-REGION_2.md#c2_drowned_temple) |
| `regions[1].zones[17].biome` | `TEMPLE` |
| `regions[1].zones[17].level` | 23 |
| `regions[1].zones[17].x` | 530 |
| `regions[1].zones[17].y` | 1725 |
| `regions[1].zones[17].from` | [Ледник Хладоклыка](World-REGION_2.md#c2_frostfang_glacier); [Лестница в бездну](World-REGION_2.md#c2_abyssal_stair); [Тлеющая цитадель](World-REGION_2.md#c2_cinder_citadel) |
| `regions[1].zones[17].finale` | Да |
| `regions[1].zones[17].monsters` | [Храмовый фанатик](Monsters.md#temple_zealot); [Глубинный](Monsters.md#deep_one); [Послушник бездны](Monsters.md#abyssal_acolyte); [Храмовый страж](Monsters.md#temple_guardian) |
| `regions[1].zones[17].count` | 51; 66 |
| `regions[1].zones[17].light` | 0.85 |
| `regions[1].zones[17].chestLoot` | [loot:CHEST_3](Tables-LOOT.md#loot-chest_3) |
| `regions[1].zones[17].boss` | [Утонувший Бог](Bosses.md#boss_drowned_god) |
| `regions[1].zones[17].corrupted` | [Осквернённый Владыка](Monsters.md#corrupted_3) |
| `regions[1].zones[17].size` | 72 |
| `regions[1].zones[17].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].code` | `REGION_3` |
| `regions[2].label.x` | 880 |
| `regions[2].label.y` | 1830 |
| `regions[2].zones[0].code` | [Солончаки](World-REGION_3.md#c3_salt_flats) |
| `regions[2].zones[0].biome` | `DESERT` |
| `regions[2].zones[0].level` | 25 |
| `regions[2].zones[0].x` | 300 |
| `regions[2].zones[0].y` | 1900 |
| `regions[2].zones[0].from` | [Храм утонувших](World-REGION_2.md#c2_drowned_temple) |
| `regions[2].zones[0].monsters` | [Песчаный скорпион](Monsters.md#sand_scorpion); [Барханный хищник](Monsters.md#dune_stalker); [Мумия](Monsters.md#mummy) |
| `regions[2].zones[0].count` | 51; 66 |
| `regions[2].zones[0].light` | 1.25 |
| `regions[2].zones[0].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[0].boss` | [Солевой скиталец](Bosses.md#boss_salt_walker) |
| `regions[2].zones[0].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[0].size` | 72 |
| `regions[2].zones[0].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[1].code` | [Караванный путь](World-REGION_3.md#c3_caravan_road) |
| `regions[2].zones[1].biome` | `DESERT` |
| `regions[2].zones[1].level` | 25 |
| `regions[2].zones[1].x` | 530 |
| `regions[2].zones[1].y` | 1890 |
| `regions[2].zones[1].from` | [Храм утонувших](World-REGION_2.md#c2_drowned_temple) |
| `regions[2].zones[1].monsters` | [Барханный хищник](Monsters.md#dune_stalker); [Налётчик каньона](Monsters.md#canyon_raider); [Песчаный скорпион](Monsters.md#sand_scorpion) |
| `regions[2].zones[1].count` | 51; 66 |
| `regions[2].zones[1].light` | 1.25 |
| `regions[2].zones[1].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[1].boss` | [Мясник караванов](Bosses.md#boss_caravan_butcher) |
| `regions[2].zones[1].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[1].size` | 72 |
| `regions[2].zones[1].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[2].code` | [Красное ущелье](World-REGION_3.md#c3_red_gorge) |
| `regions[2].zones[2].biome` | `CANYON` |
| `regions[2].zones[2].level` | 25 |
| `regions[2].zones[2].x` | 760 |
| `regions[2].zones[2].y` | 1905 |
| `regions[2].zones[2].from` | [Храм утонувших](World-REGION_2.md#c2_drowned_temple) |
| `regions[2].zones[2].monsters` | [Скальный стервятник](Monsters.md#cliff_vulture); [Налётчик каньона](Monsters.md#canyon_raider); [Песчаниковый голем](Monsters.md#sandstone_golem) |
| `regions[2].zones[2].count` | 51; 66 |
| `regions[2].zones[2].light` | 1.1 |
| `regions[2].zones[2].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[2].boss` | [Тиран ущелья](Bosses.md#boss_gorge_tyrant) |
| `regions[2].zones[2].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[2].size` | 72 |
| `regions[2].zones[2].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[3].code` | [Поющие барханы](World-REGION_3.md#c3_singing_dunes) |
| `regions[2].zones[3].biome` | `DESERT` |
| `regions[2].zones[3].level` | 27 |
| `regions[2].zones[3].x` | 230 |
| `regions[2].zones[3].y` | 2045 |
| `regions[2].zones[3].from` | [Солончаки](World-REGION_3.md#c3_salt_flats) |
| `regions[2].zones[3].monsters` | [Дух самума](Monsters.md#sandstorm_spirit); [Барханный хищник](Monsters.md#dune_stalker); [Песчаный скорпион](Monsters.md#sand_scorpion) |
| `regions[2].zones[3].count` | 51; 66 |
| `regions[2].zones[3].light` | 1.25 |
| `regions[2].zones[3].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[3].boss` | [Сирена барханов](Bosses.md#boss_dune_siren) |
| `regions[2].zones[3].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[3].size` | 72 |
| `regions[2].zones[3].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[4].code` | [Засыпанный город](World-REGION_3.md#c3_buried_city) |
| `regions[2].zones[4].biome` | `RUINS` |
| `regions[2].zones[4].level` | 27 |
| `regions[2].zones[4].x` | 430 |
| `regions[2].zones[4].y` | 2050 |
| `regions[2].zones[4].from` | [Солончаки](World-REGION_3.md#c3_salt_flats); [Караванный путь](World-REGION_3.md#c3_caravan_road) |
| `regions[2].zones[4].monsters` | [Мумия](Monsters.md#mummy); [Налётчик каньона](Monsters.md#canyon_raider); [Песчаниковый голем](Monsters.md#sandstone_golem) |
| `regions[2].zones[4].count` | 51; 66 |
| `regions[2].zones[4].light` | 1 |
| `regions[2].zones[4].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[4].boss` | [Песчаный магистрат](Bosses.md#boss_sand_magistrate) |
| `regions[2].zones[4].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[4].size` | 72 |
| `regions[2].zones[4].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[5].code` | [Гнездовье стервятников](World-REGION_3.md#c3_vulture_roost) |
| `regions[2].zones[5].biome` | `CANYON` |
| `regions[2].zones[5].level` | 27 |
| `regions[2].zones[5].x` | 630 |
| `regions[2].zones[5].y` | 2040 |
| `regions[2].zones[5].from` | [Караванный путь](World-REGION_3.md#c3_caravan_road); [Красное ущелье](World-REGION_3.md#c3_red_gorge) |
| `regions[2].zones[5].monsters` | [Скальный стервятник](Monsters.md#cliff_vulture); [Барханный хищник](Monsters.md#dune_stalker); [Налётчик каньона](Monsters.md#canyon_raider) |
| `regions[2].zones[5].count` | 51; 66 |
| `regions[2].zones[5].light` | 1.1 |
| `regions[2].zones[5].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[5].boss` | [Мать падали](Bosses.md#boss_carrion_mother) |
| `regions[2].zones[5].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[5].size` | 72 |
| `regions[2].zones[5].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[6].code` | [Иссохший оазис](World-REGION_3.md#c3_withered_oasis) |
| `regions[2].zones[6].biome` | `DESERT` |
| `regions[2].zones[6].level` | 27 |
| `regions[2].zones[6].x` | 840 |
| `regions[2].zones[6].y` | 2055 |
| `regions[2].zones[6].from` | [Красное ущелье](World-REGION_3.md#c3_red_gorge) |
| `regions[2].zones[6].monsters` | [Песчаный скорпион](Monsters.md#sand_scorpion); [Мумия](Monsters.md#mummy); [Скальный стервятник](Monsters.md#cliff_vulture) |
| `regions[2].zones[6].count` | 51; 66 |
| `regions[2].zones[6].light` | 1.25 |
| `regions[2].zones[6].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[6].boss` | [Змей оазиса](Bosses.md#boss_oasis_serpent) |
| `regions[2].zones[6].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[6].size` | 72 |
| `regions[2].zones[6].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[7].code` | [Стеклянная пустыня](World-REGION_3.md#c3_glass_desert) |
| `regions[2].zones[7].biome` | `DESERT` |
| `regions[2].zones[7].level` | 29 |
| `regions[2].zones[7].x` | 310 |
| `regions[2].zones[7].y` | 2190 |
| `regions[2].zones[7].from` | [Поющие барханы](World-REGION_3.md#c3_singing_dunes); [Засыпанный город](World-REGION_3.md#c3_buried_city) |
| `regions[2].zones[7].monsters` | [Дух самума](Monsters.md#sandstorm_spirit); [Песчаный скорпион](Monsters.md#sand_scorpion); [Песчаниковый голем](Monsters.md#sandstone_golem) |
| `regions[2].zones[7].count` | 51; 66 |
| `regions[2].zones[7].light` | 1.25 |
| `regions[2].zones[7].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[7].boss` | [Стеклянный колосс](Bosses.md#boss_glass_colossus) |
| `regions[2].zones[7].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[7].size` | 72 |
| `regions[2].zones[7].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[8].code` | [Стан налётчиков](World-REGION_3.md#c3_raider_camp) |
| `regions[2].zones[8].biome` | `CANYON` |
| `regions[2].zones[8].level` | 29 |
| `regions[2].zones[8].x` | 540 |
| `regions[2].zones[8].y` | 2185 |
| `regions[2].zones[8].from` | [Засыпанный город](World-REGION_3.md#c3_buried_city); [Гнездовье стервятников](World-REGION_3.md#c3_vulture_roost) |
| `regions[2].zones[8].monsters` | [Налётчик каньона](Monsters.md#canyon_raider); [Барханный хищник](Monsters.md#dune_stalker); [Скальный стервятник](Monsters.md#cliff_vulture) |
| `regions[2].zones[8].count` | 51; 66 |
| `regions[2].zones[8].light` | 1.1 |
| `regions[2].zones[8].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[8].boss` | [Хан налётчиков](Bosses.md#boss_raider_khan) |
| `regions[2].zones[8].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[8].size` | 72 |
| `regions[2].zones[8].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[9].code` | [Скорпионье гнездо](World-REGION_3.md#c3_scorpion_nest) |
| `regions[2].zones[9].biome` | `CANYON` |
| `regions[2].zones[9].level` | 29 |
| `regions[2].zones[9].x` | 770 |
| `regions[2].zones[9].y` | 2195 |
| `regions[2].zones[9].from` | [Гнездовье стервятников](World-REGION_3.md#c3_vulture_roost) |
| `regions[2].zones[9].monsters` | [Песчаный скорпион](Monsters.md#sand_scorpion); [Скальный стервятник](Monsters.md#cliff_vulture); [Песчаниковый голем](Monsters.md#sandstone_golem) |
| `regions[2].zones[9].count` | 51; 66 |
| `regions[2].zones[9].light` | 1.1 |
| `regions[2].zones[9].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[9].boss` | [Скорпионья матриарх](Bosses.md#boss_scorpion_matriarch) |
| `regions[2].zones[9].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[9].size` | 72 |
| `regions[2].zones[9].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[10].code` | [Долина миражей](World-REGION_3.md#c3_mirage_vale) |
| `regions[2].zones[10].biome` | `DESERT` |
| `regions[2].zones[10].level` | 31 |
| `regions[2].zones[10].x` | 230 |
| `regions[2].zones[10].y` | 2335 |
| `regions[2].zones[10].from` | [Стеклянная пустыня](World-REGION_3.md#c3_glass_desert) |
| `regions[2].zones[10].monsters` | [Дух самума](Monsters.md#sandstorm_spirit); [Барханный хищник](Monsters.md#dune_stalker); [Мумия](Monsters.md#mummy) |
| `regions[2].zones[10].count` | 51; 66 |
| `regions[2].zones[10].light` | 1.25 |
| `regions[2].zones[10].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[10].boss` | [Ткач миражей](Bosses.md#boss_mirage_weaver) |
| `regions[2].zones[10].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[10].size` | 72 |
| `regions[2].zones[10].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[11].code` | [Гробницы царей](World-REGION_3.md#c3_tombs_of_kings) |
| `regions[2].zones[11].biome` | `CRYPT` |
| `regions[2].zones[11].level` | 31 |
| `regions[2].zones[11].x` | 430 |
| `regions[2].zones[11].y` | 2340 |
| `regions[2].zones[11].from` | [Стеклянная пустыня](World-REGION_3.md#c3_glass_desert); [Стан налётчиков](World-REGION_3.md#c3_raider_camp) |
| `regions[2].zones[11].monsters` | [Мумия](Monsters.md#mummy); [Дух самума](Monsters.md#sandstorm_spirit); [Песчаниковый голем](Monsters.md#sandstone_golem) |
| `regions[2].zones[11].count` | 51; 66 |
| `regions[2].zones[11].light` | 0.7 |
| `regions[2].zones[11].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[11].boss` | [Царь-мумия](Bosses.md#boss_mummy_king) |
| `regions[2].zones[11].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[11].size` | 72 |
| `regions[2].zones[11].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[12].code` | [Песчаниковая арка](World-REGION_3.md#c3_sandstone_arch) |
| `regions[2].zones[12].biome` | `CANYON` |
| `regions[2].zones[12].level` | 31 |
| `regions[2].zones[12].x` | 640 |
| `regions[2].zones[12].y` | 2330 |
| `regions[2].zones[12].from` | [Стан налётчиков](World-REGION_3.md#c3_raider_camp); [Скорпионье гнездо](World-REGION_3.md#c3_scorpion_nest) |
| `regions[2].zones[12].monsters` | [Песчаниковый голем](Monsters.md#sandstone_golem); [Скальный стервятник](Monsters.md#cliff_vulture); [Налётчик каньона](Monsters.md#canyon_raider) |
| `regions[2].zones[12].count` | 51; 66 |
| `regions[2].zones[12].light` | 1.1 |
| `regions[2].zones[12].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[12].boss` | [Страж арки](Bosses.md#boss_arch_sentinel) |
| `regions[2].zones[12].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[12].size` | 72 |
| `regions[2].zones[12].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[13].code` | [Сухое русло](World-REGION_3.md#c3_dry_riverbed) |
| `regions[2].zones[13].biome` | `DESERT` |
| `regions[2].zones[13].level` | 31 |
| `regions[2].zones[13].x` | 850 |
| `regions[2].zones[13].y` | 2345 |
| `regions[2].zones[13].from` | [Скорпионье гнездо](World-REGION_3.md#c3_scorpion_nest) |
| `regions[2].zones[13].monsters` | [Песчаный скорпион](Monsters.md#sand_scorpion); [Барханный хищник](Monsters.md#dune_stalker); [Скальный стервятник](Monsters.md#cliff_vulture) |
| `regions[2].zones[13].count` | 51; 66 |
| `regions[2].zones[13].light` | 1.25 |
| `regions[2].zones[13].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[13].boss` | [Обитатель русла](Bosses.md#boss_riverbed_lurker) |
| `regions[2].zones[13].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[13].size` | 72 |
| `regions[2].zones[13].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[14].code` | [Зал бальзамировщиков](World-REGION_3.md#c3_embalmers_hall) |
| `regions[2].zones[14].biome` | `CRYPT` |
| `regions[2].zones[14].level` | 33 |
| `regions[2].zones[14].x` | 320 |
| `regions[2].zones[14].y` | 2480 |
| `regions[2].zones[14].from` | [Гробницы царей](World-REGION_3.md#c3_tombs_of_kings) |
| `regions[2].zones[14].monsters` | [Мумия](Monsters.md#mummy); [Песчаный скорпион](Monsters.md#sand_scorpion); [Дух самума](Monsters.md#sandstorm_spirit) |
| `regions[2].zones[14].count` | 51; 66 |
| `regions[2].zones[14].light` | 0.7 |
| `regions[2].zones[14].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[14].boss` | [Верховный бальзамировщик](Bosses.md#boss_high_embalmer) |
| `regions[2].zones[14].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[14].size` | 72 |
| `regions[2].zones[14].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[15].code` | [Храм Солнца](World-REGION_3.md#c3_sun_temple) |
| `regions[2].zones[15].biome` | `TEMPLE` |
| `regions[2].zones[15].level` | 33 |
| `regions[2].zones[15].x` | 530 |
| `regions[2].zones[15].y` | 2475 |
| `regions[2].zones[15].from` | [Гробницы царей](World-REGION_3.md#c3_tombs_of_kings); [Песчаниковая арка](World-REGION_3.md#c3_sandstone_arch) |
| `regions[2].zones[15].monsters` | [Мумия](Monsters.md#mummy); [Дух самума](Monsters.md#sandstorm_spirit); [Песчаниковый голем](Monsters.md#sandstone_golem); [Налётчик каньона](Monsters.md#canyon_raider) |
| `regions[2].zones[15].count` | 51; 66 |
| `regions[2].zones[15].light` | 0.85 |
| `regions[2].zones[15].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[15].boss` | [Жрец Солнца](Bosses.md#boss_sun_priest) |
| `regions[2].zones[15].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[15].size` | 72 |
| `regions[2].zones[15].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[16].code` | [Грозовое плато](World-REGION_3.md#c3_storm_mesa) |
| `regions[2].zones[16].biome` | `CANYON` |
| `regions[2].zones[16].level` | 33 |
| `regions[2].zones[16].x` | 750 |
| `regions[2].zones[16].y` | 2485 |
| `regions[2].zones[16].from` | [Песчаниковая арка](World-REGION_3.md#c3_sandstone_arch); [Сухое русло](World-REGION_3.md#c3_dry_riverbed) |
| `regions[2].zones[16].monsters` | [Дух самума](Monsters.md#sandstorm_spirit); [Скальный стервятник](Monsters.md#cliff_vulture); [Налётчик каньона](Monsters.md#canyon_raider) |
| `regions[2].zones[16].count` | 51; 66 |
| `regions[2].zones[16].light` | 1.1 |
| `regions[2].zones[16].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[16].boss` | [Грозовой Рух](Bosses.md#boss_storm_roc) |
| `regions[2].zones[16].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[16].size` | 72 |
| `regions[2].zones[16].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[2].zones[17].code` | [Пирамида Солнечного царя](World-REGION_3.md#c3_sun_kings_pyramid) |
| `regions[2].zones[17].biome` | `CRYPT` |
| `regions[2].zones[17].level` | 35 |
| `regions[2].zones[17].x` | 530 |
| `regions[2].zones[17].y` | 2625 |
| `regions[2].zones[17].from` | [Зал бальзамировщиков](World-REGION_3.md#c3_embalmers_hall); [Храм Солнца](World-REGION_3.md#c3_sun_temple); [Грозовое плато](World-REGION_3.md#c3_storm_mesa) |
| `regions[2].zones[17].finale` | Да |
| `regions[2].zones[17].monsters` | [Мумия](Monsters.md#mummy); [Дух самума](Monsters.md#sandstorm_spirit); [Песчаниковый голем](Monsters.md#sandstone_golem); [Песчаный скорпион](Monsters.md#sand_scorpion) |
| `regions[2].zones[17].count` | 51; 66 |
| `regions[2].zones[17].light` | 0.7 |
| `regions[2].zones[17].chestLoot` | [loot:CHEST_4](Tables-LOOT.md#loot-chest_4) |
| `regions[2].zones[17].boss` | [Солнечный царь](Bosses.md#boss_sun_king) |
| `regions[2].zones[17].corrupted` | [Осквернённый Палач](Monsters.md#corrupted_4) |
| `regions[2].zones[17].size` | 72 |
| `regions[2].zones[17].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].code` | `REGION_4` |
| `regions[3].label.x` | 880 |
| `regions[3].label.y` | 2730 |
| `regions[3].zones[0].code` | [Кромка джунглей](World-REGION_4.md#c4_jungle_edge) |
| `regions[3].zones[0].biome` | `JUNGLE` |
| `regions[3].zones[0].level` | 37 |
| `regions[3].zones[0].x` | 290 |
| `regions[3].zones[0].y` | 2800 |
| `regions[3].zones[0].from` | [Пирамида Солнечного царя](World-REGION_3.md#c3_sun_kings_pyramid) |
| `regions[3].zones[0].monsters` | [Изумрудная пантера](Monsters.md#emerald_panther); [Охотник с трубкой](Monsters.md#blowgun_hunter); [Богомол-великан](Monsters.md#giant_mantis) |
| `regions[3].zones[0].count` | 51; 66 |
| `regions[3].zones[0].light` | 0.9 |
| `regions[3].zones[0].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[0].boss` | [Лесной ловчий](Bosses.md#boss_jungle_stalker) |
| `regions[3].zones[0].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[0].size` | 72 |
| `regions[3].zones[0].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[1].code` | [Мангровая дельта](World-REGION_4.md#c4_mangrove_delta) |
| `regions[3].zones[1].biome` | `MIRE` |
| `regions[3].zones[1].level` | 37 |
| `regions[3].zones[1].x` | 520 |
| `regions[3].zones[1].y` | 2790 |
| `regions[3].zones[1].from` | [Пирамида Солнечного царя](World-REGION_3.md#c3_sun_kings_pyramid) |
| `regions[3].zones[1].monsters` | [Споровик](Monsters.md#sporeling); [Охотник с трубкой](Monsters.md#blowgun_hunter); [Богомол-великан](Monsters.md#giant_mantis) |
| `regions[3].zones[1].count` | 51; 66 |
| `regions[3].zones[1].light` | 0.85 |
| `regions[3].zones[1].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[1].boss` | [Мангровая Пасть](Bosses.md#boss_mangrove_maw) |
| `regions[3].zones[1].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[1].size` | 72 |
| `regions[3].zones[1].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[2].code` | [Заросшая дорога](World-REGION_4.md#c4_overgrown_road) |
| `regions[3].zones[2].biome` | `RUINS` |
| `regions[3].zones[2].level` | 37 |
| `regions[3].zones[2].x` | 770 |
| `regions[3].zones[2].y` | 2805 |
| `regions[3].zones[2].from` | [Пирамида Солнечного царя](World-REGION_3.md#c3_sun_kings_pyramid) |
| `regions[3].zones[2].monsters` | [Охотник с трубкой](Monsters.md#blowgun_hunter); [Изумрудная пантера](Monsters.md#emerald_panther); [Споровик](Monsters.md#sporeling) |
| `regions[3].zones[2].count` | 51; 66 |
| `regions[3].zones[2].light` | 1 |
| `regions[3].zones[2].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[2].boss` | [Замшелый легионер](Bosses.md#boss_moss_legionary) |
| `regions[3].zones[2].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[2].size` | 72 |
| `regions[3].zones[2].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[3].code` | [Орхидейная поляна](World-REGION_4.md#c4_orchid_glade) |
| `regions[3].zones[3].biome` | `JUNGLE` |
| `regions[3].zones[3].level` | 39 |
| `regions[3].zones[3].x` | 220 |
| `regions[3].zones[3].y` | 2945 |
| `regions[3].zones[3].from` | [Кромка джунглей](World-REGION_4.md#c4_jungle_edge) |
| `regions[3].zones[3].monsters` | [Споровик](Monsters.md#sporeling); [Богомол-великан](Monsters.md#giant_mantis); [Трутень улья](Monsters.md#hive_drone) |
| `regions[3].zones[3].count` | 51; 66 |
| `regions[3].zones[3].light` | 0.9 |
| `regions[3].zones[3].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[3].boss` | [Орхидейная дриада](Bosses.md#boss_orchid_dryad) |
| `regions[3].zones[3].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[3].size` | 72 |
| `regions[3].zones[3].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[4].code` | [Тропа пантеры](World-REGION_4.md#c4_panther_trail) |
| `regions[3].zones[4].biome` | `JUNGLE` |
| `regions[3].zones[4].level` | 39 |
| `regions[3].zones[4].x` | 420 |
| `regions[3].zones[4].y` | 2950 |
| `regions[3].zones[4].from` | [Кромка джунглей](World-REGION_4.md#c4_jungle_edge); [Мангровая дельта](World-REGION_4.md#c4_mangrove_delta) |
| `regions[3].zones[4].monsters` | [Изумрудная пантера](Monsters.md#emerald_panther); [Охотник с трубкой](Monsters.md#blowgun_hunter); [Богомол-великан](Monsters.md#giant_mantis) |
| `regions[3].zones[4].count` | 51; 66 |
| `regions[3].zones[4].light` | 0.9 |
| `regions[3].zones[4].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[4].boss` | [Теневая пантера](Bosses.md#boss_shadow_panther) |
| `regions[3].zones[4].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[4].size` | 72 |
| `regions[3].zones[4].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[5].code` | [Осиная лощина](World-REGION_4.md#c4_wasp_hollow) |
| `regions[3].zones[5].biome` | `HIVE` |
| `regions[3].zones[5].level` | 39 |
| `regions[3].zones[5].x` | 640 |
| `regions[3].zones[5].y` | 2940 |
| `regions[3].zones[5].from` | [Мангровая дельта](World-REGION_4.md#c4_mangrove_delta) |
| `regions[3].zones[5].monsters` | [Трутень улья](Monsters.md#hive_drone); [Плевун выводка](Monsters.md#brood_spitter); [Солдат улья](Monsters.md#hive_soldier) |
| `regions[3].zones[5].count` | 51; 66 |
| `regions[3].zones[5].light` | 0.7 |
| `regions[3].zones[5].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[5].boss` | [Осиная матрона](Bosses.md#boss_wasp_matron) |
| `regions[3].zones[5].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[5].size` | 72 |
| `regions[3].zones[5].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[6].code` | [Роща душителей](World-REGION_4.md#c4_strangler_grove) |
| `regions[3].zones[6].biome` | `JUNGLE` |
| `regions[3].zones[6].level` | 39 |
| `regions[3].zones[6].x` | 850 |
| `regions[3].zones[6].y` | 2955 |
| `regions[3].zones[6].from` | [Заросшая дорога](World-REGION_4.md#c4_overgrown_road) |
| `regions[3].zones[6].monsters` | [Споровик](Monsters.md#sporeling); [Изумрудная пантера](Monsters.md#emerald_panther); [Охотник с трубкой](Monsters.md#blowgun_hunter) |
| `regions[3].zones[6].count` | 51; 66 |
| `regions[3].zones[6].light` | 0.9 |
| `regions[3].zones[6].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[6].boss` | [Лиана-душитель](Bosses.md#boss_strangler_vine) |
| `regions[3].zones[6].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[6].size` | 72 |
| `regions[3].zones[6].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[7].code` | [Змеиное святилище](World-REGION_4.md#c4_serpent_shrine) |
| `regions[3].zones[7].biome` | `TEMPLE` |
| `regions[3].zones[7].level` | 41 |
| `regions[3].zones[7].x` | 330 |
| `regions[3].zones[7].y` | 3090 |
| `regions[3].zones[7].from` | [Тропа пантеры](World-REGION_4.md#c4_panther_trail) |
| `regions[3].zones[7].monsters` | [Охотник с трубкой](Monsters.md#blowgun_hunter); [Богомол-великан](Monsters.md#giant_mantis); [Споровик](Monsters.md#sporeling) |
| `regions[3].zones[7].count` | 51; 66 |
| `regions[3].zones[7].light` | 0.85 |
| `regions[3].zones[7].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[7].boss` | [Змеиный оракул](Bosses.md#boss_serpent_oracle) |
| `regions[3].zones[7].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[7].size` | 72 |
| `regions[3].zones[7].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[8].code` | [Янтарные ходы](World-REGION_4.md#c4_amber_tunnels) |
| `regions[3].zones[8].biome` | `HIVE` |
| `regions[3].zones[8].level` | 41 |
| `regions[3].zones[8].x` | 560 |
| `regions[3].zones[8].y` | 3085 |
| `regions[3].zones[8].from` | [Тропа пантеры](World-REGION_4.md#c4_panther_trail); [Осиная лощина](World-REGION_4.md#c4_wasp_hollow) |
| `regions[3].zones[8].monsters` | [Солдат улья](Monsters.md#hive_soldier); [Трутень улья](Monsters.md#hive_drone); [Плевун выводка](Monsters.md#brood_spitter) |
| `regions[3].zones[8].count` | 51; 66 |
| `regions[3].zones[8].light` | 0.7 |
| `regions[3].zones[8].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[8].boss` | [Янтарный страж](Bosses.md#boss_amber_warden) |
| `regions[3].zones[8].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[8].size` | 72 |
| `regions[3].zones[8].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[9].code` | [Плачущий полог](World-REGION_4.md#c4_weeping_canopy) |
| `regions[3].zones[9].biome` | `JUNGLE` |
| `regions[3].zones[9].level` | 41 |
| `regions[3].zones[9].x` | 780 |
| `regions[3].zones[9].y` | 3095 |
| `regions[3].zones[9].from` | [Осиная лощина](World-REGION_4.md#c4_wasp_hollow); [Роща душителей](World-REGION_4.md#c4_strangler_grove) |
| `regions[3].zones[9].monsters` | [Богомол-великан](Monsters.md#giant_mantis); [Изумрудная пантера](Monsters.md#emerald_panther); [Трутень улья](Monsters.md#hive_drone) |
| `regions[3].zones[9].count` | 51; 66 |
| `regions[3].zones[9].light` | 0.9 |
| `regions[3].zones[9].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[9].boss` | [Ужас полога](Bosses.md#boss_canopy_horror) |
| `regions[3].zones[9].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[9].size` | 72 |
| `regions[3].zones[9].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[10].code` | [Грибные низины](World-REGION_4.md#c4_fungal_lowlands) |
| `regions[3].zones[10].biome` | `JUNGLE` |
| `regions[3].zones[10].level` | 43 |
| `regions[3].zones[10].x` | 240 |
| `regions[3].zones[10].y` | 3235 |
| `regions[3].zones[10].from` | [Змеиное святилище](World-REGION_4.md#c4_serpent_shrine) |
| `regions[3].zones[10].monsters` | [Споровик](Monsters.md#sporeling); [Охотник с трубкой](Monsters.md#blowgun_hunter); [Богомол-великан](Monsters.md#giant_mantis) |
| `regions[3].zones[10].count` | 51; 66 |
| `regions[3].zones[10].light` | 0.9 |
| `regions[3].zones[10].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[10].boss` | [Споровый владыка](Bosses.md#boss_spore_lord) |
| `regions[3].zones[10].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[10].size` | 72 |
| `regions[3].zones[10].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[11].code` | [Стоянка пропавшей экспедиции](World-REGION_4.md#c4_lost_expedition) |
| `regions[3].zones[11].biome` | `RUINS` |
| `regions[3].zones[11].level` | 43 |
| `regions[3].zones[11].x` | 440 |
| `regions[3].zones[11].y` | 3240 |
| `regions[3].zones[11].from` | [Змеиное святилище](World-REGION_4.md#c4_serpent_shrine); [Янтарные ходы](World-REGION_4.md#c4_amber_tunnels) |
| `regions[3].zones[11].monsters` | [Охотник с трубкой](Monsters.md#blowgun_hunter); [Изумрудная пантера](Monsters.md#emerald_panther); [Споровик](Monsters.md#sporeling) |
| `regions[3].zones[11].count` | 51; 66 |
| `regions[3].zones[11].light` | 1 |
| `regions[3].zones[11].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[11].boss` | [Обезумевший первопроходец](Bosses.md#boss_mad_explorer) |
| `regions[3].zones[11].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[11].size` | 72 |
| `regions[3].zones[11].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[12].code` | [Выводковые камеры](World-REGION_4.md#c4_brood_chambers) |
| `regions[3].zones[12].biome` | `HIVE` |
| `regions[3].zones[12].level` | 43 |
| `regions[3].zones[12].x` | 650 |
| `regions[3].zones[12].y` | 3230 |
| `regions[3].zones[12].from` | [Янтарные ходы](World-REGION_4.md#c4_amber_tunnels); [Плачущий полог](World-REGION_4.md#c4_weeping_canopy) |
| `regions[3].zones[12].monsters` | [Трутень улья](Monsters.md#hive_drone); [Плевун выводка](Monsters.md#brood_spitter); [Солдат улья](Monsters.md#hive_soldier) |
| `regions[3].zones[12].count` | 51; 66 |
| `regions[3].zones[12].light` | 0.7 |
| `regions[3].zones[12].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[12].boss` | [Кормилица выводка](Bosses.md#boss_brood_nurse) |
| `regions[3].zones[12].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[12].size` | 72 |
| `regions[3].zones[12].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[13].code` | [Затопленная чаща](World-REGION_4.md#c4_flooded_thicket) |
| `regions[3].zones[13].biome` | `MIRE` |
| `regions[3].zones[13].level` | 43 |
| `regions[3].zones[13].x` | 850 |
| `regions[3].zones[13].y` | 3245 |
| `regions[3].zones[13].from` | [Плачущий полог](World-REGION_4.md#c4_weeping_canopy) |
| `regions[3].zones[13].monsters` | [Споровик](Monsters.md#sporeling); [Изумрудная пантера](Monsters.md#emerald_panther); [Плевун выводка](Monsters.md#brood_spitter) |
| `regions[3].zones[13].count` | 51; 66 |
| `regions[3].zones[13].light` | 0.85 |
| `regions[3].zones[13].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[13].boss` | [Гидра чащи](Bosses.md#boss_thicket_hydra) |
| `regions[3].zones[13].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[13].size` | 72 |
| `regions[3].zones[13].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[14].code` | [Древо-великан](World-REGION_4.md#c4_great_tree) |
| `regions[3].zones[14].biome` | `JUNGLE` |
| `regions[3].zones[14].level` | 45 |
| `regions[3].zones[14].x` | 300 |
| `regions[3].zones[14].y` | 3380 |
| `regions[3].zones[14].from` | [Грибные низины](World-REGION_4.md#c4_fungal_lowlands); [Стоянка пропавшей экспедиции](World-REGION_4.md#c4_lost_expedition) |
| `regions[3].zones[14].monsters` | [Изумрудная пантера](Monsters.md#emerald_panther); [Богомол-великан](Monsters.md#giant_mantis); [Охотник с трубкой](Monsters.md#blowgun_hunter) |
| `regions[3].zones[14].count` | 51; 66 |
| `regions[3].zones[14].light` | 0.9 |
| `regions[3].zones[14].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[14].boss` | [Хранитель Древа](Bosses.md#boss_tree_warden) |
| `regions[3].zones[14].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[14].size` | 72 |
| `regions[3].zones[14].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[15].code` | [Сады богомолов](World-REGION_4.md#c4_mantis_gardens) |
| `regions[3].zones[15].biome` | `JUNGLE` |
| `regions[3].zones[15].level` | 45 |
| `regions[3].zones[15].x` | 520 |
| `regions[3].zones[15].y` | 3375 |
| `regions[3].zones[15].from` | [Стоянка пропавшей экспедиции](World-REGION_4.md#c4_lost_expedition) |
| `regions[3].zones[15].monsters` | [Богомол-великан](Monsters.md#giant_mantis); [Трутень улья](Monsters.md#hive_drone); [Споровик](Monsters.md#sporeling) |
| `regions[3].zones[15].count` | 51; 66 |
| `regions[3].zones[15].light` | 0.9 |
| `regions[3].zones[15].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[15].boss` | [Принц богомолов](Bosses.md#boss_mantis_prince) |
| `regions[3].zones[15].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[15].size` | 72 |
| `regions[3].zones[15].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[16].code` | [Маточные соты](World-REGION_4.md#c4_royal_combs) |
| `regions[3].zones[16].biome` | `HIVE` |
| `regions[3].zones[16].level` | 45 |
| `regions[3].zones[16].x` | 760 |
| `regions[3].zones[16].y` | 3385 |
| `regions[3].zones[16].from` | [Затопленная чаща](World-REGION_4.md#c4_flooded_thicket) |
| `regions[3].zones[16].monsters` | [Солдат улья](Monsters.md#hive_soldier); [Плевун выводка](Monsters.md#brood_spitter); [Трутень улья](Monsters.md#hive_drone) |
| `regions[3].zones[16].count` | 51; 66 |
| `regions[3].zones[16].light` | 0.7 |
| `regions[3].zones[16].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[16].boss` | [Страж царицы](Bosses.md#boss_royal_guardian) |
| `regions[3].zones[16].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[16].size` | 72 |
| `regions[3].zones[16].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[3].zones[17].code` | [Сердце улья](World-REGION_4.md#c4_hive_heart) |
| `regions[3].zones[17].biome` | `HIVE` |
| `regions[3].zones[17].level` | 47 |
| `regions[3].zones[17].x` | 530 |
| `regions[3].zones[17].y` | 3525 |
| `regions[3].zones[17].from` | [Древо-великан](World-REGION_4.md#c4_great_tree); [Сады богомолов](World-REGION_4.md#c4_mantis_gardens); [Маточные соты](World-REGION_4.md#c4_royal_combs) |
| `regions[3].zones[17].finale` | Да |
| `regions[3].zones[17].monsters` | [Солдат улья](Monsters.md#hive_soldier); [Трутень улья](Monsters.md#hive_drone); [Плевун выводка](Monsters.md#brood_spitter); [Богомол-великан](Monsters.md#giant_mantis) |
| `regions[3].zones[17].count` | 51; 66 |
| `regions[3].zones[17].light` | 0.7 |
| `regions[3].zones[17].chestLoot` | [loot:CHEST_5](Tables-LOOT.md#loot-chest_5) |
| `regions[3].zones[17].boss` | [Царица улья](Bosses.md#boss_hive_queen) |
| `regions[3].zones[17].corrupted` | [Осквернённый Пожиратель](Monsters.md#corrupted_5) |
| `regions[3].zones[17].size` | 72 |
| `regions[3].zones[17].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].code` | `REGION_5` |
| `regions[4].label.x` | 880 |
| `regions[4].label.y` | 3630 |
| `regions[4].zones[0].code` | [Опалённые предгорья](World-REGION_5.md#c5_scorched_foothills) |
| `regions[4].zones[0].biome` | `VOLCANO` |
| `regions[4].zones[0].level` | 49 |
| `regions[4].zones[0].x` | 310 |
| `regions[4].zones[0].y` | 3700 |
| `regions[4].zones[0].from` | [Сердце улья](World-REGION_4.md#c4_hive_heart) |
| `regions[4].zones[0].monsters` | [Лавовый бес](Monsters.md#lava_imp); [Огненный змей](Monsters.md#fire_drake); [Магмовый голем](Monsters.md#magma_golem) |
| `regions[4].zones[0].count` | 51; 66 |
| `regions[4].zones[0].light` | 0.85 |
| `regions[4].zones[0].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[0].boss` | [Пепельный бегемот](Bosses.md#boss_cinder_behemoth) |
| `regions[4].zones[0].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[0].size` | 72 |
| `regions[4].zones[0].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[1].code` | [Серные копи](World-REGION_5.md#c5_brimstone_mines) |
| `regions[4].zones[1].biome` | `MINES` |
| `regions[4].zones[1].level` | 49 |
| `regions[4].zones[1].x` | 540 |
| `regions[4].zones[1].y` | 3690 |
| `regions[4].zones[1].from` | [Сердце улья](World-REGION_4.md#c4_hive_heart) |
| `regions[4].zones[1].monsters` | [Магмовый голем](Monsters.md#magma_golem); [Лавовый бес](Monsters.md#lava_imp); [Чернокнижник цитадели](Monsters.md#citadel_warlock) |
| `regions[4].zones[1].count` | 51; 66 |
| `regions[4].zones[1].light` | 0.75 |
| `regions[4].zones[1].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[1].boss` | [Серный король](Bosses.md#boss_sulphur_king) |
| `regions[4].zones[1].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[1].size` | 72 |
| `regions[4].zones[1].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[2].code` | [Пограничный форт](World-REGION_5.md#c5_frontier_fort) |
| `regions[4].zones[2].biome` | `CITADEL` |
| `regions[4].zones[2].level` | 49 |
| `regions[4].zones[2].x` | 770 |
| `regions[4].zones[2].y` | 3705 |
| `regions[4].zones[2].from` | [Сердце улья](World-REGION_4.md#c4_hive_heart) |
| `regions[4].zones[2].monsters` | [Инфернальный рыцарь](Monsters.md#infernal_knight); [Чернокнижник цитадели](Monsters.md#citadel_warlock); [Горгулья](Monsters.md#gargoyle) |
| `regions[4].zones[2].count` | 51; 66 |
| `regions[4].zones[2].light` | 0.8 |
| `regions[4].zones[2].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[2].boss` | [Генерал-предатель](Bosses.md#boss_turncoat_general) |
| `regions[4].zones[2].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[2].size` | 72 |
| `regions[4].zones[2].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[3].code` | [Лавовые поля](World-REGION_5.md#c5_lava_fields) |
| `regions[4].zones[3].biome` | `VOLCANO` |
| `regions[4].zones[3].level` | 51 |
| `regions[4].zones[3].x` | 230 |
| `regions[4].zones[3].y` | 3845 |
| `regions[4].zones[3].from` | [Опалённые предгорья](World-REGION_5.md#c5_scorched_foothills) |
| `regions[4].zones[3].monsters` | [Лавовый бес](Monsters.md#lava_imp); [Огненный змей](Monsters.md#fire_drake); [Магмовый голем](Monsters.md#magma_golem) |
| `regions[4].zones[3].count` | 51; 66 |
| `regions[4].zones[3].light` | 0.85 |
| `regions[4].zones[3].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[3].boss` | [Лавовый змей](Bosses.md#boss_lava_wyrm) |
| `regions[4].zones[3].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[3].size` | 72 |
| `regions[4].zones[3].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[4].code` | [Обсидиановые шпили](World-REGION_5.md#c5_obsidian_spires) |
| `regions[4].zones[4].biome` | `VOLCANO` |
| `regions[4].zones[4].level` | 51 |
| `regions[4].zones[4].x` | 440 |
| `regions[4].zones[4].y` | 3850 |
| `regions[4].zones[4].from` | [Опалённые предгорья](World-REGION_5.md#c5_scorched_foothills); [Серные копи](World-REGION_5.md#c5_brimstone_mines) |
| `regions[4].zones[4].monsters` | [Горгулья](Monsters.md#gargoyle); [Магмовый голем](Monsters.md#magma_golem); [Лавовый бес](Monsters.md#lava_imp) |
| `regions[4].zones[4].count` | 51; 66 |
| `regions[4].zones[4].light` | 0.85 |
| `regions[4].zones[4].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[4].boss` | [Обсидиановая гарпия](Bosses.md#boss_obsidian_harpy) |
| `regions[4].zones[4].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[4].size` | 72 |
| `regions[4].zones[4].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[5].code` | [Плавильня](World-REGION_5.md#c5_smeltery) |
| `regions[4].zones[5].biome` | `MINES` |
| `regions[4].zones[5].level` | 51 |
| `regions[4].zones[5].x` | 640 |
| `regions[4].zones[5].y` | 3840 |
| `regions[4].zones[5].from` | [Серные копи](World-REGION_5.md#c5_brimstone_mines); [Пограничный форт](World-REGION_5.md#c5_frontier_fort) |
| `regions[4].zones[5].monsters` | [Магмовый голем](Monsters.md#magma_golem); [Инфернальный рыцарь](Monsters.md#infernal_knight); [Лавовый бес](Monsters.md#lava_imp) |
| `regions[4].zones[5].count` | 51; 66 |
| `regions[4].zones[5].light` | 0.75 |
| `regions[4].zones[5].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[5].boss` | [Хозяин печей](Bosses.md#boss_furnace_master) |
| `regions[4].zones[5].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[5].size` | 72 |
| `regions[4].zones[5].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[6].code` | [Осадный лагерь](World-REGION_5.md#c5_siege_camp) |
| `regions[4].zones[6].biome` | `CITADEL` |
| `regions[4].zones[6].level` | 51 |
| `regions[4].zones[6].x` | 850 |
| `regions[4].zones[6].y` | 3855 |
| `regions[4].zones[6].from` | [Пограничный форт](World-REGION_5.md#c5_frontier_fort) |
| `regions[4].zones[6].monsters` | [Инфернальный рыцарь](Monsters.md#infernal_knight); [Чернокнижник цитадели](Monsters.md#citadel_warlock); [Горгулья](Monsters.md#gargoyle) |
| `regions[4].zones[6].count` | 51; 66 |
| `regions[4].zones[6].light` | 0.8 |
| `regions[4].zones[6].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[6].boss` | [Вечный осаждающий](Bosses.md#boss_eternal_besieger) |
| `regions[4].zones[6].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[6].size` | 72 |
| `regions[4].zones[6].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[7].code` | [Долина пеплопада](World-REGION_5.md#c5_ashfall_valley) |
| `regions[4].zones[7].biome` | `ASH` |
| `regions[4].zones[7].level` | 53 |
| `regions[4].zones[7].x` | 300 |
| `regions[4].zones[7].y` | 3990 |
| `regions[4].zones[7].from` | [Лавовые поля](World-REGION_5.md#c5_lava_fields); [Обсидиановые шпили](World-REGION_5.md#c5_obsidian_spires) |
| `regions[4].zones[7].monsters` | [Лавовый бес](Monsters.md#lava_imp); [Огненный змей](Monsters.md#fire_drake); [Горгулья](Monsters.md#gargoyle) |
| `regions[4].zones[7].count` | 51; 66 |
| `regions[4].zones[7].light` | 0.9 |
| `regions[4].zones[7].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[7].boss` | [Пепельная вдова](Bosses.md#boss_ash_widow) |
| `regions[4].zones[7].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[7].size` | 72 |
| `regions[4].zones[7].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[8].code` | [Огненная река](World-REGION_5.md#c5_magma_river) |
| `regions[4].zones[8].biome` | `VOLCANO` |
| `regions[4].zones[8].level` | 53 |
| `regions[4].zones[8].x` | 530 |
| `regions[4].zones[8].y` | 3985 |
| `regions[4].zones[8].from` | [Обсидиановые шпили](World-REGION_5.md#c5_obsidian_spires) |
| `regions[4].zones[8].monsters` | [Огненный змей](Monsters.md#fire_drake); [Магмовый голем](Monsters.md#magma_golem); [Лавовый бес](Monsters.md#lava_imp) |
| `regions[4].zones[8].count` | 51; 66 |
| `regions[4].zones[8].light` | 0.85 |
| `regions[4].zones[8].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[8].boss` | [Огненный перевозчик](Bosses.md#boss_magma_ferryman) |
| `regions[4].zones[8].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[8].size` | 72 |
| `regions[4].zones[8].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[9].code` | [Внешние стены](World-REGION_5.md#c5_outer_walls) |
| `regions[4].zones[9].biome` | `CITADEL` |
| `regions[4].zones[9].level` | 53 |
| `regions[4].zones[9].x` | 770 |
| `regions[4].zones[9].y` | 3995 |
| `regions[4].zones[9].from` | [Осадный лагерь](World-REGION_5.md#c5_siege_camp) |
| `regions[4].zones[9].monsters` | [Инфернальный рыцарь](Monsters.md#infernal_knight); [Горгулья](Monsters.md#gargoyle); [Чернокнижник цитадели](Monsters.md#citadel_warlock) |
| `regions[4].zones[9].count` | 51; 66 |
| `regions[4].zones[9].light` | 0.8 |
| `regions[4].zones[9].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[9].boss` | [Страж стен](Bosses.md#boss_wall_warden) |
| `regions[4].zones[9].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[9].size` | 72 |
| `regions[4].zones[9].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[10].code` | [Кальдерное озеро](World-REGION_5.md#c5_caldera_lake) |
| `regions[4].zones[10].biome` | `VOLCANO` |
| `regions[4].zones[10].level` | 55 |
| `regions[4].zones[10].x` | 220 |
| `regions[4].zones[10].y` | 4135 |
| `regions[4].zones[10].from` | [Долина пеплопада](World-REGION_5.md#c5_ashfall_valley) |
| `regions[4].zones[10].monsters` | [Огненный змей](Monsters.md#fire_drake); [Лавовый бес](Monsters.md#lava_imp); [Магмовый голем](Monsters.md#magma_golem) |
| `regions[4].zones[10].count` | 51; 66 |
| `regions[4].zones[10].light` | 0.85 |
| `regions[4].zones[10].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[10].boss` | [Кипящий ужас](Bosses.md#boss_boiling_horror) |
| `regions[4].zones[10].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[10].size` | 72 |
| `regions[4].zones[10].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[11].code` | [Пепельная пустошь](World-REGION_5.md#c5_cinder_wastes) |
| `regions[4].zones[11].biome` | `ASH` |
| `regions[4].zones[11].level` | 55 |
| `regions[4].zones[11].x` | 420 |
| `regions[4].zones[11].y` | 4140 |
| `regions[4].zones[11].from` | [Долина пеплопада](World-REGION_5.md#c5_ashfall_valley); [Огненная река](World-REGION_5.md#c5_magma_river) |
| `regions[4].zones[11].monsters` | [Лавовый бес](Monsters.md#lava_imp); [Горгулья](Monsters.md#gargoyle); [Инфернальный рыцарь](Monsters.md#infernal_knight) |
| `regions[4].zones[11].count` | 51; 66 |
| `regions[4].zones[11].light` | 0.9 |
| `regions[4].zones[11].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[11].boss` | [Тлеющий лич](Bosses.md#boss_smouldering_lich) |
| `regions[4].zones[11].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[11].size` | 72 |
| `regions[4].zones[11].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[12].code` | [Военные кузни](World-REGION_5.md#c5_war_forges) |
| `regions[4].zones[12].biome` | `CITADEL` |
| `regions[4].zones[12].level` | 55 |
| `regions[4].zones[12].x` | 630 |
| `regions[4].zones[12].y` | 4130 |
| `regions[4].zones[12].from` | [Огненная река](World-REGION_5.md#c5_magma_river); [Внешние стены](World-REGION_5.md#c5_outer_walls) |
| `regions[4].zones[12].monsters` | [Инфернальный рыцарь](Monsters.md#infernal_knight); [Магмовый голем](Monsters.md#magma_golem); [Чернокнижник цитадели](Monsters.md#citadel_warlock) |
| `regions[4].zones[12].count` | 51; 66 |
| `regions[4].zones[12].light` | 0.8 |
| `regions[4].zones[12].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[12].boss` | [Кузнец войны](Bosses.md#boss_warsmith) |
| `regions[4].zones[12].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[12].size` | 72 |
| `regions[4].zones[12].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[13].code` | [Горящие казармы](World-REGION_5.md#c5_burning_barracks) |
| `regions[4].zones[13].biome` | `CITADEL` |
| `regions[4].zones[13].level` | 55 |
| `regions[4].zones[13].x` | 840 |
| `regions[4].zones[13].y` | 4145 |
| `regions[4].zones[13].from` | [Внешние стены](World-REGION_5.md#c5_outer_walls) |
| `regions[4].zones[13].monsters` | [Инфернальный рыцарь](Monsters.md#infernal_knight); [Чернокнижник цитадели](Monsters.md#citadel_warlock); [Лавовый бес](Monsters.md#lava_imp) |
| `regions[4].zones[13].count` | 51; 66 |
| `regions[4].zones[13].light` | 0.8 |
| `regions[4].zones[13].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[13].boss` | [Горящий капитан](Bosses.md#boss_burning_captain) |
| `regions[4].zones[13].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[13].size` | 72 |
| `regions[4].zones[13].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[14].code` | [Драконье жерло](World-REGION_5.md#c5_dragons_vent) |
| `regions[4].zones[14].biome` | `VOLCANO` |
| `regions[4].zones[14].level` | 57 |
| `regions[4].zones[14].x` | 300 |
| `regions[4].zones[14].y` | 4280 |
| `regions[4].zones[14].from` | [Кальдерное озеро](World-REGION_5.md#c5_caldera_lake); [Пепельная пустошь](World-REGION_5.md#c5_cinder_wastes) |
| `regions[4].zones[14].monsters` | [Огненный змей](Monsters.md#fire_drake); [Лавовый бес](Monsters.md#lava_imp); [Горгулья](Monsters.md#gargoyle) |
| `regions[4].zones[14].count` | 51; 66 |
| `regions[4].zones[14].light` | 0.85 |
| `regions[4].zones[14].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[14].boss` | [Жерловой дракон](Bosses.md#boss_vent_dragon) |
| `regions[4].zones[14].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[14].size` | 72 |
| `regions[4].zones[14].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[15].code` | [Инфернальные врата](World-REGION_5.md#c5_infernal_gate) |
| `regions[4].zones[15].biome` | `CITADEL` |
| `regions[4].zones[15].level` | 57 |
| `regions[4].zones[15].x` | 530 |
| `regions[4].zones[15].y` | 4275 |
| `regions[4].zones[15].from` | [Пепельная пустошь](World-REGION_5.md#c5_cinder_wastes); [Военные кузни](World-REGION_5.md#c5_war_forges) |
| `regions[4].zones[15].monsters` | [Инфернальный рыцарь](Monsters.md#infernal_knight); [Чернокнижник цитадели](Monsters.md#citadel_warlock); [Горгулья](Monsters.md#gargoyle) |
| `regions[4].zones[15].count` | 51; 66 |
| `regions[4].zones[15].light` | 0.8 |
| `regions[4].zones[15].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[15].boss` | [Привратник преисподней](Bosses.md#boss_infernal_gatekeeper) |
| `regions[4].zones[15].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[15].size` | 72 |
| `regions[4].zones[15].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[16].code` | [Расплавленное ядро](World-REGION_5.md#c5_molten_core) |
| `regions[4].zones[16].biome` | `VOLCANO` |
| `regions[4].zones[16].level` | 57 |
| `regions[4].zones[16].x` | 760 |
| `regions[4].zones[16].y` | 4285 |
| `regions[4].zones[16].from` | [Военные кузни](World-REGION_5.md#c5_war_forges) |
| `regions[4].zones[16].monsters` | [Магмовый голем](Monsters.md#magma_golem); [Огненный змей](Monsters.md#fire_drake); [Лавовый бес](Monsters.md#lava_imp) |
| `regions[4].zones[16].count` | 51; 66 |
| `regions[4].zones[16].light` | 0.85 |
| `regions[4].zones[16].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[16].boss` | [Сердце горы](Bosses.md#boss_mountain_heart) |
| `regions[4].zones[16].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[16].size` | 72 |
| `regions[4].zones[16].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[4].zones[17].code` | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| `regions[4].zones[17].biome` | `CITADEL` |
| `regions[4].zones[17].level` | 59 |
| `regions[4].zones[17].x` | 530 |
| `regions[4].zones[17].y` | 4425 |
| `regions[4].zones[17].from` | [Драконье жерло](World-REGION_5.md#c5_dragons_vent); [Инфернальные врата](World-REGION_5.md#c5_infernal_gate); [Расплавленное ядро](World-REGION_5.md#c5_molten_core) |
| `regions[4].zones[17].finale` | Да |
| `regions[4].zones[17].monsters` | [Инфернальный рыцарь](Monsters.md#infernal_knight); [Чернокнижник цитадели](Monsters.md#citadel_warlock); [Горгулья](Monsters.md#gargoyle); [Лавовый бес](Monsters.md#lava_imp) |
| `regions[4].zones[17].count` | 51; 66 |
| `regions[4].zones[17].light` | 0.8 |
| `regions[4].zones[17].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[4].zones[17].boss` | [Владыка Пламени](Bosses.md#boss_flame_sovereign) |
| `regions[4].zones[17].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[4].zones[17].size` | 72 |
| `regions[4].zones[17].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].code` | `REGION_6` |
| `regions[5].label.x` | 880 |
| `regions[5].label.y` | 4530 |
| `regions[5].zones[0].code` | [Край разлома](World-REGION_6.md#c6_rift_edge) |
| `regions[5].zones[0].biome` | `ABYSS` |
| `regions[5].zones[0].level` | 61 |
| `regions[5].zones[0].x` | 300 |
| `regions[5].zones[0].y` | 4600 |
| `regions[5].zones[0].from` | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| `regions[5].zones[0].monsters` | [Порождение пустоты](Monsters.md#void_spawn); [Ползун бездны](Monsters.md#abyss_crawler); [Тень бездны](Monsters.md#abyssal_shade) |
| `regions[5].zones[0].count` | 51; 66 |
| `regions[5].zones[0].light` | 0.6 |
| `regions[5].zones[0].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[0].boss` | [Страж разлома](Bosses.md#boss_rift_watcher) |
| `regions[5].zones[0].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[0].size` | 72 |
| `regions[5].zones[0].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[1].code` | [Чумные поля](World-REGION_6.md#c6_blighted_fields) |
| `regions[5].zones[1].biome` | `BLIGHT` |
| `regions[5].zones[1].level` | 61 |
| `regions[5].zones[1].x` | 530 |
| `regions[5].zones[1].y` | 4590 |
| `regions[5].zones[1].from` | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| `regions[5].zones[1].monsters` | [Гнилошляп](Monsters.md#blightcap); [Чумоносец](Monsters.md#plague_bearer); [Чумная оса](Monsters.md#plague_wasp) |
| `regions[5].zones[1].count` | 51; 66 |
| `regions[5].zones[1].light` | 0.75 |
| `regions[5].zones[1].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[1].boss` | [Чумной жнец](Bosses.md#boss_plague_reaper) |
| `regions[5].zones[1].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[1].size` | 72 |
| `regions[5].zones[1].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[2].code` | [Покинутое аббатство](World-REGION_6.md#c6_forsaken_abbey) |
| `regions[5].zones[2].biome` | `TEMPLE` |
| `regions[5].zones[2].level` | 61 |
| `regions[5].zones[2].x` | 760 |
| `regions[5].zones[2].y` | 4605 |
| `regions[5].zones[2].from` | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| `regions[5].zones[2].monsters` | [Чумоносец](Monsters.md#plague_bearer); [Тень бездны](Monsters.md#abyssal_shade); [Порождение пустоты](Monsters.md#void_spawn) |
| `regions[5].zones[2].count` | 51; 66 |
| `regions[5].zones[2].light` | 0.85 |
| `regions[5].zones[2].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[2].boss` | [Падший аббат](Bosses.md#boss_fallen_abbot) |
| `regions[5].zones[2].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[2].size` | 72 |
| `regions[5].zones[2].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[3].code` | [Шепчущая пропасть](World-REGION_6.md#c6_whispering_chasm) |
| `regions[5].zones[3].biome` | `ABYSS` |
| `regions[5].zones[3].level` | 63 |
| `regions[5].zones[3].x` | 230 |
| `regions[5].zones[3].y` | 4745 |
| `regions[5].zones[3].from` | [Край разлома](World-REGION_6.md#c6_rift_edge) |
| `regions[5].zones[3].monsters` | [Тень бездны](Monsters.md#abyssal_shade); [Порождение пустоты](Monsters.md#void_spawn); [Ползун бездны](Monsters.md#abyss_crawler) |
| `regions[5].zones[3].count` | 51; 66 |
| `regions[5].zones[3].light` | 0.6 |
| `regions[5].zones[3].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[3].boss` | [Шептун](Bosses.md#boss_whisperer) |
| `regions[5].zones[3].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[3].size` | 72 |
| `regions[5].zones[3].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[4].code` | [Гниющий сад](World-REGION_6.md#c6_rotting_orchard) |
| `regions[5].zones[4].biome` | `BLIGHT` |
| `regions[5].zones[4].level` | 63 |
| `regions[5].zones[4].x` | 430 |
| `regions[5].zones[4].y` | 4750 |
| `regions[5].zones[4].from` | [Край разлома](World-REGION_6.md#c6_rift_edge); [Чумные поля](World-REGION_6.md#c6_blighted_fields) |
| `regions[5].zones[4].monsters` | [Гнилошляп](Monsters.md#blightcap); [Чумная оса](Monsters.md#plague_wasp); [Чумоносец](Monsters.md#plague_bearer) |
| `regions[5].zones[4].count` | 51; 66 |
| `regions[5].zones[4].light` | 0.75 |
| `regions[5].zones[4].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[4].boss` | [Садовник гнили](Bosses.md#boss_rot_gardener) |
| `regions[5].zones[4].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[4].size` | 72 |
| `regions[5].zones[4].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[5].code` | [Чумная деревня](World-REGION_6.md#c6_plague_village) |
| `regions[5].zones[5].biome` | `BLIGHT` |
| `regions[5].zones[5].level` | 63 |
| `regions[5].zones[5].x` | 640 |
| `regions[5].zones[5].y` | 4740 |
| `regions[5].zones[5].from` | [Чумные поля](World-REGION_6.md#c6_blighted_fields); [Покинутое аббатство](World-REGION_6.md#c6_forsaken_abbey) |
| `regions[5].zones[5].monsters` | [Чумоносец](Monsters.md#plague_bearer); [Чумная оса](Monsters.md#plague_wasp); [Гнилошляп](Monsters.md#blightcap) |
| `regions[5].zones[5].count` | 51; 66 |
| `regions[5].zones[5].light` | 0.75 |
| `regions[5].zones[5].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[5].boss` | [Чумной лекарь](Bosses.md#boss_plague_doctor) |
| `regions[5].zones[5].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[5].size` | 72 |
| `regions[5].zones[5].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[6].code` | [Костница](World-REGION_6.md#c6_ossuary) |
| `regions[5].zones[6].biome` | `CRYPT` |
| `regions[5].zones[6].level` | 63 |
| `regions[5].zones[6].x` | 850 |
| `regions[5].zones[6].y` | 4755 |
| `regions[5].zones[6].from` | [Покинутое аббатство](World-REGION_6.md#c6_forsaken_abbey) |
| `regions[5].zones[6].monsters` | [Чумоносец](Monsters.md#plague_bearer); [Ползун бездны](Monsters.md#abyss_crawler); [Тень бездны](Monsters.md#abyssal_shade) |
| `regions[5].zones[6].count` | 51; 66 |
| `regions[5].zones[6].light` | 0.7 |
| `regions[5].zones[6].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[6].boss` | [Собиратель костей](Bosses.md#boss_bone_collector) |
| `regions[5].zones[6].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[6].size` | 72 |
| `regions[5].zones[6].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[7].code` | [Берег пустоты](World-REGION_6.md#c6_void_shore) |
| `regions[5].zones[7].biome` | `ABYSS` |
| `regions[5].zones[7].level` | 65 |
| `regions[5].zones[7].x` | 310 |
| `regions[5].zones[7].y` | 4890 |
| `regions[5].zones[7].from` | [Шепчущая пропасть](World-REGION_6.md#c6_whispering_chasm) |
| `regions[5].zones[7].monsters` | [Порождение пустоты](Monsters.md#void_spawn); [Тень бездны](Monsters.md#abyssal_shade); [Ползун бездны](Monsters.md#abyss_crawler) |
| `regions[5].zones[7].count` | 51; 66 |
| `regions[5].zones[7].light` | 0.6 |
| `regions[5].zones[7].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[7].boss` | [Кракен пустоты](Bosses.md#boss_void_kraken) |
| `regions[5].zones[7].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[7].size` | 72 |
| `regions[5].zones[7].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[8].code` | [Споровый собор](World-REGION_6.md#c6_spore_cathedral) |
| `regions[5].zones[8].biome` | `BLIGHT` |
| `regions[5].zones[8].level` | 65 |
| `regions[5].zones[8].x` | 540 |
| `regions[5].zones[8].y` | 4885 |
| `regions[5].zones[8].from` | [Гниющий сад](World-REGION_6.md#c6_rotting_orchard); [Чумная деревня](World-REGION_6.md#c6_plague_village) |
| `regions[5].zones[8].monsters` | [Гнилошляп](Monsters.md#blightcap); [Чумоносец](Monsters.md#plague_bearer); [Тень бездны](Monsters.md#abyssal_shade) |
| `regions[5].zones[8].count` | 51; 66 |
| `regions[5].zones[8].light` | 0.75 |
| `regions[5].zones[8].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[8].boss` | [Споровый кардинал](Bosses.md#boss_spore_cardinal) |
| `regions[5].zones[8].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[8].size` | 72 |
| `regions[5].zones[8].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[9].code` | [Павшая звезда](World-REGION_6.md#c6_fallen_star) |
| `regions[5].zones[9].biome` | `ABYSS` |
| `regions[5].zones[9].level` | 65 |
| `regions[5].zones[9].x` | 770 |
| `regions[5].zones[9].y` | 4895 |
| `regions[5].zones[9].from` | [Чумная деревня](World-REGION_6.md#c6_plague_village) |
| `regions[5].zones[9].monsters` | [Порождение пустоты](Monsters.md#void_spawn); [Ползун бездны](Monsters.md#abyss_crawler); [Чумная оса](Monsters.md#plague_wasp) |
| `regions[5].zones[9].count` | 51; 66 |
| `regions[5].zones[9].light` | 0.6 |
| `regions[5].zones[9].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[9].boss` | [Звёздное отродье](Bosses.md#boss_star_spawn) |
| `regions[5].zones[9].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[9].size` | 72 |
| `regions[5].zones[9].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[10].code` | [Пасть катакомб](World-REGION_6.md#c6_catacomb_maw) |
| `regions[5].zones[10].biome` | `CRYPT` |
| `regions[5].zones[10].level` | 67 |
| `regions[5].zones[10].x` | 230 |
| `regions[5].zones[10].y` | 5035 |
| `regions[5].zones[10].from` | [Берег пустоты](World-REGION_6.md#c6_void_shore) |
| `regions[5].zones[10].monsters` | [Ползун бездны](Monsters.md#abyss_crawler); [Чумоносец](Monsters.md#plague_bearer); [Тень бездны](Monsters.md#abyssal_shade) |
| `regions[5].zones[10].count` | 51; 66 |
| `regions[5].zones[10].light` | 0.7 |
| `regions[5].zones[10].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[10].boss` | [Червь катакомб](Bosses.md#boss_catacomb_worm) |
| `regions[5].zones[10].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[10].size` | 72 |
| `regions[5].zones[10].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[11].code` | [Иссохший лес](World-REGION_6.md#c6_withered_wood) |
| `regions[5].zones[11].biome` | `BLIGHT` |
| `regions[5].zones[11].level` | 67 |
| `regions[5].zones[11].x` | 430 |
| `regions[5].zones[11].y` | 5040 |
| `regions[5].zones[11].from` | [Берег пустоты](World-REGION_6.md#c6_void_shore); [Споровый собор](World-REGION_6.md#c6_spore_cathedral) |
| `regions[5].zones[11].monsters` | [Гнилошляп](Monsters.md#blightcap); [Чумная оса](Monsters.md#plague_wasp); [Чумоносец](Monsters.md#plague_bearer) |
| `regions[5].zones[11].count` | 51; 66 |
| `regions[5].zones[11].light` | 0.75 |
| `regions[5].zones[11].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[11].boss` | [Иссохший великан](Bosses.md#boss_withered_giant) |
| `regions[5].zones[11].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[11].size` | 72 |
| `regions[5].zones[11].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[12].code` | [Алтарь затмения](World-REGION_6.md#c6_eclipse_altar) |
| `regions[5].zones[12].biome` | `TEMPLE` |
| `regions[5].zones[12].level` | 67 |
| `regions[5].zones[12].x` | 640 |
| `regions[5].zones[12].y` | 5030 |
| `regions[5].zones[12].from` | [Споровый собор](World-REGION_6.md#c6_spore_cathedral); [Павшая звезда](World-REGION_6.md#c6_fallen_star) |
| `regions[5].zones[12].monsters` | [Тень бездны](Monsters.md#abyssal_shade); [Порождение пустоты](Monsters.md#void_spawn); [Чумоносец](Monsters.md#plague_bearer) |
| `regions[5].zones[12].count` | 51; 66 |
| `regions[5].zones[12].light` | 0.85 |
| `regions[5].zones[12].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[12].boss` | [Жрица затмения](Bosses.md#boss_eclipse_priestess) |
| `regions[5].zones[12].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[12].size` | 72 |
| `regions[5].zones[12].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[13].code` | [Беззвёздная глубь](World-REGION_6.md#c6_starless_deep) |
| `regions[5].zones[13].biome` | `ABYSS` |
| `regions[5].zones[13].level` | 67 |
| `regions[5].zones[13].x` | 850 |
| `regions[5].zones[13].y` | 5045 |
| `regions[5].zones[13].from` | [Павшая звезда](World-REGION_6.md#c6_fallen_star) |
| `regions[5].zones[13].monsters` | [Ползун бездны](Monsters.md#abyss_crawler); [Порождение пустоты](Monsters.md#void_spawn); [Тень бездны](Monsters.md#abyssal_shade) |
| `regions[5].zones[13].count` | 51; 66 |
| `regions[5].zones[13].light` | 0.6 |
| `regions[5].zones[13].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[13].boss` | [Безглазый](Bosses.md#boss_eyeless_one) |
| `regions[5].zones[13].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[13].size` | 72 |
| `regions[5].zones[13].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[14].code` | [Гнилое сердце](World-REGION_6.md#c6_rotten_heart) |
| `regions[5].zones[14].biome` | `BLIGHT` |
| `regions[5].zones[14].level` | 69 |
| `regions[5].zones[14].x` | 320 |
| `regions[5].zones[14].y` | 5180 |
| `regions[5].zones[14].from` | [Иссохший лес](World-REGION_6.md#c6_withered_wood) |
| `regions[5].zones[14].monsters` | [Гнилошляп](Monsters.md#blightcap); [Чумоносец](Monsters.md#plague_bearer); [Чумная оса](Monsters.md#plague_wasp) |
| `regions[5].zones[14].count` | 51; 66 |
| `regions[5].zones[14].light` | 0.75 |
| `regions[5].zones[14].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[14].boss` | [Мать гнили](Bosses.md#boss_mother_blight) |
| `regions[5].zones[14].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[14].size` | 72 |
| `regions[5].zones[14].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[15].code` | [Завеса теней](World-REGION_6.md#c6_veil_of_shadows) |
| `regions[5].zones[15].biome` | `ABYSS` |
| `regions[5].zones[15].level` | 69 |
| `regions[5].zones[15].x` | 530 |
| `regions[5].zones[15].y` | 5175 |
| `regions[5].zones[15].from` | [Иссохший лес](World-REGION_6.md#c6_withered_wood); [Алтарь затмения](World-REGION_6.md#c6_eclipse_altar) |
| `regions[5].zones[15].monsters` | [Тень бездны](Monsters.md#abyssal_shade); [Порождение пустоты](Monsters.md#void_spawn); [Ползун бездны](Monsters.md#abyss_crawler) |
| `regions[5].zones[15].count` | 51; 66 |
| `regions[5].zones[15].light` | 0.6 |
| `regions[5].zones[15].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[15].boss` | [Ткач завесы](Bosses.md#boss_veil_weaver) |
| `regions[5].zones[15].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[15].size` | 72 |
| `regions[5].zones[15].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[16].code` | [Бесконечная лестница](World-REGION_6.md#c6_endless_stair) |
| `regions[5].zones[16].biome` | `ABYSS` |
| `regions[5].zones[16].level` | 69 |
| `regions[5].zones[16].x` | 750 |
| `regions[5].zones[16].y` | 5185 |
| `regions[5].zones[16].from` | [Алтарь затмения](World-REGION_6.md#c6_eclipse_altar); [Беззвёздная глубь](World-REGION_6.md#c6_starless_deep) |
| `regions[5].zones[16].monsters` | [Порождение пустоты](Monsters.md#void_spawn); [Ползун бездны](Monsters.md#abyss_crawler); [Чумная оса](Monsters.md#plague_wasp) |
| `regions[5].zones[16].count` | 51; 66 |
| `regions[5].zones[16].light` | 0.6 |
| `regions[5].zones[16].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[16].boss` | [Вечный привратник](Bosses.md#boss_endless_warden) |
| `regions[5].zones[16].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[16].size` | 72 |
| `regions[5].zones[16].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[5].zones[17].code` | [Трон бездны](World-REGION_6.md#c6_abyssal_throne) |
| `regions[5].zones[17].biome` | `ABYSS` |
| `regions[5].zones[17].level` | 70 |
| `regions[5].zones[17].x` | 530 |
| `regions[5].zones[17].y` | 5325 |
| `regions[5].zones[17].from` | [Гнилое сердце](World-REGION_6.md#c6_rotten_heart); [Завеса теней](World-REGION_6.md#c6_veil_of_shadows); [Бесконечная лестница](World-REGION_6.md#c6_endless_stair) |
| `regions[5].zones[17].finale` | Да |
| `regions[5].zones[17].monsters` | [Порождение пустоты](Monsters.md#void_spawn); [Тень бездны](Monsters.md#abyssal_shade); [Ползун бездны](Monsters.md#abyss_crawler); [Чумоносец](Monsters.md#plague_bearer) |
| `regions[5].zones[17].count` | 51; 66 |
| `regions[5].zones[17].light` | 0.6 |
| `regions[5].zones[17].chestLoot` | [loot:CHEST_6](Tables-LOOT.md#loot-chest_6) |
| `regions[5].zones[17].boss` | [Король бездны](Bosses.md#boss_abyss_king) |
| `regions[5].zones[17].corrupted` | [Осквернённый Архонт](Monsters.md#corrupted_6) |
| `regions[5].zones[17].size` | 72 |
| `regions[5].zones[17].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].code` | `REGION_7` |
| `regions[6].label.x` | 880 |
| `regions[6].label.y` | 5410 |
| `regions[6].zones[0].code` | [Небесная пристань](World-REGION_7.md#c7_sky_landing) |
| `regions[6].zones[0].biome` | `SKYREACH` |
| `regions[6].zones[0].level` | 71 |
| `regions[6].zones[0].x` | 298 |
| `regions[6].zones[0].y` | 5483 |
| `regions[6].zones[0].from` | [Трон бездны](World-REGION_6.md#c6_abyssal_throne) |
| `regions[6].zones[0].monsters` | [Грозовая гарпия](Monsters.md#storm_harpy); [Падший серафим](Monsters.md#fallen_seraph); [Зефирный призрак](Monsters.md#zephyr_wraith) |
| `regions[6].zones[0].count` | 53; 69 |
| `regions[6].zones[0].light` | 0.8 |
| `regions[6].zones[0].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[0].boss` | [Вестник ветров](Bosses.md#boss_wind_herald) |
| `regions[6].zones[0].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[0].size` | 72 |
| `regions[6].zones[0].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[1].code` | [Стеклянные дюны](World-REGION_7.md#c7_glass_dunes) |
| `regions[6].zones[1].biome` | `GLASSWASTE` |
| `regions[6].zones[1].level` | 71 |
| `regions[6].zones[1].x` | 522 |
| `regions[6].zones[1].y` | 5486 |
| `regions[6].zones[1].from` | [Трон бездны](World-REGION_6.md#c6_abyssal_throne) |
| `regions[6].zones[1].monsters` | [Стеклянный ловчий](Monsters.md#glass_stalker); [Осколочный голем](Monsters.md#shard_golem); [Небесный часовой](Monsters.md#sky_sentinel) |
| `regions[6].zones[1].count` | 53; 69 |
| `regions[6].zones[1].light` | 0.9 |
| `regions[6].zones[1].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[1].boss` | [Стеклянная праматерь](Bosses.md#boss_glass_matriarch) |
| `regions[6].zones[1].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[1].size` | 72 |
| `regions[6].zones[1].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[2].code` | [Громовые ступени](World-REGION_7.md#c7_thunder_steps) |
| `regions[6].zones[2].biome` | `STORMPEAK` |
| `regions[6].zones[2].level` | 71 |
| `regions[6].zones[2].x` | 753 |
| `regions[6].zones[2].y` | 5482 |
| `regions[6].zones[2].from` | [Трон бездны](World-REGION_6.md#c6_abyssal_throne) |
| `regions[6].zones[2].monsters` | [Громовой дрейк](Monsters.md#thunder_drake); [Небесный часовой](Monsters.md#sky_sentinel); [Зефирный призрак](Monsters.md#zephyr_wraith) |
| `regions[6].zones[2].count` | 53; 69 |
| `regions[6].zones[2].light` | 0.7 |
| `regions[6].zones[2].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[2].boss` | [Громовой змей](Bosses.md#boss_thunder_wyrm) |
| `regions[6].zones[2].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[2].size` | 72 |
| `regions[6].zones[2].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[3].code` | [Разбитый мост](World-REGION_7.md#c7_broken_bridge) |
| `regions[6].zones[3].biome` | `SKYREACH` |
| `regions[6].zones[3].level` | 73 |
| `regions[6].zones[3].x` | 229 |
| `regions[6].zones[3].y` | 5622 |
| `regions[6].zones[3].from` | [Небесная пристань](World-REGION_7.md#c7_sky_landing) |
| `regions[6].zones[3].monsters` | [Грозовая гарпия](Monsters.md#storm_harpy); [Падший серафим](Monsters.md#fallen_seraph); [Зефирный призрак](Monsters.md#zephyr_wraith) |
| `regions[6].zones[3].count` | 53; 69 |
| `regions[6].zones[3].light` | 0.8 |
| `regions[6].zones[3].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[3].boss` | [Хранитель моста](Bosses.md#boss_bridge_keeper) |
| `regions[6].zones[3].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[3].size` | 72 |
| `regions[6].zones[3].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[4].code` | [Зеркальная равнина](World-REGION_7.md#c7_mirror_flats) |
| `regions[6].zones[4].biome` | `GLASSWASTE` |
| `regions[6].zones[4].level` | 73 |
| `regions[6].zones[4].x` | 433 |
| `regions[6].zones[4].y` | 5623 |
| `regions[6].zones[4].from` | [Небесная пристань](World-REGION_7.md#c7_sky_landing); [Стеклянные дюны](World-REGION_7.md#c7_glass_dunes) |
| `regions[6].zones[4].monsters` | [Стеклянный ловчий](Monsters.md#glass_stalker); [Осколочный голем](Monsters.md#shard_golem); [Небесный часовой](Monsters.md#sky_sentinel) |
| `regions[6].zones[4].count` | 53; 69 |
| `regions[6].zones[4].light` | 0.9 |
| `regions[6].zones[4].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[4].boss` | [Зеркальный рыцарь](Bosses.md#boss_mirror_knight) |
| `regions[6].zones[4].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[4].size` | 72 |
| `regions[6].zones[4].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[5].code` | [Шпиль молний](World-REGION_7.md#c7_lightning_spire) |
| `regions[6].zones[5].biome` | `STORMPEAK` |
| `regions[6].zones[5].level` | 73 |
| `regions[6].zones[5].x` | 633 |
| `regions[6].zones[5].y` | 5624 |
| `regions[6].zones[5].from` | [Стеклянные дюны](World-REGION_7.md#c7_glass_dunes); [Громовые ступени](World-REGION_7.md#c7_thunder_steps) |
| `regions[6].zones[5].monsters` | [Громовой дрейк](Monsters.md#thunder_drake); [Небесный часовой](Monsters.md#sky_sentinel); [Зефирный призрак](Monsters.md#zephyr_wraith) |
| `regions[6].zones[5].count` | 53; 69 |
| `regions[6].zones[5].light` | 0.7 |
| `regions[6].zones[5].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[5].boss` | [Тиран шпиля](Bosses.md#boss_spire_tyrant) |
| `regions[6].zones[5].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[5].size` | 72 |
| `regions[6].zones[5].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[6].code` | [Храм ветров](World-REGION_7.md#c7_wind_temple) |
| `regions[6].zones[6].biome` | `SKYREACH` |
| `regions[6].zones[6].level` | 73 |
| `regions[6].zones[6].x` | 844 |
| `regions[6].zones[6].y` | 5619 |
| `regions[6].zones[6].from` | [Громовые ступени](World-REGION_7.md#c7_thunder_steps) |
| `regions[6].zones[6].monsters` | [Грозовая гарпия](Monsters.md#storm_harpy); [Падший серафим](Monsters.md#fallen_seraph); [Зефирный призрак](Monsters.md#zephyr_wraith) |
| `regions[6].zones[6].count` | 53; 69 |
| `regions[6].zones[6].light` | 0.8 |
| `regions[6].zones[6].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[6].boss` | [Страж колоколов](Bosses.md#boss_bell_warden) |
| `regions[6].zones[6].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[6].size` | 72 |
| `regions[6].zones[6].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[7].code` | [Расколотый венец](World-REGION_7.md#c7_shattered_crown) |
| `regions[6].zones[7].biome` | `GLASSWASTE` |
| `regions[6].zones[7].level` | 75 |
| `regions[6].zones[7].x` | 223 |
| `regions[6].zones[7].y` | 5764 |
| `regions[6].zones[7].from` | [Разбитый мост](World-REGION_7.md#c7_broken_bridge); [Зеркальная равнина](World-REGION_7.md#c7_mirror_flats) |
| `regions[6].zones[7].monsters` | [Стеклянный ловчий](Monsters.md#glass_stalker); [Осколочный голем](Monsters.md#shard_golem); [Небесный часовой](Monsters.md#sky_sentinel) |
| `regions[6].zones[7].count` | 53; 69 |
| `regions[6].zones[7].light` | 0.9 |
| `regions[6].zones[7].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[7].boss` | [Коронованный осколок](Bosses.md#boss_crown_shard) |
| `regions[6].zones[7].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[7].size` | 72 |
| `regions[6].zones[7].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[8].code` | [Облачная цитадель](World-REGION_7.md#c7_cloud_citadel) |
| `regions[6].zones[8].biome` | `SKYREACH` |
| `regions[6].zones[8].level` | 75 |
| `regions[6].zones[8].x` | 437 |
| `regions[6].zones[8].y` | 5770 |
| `regions[6].zones[8].from` | [Разбитый мост](World-REGION_7.md#c7_broken_bridge); [Зеркальная равнина](World-REGION_7.md#c7_mirror_flats); [Шпиль молний](World-REGION_7.md#c7_lightning_spire) |
| `regions[6].zones[8].monsters` | [Грозовая гарпия](Monsters.md#storm_harpy); [Падший серафим](Monsters.md#fallen_seraph); [Зефирный призрак](Monsters.md#zephyr_wraith) |
| `regions[6].zones[8].count` | 53; 69 |
| `regions[6].zones[8].light` | 0.8 |
| `regions[6].zones[8].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[8].boss` | [Облачный маршал](Bosses.md#boss_cloud_marshal) |
| `regions[6].zones[8].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[8].size` | 72 |
| `regions[6].zones[8].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[9].code` | [Стена бурь](World-REGION_7.md#c7_stormwall) |
| `regions[6].zones[9].biome` | `STORMPEAK` |
| `regions[6].zones[9].level` | 75 |
| `regions[6].zones[9].x` | 631 |
| `regions[6].zones[9].y` | 5776 |
| `regions[6].zones[9].from` | [Зеркальная равнина](World-REGION_7.md#c7_mirror_flats); [Шпиль молний](World-REGION_7.md#c7_lightning_spire); [Храм ветров](World-REGION_7.md#c7_wind_temple) |
| `regions[6].zones[9].monsters` | [Громовой дрейк](Monsters.md#thunder_drake); [Небесный часовой](Monsters.md#sky_sentinel); [Зефирный призрак](Monsters.md#zephyr_wraith) |
| `regions[6].zones[9].count` | 53; 69 |
| `regions[6].zones[9].light` | 0.7 |
| `regions[6].zones[9].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[9].boss` | [Титан бури](Bosses.md#boss_storm_titan) |
| `regions[6].zones[9].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[9].size` | 72 |
| `regions[6].zones[9].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[10].code` | [Призменный каньон](World-REGION_7.md#c7_prism_canyon) |
| `regions[6].zones[10].biome` | `GLASSWASTE` |
| `regions[6].zones[10].level` | 75 |
| `regions[6].zones[10].x` | 847 |
| `regions[6].zones[10].y` | 5767 |
| `regions[6].zones[10].from` | [Шпиль молний](World-REGION_7.md#c7_lightning_spire); [Храм ветров](World-REGION_7.md#c7_wind_temple) |
| `regions[6].zones[10].monsters` | [Стеклянный ловчий](Monsters.md#glass_stalker); [Осколочный голем](Monsters.md#shard_golem); [Небесный часовой](Monsters.md#sky_sentinel) |
| `regions[6].zones[10].count` | 53; 69 |
| `regions[6].zones[10].light` | 0.9 |
| `regions[6].zones[10].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[10].boss` | [Призменный охотник](Bosses.md#boss_prism_hunter) |
| `regions[6].zones[10].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[10].size` | 72 |
| `regions[6].zones[10].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[11].code` | [Павший планетарий](World-REGION_7.md#c7_fallen_orrery) |
| `regions[6].zones[11].biome` | `SKYREACH` |
| `regions[6].zones[11].level` | 78 |
| `regions[6].zones[11].x` | 308 |
| `regions[6].zones[11].y` | 5911 |
| `regions[6].zones[11].from` | [Расколотый венец](World-REGION_7.md#c7_shattered_crown); [Облачная цитадель](World-REGION_7.md#c7_cloud_citadel) |
| `regions[6].zones[11].monsters` | [Грозовая гарпия](Monsters.md#storm_harpy); [Падший серафим](Monsters.md#fallen_seraph); [Зефирный призрак](Monsters.md#zephyr_wraith) |
| `regions[6].zones[11].count` | 53; 69 |
| `regions[6].zones[11].light` | 0.8 |
| `regions[6].zones[11].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[11].boss` | [Машина планетария](Bosses.md#boss_orrery_engine) |
| `regions[6].zones[11].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[11].size` | 72 |
| `regions[6].zones[11].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[12].code` | [Око бури](World-REGION_7.md#c7_eye_of_the_storm) |
| `regions[6].zones[12].biome` | `STORMPEAK` |
| `regions[6].zones[12].level` | 78 |
| `regions[6].zones[12].x` | 526 |
| `regions[6].zones[12].y` | 5918 |
| `regions[6].zones[12].from` | [Облачная цитадель](World-REGION_7.md#c7_cloud_citadel); [Стена бурь](World-REGION_7.md#c7_stormwall) |
| `regions[6].zones[12].monsters` | [Громовой дрейк](Monsters.md#thunder_drake); [Небесный часовой](Monsters.md#sky_sentinel); [Зефирный призрак](Monsters.md#zephyr_wraith) |
| `regions[6].zones[12].count` | 53; 69 |
| `regions[6].zones[12].light` | 0.7 |
| `regions[6].zones[12].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[12].boss` | [Недвижное око](Bosses.md#boss_still_eye) |
| `regions[6].zones[12].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[12].size` | 72 |
| `regions[6].zones[12].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[13].code` | [Поющее стекло](World-REGION_7.md#c7_singing_glass) |
| `regions[6].zones[13].biome` | `GLASSWASTE` |
| `regions[6].zones[13].level` | 78 |
| `regions[6].zones[13].x` | 768 |
| `regions[6].zones[13].y` | 5914 |
| `regions[6].zones[13].from` | [Стена бурь](World-REGION_7.md#c7_stormwall); [Призменный каньон](World-REGION_7.md#c7_prism_canyon) |
| `regions[6].zones[13].monsters` | [Стеклянный ловчий](Monsters.md#glass_stalker); [Осколочный голем](Monsters.md#shard_golem); [Небесный часовой](Monsters.md#sky_sentinel) |
| `regions[6].zones[13].count` | 53; 69 |
| `regions[6].zones[13].light` | 0.9 |
| `regions[6].zones[13].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[13].boss` | [Стеклянный хор](Bosses.md#boss_glass_choir) |
| `regions[6].zones[13].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[13].size` | 72 |
| `regions[6].zones[13].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[6].zones[14].code` | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| `regions[6].zones[14].biome` | `STORMPEAK` |
| `regions[6].zones[14].level` | 80 |
| `regions[6].zones[14].x` | 524 |
| `regions[6].zones[14].y` | 6052 |
| `regions[6].zones[14].from` | [Павший планетарий](World-REGION_7.md#c7_fallen_orrery); [Око бури](World-REGION_7.md#c7_eye_of_the_storm) |
| `regions[6].zones[14].finale` | Да |
| `regions[6].zones[14].monsters` | [Громовой дрейк](Monsters.md#thunder_drake); [Небесный часовой](Monsters.md#sky_sentinel); [Зефирный призрак](Monsters.md#zephyr_wraith); [Осколочный голем](Monsters.md#shard_golem) |
| `regions[6].zones[14].count` | 53; 69 |
| `regions[6].zones[14].light` | 0.7 |
| `regions[6].zones[14].chestLoot` | [loot:CHEST_7](Tables-LOOT.md#loot-chest_7) |
| `regions[6].zones[14].boss` | [Владыка разбитого неба](Bosses.md#boss_sky_sovereign) |
| `regions[6].zones[14].corrupted` | [Осквернённый повелитель бурь](Monsters.md#corrupted_7) |
| `regions[6].zones[14].size` | 72 |
| `regions[6].zones[14].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].code` | `REGION_8` |
| `regions[7].label.x` | 880 |
| `regions[7].label.y` | 6310 |
| `regions[7].zones[0].code` | [Затонувшие врата](World-REGION_8.md#c8_sunken_gate) |
| `regions[7].zones[0].biome` | `SUNKEN` |
| `regions[7].zones[0].level` | 81 |
| `regions[7].zones[0].x` | 303 |
| `regions[7].zones[0].y` | 6375 |
| `regions[7].zones[0].from` | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| `regions[7].zones[0].monsters` | [Утопленник-легионер](Monsters.md#drowned_legionary); [Глубинный скрытень](Monsters.md#deep_lurker); [Солёный дрейк](Monsters.md#brine_drake) |
| `regions[7].zones[0].count` | 55; 71 |
| `regions[7].zones[0].light` | 0.8 |
| `regions[7].zones[0].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[0].boss` | [Претор врат](Bosses.md#boss_gate_praetor) |
| `regions[7].zones[0].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[0].size` | 72 |
| `regions[7].zones[0].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[1].code` | [Коралловые сады](World-REGION_8.md#c8_coral_gardens) |
| `regions[7].zones[1].biome` | `CORAL` |
| `regions[7].zones[1].level` | 81 |
| `regions[7].zones[1].x` | 536 |
| `regions[7].zones[1].y` | 6372 |
| `regions[7].zones[1].from` | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| `regions[7].zones[1].monsters` | [Коралловый исполин](Monsters.md#coral_behemoth); [Жемчужная сирена](Monsters.md#pearl_siren); [Бездонный угорь](Monsters.md#abyssal_eel) |
| `regions[7].zones[1].count` | 55; 71 |
| `regions[7].zones[1].light` | 0.9 |
| `regions[7].zones[1].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[1].boss` | [Коралловая королева](Bosses.md#boss_coral_queen) |
| `regions[7].zones[1].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[1].size` | 72 |
| `regions[7].zones[1].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[2].code` | [Приливные хранилища](World-REGION_8.md#c8_tide_vaults) |
| `regions[7].zones[2].biome` | `TIDEVAULT` |
| `regions[7].zones[2].level` | 81 |
| `regions[7].zones[2].x` | 767 |
| `regions[7].zones[2].y` | 6379 |
| `regions[7].zones[2].from` | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| `regions[7].zones[2].monsters` | [Приливный конструкт](Monsters.md#tide_construct); [Бездонный угорь](Monsters.md#abyssal_eel); [Солёный дрейк](Monsters.md#brine_drake) |
| `regions[7].zones[2].count` | 55; 71 |
| `regions[7].zones[2].light` | 0.7 |
| `regions[7].zones[2].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[2].boss` | [Страж хранилищ](Bosses.md#boss_vault_warden) |
| `regions[7].zones[2].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[2].size` | 72 |
| `regions[7].zones[2].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[3].code` | [Утонувший форум](World-REGION_8.md#c8_drowned_forum) |
| `regions[7].zones[3].biome` | `SUNKEN` |
| `regions[7].zones[3].level` | 83 |
| `regions[7].zones[3].x` | 233 |
| `regions[7].zones[3].y` | 6530 |
| `regions[7].zones[3].from` | [Затонувшие врата](World-REGION_8.md#c8_sunken_gate) |
| `regions[7].zones[3].monsters` | [Утопленник-легионер](Monsters.md#drowned_legionary); [Глубинный скрытень](Monsters.md#deep_lurker); [Солёный дрейк](Monsters.md#brine_drake) |
| `regions[7].zones[3].count` | 55; 71 |
| `regions[7].zones[3].light` | 0.8 |
| `regions[7].zones[3].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[3].boss` | [Утонувший оратор](Bosses.md#boss_drowned_orator) |
| `regions[7].zones[3].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[3].size` | 72 |
| `regions[7].zones[3].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[4].code` | [Жемчужный риф](World-REGION_8.md#c8_pearl_reef) |
| `regions[7].zones[4].biome` | `CORAL` |
| `regions[7].zones[4].level` | 83 |
| `regions[7].zones[4].x` | 425 |
| `regions[7].zones[4].y` | 6531 |
| `regions[7].zones[4].from` | [Затонувшие врата](World-REGION_8.md#c8_sunken_gate); [Коралловые сады](World-REGION_8.md#c8_coral_gardens) |
| `regions[7].zones[4].monsters` | [Коралловый исполин](Monsters.md#coral_behemoth); [Жемчужная сирена](Monsters.md#pearl_siren); [Бездонный угорь](Monsters.md#abyssal_eel) |
| `regions[7].zones[4].count` | 55; 71 |
| `regions[7].zones[4].light` | 0.9 |
| `regions[7].zones[4].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[4].boss` | [Пожиратель жемчуга](Bosses.md#boss_pearl_eater) |
| `regions[7].zones[4].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[4].size` | 72 |
| `regions[7].zones[4].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[5].code` | [Затопленный акведук](World-REGION_8.md#c8_flooded_aqueduct) |
| `regions[7].zones[5].biome` | `TIDEVAULT` |
| `regions[7].zones[5].level` | 83 |
| `regions[7].zones[5].x` | 649 |
| `regions[7].zones[5].y` | 6528 |
| `regions[7].zones[5].from` | [Коралловые сады](World-REGION_8.md#c8_coral_gardens); [Приливные хранилища](World-REGION_8.md#c8_tide_vaults) |
| `regions[7].zones[5].monsters` | [Приливный конструкт](Monsters.md#tide_construct); [Бездонный угорь](Monsters.md#abyssal_eel); [Солёный дрейк](Monsters.md#brine_drake) |
| `regions[7].zones[5].count` | 55; 71 |
| `regions[7].zones[5].light` | 0.7 |
| `regions[7].zones[5].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[5].boss` | [Ужас акведука](Bosses.md#boss_aqueduct_horror) |
| `regions[7].zones[5].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[5].size` | 72 |
| `regions[7].zones[5].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[6].code` | [Казармы легиона](World-REGION_8.md#c8_legion_barracks) |
| `regions[7].zones[6].biome` | `SUNKEN` |
| `regions[7].zones[6].level` | 83 |
| `regions[7].zones[6].x` | 849 |
| `regions[7].zones[6].y` | 6527 |
| `regions[7].zones[6].from` | [Приливные хранилища](World-REGION_8.md#c8_tide_vaults) |
| `regions[7].zones[6].monsters` | [Утопленник-легионер](Monsters.md#drowned_legionary); [Глубинный скрытень](Monsters.md#deep_lurker); [Солёный дрейк](Monsters.md#brine_drake) |
| `regions[7].zones[6].count` | 55; 71 |
| `regions[7].zones[6].light` | 0.8 |
| `regions[7].zones[6].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[6].boss` | [Трибун легиона](Bosses.md#boss_legion_tribune) |
| `regions[7].zones[6].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[6].size` | 72 |
| `regions[7].zones[6].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[7].code` | [Собор водорослей](World-REGION_8.md#c8_kelp_cathedral) |
| `regions[7].zones[7].biome` | `CORAL` |
| `regions[7].zones[7].level` | 85 |
| `regions[7].zones[7].x` | 221 |
| `regions[7].zones[7].y` | 6668 |
| `regions[7].zones[7].from` | [Утонувший форум](World-REGION_8.md#c8_drowned_forum); [Жемчужный риф](World-REGION_8.md#c8_pearl_reef) |
| `regions[7].zones[7].monsters` | [Коралловый исполин](Monsters.md#coral_behemoth); [Жемчужная сирена](Monsters.md#pearl_siren); [Бездонный угорь](Monsters.md#abyssal_eel) |
| `regions[7].zones[7].count` | 55; 71 |
| `regions[7].zones[7].light` | 0.9 |
| `regions[7].zones[7].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[7].boss` | [Жрец водорослей](Bosses.md#boss_kelp_priest) |
| `regions[7].zones[7].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[7].size` | 72 |
| `regions[7].zones[7].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[8].code` | [Запечатанная цистерна](World-REGION_8.md#c8_sealed_cistern) |
| `regions[7].zones[8].biome` | `TIDEVAULT` |
| `regions[7].zones[8].level` | 85 |
| `regions[7].zones[8].x` | 425 |
| `regions[7].zones[8].y` | 6672 |
| `regions[7].zones[8].from` | [Утонувший форум](World-REGION_8.md#c8_drowned_forum); [Жемчужный риф](World-REGION_8.md#c8_pearl_reef); [Затопленный акведук](World-REGION_8.md#c8_flooded_aqueduct) |
| `regions[7].zones[8].monsters` | [Приливный конструкт](Monsters.md#tide_construct); [Бездонный угорь](Monsters.md#abyssal_eel); [Солёный дрейк](Monsters.md#brine_drake) |
| `regions[7].zones[8].count` | 55; 71 |
| `regions[7].zones[8].light` | 0.7 |
| `regions[7].zones[8].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[8].boss` | [То, что в цистерне](Bosses.md#boss_cistern_thing) |
| `regions[7].zones[8].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[8].size` | 72 |
| `regions[7].zones[8].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[9].code` | [Затонувший рынок](World-REGION_8.md#c8_sunken_market) |
| `regions[7].zones[9].biome` | `SUNKEN` |
| `regions[7].zones[9].level` | 85 |
| `regions[7].zones[9].x` | 632 |
| `regions[7].zones[9].y` | 6678 |
| `regions[7].zones[9].from` | [Жемчужный риф](World-REGION_8.md#c8_pearl_reef); [Затопленный акведук](World-REGION_8.md#c8_flooded_aqueduct); [Казармы легиона](World-REGION_8.md#c8_legion_barracks) |
| `regions[7].zones[9].monsters` | [Утопленник-легионер](Monsters.md#drowned_legionary); [Глубинный скрытень](Monsters.md#deep_lurker); [Солёный дрейк](Monsters.md#brine_drake) |
| `regions[7].zones[9].count` | 55; 71 |
| `regions[7].zones[9].light` | 0.8 |
| `regions[7].zones[9].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[9].boss` | [Рыночный барон](Bosses.md#boss_market_baron) |
| `regions[7].zones[9].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[9].size` | 72 |
| `regions[7].zones[9].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[10].code` | [Выбеленный атолл](World-REGION_8.md#c8_bleached_atoll) |
| `regions[7].zones[10].biome` | `CORAL` |
| `regions[7].zones[10].level` | 85 |
| `regions[7].zones[10].x` | 856 |
| `regions[7].zones[10].y` | 6665 |
| `regions[7].zones[10].from` | [Затопленный акведук](World-REGION_8.md#c8_flooded_aqueduct); [Казармы легиона](World-REGION_8.md#c8_legion_barracks) |
| `regions[7].zones[10].monsters` | [Коралловый исполин](Monsters.md#coral_behemoth); [Жемчужная сирена](Monsters.md#pearl_siren); [Бездонный угорь](Monsters.md#abyssal_eel) |
| `regions[7].zones[10].count` | 55; 71 |
| `regions[7].zones[10].light` | 0.9 |
| `regions[7].zones[10].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[10].boss` | [Пасть атолла](Bosses.md#boss_atoll_maw) |
| `regions[7].zones[10].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[10].size` | 72 |
| `regions[7].zones[10].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[11].code` | [Машина приливов](World-REGION_8.md#c8_tidal_engine) |
| `regions[7].zones[11].biome` | `TIDEVAULT` |
| `regions[7].zones[11].level` | 88 |
| `regions[7].zones[11].x` | 296 |
| `regions[7].zones[11].y` | 6822 |
| `regions[7].zones[11].from` | [Собор водорослей](World-REGION_8.md#c8_kelp_cathedral); [Запечатанная цистерна](World-REGION_8.md#c8_sealed_cistern) |
| `regions[7].zones[11].monsters` | [Приливный конструкт](Monsters.md#tide_construct); [Бездонный угорь](Monsters.md#abyssal_eel); [Солёный дрейк](Monsters.md#brine_drake) |
| `regions[7].zones[11].count` | 55; 71 |
| `regions[7].zones[11].light` | 0.7 |
| `regions[7].zones[11].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[11].boss` | [Инженер приливов](Bosses.md#boss_tide_engineer) |
| `regions[7].zones[11].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[11].size` | 72 |
| `regions[7].zones[11].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[12].code` | [Императорские термы](World-REGION_8.md#c8_imperial_baths) |
| `regions[7].zones[12].biome` | `SUNKEN` |
| `regions[7].zones[12].level` | 88 |
| `regions[7].zones[12].x` | 520 |
| `regions[7].zones[12].y` | 6821 |
| `regions[7].zones[12].from` | [Запечатанная цистерна](World-REGION_8.md#c8_sealed_cistern); [Затонувший рынок](World-REGION_8.md#c8_sunken_market) |
| `regions[7].zones[12].monsters` | [Утопленник-легионер](Monsters.md#drowned_legionary); [Глубинный скрытень](Monsters.md#deep_lurker); [Солёный дрейк](Monsters.md#brine_drake) |
| `regions[7].zones[12].count` | 55; 71 |
| `regions[7].zones[12].light` | 0.8 |
| `regions[7].zones[12].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[12].boss` | [Хозяйка терм](Bosses.md#boss_bath_matron) |
| `regions[7].zones[12].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[12].size` | 72 |
| `regions[7].zones[12].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[13].code` | [Бездонная впадина](World-REGION_8.md#c8_abyssal_trench) |
| `regions[7].zones[13].biome` | `CORAL` |
| `regions[7].zones[13].level` | 88 |
| `regions[7].zones[13].x` | 764 |
| `regions[7].zones[13].y` | 6816 |
| `regions[7].zones[13].from` | [Затонувший рынок](World-REGION_8.md#c8_sunken_market); [Выбеленный атолл](World-REGION_8.md#c8_bleached_atoll) |
| `regions[7].zones[13].monsters` | [Коралловый исполин](Monsters.md#coral_behemoth); [Жемчужная сирена](Monsters.md#pearl_siren); [Бездонный угорь](Monsters.md#abyssal_eel) |
| `regions[7].zones[13].count` | 55; 71 |
| `regions[7].zones[13].light` | 0.9 |
| `regions[7].zones[13].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[13].boss` | [Дрейк впадины](Bosses.md#boss_trench_drake) |
| `regions[7].zones[13].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[13].size` | 72 |
| `regions[7].zones[13].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[7].zones[14].code` | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| `regions[7].zones[14].biome` | `TIDEVAULT` |
| `regions[7].zones[14].level` | 90 |
| `regions[7].zones[14].x` | 521 |
| `regions[7].zones[14].y` | 6957 |
| `regions[7].zones[14].from` | [Машина приливов](World-REGION_8.md#c8_tidal_engine); [Императорские термы](World-REGION_8.md#c8_imperial_baths) |
| `regions[7].zones[14].finale` | Да |
| `regions[7].zones[14].monsters` | [Приливный конструкт](Monsters.md#tide_construct); [Бездонный угорь](Monsters.md#abyssal_eel); [Солёный дрейк](Monsters.md#brine_drake); [Жемчужная сирена](Monsters.md#pearl_siren) |
| `regions[7].zones[14].count` | 55; 71 |
| `regions[7].zones[14].light` | 0.7 |
| `regions[7].zones[14].chestLoot` | [loot:CHEST_8](Tables-LOOT.md#loot-chest_8) |
| `regions[7].zones[14].boss` | [Утонувший император](Bosses.md#boss_drowned_emperor) |
| `regions[7].zones[14].corrupted` | [Осквернённый левиафан](Monsters.md#corrupted_8) |
| `regions[7].zones[14].size` | 72 |
| `regions[7].zones[14].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].code` | `REGION_9` |
| `regions[8].label.x` | 880 |
| `regions[8].label.y` | 7210 |
| `regions[8].zones[0].code` | [Порог богов](World-REGION_9.md#c9_gods_threshold) |
| `regions[8].zones[0].biome` | `GODHALL` |
| `regions[8].zones[0].level` | 91 |
| `regions[8].zones[0].x` | 306 |
| `regions[8].zones[0].y` | 7277 |
| `regions[8].zones[0].from` | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| `regions[8].zones[0].monsters` | [Божественный страж](Monsters.md#divine_warden); [Падший архонт](Monsters.md#fallen_archon); [Титан пустоты](Monsters.md#void_titan) |
| `regions[8].zones[0].count` | 57; 74 |
| `regions[8].zones[0].light` | 0.8 |
| `regions[8].zones[0].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[0].boss` | [Страж порога](Bosses.md#boss_threshold_guardian) |
| `regions[8].zones[0].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[0].size` | 72 |
| `regions[8].zones[0].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[1].code` | [Астральный берег](World-REGION_9.md#c9_astral_shore) |
| `regions[8].zones[1].biome` | `ASTRAL` |
| `regions[8].zones[1].level` | 91 |
| `regions[8].zones[1].x` | 536 |
| `regions[8].zones[1].y` | 7272 |
| `regions[8].zones[1].from` | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| `regions[8].zones[1].monsters` | [Астральный зверь](Monsters.md#astral_beast); [Звёздный дракон](Monsters.md#star_dragon); [Клятвенный фанатик](Monsters.md#godsworn_zealot) |
| `regions[8].zones[1].count` | 57; 74 |
| `regions[8].zones[1].light` | 0.9 |
| `regions[8].zones[1].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[1].boss` | [Звёздный прилив](Bosses.md#boss_star_tide) |
| `regions[8].zones[1].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[1].size` | 72 |
| `regions[8].zones[1].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[2].code` | [Поля забвения](World-REGION_9.md#c9_fields_of_oblivion) |
| `regions[8].zones[2].biome` | `OBLIVION` |
| `regions[8].zones[2].level` | 91 |
| `regions[8].zones[2].x` | 764 |
| `regions[8].zones[2].y` | 7278 |
| `regions[8].zones[2].from` | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| `regions[8].zones[2].monsters` | [Тень забвения](Monsters.md#oblivion_shade); [Клятвенный фанатик](Monsters.md#godsworn_zealot); [Титан пустоты](Monsters.md#void_titan) |
| `regions[8].zones[2].count` | 57; 74 |
| `regions[8].zones[2].light` | 0.7 |
| `regions[8].zones[2].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[2].boss` | [Забвение](Bosses.md#boss_forgetting) |
| `regions[8].zones[2].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[2].size` | 72 |
| `regions[8].zones[2].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[3].code` | [Зал суда](World-REGION_9.md#c9_hall_of_judgement) |
| `regions[8].zones[3].biome` | `GODHALL` |
| `regions[8].zones[3].level` | 93 |
| `regions[8].zones[3].x` | 240 |
| `regions[8].zones[3].y` | 7430 |
| `regions[8].zones[3].from` | [Порог богов](World-REGION_9.md#c9_gods_threshold) |
| `regions[8].zones[3].monsters` | [Божественный страж](Monsters.md#divine_warden); [Падший архонт](Monsters.md#fallen_archon); [Титан пустоты](Monsters.md#void_titan) |
| `regions[8].zones[3].count` | 57; 74 |
| `regions[8].zones[3].light` | 0.8 |
| `regions[8].zones[3].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[3].boss` | [Судья душ](Bosses.md#boss_judge_of_souls) |
| `regions[8].zones[3].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[3].size` | 72 |
| `regions[8].zones[3].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[4].code` | [Мост созвездий](World-REGION_9.md#c9_constellation_bridge) |
| `regions[8].zones[4].biome` | `ASTRAL` |
| `regions[8].zones[4].level` | 93 |
| `regions[8].zones[4].x` | 424 |
| `regions[8].zones[4].y` | 7423 |
| `regions[8].zones[4].from` | [Порог богов](World-REGION_9.md#c9_gods_threshold); [Астральный берег](World-REGION_9.md#c9_astral_shore) |
| `regions[8].zones[4].monsters` | [Астральный зверь](Monsters.md#astral_beast); [Звёздный дракон](Monsters.md#star_dragon); [Клятвенный фанатик](Monsters.md#godsworn_zealot) |
| `regions[8].zones[4].count` | 57; 74 |
| `regions[8].zones[4].light` | 0.9 |
| `regions[8].zones[4].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[4].boss` | [Зверь созвездий](Bosses.md#boss_constellation_beast) |
| `regions[8].zones[4].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[4].size` | 72 |
| `regions[8].zones[4].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[5].code` | [Безымянный архив](World-REGION_9.md#c9_nameless_archive) |
| `regions[8].zones[5].biome` | `OBLIVION` |
| `regions[8].zones[5].level` | 93 |
| `regions[8].zones[5].x` | 645 |
| `regions[8].zones[5].y` | 7422 |
| `regions[8].zones[5].from` | [Астральный берег](World-REGION_9.md#c9_astral_shore); [Поля забвения](World-REGION_9.md#c9_fields_of_oblivion) |
| `regions[8].zones[5].monsters` | [Тень забвения](Monsters.md#oblivion_shade); [Клятвенный фанатик](Monsters.md#godsworn_zealot); [Титан пустоты](Monsters.md#void_titan) |
| `regions[8].zones[5].count` | 57; 74 |
| `regions[8].zones[5].light` | 0.7 |
| `regions[8].zones[5].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[5].boss` | [Последний архивариус](Bosses.md#boss_archivist) |
| `regions[8].zones[5].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[5].size` | 72 |
| `regions[8].zones[5].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[6].code` | [Солнечная кузня](World-REGION_9.md#c9_sunforge) |
| `regions[8].zones[6].biome` | `GODHALL` |
| `regions[8].zones[6].level` | 93 |
| `regions[8].zones[6].x` | 852 |
| `regions[8].zones[6].y` | 7426 |
| `regions[8].zones[6].from` | [Поля забвения](World-REGION_9.md#c9_fields_of_oblivion) |
| `regions[8].zones[6].monsters` | [Божественный страж](Monsters.md#divine_warden); [Падший архонт](Monsters.md#fallen_archon); [Титан пустоты](Monsters.md#void_titan) |
| `regions[8].zones[6].count` | 57; 74 |
| `regions[8].zones[6].light` | 0.8 |
| `regions[8].zones[6].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[6].boss` | [Солнечный кузнец](Bosses.md#boss_sun_smith) |
| `regions[8].zones[6].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[6].size` | 72 |
| `regions[8].zones[6].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[7].code` | [Астральный сад](World-REGION_9.md#c9_astral_orchard) |
| `regions[8].zones[7].biome` | `ASTRAL` |
| `regions[8].zones[7].level` | 95 |
| `regions[8].zones[7].x` | 226 |
| `regions[8].zones[7].y` | 7563 |
| `regions[8].zones[7].from` | [Зал суда](World-REGION_9.md#c9_hall_of_judgement); [Мост созвездий](World-REGION_9.md#c9_constellation_bridge) |
| `regions[8].zones[7].monsters` | [Астральный зверь](Monsters.md#astral_beast); [Звёздный дракон](Monsters.md#star_dragon); [Клятвенный фанатик](Monsters.md#godsworn_zealot) |
| `regions[8].zones[7].count` | 57; 74 |
| `regions[8].zones[7].light` | 0.9 |
| `regions[8].zones[7].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[7].boss` | [Хранитель сада](Bosses.md#boss_orchard_keeper) |
| `regions[8].zones[7].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[7].size` | 72 |
| `regions[8].zones[7].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[8].code` | [Пустой пантеон](World-REGION_9.md#c9_hollow_pantheon) |
| `regions[8].zones[8].biome` | `OBLIVION` |
| `regions[8].zones[8].level` | 95 |
| `regions[8].zones[8].x` | 428 |
| `regions[8].zones[8].y` | 7568 |
| `regions[8].zones[8].from` | [Зал суда](World-REGION_9.md#c9_hall_of_judgement); [Мост созвездий](World-REGION_9.md#c9_constellation_bridge); [Безымянный архив](World-REGION_9.md#c9_nameless_archive) |
| `regions[8].zones[8].monsters` | [Тень забвения](Monsters.md#oblivion_shade); [Клятвенный фанатик](Monsters.md#godsworn_zealot); [Титан пустоты](Monsters.md#void_titan) |
| `regions[8].zones[8].count` | 57; 74 |
| `regions[8].zones[8].light` | 0.7 |
| `regions[8].zones[8].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[8].boss` | [Пустой бог](Bosses.md#boss_hollow_god) |
| `regions[8].zones[8].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[8].size` | 72 |
| `regions[8].zones[8].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[9].code` | [Зал войны](World-REGION_9.md#c9_hall_of_war) |
| `regions[8].zones[9].biome` | `GODHALL` |
| `regions[8].zones[9].level` | 95 |
| `regions[8].zones[9].x` | 638 |
| `regions[8].zones[9].y` | 7564 |
| `regions[8].zones[9].from` | [Мост созвездий](World-REGION_9.md#c9_constellation_bridge); [Безымянный архив](World-REGION_9.md#c9_nameless_archive); [Солнечная кузня](World-REGION_9.md#c9_sunforge) |
| `regions[8].zones[9].monsters` | [Божественный страж](Monsters.md#divine_warden); [Падший архонт](Monsters.md#fallen_archon); [Титан пустоты](Monsters.md#void_titan) |
| `regions[8].zones[9].count` | 57; 74 |
| `regions[8].zones[9].light` | 0.8 |
| `regions[8].zones[9].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[9].boss` | [Аватар войны](Bosses.md#boss_war_avatar) |
| `regions[8].zones[9].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[9].size` | 72 |
| `regions[8].zones[9].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[10].code` | [Беззвёздная пустота](World-REGION_9.md#c9_starless_void) |
| `regions[8].zones[10].biome` | `OBLIVION` |
| `regions[8].zones[10].level` | 95 |
| `regions[8].zones[10].x` | 858 |
| `regions[8].zones[10].y` | 7577 |
| `regions[8].zones[10].from` | [Безымянный архив](World-REGION_9.md#c9_nameless_archive); [Солнечная кузня](World-REGION_9.md#c9_sunforge) |
| `regions[8].zones[10].monsters` | [Тень забвения](Monsters.md#oblivion_shade); [Клятвенный фанатик](Monsters.md#godsworn_zealot); [Титан пустоты](Monsters.md#void_titan) |
| `regions[8].zones[10].count` | 57; 74 |
| `regions[8].zones[10].light` | 0.7 |
| `regions[8].zones[10].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[10].boss` | [Мать пустоты](Bosses.md#boss_void_mother) |
| `regions[8].zones[10].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[10].size` | 72 |
| `regions[8].zones[10].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[11].code` | [Небесная обсерватория](World-REGION_9.md#c9_celestial_observatory) |
| `regions[8].zones[11].biome` | `ASTRAL` |
| `regions[8].zones[11].level` | 98 |
| `regions[8].zones[11].x` | 294 |
| `regions[8].zones[11].y` | 7710 |
| `regions[8].zones[11].from` | [Астральный сад](World-REGION_9.md#c9_astral_orchard); [Пустой пантеон](World-REGION_9.md#c9_hollow_pantheon) |
| `regions[8].zones[11].monsters` | [Астральный зверь](Monsters.md#astral_beast); [Звёздный дракон](Monsters.md#star_dragon); [Клятвенный фанатик](Monsters.md#godsworn_zealot) |
| `regions[8].zones[11].count` | 57; 74 |
| `regions[8].zones[11].light` | 0.9 |
| `regions[8].zones[11].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[11].boss` | [Звездочёт](Bosses.md#boss_stargazer) |
| `regions[8].zones[11].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[11].size` | 72 |
| `regions[8].zones[11].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[12].code` | [Дорога к трону](World-REGION_9.md#c9_throne_road) |
| `regions[8].zones[12].biome` | `GODHALL` |
| `regions[8].zones[12].level` | 98 |
| `regions[8].zones[12].x` | 528 |
| `regions[8].zones[12].y` | 7716 |
| `regions[8].zones[12].from` | [Пустой пантеон](World-REGION_9.md#c9_hollow_pantheon); [Зал войны](World-REGION_9.md#c9_hall_of_war) |
| `regions[8].zones[12].monsters` | [Божественный страж](Monsters.md#divine_warden); [Падший архонт](Monsters.md#fallen_archon); [Титан пустоты](Monsters.md#void_titan) |
| `regions[8].zones[12].count` | 57; 74 |
| `regions[8].zones[12].light` | 0.8 |
| `regions[8].zones[12].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[12].boss` | [Собиратель корон](Bosses.md#boss_crown_collector) |
| `regions[8].zones[12].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[12].size` | 72 |
| `regions[8].zones[12].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[13].code` | [Край ничто](World-REGION_9.md#c9_edge_of_nothing) |
| `regions[8].zones[13].biome` | `OBLIVION` |
| `regions[8].zones[13].level` | 98 |
| `regions[8].zones[13].x` | 753 |
| `regions[8].zones[13].y` | 7711 |
| `regions[8].zones[13].from` | [Зал войны](World-REGION_9.md#c9_hall_of_war); [Беззвёздная пустота](World-REGION_9.md#c9_starless_void) |
| `regions[8].zones[13].monsters` | [Тень забвения](Monsters.md#oblivion_shade); [Клятвенный фанатик](Monsters.md#godsworn_zealot); [Титан пустоты](Monsters.md#void_titan) |
| `regions[8].zones[13].count` | 57; 74 |
| `regions[8].zones[13].light` | 0.7 |
| `regions[8].zones[13].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[13].boss` | [Странник ничто](Bosses.md#boss_nothing_walker) |
| `regions[8].zones[13].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[13].size` | 72 |
| `regions[8].zones[13].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `regions[8].zones[14].code` | [Последний трон](World-REGION_9.md#c9_last_throne) |
| `regions[8].zones[14].biome` | `GODHALL` |
| `regions[8].zones[14].level` | 100 |
| `regions[8].zones[14].x` | 533 |
| `regions[8].zones[14].y` | 7856 |
| `regions[8].zones[14].from` | [Небесная обсерватория](World-REGION_9.md#c9_celestial_observatory); [Дорога к трону](World-REGION_9.md#c9_throne_road); [Край ничто](World-REGION_9.md#c9_edge_of_nothing) |
| `regions[8].zones[14].finale` | Да |
| `regions[8].zones[14].monsters` | [Божественный страж](Monsters.md#divine_warden); [Падший архонт](Monsters.md#fallen_archon); [Титан пустоты](Monsters.md#void_titan); [Звёздный дракон](Monsters.md#star_dragon) |
| `regions[8].zones[14].count` | 57; 74 |
| `regions[8].zones[14].light` | 0.8 |
| `regions[8].zones[14].chestLoot` | [loot:CHEST_9](Tables-LOOT.md#loot-chest_9) |
| `regions[8].zones[14].boss` | [Последний бог](Bosses.md#boss_last_god) |
| `regions[8].zones[14].corrupted` | [Осквернённая машина богов](Monsters.md#corrupted_9) |
| `regions[8].zones[14].size` | 72 |
| `regions[8].zones[14].tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `combat.ailmentShare` | 0.5 |
| `combat.attackSpeedMin` | 0.3 |
| `combat.attackSpeedMax` | 5 |
| `combat.recentSeconds` | 4 |
| `combat.variance` | 20 |
| `combat.resistCap` | 75 |
| `combat.ceilings.block.base` | 50 |
| `combat.ceilings.block.hard` | 75 |
| `combat.ceilings.block.raise` | [Максимум блока](Stats-HERO.md#stock_block_max) |
| `combat.ceilings.evasion.base` | 50 |
| `combat.ceilings.evasion.hard` | 75 |
| `combat.ceilings.evasion.raise` | [Максимум уклонения](Stats-HERO.md#stock_evasion_max) |
| `combat.ceilings.physical.base` | 75 |
| `combat.ceilings.physical.hard` | 90 |
| `combat.ceilings.physical.raise` | [Максимум физического снижения](Stats-HERO.md#stock_physical_reduction_max) |
| `combat.ceilings.critical.base` | 70 |
| `combat.ceilings.critical.hard` | 90 |
| `combat.ceilings.critical.raise` | [Максимум шанса крита](Stats-HERO.md#stock_critical_max) |
| `combat.resistHardCap` | 90 |
| `combat.ailmentDurationCap` | 75 |
| `combat.unarmed.damage` | 4 |
| `combat.unarmed.speed` | 1.2 |
| `combat.critical.chance` | 5 |
| `combat.critical.multiplier` | 150 |
| `combat.critical.spellChance` | 5 |
| `combat.critical.spellMultiplier` | 150 |
| `combat.armour.factor` | 5 |
| `combat.evasion.base` | 150 |
| `combat.evasion.perLevel` | 40 |
| `combat.stun.share` | 15 |
| `combat.stun.duration` | 0.4 |
| `combat.shield.rechargeDelay` | 2 |
| `combat.shield.rechargePerSecond` | 20 |
| `combat.loneWolf.dealt` | 10 |
| `combat.loneWolf.taken` | 10 |
| `combat.retreat.delay` | 1.5 |
| `combat.death.fromLevel` | 10 |
| `combat.death.experienceShare` | 10 |
| `combat.ailments[0].ailment` | `BURNING` |
| `combat.ailments[0].type` | [Урон огнём](Stats-HERO.md#stock_attack_fire) |
| `combat.ailments[0].chance` | 30 |
| `combat.ailments[0].magnitude` | 60 |
| `combat.ailments[0].duration` | 4 |
| `combat.ailments[0].heroChance` | 0 |
| `combat.ailments[1].ailment` | `CHILLED` |
| `combat.ailments[1].type` | [Урон холодом](Stats-HERO.md#stock_attack_cold) |
| `combat.ailments[1].chance` | 100 |
| `combat.ailments[1].magnitude` | 15 |
| `combat.ailments[1].duration` | 2 |
| `combat.ailments[2].ailment` | `FROZEN` |
| `combat.ailments[2].type` | [Урон холодом](Stats-HERO.md#stock_attack_cold) |
| `combat.ailments[2].chance` | 50 |
| `combat.ailments[2].magnitude` | 0 |
| `combat.ailments[2].duration` | 0.8 |
| `combat.ailments[2].threshold` | 15 |
| `combat.ailments[3].ailment` | `SHOCKED` |
| `combat.ailments[3].type` | [Урон молнией](Stats-HERO.md#stock_attack_lightning) |
| `combat.ailments[3].chance` | 35 |
| `combat.ailments[3].magnitude` | 20 |
| `combat.ailments[3].duration` | 3 |
| `combat.ailments[3].heroChance` | 0 |
| `combat.ailments[4].ailment` | `POISONED` |
| `combat.ailments[4].type` | [Урон хаосом](Stats-HERO.md#stock_attack_chaos) |
| `combat.ailments[4].chance` | 40 |
| `combat.ailments[4].magnitude` | 30 |
| `combat.ailments[4].duration` | 3 |
| `combat.ailments[4].stacks` | Да |
| `combat.ailments[4].heroChance` | 0 |
| `combat.ailments[5].ailment` | `BLEEDING` |
| `combat.ailments[5].type` | [Физический урон](Stats-HERO.md#stock_attack_physical) |
| `combat.ailments[5].chance` | 15 |
| `combat.ailments[5].magnitude` | 50 |
| `combat.ailments[5].duration` | 4 |
| `combat.ailments[5].heroChance` | 0 |
| `combat.mana.regen` | 3 |
| `combat.resistPenalty` | 0; 10; 20; 30; 40; 50 |
| `combat.flasks.perKill.NORMAL` | 1 |
| `combat.flasks.perKill.MAGIC` | 2 |
| `combat.flasks.perKill.RARE` | 3 |
| `combat.flasks.perKill.UNIQUE` | 5 |
| `combat.opening` | 50 |
| `combat.preparationCap` | 75 |
| `combat.reinforceDelay` | 3 |
| `combat.lunge` | 0.16 |
| `combat.entry` | 0.55 |
| `combat.stagger` | 0.13 |
| `combat.minDot` | 1 |
| `combat.petMendEvery` | 1 |
| `combat.lifeDelay` | 4 |
| `combat.selfBurn` | 0.01 |
| `expedition.heroSpeed` | 3.2 |
| `expedition.heroRadius` | 0.28 |
| `expedition.monsterRadius` | 0.3 |
| `expedition.contact` | 0.8 |
| `expedition.gatherRadius` | 3 |
| `expedition.stageMonsters` | 3 |
| `expedition.exitReach` | 0.7 |
| `expedition.chestReach` | 0.7 |
| `expedition.calmAfterRetreat` | 4 |
| `expedition.chestSteps` | 8 |
| `expedition.chestSpacing` | 5 |
| `expedition.fountainSteps` | 6 |
| `expedition.fountainSpacing` | 8 |
| `expedition.defaultLight` | 5 |
| `expedition.minLight` | 2 |
| `expedition.maxLight` | 14 |
| `expedition.autoWavesMin` | 8 |
| `expedition.autoWavesMax` | 15 |
| `expedition.autoBeat` | 0.6 |
| `expedition.aftermath` | 0.8 |
| `expedition.stagePause` | 3 |
| `behaviour.default.type` | `WANDER` |
| `behaviour.default.wanderSpeed` | 1.1 |
| `behaviour.default.chaseSpeed` | 2.2 |
| `behaviour.default.sight` | 4.5 |
| `behaviour.default.wanderRadius` | 3 |
| `behaviour.default.giveUp` | 3 |
| `behaviour.forms.HUMANOID.type` | `WANDER` |
| `behaviour.forms.HUMANOID.wanderSpeed` | 1.1 |
| `behaviour.forms.HUMANOID.chaseSpeed` | 2.2 |
| `behaviour.forms.HUMANOID.sight` | 4.5 |
| `behaviour.forms.HUMANOID.wanderRadius` | 3 |
| `behaviour.forms.HUMANOID.giveUp` | 3 |
| `behaviour.forms.UNDEAD.type` | `PATROL` |
| `behaviour.forms.UNDEAD.wanderSpeed` | 0.9 |
| `behaviour.forms.UNDEAD.chaseSpeed` | 1.8 |
| `behaviour.forms.UNDEAD.sight` | 4 |
| `behaviour.forms.UNDEAD.wanderRadius` | 5 |
| `behaviour.forms.UNDEAD.giveUp` | 4 |
| `behaviour.forms.WRAITH.type` | `WANDER` |
| `behaviour.forms.WRAITH.wanderSpeed` | 1.3 |
| `behaviour.forms.WRAITH.chaseSpeed` | 2.4 |
| `behaviour.forms.WRAITH.sight` | 5.5 |
| `behaviour.forms.WRAITH.wanderRadius` | 4 |
| `behaviour.forms.WRAITH.giveUp` | 2 |
| `behaviour.forms.BEAST.type` | `WANDER` |
| `behaviour.forms.BEAST.wanderSpeed` | 1.4 |
| `behaviour.forms.BEAST.chaseSpeed` | 3 |
| `behaviour.forms.BEAST.sight` | 5 |
| `behaviour.forms.BEAST.wanderRadius` | 4 |
| `behaviour.forms.BEAST.giveUp` | 3 |
| `behaviour.forms.SERPENT.type` | `AMBUSH` |
| `behaviour.forms.SERPENT.wanderSpeed` | 0.8 |
| `behaviour.forms.SERPENT.chaseSpeed` | 3.2 |
| `behaviour.forms.SERPENT.sight` | 3.5 |
| `behaviour.forms.SERPENT.wake` | 2.5 |
| `behaviour.forms.SERPENT.giveUp` | 2 |
| `behaviour.forms.SPIDER.type` | `AMBUSH` |
| `behaviour.forms.SPIDER.wanderSpeed` | 0.8 |
| `behaviour.forms.SPIDER.chaseSpeed` | 3 |
| `behaviour.forms.SPIDER.sight` | 3.5 |
| `behaviour.forms.SPIDER.wake` | 3 |
| `behaviour.forms.SPIDER.giveUp` | 2 |
| `behaviour.forms.SLUG.type` | `WANDER` |
| `behaviour.forms.SLUG.wanderSpeed` | 0.5 |
| `behaviour.forms.SLUG.chaseSpeed` | 1.2 |
| `behaviour.forms.SLUG.sight` | 3 |
| `behaviour.forms.SLUG.wanderRadius` | 2 |
| `behaviour.forms.SLUG.giveUp` | 5 |
| `behaviour.forms.BAT.type` | `WANDER` |
| `behaviour.forms.BAT.wanderSpeed` | 1.8 |
| `behaviour.forms.BAT.chaseSpeed` | 2.8 |
| `behaviour.forms.BAT.sight` | 5 |
| `behaviour.forms.BAT.wanderRadius` | 5 |
| `behaviour.forms.BAT.giveUp` | 2 |
| `behaviour.forms.CRAB.type` | `PATROL` |
| `behaviour.forms.CRAB.wanderSpeed` | 0.8 |
| `behaviour.forms.CRAB.chaseSpeed` | 1.6 |
| `behaviour.forms.CRAB.sight` | 3.5 |
| `behaviour.forms.CRAB.wanderRadius` | 3 |
| `behaviour.forms.CRAB.giveUp` | 3 |
| `behaviour.forms.GOLEM.type` | `SLEEP` |
| `behaviour.forms.GOLEM.wanderSpeed` | 0.7 |
| `behaviour.forms.GOLEM.chaseSpeed` | 1.5 |
| `behaviour.forms.GOLEM.sight` | 4 |
| `behaviour.forms.GOLEM.wanderRadius` | 2.5 |
| `behaviour.forms.GOLEM.wake` | 2.5 |
| `behaviour.forms.GOLEM.giveUp` | 6 |
| `behaviour.forms.BRUTE.type` | `PATROL` |
| `behaviour.forms.BRUTE.wanderSpeed` | 1 |
| `behaviour.forms.BRUTE.chaseSpeed` | 2 |
| `behaviour.forms.BRUTE.sight` | 4 |
| `behaviour.forms.BRUTE.wanderRadius` | 4 |
| `behaviour.forms.BRUTE.giveUp` | 4 |
| `behaviour.forms.SCORPION.type` | `AMBUSH` |
| `behaviour.forms.SCORPION.wanderSpeed` | 0.9 |
| `behaviour.forms.SCORPION.chaseSpeed` | 3 |
| `behaviour.forms.SCORPION.sight` | 3.5 |
| `behaviour.forms.SCORPION.wake` | 3 |
| `behaviour.forms.SCORPION.giveUp` | 2.5 |
| `behaviour.forms.INSECT.type` | `WANDER` |
| `behaviour.forms.INSECT.wanderSpeed` | 1.7 |
| `behaviour.forms.INSECT.chaseSpeed` | 3 |
| `behaviour.forms.INSECT.sight` | 5 |
| `behaviour.forms.INSECT.wanderRadius` | 4.5 |
| `behaviour.forms.INSECT.giveUp` | 2 |
| `behaviour.forms.DEMON.type` | `PATROL` |
| `behaviour.forms.DEMON.wanderSpeed` | 1.2 |
| `behaviour.forms.DEMON.chaseSpeed` | 2.6 |
| `behaviour.forms.DEMON.sight` | 5.5 |
| `behaviour.forms.DEMON.wanderRadius` | 4 |
| `behaviour.forms.DEMON.giveUp` | 5 |
| `behaviour.forms.FUNGUS.type` | `SLEEP` |
| `behaviour.forms.FUNGUS.wanderSpeed` | 0.4 |
| `behaviour.forms.FUNGUS.chaseSpeed` | 1 |
| `behaviour.forms.FUNGUS.sight` | 3 |
| `behaviour.forms.FUNGUS.wanderRadius` | 1.5 |
| `behaviour.forms.FUNGUS.wake` | 2 |
| `behaviour.forms.FUNGUS.giveUp` | 6 |
| `chests.count` | 0; 2 |
| `chests.refreshHours` | 6 |
| `chests.quantity` | 1 |
| `chests.rarityBonus` | 60 |
| `bosses.respawnHours` | 1 |
| `bosses.uniqueChance` | 0.1 |
| `bosses.ownUniqueChance` | 0.2 |
| `bosses.behaviour.type` | `AMBUSH` |
| `bosses.behaviour.wanderSpeed` | 1 |
| `bosses.behaviour.chaseSpeed` | 2 |
| `bosses.behaviour.sight` | 6 |
| `bosses.behaviour.wake` | 3.5 |
| `bosses.behaviour.giveUp` | 4 |
| `bosses.rolls` | 1; 2 |
| `bosses.tierReach` | 5 |
| `bosses.tables` | [unique:world](Tables-TEMPLATE.md#unique-world) |
| `bosses.mythicChance` | 0.01 |
| `bosses.mythicTables` | [mythic:all](Tables-TEMPLATE.md#mythic-all) |
| `bosses.modifiers` | [mob:boss](Tables-MODIFIER.md#mob-boss) |
| `bosses.goldShare` | 0.5 |
| `bosses.orbShare` | 0.5 |
| `bosses.earlyRolls` | 0; 1 |
| `bosses.earlyUntil` | 25 |
| `bosses.blockCap` | 50 |
| `services.summonPerLevel` | 150 |
| `fountains.count` | 0; 2 |
| `fountains.heal` | 30 |
| `corruption.chance` | 0.35 |
| `corruption.uniqueChance` | 0.12 |
| `corruption.tables` | [unique:vaal](Tables-TEMPLATE.md#unique-vaal) |
| `corruption.mythicChance` | 0.01 |
| `corruption.mythicTables` | [mythic:all](Tables-TEMPLATE.md#mythic-all) |
| `vaal.mods` | 3; 9 |
| `vaal.power` | 1.5 |
| `vaal.reward` | 1.5 |
| `vaal.perMod` | 4 |
| `maps.dropChance` | 0.03 |
| `maps.bossChance` | 0.4 |
| `maps.nextChance` | 0.25 |
| `maps.levelSpread` | 2 |
| `maps.uniqueChance` | 0.08 |
| `maps.uniqueTables` | [unique:map](Tables-TEMPLATE.md#unique-map) |
| `maps.atlasUniqueChance` | 0.04 |
| `maps.atlasUniqueTables` | [unique:atlas](Tables-TEMPLATE.md#unique-atlas) |
| `maps.atlasUniqueNodes` | 100 |
| `maps.rarityBonus.MAGIC` | 10 |
| `maps.rarityBonus.RARE` | 25 |
| `maps.risk.MAP_MONSTER_LIFE` | 0.4 |
| `maps.risk.MAP_MONSTER_DAMAGE` | 0.6 |
| `maps.risk.MAP_MONSTER_SPEED` | 0.8 |
| `maps.risk.MAP_MONSTER_RESIST` | 0.4 |
| `maps.risk.MAP_HERO_RESIST` | 0.8 |
| `maps.risk.MAP_HERO_REGEN` | 0.15 |
| `maps.risk.MAP_MONSTER_PENETRATION` | 0.8 |
| `maps.risk.MAP_MONSTER_REFLECT` | 1 |
| `maps.risk.MAP_MONSTER_CRITICAL` | 0.6 |
| `maps.risk.MAP_MONSTER_AILMENTS` | 0.4 |
| `maps.risk.MAP_MONSTER_ARMOUR` | 0.2 |
| `maps.risk.MAP_MONSTER_LEECH` | 1.5 |
| `maps.risk.MAP_MONSTER_STUN` | 0.1 |
| `maps.risk.MAP_MONSTER_MAGIC_MIN` | 12 |
| `maps.risk.MAP_HERO_DAMAGE_TAKEN` | 0.8 |
| `maps.risk.MAP_HERO_RECOVERY` | 0.4 |
| `maps.risk.MAP_HERO_MAX_RESIST` | 1.5 |
| `maps.risk.MAP_HERO_DEFENCES` | 0.4 |
| `maps.risk.MAP_HERO_BLOCK` | 0.5 |
| `maps.risk.MAP_HERO_CRIT` | 0.25 |
| `maps.risk.MAP_FLASK_CHARGES` | 0.4 |
| `maps.risk.MAP_HERO_MANA_REGEN` | 0.2 |
| `maps.risk.MAP_MONSTER_CAST` | 0.5 |
| `maps.risk.MAP_SKILL_COST` | 0.5 |
| `maps.risk.MAP_ABYSS_LIFE` | 0.1 |
| `maps.risk.MAP_ABYSS_DAMAGE` | 0.15 |
| `maps.risk.MAP_ABYSS_SWARM` | 0.1 |
| `maps.risk.MAP_ABYSS_LEADER` | 0.1 |
| `maps.risk.MAP_BOSS_POWER` | 0.6 |
| `maps.risk.MAP_HERO_ENFEEBLE` | 0.6 |
| `maps.risk.MAP_HERO_VULNERABILITY` | 0.5 |
| `maps.risk.MAP_HERO_FLASK_EFFECT` | 0.3 |
| `maps.risk.MAP_HERO_BUFF_DURATION` | 0.1 |
| `maps.risk.MAP_MONSTER_ONSLAUGHT` | 15 |
| `maps.risk.MAP_MONSTER_FORTIFY` | 12 |
| `maps.risk.MAP_MONSTER_EXTRA_ELEMENTAL` | 0.6 |
| `maps.tiers.fromLevel` | 98 |
| `maps.tiers.max` | 16 |
| `maps.tiers.effects.MAP_MONSTER_LIFE` | 8 |
| `maps.tiers.effects.MAP_MONSTER_DAMAGE` | 5 |
| `maps.tiers.effects.MAP_PACK_SIZE` | 2 |
| `maps.tiers.effects.MAP_QUANTITY` | 3 |
| `maps.tiers.effects.MAP_RARITY` | 5 |
| `maps.tiers.climb` | 30 |
| `maps.tiers.uniqueChance` | 0.004 |
| `maps.tiers.uniqueTables` | [unique:tier](Tables-TEMPLATE.md#unique-tier) |
| `abyss.chance` | 20 |
| `abyss.minLevel` | 10 |
| `abyss.refreshHours` | 6 |
| `abyss.depth` | 4; 6 |
| `abyss.monsters` | [Отродье Бездны](Monsters.md#chasm_spawn); [Пиявка глубин](Monsters.md#chasm_leech); [Шептун Бездны](Monsters.md#chasm_whisperer); [Ловец из пропасти](Monsters.md#chasm_stalker); [Проклинатель пустоты](Monsters.md#chasm_hexer); [Громила Бездны](Monsters.md#chasm_brute) |
| `abyss.waves[0].count` | 3; 4 |
| `abyss.waves[0].level` | 0 |
| `abyss.waves[0].magic` | 10 |
| `abyss.waves[0].rare` | 0 |
| `abyss.waves[1].count` | 4; 5 |
| `abyss.waves[1].level` | 1 |
| `abyss.waves[1].magic` | 15 |
| `abyss.waves[1].rare` | 3 |
| `abyss.waves[2].count` | 4; 6 |
| `abyss.waves[2].level` | 1 |
| `abyss.waves[2].magic` | 20 |
| `abyss.waves[2].rare` | 5 |
| `abyss.waves[2].leader` | [Хозяин ямы](Monsters.md#chasm_pit_master) |
| `abyss.waves[3].count` | 5; 6 |
| `abyss.waves[3].level` | 2 |
| `abyss.waves[3].magic` | 25 |
| `abyss.waves[3].rare` | 8 |
| `abyss.waves[4].count` | 5; 7 |
| `abyss.waves[4].level` | 2 |
| `abyss.waves[4].magic` | 30 |
| `abyss.waves[4].rare` | 10 |
| `abyss.waves[4].leader` | [Королева шёпотов](Monsters.md#chasm_whisper_queen) |
| `abyss.waves[5].count` | 6; 7 |
| `abyss.waves[5].level` | 3 |
| `abyss.waves[5].magic` | 35 |
| `abyss.waves[5].rare` | 14 |
| `abyss.waves[6].count` | 6; 8 |
| `abyss.waves[6].level` | 4 |
| `abyss.waves[6].magic` | 40 |
| `abyss.waves[6].rare` | 18 |
| `abyss.waves[6].leader` | [Пожиратель глубин](Monsters.md#chasm_devourer) |
| `abyss.hoard[0].items` | 1; 1 |
| `abyss.hoard[0].rare` | 10 |
| `abyss.hoard[0].orbs` | 0; 1 |
| `abyss.hoard[0].unique` | 1 |
| `abyss.hoard[0].experience` | 4 |
| `abyss.hoard[1].items` | 1; 2 |
| `abyss.hoard[1].rare` | 15 |
| `abyss.hoard[1].orbs` | 1; 1 |
| `abyss.hoard[1].unique` | 2 |
| `abyss.hoard[1].experience` | 10 |
| `abyss.hoard[2].items` | 2; 3 |
| `abyss.hoard[2].rare` | 25 |
| `abyss.hoard[2].orbs` | 1; 2 |
| `abyss.hoard[2].unique` | 4 |
| `abyss.hoard[2].experience` | 53 |
| `abyss.hoard[3].items` | 3; 4 |
| `abyss.hoard[3].rare` | 30 |
| `abyss.hoard[3].orbs` | 2; 3 |
| `abyss.hoard[3].unique` | 6 |
| `abyss.hoard[3].experience` | 62 |
| `abyss.hoard[4].items` | 4; 5 |
| `abyss.hoard[4].rare` | 40 |
| `abyss.hoard[4].orbs` | 2; 4 |
| `abyss.hoard[4].unique` | 10 |
| `abyss.hoard[4].experience` | 108 |
| `abyss.hoard[5].items` | 5; 6 |
| `abyss.hoard[5].rare` | 50 |
| `abyss.hoard[5].orbs` | 3; 5 |
| `abyss.hoard[5].unique` | 14 |
| `abyss.hoard[5].experience` | 121 |
| `abyss.hoard[6].items` | 6; 8 |
| `abyss.hoard[6].rare` | 65 |
| `abyss.hoard[6].orbs` | 4; 6 |
| `abyss.hoard[6].unique` | 20 |
| `abyss.hoard[6].experience` | 173 |
| `abyss.rolls` | 1; 2 |
| `abyss.tierReach` | 5 |
| `abyss.uniques` | [abyss](Tables-TEMPLATE.md#abyss) |
| `abyss.tables` | [drop](Tables-TEMPLATE.md#drop) |
| `abyss.modifiers` | [mob:abyss](Tables-MODIFIER.md#mob-abyss); [mob:monster](Tables-MODIFIER.md#mob-monster) |
| `trials.crest` | 0.8 |
| `trials.seal` | 0.8 |
| `trials.atlasFloors` | 25 |
| `trials.rush.key` | 5 |
| `trials.rush.life` | 25 |
| `trials.rush.flaskCharges` | 1 |
| `trials.rush.orbs` | 1; 2 |
| `trials.rush.orbTable` | [orbs:abyss](Tables-ITEM.md#orbs-abyss) |
| `trials.rush.uniqueTables` | — |
| `trials.rush.itemTables` | [drop](Tables-TEMPLATE.md#drop) |
| `trials.rush.seconds` | 45 |
| `trials.rush.fastItems` | 1 |
| `trials.tower.levelStep` | 2 |
| `trials.tower.growth` | 8 |
| `trials.tower.hoardEvery` | 5 |
| `trials.tower.hoardGrowth` | 15 |
| `trials.tower.modEvery` | 10 |
| `trials.tower.modHoard` | 10 |
| `trials.tower.checkpoint` | 10 |
| `trials.tower.maxFloor` | 100 |
| `trials.tower.mods[0].stat` | [Здоровье монстров карты](Modifiers-PREFIX.md#map_monster_life) |
| `trials.tower.mods[0].op` | `INCREASED` |
| `trials.tower.mods[0].value` | 40 |
| `trials.tower.mods[1].stat` | [Урон монстров карты](Modifiers-PREFIX.md#map_monster_damage) |
| `trials.tower.mods[1].op` | `INCREASED` |
| `trials.tower.mods[1].value` | 30 |
| `trials.tower.mods[2].stat` | [Скорость атаки монстров карты](Modifiers-PREFIX.md#map_monster_speed) |
| `trials.tower.mods[2].op` | `INCREASED` |
| `trials.tower.mods[2].value` | 20 |
| `trials.tower.mods[3].stat` | [Сопротивления монстров карты](Modifiers-PREFIX.md#map_monster_resist) |
| `trials.tower.mods[3].op` | `ADD` |
| `trials.tower.mods[3].value` | 30 |
| `trials.tower.mods[4].stat` | [Пробивание монстров карты](Modifiers-PREFIX.md#map_monster_penetration) |
| `trials.tower.mods[4].op` | `ADD` |
| `trials.tower.mods[4].value` | 15 |
| `trials.tower.mods[5].stat` | [Крит монстров карты](Modifiers-PREFIX.md#map_monster_critical) |
| `trials.tower.mods[5].op` | `ADD` |
| `trials.tower.mods[5].value` | 15 |
| `trials.tower.mods[6].stat` | [Защита монстров карты](Modifiers-PREFIX.md#map_monster_armour) |
| `trials.tower.mods[6].op` | `INCREASED` |
| `trials.tower.mods[6].value` | 60 |
| `trials.tower.mods[7].stat` | [Ослабление сопротивлений на карте](Modifiers-SUFFIX.md#map_hero_resist) |
| `trials.tower.mods[7].op` | `ADD` |
| `trials.tower.mods[7].value` | 25 |
| `trials.tower.mods[8].stat` | [Ослабление регенерации на карте](Modifiers-SUFFIX.md#map_hero_regen) |
| `trials.tower.mods[8].op` | `INCREASED` |
| `trials.tower.mods[8].value` | 60 |
| `trials.tower.mods[9].stat` | [Карта: максимум сопротивлений](Modifiers-SUFFIX.md#map_hero_max_resist) |
| `trials.tower.mods[9].op` | `ADD` |
| `trials.tower.mods[9].value` | 8 |
| `trials.tower.mods[10].stat` | [Стихийный урон монстров](Modifiers-PREFIX.md#map_monster_extra_elemental) |
| `trials.tower.mods[10].op` | `ADD` |
| `trials.tower.mods[10].value` | 20 |
| `traits.skillMana` | 30 |
| `traits.list[0].code` | `TRAIT_DISCIPLINE` |
| `traits.list[0].icon` | `combat` |
| `traits.list[0].lines[0].stat` | [Меткость](Stats-HERO.md#stock_accuracy) |
| `traits.list[0].lines[0].op` | `INCREASED` |
| `traits.list[0].lines[0].value` | 20 |
| `traits.list[0].lines[1].stat` | [Шанс блока](Stats-HERO.md#stock_block_chance) |
| `traits.list[0].lines[1].op` | `ADD` |
| `traits.list[0].lines[1].value` | 5 |
| `traits.list[1].code` | `TRAIT_CARAPACE` |
| `traits.list[1].icon` | [armour](Tables-MODIFIER.md#armour) |
| `traits.list[1].lines[0].stat` | [Снижение физического урона](Stats-HERO.md#stock_physical_reduction) |
| `traits.list[1].lines[0].op` | `ADD` |
| `traits.list[1].lines[0].value` | 8 |
| `traits.list[2].code` | `TRAIT_FLUTTER` |
| `traits.list[2].icon` | `evasion` |
| `traits.list[2].lines[0].stat` | [Уклонение](Stats-HERO.md#stock_evasion) |
| `traits.list[2].lines[0].op` | `INCREASED` |
| `traits.list[2].lines[0].value` | 25 |
| `traits.list[3].code` | `TRAIT_COILED_STRIKE` |
| `traits.list[3].icon` | `poison` |
| `traits.list[3].trigger.act` | `FIRST_STRIKE` |
| `traits.list[3].trigger.value` | 50 |
| `traits.list[4].code` | `TRAIT_SLIME` |
| `traits.list[4].icon` | `regen` |
| `traits.list[4].lines[0].stat` | [Регенерация здоровья, %](Stats-HERO.md#stock_life_regen_percent) |
| `traits.list[4].lines[0].op` | `ADD` |
| `traits.list[4].lines[0].value` | 0.8 |
| `traits.list[5].code` | `TRAIT_PACK_HUNTER` |
| `traits.list[5].icon` | `summon` |
| `traits.list[5].trigger.act` | `RALLY` |
| `traits.list[5].trigger.lines[0].stat` | [Скорость атаки](Stats-HERO.md#stock_attack_speed) |
| `traits.list[5].trigger.lines[0].op` | `INCREASED` |
| `traits.list[5].trigger.lines[0].value` | 20 |
| `traits.list[5].trigger.duration` | 6 |
| `traits.list[6].code` | `TRAIT_VENOM` |
| `traits.list[6].icon` | `poison` |
| `traits.list[6].lines[0].stat` | [Шанс отравления](Stats-HERO.md#stock_poison_chance) |
| `traits.list[6].lines[0].op` | `ADD` |
| `traits.list[6].lines[0].value` | 12 |
| `traits.list[7].code` | `TRAIT_UNDYING` |
| `traits.list[7].icon` | `dark` |
| `traits.list[7].lines[0].stat` | [Невосприимчивость к кровотечению](Stats-HERO.md#stock_immune_bleed) |
| `traits.list[7].lines[0].op` | `SET` |
| `traits.list[7].lines[0].value` | 1 |
| `traits.list[7].lines[1].stat` | [Сопротивление холоду](Stats-HERO.md#stock_resist_cold) |
| `traits.list[7].lines[1].op` | `ADD` |
| `traits.list[7].lines[1].value` | 15 |
| `traits.list[8].code` | `TRAIT_STONE_BODY` |
| `traits.list[8].icon` | `stun` |
| `traits.list[8].lines[0].stat` | [Порог оглушения](Stats-HERO.md#stock_stun_threshold) |
| `traits.list[8].lines[0].op` | `INCREASED` |
| `traits.list[8].lines[0].value` | 50 |
| `traits.list[8].lines[1].stat` | [Броня](Stats-HERO.md#stock_armor) |
| `traits.list[8].lines[1].op` | `INCREASED` |
| `traits.list[8].lines[1].value` | 20 |
| `traits.list[9].code` | `TRAIT_ETHEREAL` |
| `traits.list[9].icon` | `cold` |
| `traits.list[9].lines[0].stat` | [Получаемый физический урон](Stats-HERO.md#stock_physical_taken) |
| `traits.list[9].lines[0].op` | `ADD` |
| `traits.list[9].lines[0].value` | -15 |
| `traits.list[10].code` | `TRAIT_BLOODLUST` |
| `traits.list[10].icon` | `bleeding` |
| `traits.list[10].trigger.act` | `ENRAGE` |
| `traits.list[10].trigger.threshold` | 35 |
| `traits.list[10].trigger.lines[0].stat` | [Урон](Stats-HERO.md#stock_damage) |
| `traits.list[10].trigger.lines[0].op` | `MORE` |
| `traits.list[10].trigger.lines[0].value` | 20 |
| `traits.list[10].trigger.lines[1].stat` | [Скорость атаки](Stats-HERO.md#stock_attack_speed) |
| `traits.list[10].trigger.lines[1].op` | `INCREASED` |
| `traits.list[10].trigger.lines[1].value` | 15 |
| `traits.list[11].code` | `TRAIT_STINGER` |
| `traits.list[11].icon` | `poison` |
| `traits.list[11].lines[0].stat` | [Шанс отравления](Stats-HERO.md#stock_poison_chance) |
| `traits.list[11].lines[0].op` | `ADD` |
| `traits.list[11].lines[0].value` | 15 |
| `traits.list[11].lines[1].stat` | [Урон от яда](Stats-HERO.md#stock_poison_damage) |
| `traits.list[11].lines[1].op` | `INCREASED` |
| `traits.list[11].lines[1].value` | 20 |
| `traits.list[12].code` | `TRAIT_SPORE_BURST` |
| `traits.list[12].icon` | `chaos` |
| `traits.list[12].trigger.act` | `BURST` |
| `traits.list[12].trigger.value` | 10 |
| `traits.list[12].trigger.element` | `CHAOS` |
| `traits.list[13].code` | `TRAIT_SWARM` |
| `traits.list[13].icon` | `speed` |
| `traits.list[13].lines[0].stat` | [Скорость атаки](Stats-HERO.md#stock_attack_speed) |
| `traits.list[13].lines[0].op` | `INCREASED` |
| `traits.list[13].lines[0].value` | 12 |
| `traits.list[14].code` | `TRAIT_HELLFIRE` |
| `traits.list[14].icon` | `fire` |
| `traits.list[14].lines[0].stat` | [Шанс поджога](Stats-HERO.md#stock_ignite_chance) |
| `traits.list[14].lines[0].op` | `ADD` |
| `traits.list[14].lines[0].value` | 10 |
| `traits.list[14].lines[1].stat` | [Сопротивление огню](Stats-HERO.md#stock_resist_fire) |
| `traits.list[14].lines[1].op` | `ADD` |
| `traits.list[14].lines[1].value` | 20 |
| `traits.list[15].code` | `TRAIT_SHATTER` |
| `traits.list[15].icon` | `cold` |
| `traits.list[15].trigger.act` | `BURST` |
| `traits.list[15].trigger.value` | 10 |
| `traits.list[15].trigger.element` | `COLD` |
| `traits.list[16].code` | `TRAIT_PLATING` |
| `traits.list[16].icon` | [armour](Tables-MODIFIER.md#armour) |
| `traits.list[16].lines[0].stat` | [Получаемый урон](Stats-HERO.md#stock_damage_taken) |
| `traits.list[16].lines[0].op` | `ADD` |
| `traits.list[16].lines[0].value` | -10 |
| `traits.list[17].code` | `TRAIT_DRAGON_SCALES` |
| `traits.list[17].icon` | `fire` |
| `traits.list[17].lines[0].stat` | [Все сопротивления стихиям](Stats-HERO.md#stock_resist_all) |
| `traits.list[17].lines[0].op` | `ADD` |
| `traits.list[17].lines[0].value` | 12 |
| `traits.list[17].lines[1].stat` | [Шанс критического удара](Stats-HERO.md#stock_critical_chance) |
| `traits.list[17].lines[1].op` | `ADD` |
| `traits.list[17].lines[1].value` | 3 |
| `traits.list[18].code` | `TRAIT_RADIANCE` |
| `traits.list[18].icon` | `lightning` |
| `traits.list[18].trigger.act` | `MEND` |
| `traits.list[18].trigger.value` | 15 |
| `traits.list[19].code` | `TRAIT_PROVOKE` |
| `traits.list[19].icon` | [armour](Tables-MODIFIER.md#armour) |
| `traits.list[19].lines[0].stat` | [Провокация](Stats-HERO.md#stock_taunt) |
| `traits.list[19].lines[0].op` | `SET` |
| `traits.list[19].lines[0].value` | 1 |
| `traits.list[20].code` | `TRAIT_RENDING` |
| `traits.list[20].icon` | `bleeding` |
| `traits.list[20].lines[0].stat` | [Шанс кровотечения](Stats-HERO.md#stock_bleed_chance) |
| `traits.list[20].lines[0].op` | `ADD` |
| `traits.list[20].lines[0].value` | 20 |
| `traits.list[21].code` | `TRAIT_FROSTBITE` |
| `traits.list[21].icon` | `cold` |
| `traits.list[21].lines[0].stat` | [Накопление заморозки](Stats-HERO.md#stock_freeze_chance) |
| `traits.list[21].lines[0].op` | `ADD` |
| `traits.list[21].lines[0].value` | 8 |
| `traits.list[22].code` | `TRAIT_STATIC` |
| `traits.list[22].icon` | `lightning` |
| `traits.list[22].lines[0].stat` | [Шанс шока](Stats-HERO.md#stock_shock_chance) |
| `traits.list[22].lines[0].op` | `ADD` |
| `traits.list[22].lines[0].value` | 15 |
| `traits.list[23].code` | `TRAIT_SMOULDER` |
| `traits.list[23].icon` | `fire` |
| `traits.list[23].lines[0].stat` | [Шанс поджога](Stats-HERO.md#stock_ignite_chance) |
| `traits.list[23].lines[0].op` | `ADD` |
| `traits.list[23].lines[0].value` | 15 |
| `traits.list[24].code` | `TRAIT_LEECHING` |
| `traits.list[24].icon` | `leech` |
| `traits.list[24].lines[0].stat` | [Общий вампиризм](Stats-HERO.md#stock_leech_all) |
| `traits.list[24].lines[0].op` | `ADD` |
| `traits.list[24].lines[0].value` | 3 |
| `traits.list[25].code` | `TRAIT_THORNS` |
| `traits.list[25].icon` | `mirror` |
| `traits.list[25].lines[0].stat` | [Отражение](Stats-HERO.md#stock_reflect) |
| `traits.list[25].lines[0].op` | `ADD` |
| `traits.list[25].lines[0].value` | 8 |
| `traits.list[26].code` | `TRAIT_REGROWTH` |
| `traits.list[26].icon` | `regen` |
| `traits.list[26].lines[0].stat` | [Регенерация здоровья, %](Stats-HERO.md#stock_life_regen_percent) |
| `traits.list[26].lines[0].op` | `ADD` |
| `traits.list[26].lines[0].value` | 1.2 |
| `traits.list[27].code` | `TRAIT_KEEN_EYE` |
| `traits.list[27].icon` | `critical` |
| `traits.list[27].lines[0].stat` | [Шанс критического удара](Stats-HERO.md#stock_critical_chance) |
| `traits.list[27].lines[0].op` | `ADD` |
| `traits.list[27].lines[0].value` | 5 |
| `traits.list[27].lines[1].stat` | [Множитель критического удара](Stats-HERO.md#stock_critical_multiplier) |
| `traits.list[27].lines[1].op` | `ADD` |
| `traits.list[27].lines[1].value` | 20 |
| `traits.list[28].code` | `TRAIT_SHIELD_WALL` |
| `traits.list[28].icon` | `block` |
| `traits.list[28].lines[0].stat` | [Шанс блока](Stats-HERO.md#stock_block_chance) |
| `traits.list[28].lines[0].op` | `ADD` |
| `traits.list[28].lines[0].value` | 12 |
| `traits.list[29].code` | `TRAIT_NIMBLE` |
| `traits.list[29].icon` | `evasion` |
| `traits.list[29].lines[0].stat` | [Уклонение](Stats-HERO.md#stock_evasion) |
| `traits.list[29].lines[0].op` | `INCREASED` |
| `traits.list[29].lines[0].value` | 30 |
| `traits.list[30].code` | `TRAIT_IRONHIDE` |
| `traits.list[30].icon` | [armour](Tables-MODIFIER.md#armour) |
| `traits.list[30].lines[0].stat` | [Броня](Stats-HERO.md#stock_armor) |
| `traits.list[30].lines[0].op` | `INCREASED` |
| `traits.list[30].lines[0].value` | 40 |
| `traits.list[31].code` | `TRAIT_FRENZIED` |
| `traits.list[31].icon` | `speed` |
| `traits.list[31].lines[0].stat` | [Скорость атаки](Stats-HERO.md#stock_attack_speed) |
| `traits.list[31].lines[0].op` | `INCREASED` |
| `traits.list[31].lines[0].value` | 18 |
| `traits.list[32].code` | `TRAIT_WARDED` |
| `traits.list[32].icon` | `resist` |
| `traits.list[32].lines[0].stat` | [Все сопротивления стихиям](Stats-HERO.md#stock_resist_all) |
| `traits.list[32].lines[0].op` | `ADD` |
| `traits.list[32].lines[0].value` | 15 |
| `traits.list[33].code` | `TRAIT_PENETRATING` |
| `traits.list[33].icon` | `magical` |
| `traits.list[33].lines[0].stat` | [Пробивание стихий](Stats-HERO.md#stock_penetrate_elemental) |
| `traits.list[33].lines[0].op` | `ADD` |
| `traits.list[33].lines[0].value` | 10 |
| `traits.list[34].code` | `TRAIT_ENERGY_WEAVE` |
| `traits.list[34].icon` | [shield](Tables-MODIFIER.md#shield) |
| `traits.list[34].lines[0].stat` | [Энергетический щит](Stats-HERO.md#stock_energy_shield) |
| `traits.list[34].lines[0].op` | `INCREASED` |
| `traits.list[34].lines[0].value` | 30 |
| `traits.list[35].code` | `TRAIT_CHILLING_TOUCH` |
| `traits.list[35].icon` | `cold` |
| `traits.list[35].lines[0].stat` | [Физический как доп. холод](Stats-HERO.md#stock_physical_as_extra_cold) |
| `traits.list[35].lines[0].op` | `ADD` |
| `traits.list[35].lines[0].value` | 15 |
| `traits.list[36].code` | `TRAIT_BURNING_BLOOD` |
| `traits.list[36].icon` | `fire` |
| `traits.list[36].lines[0].stat` | [Физический как доп. огонь](Stats-HERO.md#stock_physical_as_extra_fire) |
| `traits.list[36].lines[0].op` | `ADD` |
| `traits.list[36].lines[0].value` | 15 |
| `traits.list[37].code` | `TRAIT_STORMCHARGED` |
| `traits.list[37].icon` | `lightning` |
| `traits.list[37].lines[0].stat` | [Физический как доп. молния](Stats-HERO.md#stock_physical_as_extra_lightning) |
| `traits.list[37].lines[0].op` | `ADD` |
| `traits.list[37].lines[0].value` | 15 |
| `traits.list[38].code` | `TRAIT_VOIDTOUCHED` |
| `traits.list[38].icon` | `chaos` |
| `traits.list[38].lines[0].stat` | [Физический как доп. хаос](Stats-HERO.md#stock_physical_as_extra_chaos) |
| `traits.list[38].lines[0].op` | `ADD` |
| `traits.list[38].lines[0].value` | 15 |
| `traits.list[39].code` | `TRAIT_FIRE_BURST` |
| `traits.list[39].icon` | `fire` |
| `traits.list[39].trigger.act` | `BURST` |
| `traits.list[39].trigger.value` | 12 |
| `traits.list[39].trigger.element` | `FIRE` |
| `traits.list[40].code` | `TRAIT_VOLATILE` |
| `traits.list[40].icon` | `chaos` |
| `traits.list[40].trigger.act` | `BURST` |
| `traits.list[40].trigger.value` | 12 |
| `traits.list[40].trigger.element` | `CHAOS` |
| `traits.list[41].code` | `TRAIT_STORM_BURST` |
| `traits.list[41].icon` | `lightning` |
| `traits.list[41].trigger.act` | `BURST` |
| `traits.list[41].trigger.value` | 12 |
| `traits.list[41].trigger.element` | `LIGHTNING` |
| `traits.list[42].code` | `TRAIT_LAST_STAND` |
| `traits.list[42].icon` | [armour](Tables-MODIFIER.md#armour) |
| `traits.list[42].trigger.act` | `ENRAGE` |
| `traits.list[42].trigger.threshold` | 30 |
| `traits.list[42].trigger.lines[0].stat` | [Получаемый урон](Stats-HERO.md#stock_damage_taken) |
| `traits.list[42].trigger.lines[0].op` | `ADD` |
| `traits.list[42].trigger.lines[0].value` | -25 |
| `traits.list[43].code` | `TRAIT_BERSERK` |
| `traits.list[43].icon` | `bleeding` |
| `traits.list[43].trigger.act` | `ENRAGE` |
| `traits.list[43].trigger.threshold` | 40 |
| `traits.list[43].trigger.lines[0].stat` | [Урон](Stats-HERO.md#stock_damage) |
| `traits.list[43].trigger.lines[0].op` | `MORE` |
| `traits.list[43].trigger.lines[0].value` | 25 |
| `traits.list[44].code` | `TRAIT_AMBUSHER` |
| `traits.list[44].icon` | `evasion` |
| `traits.list[44].trigger.act` | `FIRST_STRIKE` |
| `traits.list[44].trigger.value` | 70 |
| `traits.list[45].code` | `TRAIT_HOWL` |
| `traits.list[45].icon` | `summon` |
| `traits.list[45].trigger.act` | `RALLY` |
| `traits.list[45].trigger.lines[0].stat` | [Урон](Stats-HERO.md#stock_damage) |
| `traits.list[45].trigger.lines[0].op` | `INCREASED` |
| `traits.list[45].trigger.lines[0].value` | 20 |
| `traits.list[45].trigger.duration` | 6 |
| `traits.list[46].code` | `TRAIT_DEATH_PRAYER` |
| `traits.list[46].icon` | `health` |
| `traits.list[46].trigger.act` | `MEND` |
| `traits.list[46].trigger.value` | 18 |
| `traits.list[47].code` | `TRAIT_SKILL_FIREBALL` |
| `traits.list[47].icon` | `fire` |
| `traits.list[47].skill` | [Огненный шар](Monster-skills.md#mob_fireball) |
| `traits.list[48].code` | `TRAIT_SKILL_ICE_BOLT` |
| `traits.list[48].icon` | `cold` |
| `traits.list[48].skill` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt) |
| `traits.list[49].code` | `TRAIT_SKILL_ZAP` |
| `traits.list[49].icon` | `lightning` |
| `traits.list[49].skill` | [Разряд](Monster-skills.md#mob_zap) |
| `traits.list[50].code` | `TRAIT_SKILL_SPIT` |
| `traits.list[50].icon` | `poison` |
| `traits.list[50].skill` | [Ядовитый плевок](Monster-skills.md#mob_spit) |
| `traits.list[51].code` | `TRAIT_SKILL_WARCRY` |
| `traits.list[51].icon` | `combat` |
| `traits.list[51].skill` | [Клич ярости](Monster-skills.md#mob_warcry) |
| `traits.list[52].code` | `TRAIT_SKILL_ENFEEBLE` |
| `traits.list[52].icon` | `dark` |
| `traits.list[52].skill` | [Слабость](Monster-skills.md#mob_enfeeble) |
| `traits.list[53].code` | `TRAIT_SKILL_VULNERABILITY` |
| `traits.list[53].icon` | `bleeding` |
| `traits.list[53].skill` | [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `traits.list[54].code` | `TRAIT_SKILL_SLAM` |
| `traits.list[54].icon` | `earth` |
| `traits.list[54].skill` | [Удар по земле](Monster-skills.md#mob_slam) |
| `traits.list[55].code` | `TRAIT_SKILL_HEAL` |
| `traits.list[55].icon` | `health` |
| `traits.list[55].skill` | [Исцеление](Monster-skills.md#mob_heal) |
| `traits.list[56].code` | `TRAIT_SKILL_STONESKIN` |
| `traits.list[56].icon` | [armour](Tables-MODIFIER.md#armour) |
| `traits.list[56].skill` | [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `traits.list[57].code` | `TRAIT_SKILL_MANA_DRAIN` |
| `traits.list[57].icon` | `mana` |
| `traits.list[57].skill` | [Кража маны](Monster-skills.md#mob_mana_drain) |
| `traits.list[58].code` | `TRAIT_SKILL_CHAOS_RING` |
| `traits.list[58].icon` | `chaos` |
| `traits.list[58].skill` | [Кольцо хаоса](Monster-skills.md#mob_chaos_ring) |
| `traits.list[59].code` | `TRAIT_SKILL_REND` |
| `traits.list[59].icon` | `bleeding` |
| `traits.list[59].skill` | [Разрыв](Monster-skills.md#mob_rend) |
| `traits.list[60].code` | `TRAIT_SKILL_WEB` |
| `traits.list[60].icon` | `poison` |
| `traits.list[60].skill` | [Паутина](Monster-skills.md#mob_web) |
| `traits.list[61].code` | `TRAIT_SKILL_FROST_BREATH` |
| `traits.list[61].icon` | `cold` |
| `traits.list[61].skill` | [Ледяное дыхание](Monster-skills.md#mob_frost_breath) |
| `traits.list[62].code` | `TRAIT_SKILL_FLAME_BREATH` |
| `traits.list[62].icon` | `fire` |
| `traits.list[62].skill` | [Огненное дыхание](Monster-skills.md#mob_flame_breath) |
| `traits.list[63].code` | `TRAIT_SKILL_THUNDERCLAP` |
| `traits.list[63].icon` | `lightning` |
| `traits.list[63].skill` | [Раскат грома](Monster-skills.md#mob_thunderclap) |
| `traits.list[64].code` | `TRAIT_SKILL_BONE_ARMOUR` |
| `traits.list[64].icon` | [armour](Tables-MODIFIER.md#armour) |
| `traits.list[64].skill` | [Костяная броня](Monster-skills.md#mob_bone_armour) |
| `traits.list[65].code` | `TRAIT_VENOM_STING` |
| `traits.list[65].icon` | `poison` |
| `traits.list[65].lines[0].stat` | [Шанс отравления](Stats-HERO.md#stock_poison_chance) |
| `traits.list[65].lines[0].op` | `ADD` |
| `traits.list[65].lines[0].value` | 20 |
| `traits.list[65].lines[1].stat` | [Урон от яда](Stats-HERO.md#stock_poison_damage) |
| `traits.list[65].lines[1].op` | `INCREASED` |
| `traits.list[65].lines[1].value` | 25 |
| `traits.list[66].code` | `TRAIT_UNSHAKEN` |
| `traits.list[66].icon` | `stun` |
| `traits.list[66].lines[0].stat` | [Шкала оглушения](Stats-HERO.md#stock_stun_pool) |
| `traits.list[66].lines[0].op` | `ADD` |
| `traits.list[66].lines[0].value` | 100 |
| `traits.list[67].code` | `TRAIT_ICE_SHELL` |
| `traits.list[67].icon` | `cold` |
| `traits.list[67].lines[0].stat` | [Невосприимчивость к холоду](Stats-HERO.md#stock_immune_freeze) |
| `traits.list[67].lines[0].op` | `SET` |
| `traits.list[67].lines[0].value` | 1 |
| `traits.list[67].lines[1].stat` | [Сопротивление холоду](Stats-HERO.md#stock_resist_cold) |
| `traits.list[67].lines[1].op` | `ADD` |
| `traits.list[67].lines[1].value` | -20 |
| `traits.list[68].code` | `TRAIT_GROUNDED` |
| `traits.list[68].icon` | `lightning` |
| `traits.list[68].lines[0].stat` | [Невосприимчивость к электрошоку](Stats-HERO.md#stock_immune_electrocute) |
| `traits.list[68].lines[0].op` | `SET` |
| `traits.list[68].lines[0].value` | 1 |
| `traits.list[68].lines[1].stat` | [Сила шока по цели](Stats-HERO.md#stock_shock_taken) |
| `traits.list[68].lines[1].op` | `ADD` |
| `traits.list[68].lines[1].value` | -50 |
| `traits.list[69].code` | `TRAIT_CRUSHER` |
| `traits.list[69].icon` | `strength` |
| `traits.list[69].lines[0].stat` | [Накопление оглушения](Stats-HERO.md#stock_stun_buildup) |
| `traits.list[69].lines[0].op` | `ADD` |
| `traits.list[69].lines[0].value` | 100 |
| `traits.forms.HUMANOID` | `TRAIT_DISCIPLINE` |
| `traits.forms.CRAB` | `TRAIT_CARAPACE` |
| `traits.forms.BAT` | `TRAIT_FLUTTER` |
| `traits.forms.SERPENT` | `TRAIT_COILED_STRIKE` |
| `traits.forms.SLUG` | `TRAIT_SLIME` |
| `traits.forms.BEAST` | `TRAIT_PACK_HUNTER` |
| `traits.forms.SPIDER` | `TRAIT_VENOM` |
| `traits.forms.UNDEAD` | `TRAIT_UNDYING` |
| `traits.forms.GOLEM` | `TRAIT_STONE_BODY` |
| `traits.forms.WRAITH` | `TRAIT_ETHEREAL` |
| `traits.forms.BRUTE` | `TRAIT_BLOODLUST` |
| `traits.forms.SCORPION` | `TRAIT_STINGER` |
| `traits.forms.FUNGUS` | `TRAIT_SPORE_BURST` |
| `traits.forms.INSECT` | `TRAIT_SWARM` |
| `traits.forms.DEMON` | `TRAIT_HELLFIRE` |
| `traits.forms.CRYSTAL` | `TRAIT_SHATTER` |
| `traits.forms.CONSTRUCT` | `TRAIT_PLATING` |
| `traits.forms.DRAGON` | `TRAIT_DRAGON_SCALES` |
| `traits.forms.ANGEL` | `TRAIT_RADIANCE` |
| `traits.power.NORMAL` | 1 |
| `traits.power.MAGIC` | 1.5 |
| `traits.power.RARE` | 2 |
| `traits.power.UNIQUE` | 2 |
| `lootChests[0].code` | `CHEST_WOODEN` |
| `lootChests[0].gear[0].count` | 2 |
| `lootChests[0].gear[0].rarity` | `MAGIC` |
| `lootChests[0].gear[0].rareChance` | 0.25 |
| `lootChests[0].gear[0].tables` | [drop](Tables-TEMPLATE.md#drop) |
| `lootChests[0].dropChance` | 0.001116 |
| `lootChests[0].bossChance` | 0.05 |
| `lootChests[0].table` | `loot:LCHEST_WOODEN` |
| `lootChests[1].code` | `CHEST_ARTISAN` |
| `lootChests[1].dropChance` | 0.001116 |
| `lootChests[1].minLevel` | 3 |
| `lootChests[1].table` | `loot:LCHEST_ARTISAN` |
| `lootChests[2].code` | `CHEST_TREASURY` |
| `lootChests[2].jackpotChance` | 0.1 |
| `lootChests[2].jackpot` | 3 |
| `lootChests[2].dropChance` | 0.000558 |
| `lootChests[2].bossChance` | 0.03 |
| `lootChests[2].minLevel` | 3 |
| `lootChests[2].table` | `loot:LCHEST_TREASURY` |
| `lootChests[3].code` | `CHEST_IRONBOUND` |
| `lootChests[3].gear[0].count` | 3 |
| `lootChests[3].gear[0].rarity` | `RARE` |
| `lootChests[3].gear[0].tables` | [drop](Tables-TEMPLATE.md#drop) |
| `lootChests[3].bossChance` | 0.06 |
| `lootChests[3].minLevel` | 8 |
| `lootChests[3].table` | `loot:LCHEST_IRONBOUND` |
| `lootChests[4].code` | `CHEST_ORBS` |
| `lootChests[4].dropChance` | 0.000368 |
| `lootChests[4].bossChance` | 0.01 |
| `lootChests[4].minLevel` | 5 |
| `lootChests[4].table` | `loot:LCHEST_ORBS` |
| `lootChests[5].code` | `CHEST_ESSENCE` |
| `lootChests[5].dropChance` | 0.000368 |
| `lootChests[5].bossChance` | 0.01 |
| `lootChests[5].minLevel` | 12 |
| `lootChests[5].table` | `loot:LCHEST_ESSENCE` |
| `lootChests[6].code` | `CHEST_BEAST` |
| `lootChests[6].dropChance` | 0.000279 |
| `lootChests[6].bossChance` | 0.01 |
| `lootChests[6].minLevel` | 10 |
| `lootChests[6].table` | `loot:LCHEST_BEAST` |
| `lootChests[7].code` | `CHEST_VAAL` |
| `lootChests[7].gear[0].count` | 2 |
| `lootChests[7].gear[0].rarity` | `RARE` |
| `lootChests[7].gear[0].corrupted` | Да |
| `lootChests[7].gear[0].tables` | [drop](Tables-TEMPLATE.md#drop) |
| `lootChests[7].uniqueChance` | 0.2 |
| `lootChests[7].uniqueTables` | [unique:vaal](Tables-TEMPLATE.md#unique-vaal) |
| `lootChests[7].dropChance` | 0.00019 |
| `lootChests[7].bossChance` | 0.006 |
| `lootChests[7].minLevel` | 30 |
| `lootChests[7].table` | `loot:LCHEST_VAAL` |
| `lootChests[8].code` | `CHEST_ANCIENT` |
| `lootChests[8].gear[0].count` | 3 |
| `lootChests[8].gear[0].rarity` | `RARE` |
| `lootChests[8].gear[0].tables` | [drop](Tables-TEMPLATE.md#drop) |
| `lootChests[8].gear[1].count` | 1 |
| `lootChests[8].gear[1].rarity` | `RARE` |
| `lootChests[8].gear[1].influenced` | Да |
| `lootChests[8].gear[1].tables` | [drop](Tables-TEMPLATE.md#drop) |
| `lootChests[8].uniqueChance` | 0.35 |
| `lootChests[8].uniqueTables` | [unique:world](Tables-TEMPLATE.md#unique-world) |
| `lootChests[8].dropChance` | 0.000279 |
| `lootChests[8].bossChance` | 0.01 |
| `lootChests[8].finaleChance` | 0.1 |
| `lootChests[8].minLevel` | 20 |
| `lootChests[8].table` | `loot:LCHEST_ANCIENT` |
| `lootChests[9].code` | `CHEST_RELIC` |
| `lootChests[9].uniqueChance` | 1 |
| `lootChests[9].uniqueTables` | [unique:world](Tables-TEMPLATE.md#unique-world) |
| `lootChests[9].uniqueFlat` | Да |
| `lootChests[9].dropChance` | 5.6e-05 |
| `lootChests[9].bossChance` | 0.004 |
| `lootChests[9].finaleChance` | 0.08 |
| `lootChests[9].minLevel` | 15 |
| `lootChests[10].code` | `CHEST_WARLORD` |
| `lootChests[10].gear[0].count` | 3 |
| `lootChests[10].gear[0].rarity` | `RARE` |
| `lootChests[10].gear[0].tables` | [drop](Tables-TEMPLATE.md#drop) |
| `lootChests[10].slots` | `HELMET`; `BODY`; `GLOVES`; `BOOTS`; `WEAPON_1H`; `WEAPON_2H`; `SHIELD`; `QUIVER` |
| `lootChests[10].uniqueChance` | 0.15 |
| `lootChests[10].uniqueTables` | [unique:world](Tables-TEMPLATE.md#unique-world) |
| `lootChests[10].bossChance` | 0.03 |
| `lootChests[10].finaleChance` | 0.25 |
| `lootChests[10].minLevel` | 5 |
| `lootChests[10].table` | `loot:LCHEST_WARLORD` |
| `lootChests[11].code` | `CHEST_TRIAL` |
| `lootChests[11].gear[0].count` | 2 |
| `lootChests[11].gear[0].rarity` | `RARE` |
| `lootChests[11].gear[0].tables` | [drop](Tables-TEMPLATE.md#drop) |
| `lootChests[11].uniqueChance` | 0.1 |
| `lootChests[11].uniqueTables` | [unique:world](Tables-TEMPLATE.md#unique-world) |
| `lootChests[11].trialChance` | 0.0667 |
| `lootChests[11].trialPerLevel` | 0.00333 |
| `lootChests[11].trialMax` | 0.4 |
| `lootChests[11].minLevel` | 10 |
| `lootChests[11].table` | `loot:LCHEST_TRIAL` |
| `lootChests[12].code` | `CHEST_CARTOGRAPHER` |
| `lootChests[12].maps.count` | 3 |
| `lootChests[12].maps.spread` | 2 |
| `lootChests[12].maps.rareChance` | 0.3 |
| `lootChests[12].dropChance` | 0.000368 |
| `lootChests[12].bossChance` | 0.05 |
| `lootChests[12].minLevel` | 5 |
| `lootChests[12].table` | `loot:LCHEST_CARTOGRAPHER` |
| `lootChests[13].code` | `CHEST_JEWELLER` |
| `lootChests[13].gear[0].count` | 2 |
| `lootChests[13].gear[0].rarity` | `RARE` |
| `lootChests[13].gear[0].slots` | `RING`; `AMULET`; `BELT` |
| `lootChests[13].gear[0].tables` | [drop](Tables-TEMPLATE.md#drop) |
| `lootChests[13].gear[1].count` | 1 |
| `lootChests[13].gear[1].rarity` | `MAGIC` |
| `lootChests[13].gear[1].rareChance` | 0.5 |
| `lootChests[13].gear[1].tables` | [drop](Tables-TEMPLATE.md#drop) |
| `lootChests[13].gear[1].slots` | `JEWEL` |
| `lootChests[13].slots` | `RING`; `AMULET`; `BELT`; `JEWEL` |
| `lootChests[13].uniqueChance` | 0.1 |
| `lootChests[13].uniqueTables` | [unique:world](Tables-TEMPLATE.md#unique-world) |
| `lootChests[13].dropChance` | 0.000279 |
| `lootChests[13].bossChance` | 0.015 |
| `lootChests[13].minLevel` | 15 |
| `lootChests[14].code` | `CHEST_SEER` |
| `lootChests[14].dropChance` | 0.000223 |
| `lootChests[14].bossChance` | 0.01 |
| `lootChests[14].minLevel` | 20 |
| `lootChests[14].table` | `loot:LCHEST_SEER` |
| `lootChests[15].code` | `CHEST_CLASS` |
| `lootChests[15].gear[0].count` | 2 |
| `lootChests[15].gear[0].rarity` | `RARE` |
| `lootChests[15].gear[0].classFit` | Да |
| `lootChests[15].gear[0].tables` | [drop](Tables-TEMPLATE.md#drop) |
| `lootChests[15].uniqueChance` | 0.15 |
| `lootChests[15].uniqueTables` | [unique:world](Tables-TEMPLATE.md#unique-world) |
| `lootChests[15].classUnique` | Да |
| `lootChests[15].bossChance` | 0.01 |
| `lootChests[15].finaleChance` | 0.05 |
| `lootChests[15].minLevel` | 20 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/campaign.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
