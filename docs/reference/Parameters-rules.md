# Параметры · rules

Точный справочник значений файла. Индексы в квадратных скобках начинаются с нуля. Отсутствующие в JSON поля получают значения модели Kotlin; этот раздел показывает именно явно заданные параметры, а не полный результат загрузки.

| Путь параметра | Значение |
| --- | --- |
| `rarities.MAGIC.prefixes` | 1 |
| `rarities.MAGIC.suffixes` | 1 |
| `rarities.MAGIC.affixes` | 1; 2 |
| `rarities.RARE.prefixes` | 3 |
| `rarities.RARE.suffixes` | 3 |
| `rarities.RARE.affixes` | 4; 6 |
| `rarities.RARE:JEWEL.prefixes` | 2 |
| `rarities.RARE:JEWEL.suffixes` | 2 |
| `rarities.RARE:JEWEL.affixes` | 3; 4 |
| `merchant.resaleShare` | 0.9 |
| `merchant.windowHours` | 4 |
| `merchant.minOffers` | 12 |
| `merchant.maxOffers` | 16 |
| `merchant.levelSpread` | 2 |
| `merchant.markup` | 3 |
| `merchant.tables` | [merchant](Tables/Tables-TEMPLATE.md#merchant) |
| `merchant.rarities` | [rarity:merchant](Tables/Tables-VALUE.md#rarity-merchant) |
| `merchant.flasks` | 1; 2 |
| `merchant.orbs.codes` | [Orb of Transmutation](../Items/Items-CURRENCY.md#orb_of_transmutation); [Orb of Augmentation](../Items/Items-CURRENCY.md#orb_of_augmentation); [Orb of Alteration](../Items/Items-CURRENCY.md#orb_of_alteration); [Orb of Regret](../Items/Items-CURRENCY.md#orb_of_regret) |
| `merchant.orbs.markup` | 2 |
| `merchant.orbs.growth` | 1.25 |
| `merchant.orbs.stock.ORB_OF_TRANSMUTATION` | 5 |
| `merchant.orbs.stock.ORB_OF_AUGMENTATION` | 4 |
| `merchant.orbs.stock.ORB_OF_ALTERATION` | 3 |
| `merchant.orbs.stock.ORB_OF_REGRET` | 2 |
| `merchant.orbs.defaultStock` | 3 |
| `sell.base` | 10 |
| `sell.affixShare` | 0.15 |
| `sell.qualityFloor` | 0.5 |
| `sell.neutralQuality` | 0.5 |
| `sell.rarity.COMMON` | 1 |
| `sell.rarity.MAGIC` | 1.5 |
| `sell.rarity.RARE` | 2.5 |
| `sell.rarity.UNIQUE` | 8 |
| `sell.rarity.MYTHICAL` | 20 |
| `bench.costs[0].orb` | [Divine Orb](../Items/Items-CURRENCY.md#divine_orb) |
| `bench.costs[0].amount` | 3 |
| `bench.costs[1].orb` | [Divine Orb](../Items/Items-CURRENCY.md#divine_orb) |
| `bench.costs[1].amount` | 1 |
| `bench.costs[2].orb` | [Exalted Orb](../Items/Items-CURRENCY.md#exalted_orb) |
| `bench.costs[2].amount` | 1 |
| `bench.costs[3].orb` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |
| `bench.costs[3].amount` | 1 |
| `bench.costs[4].orb` | [Orb of Alteration](../Items/Items-CURRENCY.md#orb_of_alteration) |
| `bench.costs[4].amount` | 4 |
| `bench.costs[5].orb` | [Orb of Transmutation](../Items/Items-CURRENCY.md#orb_of_transmutation) |
| `bench.costs[5].amount` | 3 |
| `bench.costs[6].orb` | [Orb of Transmutation](../Items/Items-CURRENCY.md#orb_of_transmutation) |
| `bench.costs[6].amount` | 1 |
| `bench.uncraftOrb` | [Orb of Scouring](../Items/Items-CURRENCY.md#orb_of_scouring) |
| `bench.maxMapLevel` | 100 |
| `orbs.chanceUniquePercent` | 5 |
| `orbs.chanceRarities` | [rarity:chance](Tables/Tables-VALUE.md#rarity-chance) |
| `orbs.chanceUniques` | [unique:chance](Tables/Tables-TEMPLATE.md#unique-chance) |
| `orbs.fractureMinAffixes` | 4 |
| `orbs.vaalShift` | 0.8; 1.2 |
| `orbs.maxAlchemyLines` | 3 |
| `orbs.mapAlchemy` | [alchemy:map](Tables/Tables-MODIFIER.md#alchemy-map) |
| `orbs.choices` | 3 |
| `flasks.starter` | [Small Life Flask](../Equipment/Equipment-FLASK.md#flask_small_life) |
| `flasks.maxQuality` | 20 |
| `flasks.orbs` | [Orb of Transmutation](../Items/Items-CURRENCY.md#orb_of_transmutation); [Orb of Alteration](../Items/Items-CURRENCY.md#orb_of_alteration); [Orb of Augmentation](../Items/Items-CURRENCY.md#orb_of_augmentation); [Orb of Scouring](../Items/Items-CURRENCY.md#orb_of_scouring); [Orb of Chance](../Items/Items-CURRENCY.md#orb_of_chance); [Blessed Orb](../Items/Items-CURRENCY.md#blessed_orb); [Divine Orb](../Items/Items-CURRENCY.md#divine_orb); [Vaal Orb](../Items/Items-CURRENCY.md#vaal_orb); [Orb of Quality](../Items/Items-CURRENCY.md#quality_orb) |
| `starter.gold` | 25 |
| `starter.toolPrefix` | `BRONZE_` |
| `starter.orbs.ORB_OF_TRANSMUTATION` | 5 |
| `starter.orbs.ORB_OF_AUGMENTATION` | 3 |
| `starter.magicWeapon` | Да |
| `brews.potions.POTION_EXPERIENCE_T1.ATLAS_EXPERIENCE` | 10 |
| `brews.potions.POTION_EXPERIENCE_T2.ATLAS_EXPERIENCE` | 20 |
| `brews.potions.POTION_EXPERIENCE_T3.ATLAS_EXPERIENCE` | 30 |
| `brews.potions.POTION_GOLD_T1.ATLAS_GOLD` | 15 |
| `brews.potions.POTION_GOLD_T2.ATLAS_GOLD` | 30 |
| `brews.potions.POTION_GOLD_T3.ATLAS_GOLD` | 45 |
| `brews.potions.POTION_RARITY_T1.ATLAS_RARITY` | 10 |
| `brews.potions.POTION_RARITY_T2.ATLAS_RARITY` | 20 |
| `brews.potions.POTION_RARITY_T3.ATLAS_RARITY` | 30 |
| `brews.potions.POTION_RESIST_T1.BREW_RESIST` | 8 |
| `brews.potions.POTION_RESIST_T2.BREW_RESIST` | 15 |
| `brews.potions.POTION_RESIST_T3.BREW_RESIST` | 22 |
| `brews.potions.POTION_DAMAGE_T1.BREW_DAMAGE` | 8 |
| `brews.potions.POTION_DAMAGE_T2.BREW_DAMAGE` | 15 |
| `brews.potions.POTION_DAMAGE_T3.BREW_DAMAGE` | 22 |
| `brews.potions.POTION_LIFE_T1.BREW_LIFE` | 6 |
| `brews.potions.POTION_LIFE_T2.BREW_LIFE` | 12 |
| `brews.potions.POTION_LIFE_T3.BREW_LIFE` | 18 |
| `brews.scarabs.SCARAB_CHESTS.MAP_CHESTS` | 1 |
| `brews.scarabs.SCARAB_PACK.MAP_PACK_SIZE` | 15 |
| `brews.scarabs.SCARAB_RARITY.MAP_RARITY` | 20 |
| `brews.scarabsPerMap` | 2 |
| `brews.temper.ore` | 10 |
| `brews.temper.ores.COPPER_ORE` | 1 |
| `brews.temper.ores.IRON_ORE` | 13 |
| `brews.temper.ores.SILVER_ORE` | 25 |
| `brews.temper.ores.MITHRIL_ORE` | 40 |
| `brews.temper.ores.ADAMANT_ORE` | 55 |
| `brews.temper.ores.STARMETAL_ORE` | 70 |
| `brews.temper.minQuality` | 21 |
| `brews.temper.maxQuality` | 30 |
| `brews.temper.maxLevels` | 5 |
| `brews.temper.smithLevel` | 5 |
| `path.steps[0].code` | `ZONE` |
| `path.steps[0].check` | `ZONE` |
| `path.steps[0].reward.gold` | 50 |
| `path.steps[0].reward.bag.ORB_OF_TRANSMUTATION` | 3 |
| `path.steps[1].code` | `EQUIP` |
| `path.steps[1].check` | `EQUIP` |
| `path.steps[1].reward.item` | [Small Mana Flask](../Equipment/Equipment-FLASK.md#flask_small_mana) |
| `path.steps[1].reward.rarity` | `MAGIC` |
| `path.steps[2].code` | `TREE` |
| `path.steps[2].check` | `TREE` |
| `path.steps[2].reward.bag.ORB_OF_AUGMENTATION` | 3 |
| `path.steps[2].reward.bag.ORB_OF_ALTERATION` | 2 |
| `path.steps[3].code` | `SKILL` |
| `path.steps[3].check` | `SKILL` |
| `path.steps[3].reward.rare` | Да |
| `path.steps[4].code` | `BOSS` |
| `path.steps[4].check` | `BOSS` |
| `path.steps[4].reward.gold` | 100 |
| `path.steps[4].reward.bag.ORB_OF_ALCHEMY` | 1 |
| `path.steps[5].code` | `ORB` |
| `path.steps[5].check` | `ORB` |
| `path.steps[5].reward.bag.CHEST_WOODEN_T1` | 1 |
| `auction.slots` | 100 |
| `auction.minLevel` | 1 |
| `auction.buyerFee` | 10 |
| `auction.lotDays` | 7 |
| `inputs.heroName` | 24 |
| `inputs.login` | 32 |
| `inputs.password` | 64 |
| `inputs.search` | 40 |
| `inputs.server` | 200 |
| `inputs.code` | 64 |
| `inputs.number` | 12 |
| `inputs.report` | 2000 |
| `inputs.suggestion` | 1000 |
| `inputs.mailSubject` | 80 |
| `inputs.mailBody` | 2000 |
| `loot.goldGrowth` | 1.1 |
| `loot.goldTaper.from` | 10 |
| `loot.goldTaper.rate` | 0.16 |
| `loot.goldTaper.bends[0].from` | 25 |
| `loot.goldTaper.bends[0].rate` | 0.45 |
| `loot.goldTaper.bends[1].from` | 37 |
| `loot.goldTaper.bends[1].rate` | 0.25 |
| `loot.goldTaper.bends[2].from` | 49 |
| `loot.goldTaper.bends[2].rate` | 0.15 |
| `loot.goldTaper.bends[3].from` | 61 |
| `loot.goldTaper.bends[3].rate` | 0.05 |
| `loot.experiencePower` | 1.9 |
| `loot.rarityWeights.COMMON` | 100 |
| `loot.rarityWeights.MAGIC` | 40 |
| `loot.rarityWeights.RARE` | 15 |
| `loot.rarityWeights.UNIQUE` | 1 |
| `loot.rarityWeights.MYTHICAL` | 0.2 |
| `loot.uniqueReach` | 10 |
| `loot.recipeChance` | 0.1 |
| `loot.jewelHeroLevel` | 5 |
| `loot.baseVariance.min` | 90 |
| `loot.baseVariance.max` | 110 |
| `loot.baseVariance.perLevel` | 1 |
| `loot.baseVariance.cap` | 20 |
| `loot.baseVariance.lines` | [BASE_ARMOUR](Modifiers/Modifiers-IMPLICIT.md#base_armour); [BASE_EVASION](Modifiers/Modifiers-IMPLICIT.md#base_evasion); [BASE_ENERGY_SHIELD](Modifiers/Modifiers-IMPLICIT.md#base_energy_shield); [BASE_PHYSICAL_DAMAGE](Modifiers/Modifiers-IMPLICIT.md#base_physical_damage) |
| `maxCharacters` | 3 |
| `stash.baseSlots` | 200 |
| `stash.maxSlots` | 1000 |
| `stash.slotStep` | 50 |
| `stash.firstPrice` | 1000 |
| `stash.priceGrowth` | 1.25 |
| `stash.overflowSlots` | 100 |
| `run.packChance` | 0.35 |
| `run.packMax` | 6 |
| `run.newSeedSeconds` | 30 |
| `charges.maximum` | 3 |
| `charges.duration` | 10 |
| `charges.durationStat` | [Длительность зарядов](Stats/Stats-HERO.md#stock_charge_duration) |
| `charges.kinds.FRENZY.max` | [Максимум зарядов ярости](Stats/Stats-HERO.md#stock_max_frenzy_charges) |
| `charges.kinds.FRENZY.lines[0].stat` | [Скорость атаки](Stats/Stats-HERO.md#stock_attack_speed) |
| `charges.kinds.FRENZY.lines[0].op` | `INCREASED` |
| `charges.kinds.FRENZY.lines[0].value` | 2 |
| `charges.kinds.FRENZY.lines[1].stat` | [Скорость применения](Stats/Stats-HERO.md#stock_cast_speed) |
| `charges.kinds.FRENZY.lines[1].op` | `INCREASED` |
| `charges.kinds.FRENZY.lines[1].value` | 2 |
| `charges.kinds.FRENZY.lines[2].stat` | [Урон](Stats/Stats-HERO.md#stock_damage) |
| `charges.kinds.FRENZY.lines[2].op` | `MORE` |
| `charges.kinds.FRENZY.lines[2].value` | 2 |
| `charges.kinds.POWER.max` | [Максимум зарядов силы](Stats/Stats-HERO.md#stock_max_power_charges) |
| `charges.kinds.POWER.lines[0].stat` | [Шанс критического удара](Stats/Stats-HERO.md#stock_critical_chance) |
| `charges.kinds.POWER.lines[0].op` | `INCREASED` |
| `charges.kinds.POWER.lines[0].value` | 20 |
| `charges.kinds.POWER.lines[1].stat` | [Шанс крита заклинаний](Stats/Stats-HERO.md#stock_spell_critical_chance) |
| `charges.kinds.POWER.lines[1].op` | `INCREASED` |
| `charges.kinds.POWER.lines[1].value` | 20 |
| `charges.kinds.ENDURANCE.max` | [Максимум зарядов выносливости](Stats/Stats-HERO.md#stock_max_endurance_charges) |
| `charges.kinds.ENDURANCE.lines[0].stat` | [Снижение физического урона](Stats/Stats-HERO.md#stock_physical_reduction) |
| `charges.kinds.ENDURANCE.lines[0].value` | 2 |
| `charges.kinds.ENDURANCE.lines[1].stat` | [Все сопротивления стихиям](Stats/Stats-HERO.md#stock_resist_all) |
| `charges.kinds.ENDURANCE.lines[1].value` | 2 |
| `retired.PET_ORB_UPGRADE` | [Orb of Chance](../Items/Items-CURRENCY.md#orb_of_chance) |
| `retired.PET_ORB_REROLL` | [Orb of Alteration](../Items/Items-CURRENCY.md#orb_of_alteration) |
| `retired.PET_ORB_AUGMENT` | [Orb of Augmentation](../Items/Items-CURRENCY.md#orb_of_augmentation) |
| `retired.PET_ORB_DIVINE` | [Orb of Alteration](../Items/Items-CURRENCY.md#orb_of_alteration) |
| `retired.PET_ORB_ANNUL` | [Orb of Alteration](../Items/Items-CURRENCY.md#orb_of_alteration) |
| `retired.WHETSTONE` | [Orb of Quality](../Items/Items-CURRENCY.md#quality_orb) |
| `retired.ARMOURERS_SCRAP` | [Orb of Quality](../Items/Items-CURRENCY.md#quality_orb) |
| `retired.GLASSBLOWERS_BAUBLE` | [Orb of Quality](../Items/Items-CURRENCY.md#quality_orb) |
| `retired.ESSENCE_ORB` | [Orb of Alteration](../Items/Items-CURRENCY.md#orb_of_alteration) |
| `retired.SCRIBE_ORB` | [Orb of Alteration](../Items/Items-CURRENCY.md#orb_of_alteration) |
| `retired.PERIL_ORB` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |
| `retired.MERCY_ORB` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |
| `retired.EMPOWERING_ORB` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |
| `retired.HORDE_ORB` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |
| `retired.MAGUS_ORB` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |
| `retired.ELITE_ORB` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |
| `retired.BOUNTY_ORB` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |
| `retired.TREASURE_ORB` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |
| `retired.GILDED_ORB` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |
| `retired.WARDEN_ORB` | [Chaos Orb](../Items/Items-CURRENCY.md#chaos_orb) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/rules.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
