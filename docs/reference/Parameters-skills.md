# Параметры · skills

Точный справочник значений файла. Индексы в квадратных скобках начинаются с нуля. Отсутствующие в JSON поля получают значения модели Kotlin; этот раздел показывает именно явно заданные параметры, а не полный результат загрузки.

| Путь параметра | Значение |
| --- | --- |
| `rules.boostedMaxLevel` | 25 |
| `rules.heroMaxLevel` | 70 |
| `rules.activeSlots` | 1; 12; 28 |
| `rules.passiveSlots` | 1; 20 |
| `rules.manaPerLevel` | 5 |
| `rules.attributes.single` | 2.2; 8 |
| `rules.attributes.dual` | 1.4; 5 |
| `rules.attributes.triple` | 0.9; 3 |
| `rules.books.boss` | 0.3 |
| `rules.books.bossOwnClass` | 0.6 |
| `rules.books.rare` | 0.03 |
| `rules.books.guardian` | 0.1 |
| `rules.books.reach` | 5 |
| `rules.exchange.books` | 3 |
| `rules.exchange.goldPerLevel` | 100 |
| `rules.casterSpells.FIRE` | [Огненный шар](Monster-skills.md#mob_fireball) |
| `rules.casterSpells.COLD` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt) |
| `rules.casterSpells.LIGHTNING` | [Разряд](Monster-skills.md#mob_zap) |
| `rules.casterSpells.CHAOS` | [Ядовитый плевок](Monster-skills.md#mob_spit) |
| `rules.casterSpells.PHYSICAL` | [Кольцо хаоса](Monster-skills.md#mob_chaos_ring) |
| `classes[0].code` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `classes[0].attributes` | [Сила](Stats/Stats-HERO.md#stock_strength) |
| `classes[0].mana` | 30 |
| `classes[1].code` | [Охотница (Ranger)](../Classes.md#ranger) |
| `classes[1].attributes` | [Ловкость](Stats/Stats-HERO.md#stock_agility) |
| `classes[1].mana` | 40 |
| `classes[2].code` | [Ведьма (Witch)](../Classes.md#witch) |
| `classes[2].attributes` | [Интеллект](Stats/Stats-HERO.md#stock_intellect) |
| `classes[2].mana` | 60 |
| `classes[3].code` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `classes[3].attributes` | [Сила](Stats/Stats-HERO.md#stock_strength); [Ловкость](Stats/Stats-HERO.md#stock_agility) |
| `classes[3].mana` | 35 |
| `classes[4].code` | [Храмовник (Templar)](../Classes.md#templar) |
| `classes[4].attributes` | [Сила](Stats/Stats-HERO.md#stock_strength); [Интеллект](Stats/Stats-HERO.md#stock_intellect) |
| `classes[4].mana` | 50 |
| `classes[5].code` | [Тень (Shadow)](../Classes.md#shadow) |
| `classes[5].attributes` | [Ловкость](Stats/Stats-HERO.md#stock_agility); [Интеллект](Stats/Stats-HERO.md#stock_intellect) |
| `classes[5].mana` | 50 |
| `classes[6].code` | [Скион (Scion)](../Classes.md#scion) |
| `classes[6].attributes` | [Сила](Stats/Stats-HERO.md#stock_strength); [Ловкость](Stats/Stats-HERO.md#stock_agility); [Интеллект](Stats/Stats-HERO.md#stock_intellect) |
| `classes[6].mana` | 45 |
| `skills[0].code` | [Тяжёлый удар](../Skills/Skills-MARAUDER.md#heavy_strike) |
| `skills[0].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[0].type` | `ATTACK` |
| `skills[0].unlock` | 1 |
| `skills[0].icon` | `physical` |
| `skills[0].mana` | 6; 20 |
| `skills[0].cooldown` | 3 |
| `skills[0].prepare` | 50; 25 |
| `skills[0].condition` | `READY` |
| `skills[0].hit.targets` | 1 |
| `skills[0].hit.weapon` | 139; 232 |
| `skills[0].hit.stun` | 20; 40 |
| `skills[1].code` | [Клич стойкости](../Skills/Skills-MARAUDER.md#enduring_cry) |
| `skills[1].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[1].type` | `WARCRY` |
| `skills[1].unlock` | 6 |
| `skills[1].icon` | [armour](Tables/Tables-MODIFIER.md#armour) |
| `skills[1].mana` | 10; 30 |
| `skills[1].cooldown` | 12 |
| `skills[1].condition` | `FIGHT_START` |
| `skills[1].buff.duration` | 6 |
| `skills[1].buff.stats[0].stat` | [Броня](Stats/Stats-HERO.md#stock_armor) |
| `skills[1].buff.stats[0].op` | `INCREASED` |
| `skills[1].buff.stats[0].value` | 20; 60 |
| `skills[1].buff.stats[1].stat` | [Регенерация здоровья, %](Stats/Stats-HERO.md#stock_life_regen_percent) |
| `skills[1].buff.stats[1].value` | 1; 3 |
| `skills[2].code` | [Сотрясение земли](../Skills/Skills-MARAUDER.md#ground_slam) |
| `skills[2].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[2].type` | `ATTACK` |
| `skills[2].unlock` | 12 |
| `skills[2].icon` | `earth` |
| `skills[2].mana` | 12; 34 |
| `skills[2].cooldown` | 6 |
| `skills[2].prepare` | 50; 20 |
| `skills[2].condition` | `ENEMIES_3` |
| `skills[2].hit.targets` | 0 |
| `skills[2].hit.weapon` | 85; 147 |
| `skills[2].hit.stun` | 25; 25 |
| `skills[3].code` | [Адский удар](../Skills/Skills-MARAUDER.md#infernal_blow) |
| `skills[3].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[3].type` | `ATTACK` |
| `skills[3].unlock` | 20 |
| `skills[3].icon` | `fire` |
| `skills[3].mana` | 14; 36 |
| `skills[3].cooldown` | 5 |
| `skills[3].prepare` | 50; 20 |
| `skills[3].condition` | `READY` |
| `skills[3].hit.targets` | 1 |
| `skills[3].hit.weapon` | 116; 201 |
| `skills[3].hit.element` | `FIRE` |
| `skills[3].hit.convert` | 40; 80 |
| `skills[3].hit.ailments[0].ailment` | `IGNITE` |
| `skills[3].hit.ailments[0].chance` | 30; 50 |
| `skills[4].code` | [Уязвимость](../Skills/Skills-MARAUDER.md#vulnerability) |
| `skills[4].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[4].type` | `CURSE` |
| `skills[4].unlock` | 30 |
| `skills[4].icon` | `bleeding` |
| `skills[4].mana` | 18; 40 |
| `skills[4].cooldown` | 15 |
| `skills[4].prepare` | 30; 10 |
| `skills[4].condition` | `RARE_OR_BOSS` |
| `skills[4].curse.duration` | 8 |
| `skills[4].curse.targets` | 0 |
| `skills[4].curse.stats[0].stat` | [Получаемый физический урон](Stats/Stats-HERO.md#stock_physical_taken) |
| `skills[4].curse.stats[0].value` | 15; 35 |
| `skills[4].curse.stats[1].stat` | [Шанс кровотечения по цели](Stats/Stats-HERO.md#stock_bleed_taken) |
| `skills[4].curse.stats[1].value` | 10; 25 |
| `skills[5].code` | [Кровавая ярость](../Skills/Skills-MARAUDER.md#blood_rage) |
| `skills[5].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[5].type` | `GUARD` |
| `skills[5].unlock` | 40 |
| `skills[5].icon` | `leech` |
| `skills[5].mana` | 16; 34 |
| `skills[5].cooldown` | 20 |
| `skills[5].prepare` | 40; 15 |
| `skills[5].condition` | `LIFE_50` |
| `skills[5].buff.duration` | 8 |
| `skills[5].buff.stats[0].stat` | [Скорость атаки](Stats/Stats-HERO.md#stock_attack_speed) |
| `skills[5].buff.stats[0].op` | `INCREASED` |
| `skills[5].buff.stats[0].value` | 10; 25 |
| `skills[5].buff.stats[1].stat` | [Общий вампиризм](Stats/Stats-HERO.md#stock_leech_all) |
| `skills[5].buff.stats[1].value` | 1; 3 |
| `skills[6].code` | [Рёв стойкости](../Skills/Skills-MARAUDER.md#enduring_roar) |
| `skills[6].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[6].type` | `WARCRY` |
| `skills[6].unlock` | 50 |
| `skills[6].icon` | [armour](Tables/Tables-MODIFIER.md#armour) |
| `skills[6].mana` | 14; 34 |
| `skills[6].cooldown` | 10 |
| `skills[6].prepare` | 60; 30 |
| `skills[6].condition` | `FIGHT_START` |
| `skills[6].buff.duration` | 4 |
| `skills[6].buff.stats[0].stat` | [Снижение физического урона](Stats/Stats-HERO.md#stock_physical_reduction) |
| `skills[6].buff.stats[0].value` | 2; 6 |
| `skills[6].charges.kind` | `ENDURANCE` |
| `skills[6].charges.gain` | 1; 3 |
| `skills[7].code` | [Решимость](../Skills/Skills-MARAUDER.md#determination) |
| `skills[7].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[7].type` | `AURA` |
| `skills[7].unlock` | 1 |
| `skills[7].icon` | [armour](Tables/Tables-MODIFIER.md#armour) |
| `skills[7].reserve` | 30 |
| `skills[7].stats[0].stat` | [Броня](Stats/Stats-HERO.md#stock_armor) |
| `skills[7].stats[0].op` | `INCREASED` |
| `skills[7].stats[0].value` | 20; 50 |
| `skills[8].code` | [Железная кожа](../Skills/Skills-MARAUDER.md#iron_skin) |
| `skills[8].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[8].type` | `BONUS` |
| `skills[8].unlock` | 5 |
| `skills[8].icon` | `constitution` |
| `skills[8].stats[0].stat` | [Здоровье](Stats/Stats-HERO.md#stock_health) |
| `skills[8].stats[0].op` | `INCREASED` |
| `skills[8].stats[0].value` | 4; 12 |
| `skills[8].stats[1].stat` | [Броня](Stats/Stats-HERO.md#stock_armor) |
| `skills[8].stats[1].op` | `INCREASED` |
| `skills[8].stats[1].value` | 10; 30 |
| `skills[9].code` | [Кровопролитие](../Skills/Skills-MARAUDER.md#bloodletting) |
| `skills[9].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[9].type` | `TRIGGER` |
| `skills[9].unlock` | 10 |
| `skills[9].icon` | `health` |
| `skills[9].trigger.on` | `KILL` |
| `skills[9].trigger.heal.life` | 2; 6 |
| `skills[10].code` | [Гнев](../Skills/Skills-MARAUDER.md#anger) |
| `skills[10].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[10].type` | `AURA` |
| `skills[10].unlock` | 16 |
| `skills[10].icon` | `fire` |
| `skills[10].reserve` | 30 |
| `skills[10].stats[0].stat` | [Урон огнём](Stats/Stats-HERO.md#stock_attack_fire) |
| `skills[10].stats[0].value` | 6; 140 |
| `skills[11].code` | [Ярость берсерка](../Skills/Skills-MARAUDER.md#berserker_fury) |
| `skills[11].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[11].type` | `BONUS` |
| `skills[11].unlock` | 24 |
| `skills[11].icon` | `strength` |
| `skills[11].stats[0].stat` | [Урон](Stats/Stats-HERO.md#stock_damage) |
| `skills[11].stats[0].op` | `INCREASED` |
| `skills[11].stats[0].value` | 10; 30 |
| `skills[11].lowLife` | Да |
| `skills[12].code` | [Несокрушимость](../Skills/Skills-MARAUDER.md#unbreakable) |
| `skills[12].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[12].type` | `TRIGGER` |
| `skills[12].unlock` | 32 |
| `skills[12].icon` | [shield](Tables/Tables-MODIFIER.md#shield) |
| `skills[12].trigger.on` | `LOW_LIFE` |
| `skills[12].trigger.cooldown` | 30 |
| `skills[12].trigger.barrier.life` | 10; 25 |
| `skills[12].trigger.barrier.duration` | 4 |
| `skills[13].code` | [Крепкий лоб](../Skills/Skills-MARAUDER.md#thick_skull) |
| `skills[13].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[13].type` | `BONUS` |
| `skills[13].unlock` | 42 |
| `skills[13].icon` | `stun` |
| `skills[13].stats[0].stat` | [Порог оглушения](Stats/Stats-HERO.md#stock_stun_threshold) |
| `skills[13].stats[0].op` | `INCREASED` |
| `skills[13].stats[0].value` | 15; 45 |
| `skills[13].stats[1].stat` | [Снижение физического урона](Stats/Stats-HERO.md#stock_physical_reduction) |
| `skills[13].stats[1].value` | 1; 4 |
| `skills[14].code` | [Отмщение](../Skills/Skills-MARAUDER.md#retaliation) |
| `skills[14].heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| `skills[14].type` | `TRIGGER` |
| `skills[14].unlock` | 52 |
| `skills[14].icon` | `combat` |
| `skills[14].trigger.on` | `HIT_TAKEN` |
| `skills[14].trigger.chance` | 10; 20 |
| `skills[14].trigger.hit.targets` | 1 |
| `skills[14].trigger.hit.weapon` | 100; 180 |
| `skills[15].code` | [Расщеплённая стрела](../Skills/Skills-RANGER.md#split_arrow) |
| `skills[15].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[15].type` | `ATTACK` |
| `skills[15].unlock` | 1 |
| `skills[15].icon` | [bow](Tables/Tables-MODIFIER.md#bow) |
| `skills[15].mana` | 6; 20 |
| `skills[15].cooldown` | 2.5 |
| `skills[15].prepare` | 50; 25 |
| `skills[15].condition` | `READY` |
| `skills[15].hit.targets` | 3 |
| `skills[15].hit.weapon` | 93; 155 |
| `skills[16].code` | [Прицельный выстрел](../Skills/Skills-RANGER.md#snipe) |
| `skills[16].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[16].type` | `ATTACK` |
| `skills[16].unlock` | 6 |
| `skills[16].icon` | `critical` |
| `skills[16].mana` | 10; 28 |
| `skills[16].cooldown` | 5 |
| `skills[16].prepare` | 50; 20 |
| `skills[16].condition` | `READY` |
| `skills[16].hit.targets` | 1 |
| `skills[16].hit.weapon` | 228; 393 |
| `skills[16].hit.stats[0].stat` | [Шанс критического удара](Stats/Stats-HERO.md#stock_critical_chance) |
| `skills[16].hit.stats[0].value` | 5; 15 |
| `skills[17].code` | [Ледяной выстрел](../Skills/Skills-RANGER.md#ice_shot) |
| `skills[17].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[17].type` | `ATTACK` |
| `skills[17].unlock` | 12 |
| `skills[17].icon` | `cold` |
| `skills[17].mana` | 12; 32 |
| `skills[17].cooldown` | 5 |
| `skills[17].prepare` | 50; 20 |
| `skills[17].condition` | `READY` |
| `skills[17].hit.targets` | 1 |
| `skills[17].hit.weapon` | 135; 228 |
| `skills[17].hit.element` | `COLD` |
| `skills[17].hit.convert` | 50; 50 |
| `skills[17].hit.ailments[0].ailment` | `CHILL` |
| `skills[17].hit.ailments[0].chance` | 100; 100 |
| `skills[17].hit.ailments[1].ailment` | `FREEZE` |
| `skills[17].hit.ailments[1].chance` | 10; 25 |
| `skills[18].code` | [Дождь стрел](../Skills/Skills-RANGER.md#rain_of_arrows) |
| `skills[18].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[18].type` | `ATTACK` |
| `skills[18].unlock` | 20 |
| `skills[18].icon` | [quiver](Tables/Tables-MODIFIER.md#quiver) |
| `skills[18].mana` | 16; 40 |
| `skills[18].cooldown` | 8 |
| `skills[18].prepare` | 60; 30 |
| `skills[18].condition` | `ENEMIES_3` |
| `skills[18].hit.targets` | 0 |
| `skills[18].hit.hits` | 2 |
| `skills[18].hit.weapon` | 100.8; 173.6 |
| `skills[19].code` | [Метка снайпера](../Skills/Skills-RANGER.md#snipers_mark) |
| `skills[19].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[19].type` | `CURSE` |
| `skills[19].unlock` | 30 |
| `skills[19].icon` | `focus` |
| `skills[19].mana` | 16; 36 |
| `skills[19].cooldown` | 14 |
| `skills[19].prepare` | 30; 10 |
| `skills[19].condition` | `RARE_OR_BOSS` |
| `skills[19].curse.duration` | 8 |
| `skills[19].curse.targets` | 1 |
| `skills[19].curse.stats[0].stat` | [Множитель крита по цели](Stats/Stats-HERO.md#stock_critical_taken) |
| `skills[19].curse.stats[0].value` | 20; 50 |
| `skills[19].curse.stats[1].stat` | [Получаемый урон](Stats/Stats-HERO.md#stock_damage_taken) |
| `skills[19].curse.stats[1].value` | 10; 25 |
| `skills[20].code` | [Покров ветра](../Skills/Skills-RANGER.md#wind_veil) |
| `skills[20].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[20].type` | `GUARD` |
| `skills[20].unlock` | 40 |
| `skills[20].icon` | `air` |
| `skills[20].mana` | 14; 32 |
| `skills[20].cooldown` | 16 |
| `skills[20].prepare` | 40; 15 |
| `skills[20].condition` | `LIFE_50` |
| `skills[20].buff.duration` | 5 |
| `skills[20].buff.stats[0].stat` | [Уклонение](Stats/Stats-HERO.md#stock_evasion) |
| `skills[20].buff.stats[0].op` | `INCREASED` |
| `skills[20].buff.stats[0].value` | 30; 80 |
| `skills[21].code` | [Яростный выстрел](../Skills/Skills-RANGER.md#frenzy_strike) |
| `skills[21].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[21].type` | `ATTACK` |
| `skills[21].unlock` | 50 |
| `skills[21].icon` | `speed` |
| `skills[21].mana` | 8; 24 |
| `skills[21].cooldown` | 3 |
| `skills[21].prepare` | 50; 25 |
| `skills[21].condition` | `READY` |
| `skills[21].hit.targets` | 1 |
| `skills[21].hit.weapon` | 110; 170 |
| `skills[21].charges.kind` | `FRENZY` |
| `skills[21].charges.gain` | 1; 1 |
| `skills[22].code` | [Грация](../Skills/Skills-RANGER.md#grace) |
| `skills[22].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[22].type` | `AURA` |
| `skills[22].unlock` | 1 |
| `skills[22].icon` | `evasion` |
| `skills[22].reserve` | 30 |
| `skills[22].stats[0].stat` | [Уклонение](Stats/Stats-HERO.md#stock_evasion) |
| `skills[22].stats[0].op` | `INCREASED` |
| `skills[22].stats[0].value` | 20; 60 |
| `skills[23].code` | [Зоркий глаз](../Skills/Skills-RANGER.md#eagle_eye) |
| `skills[23].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[23].type` | `BONUS` |
| `skills[23].unlock` | 5 |
| `skills[23].icon` | `critical` |
| `skills[23].stats[0].stat` | [Шанс критического удара](Stats/Stats-HERO.md#stock_critical_chance) |
| `skills[23].stats[0].op` | `INCREASED` |
| `skills[23].stats[0].value` | 20; 60 |
| `skills[24].code` | [Охотничий азарт](../Skills/Skills-RANGER.md#hunters_thrill) |
| `skills[24].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[24].type` | `TRIGGER` |
| `skills[24].unlock` | 10 |
| `skills[24].icon` | `potion` |
| `skills[24].trigger.on` | `KILL` |
| `skills[24].trigger.chance` | 20; 50 |
| `skills[24].trigger.flaskCharges` | 1 |
| `skills[25].code` | [Спешка](../Skills/Skills-RANGER.md#haste) |
| `skills[25].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[25].type` | `AURA` |
| `skills[25].unlock` | 16 |
| `skills[25].icon` | `speed` |
| `skills[25].reserve` | 30 |
| `skills[25].stats[0].stat` | [Скорость атаки](Stats/Stats-HERO.md#stock_attack_speed) |
| `skills[25].stats[0].op` | `INCREASED` |
| `skills[25].stats[0].value` | 6; 16 |
| `skills[25].stats[1].stat` | [Скорость передвижения](Stats/Stats-HERO.md#stock_movement_speed) |
| `skills[25].stats[1].op` | `INCREASED` |
| `skills[25].stats[1].value` | 5; 15 |
| `skills[26].code` | [Лёгкая стопа](../Skills/Skills-RANGER.md#light_foot) |
| `skills[26].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[26].type` | `BONUS` |
| `skills[26].unlock` | 24 |
| `skills[26].icon` | `agility` |
| `skills[26].stats[0].stat` | [Скорость передвижения](Stats/Stats-HERO.md#stock_movement_speed) |
| `skills[26].stats[0].op` | `INCREASED` |
| `skills[26].stats[0].value` | 5; 15 |
| `skills[26].stats[1].stat` | [Уклонение](Stats/Stats-HERO.md#stock_evasion) |
| `skills[26].stats[1].op` | `INCREASED` |
| `skills[26].stats[1].value` | 10; 30 |
| `skills[27].code` | [Порыв ветра](../Skills/Skills-RANGER.md#gust) |
| `skills[27].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[27].type` | `TRIGGER` |
| `skills[27].unlock` | 32 |
| `skills[27].icon` | `air` |
| `skills[27].trigger.on` | `EVADE` |
| `skills[27].trigger.buff.duration` | 3 |
| `skills[27].trigger.buff.stats[0].stat` | [Скорость атаки](Stats/Stats-HERO.md#stock_attack_speed) |
| `skills[27].trigger.buff.stats[0].op` | `INCREASED` |
| `skills[27].trigger.buff.stats[0].value` | 10; 25 |
| `skills[28].code` | [Ядовитые наконечники](../Skills/Skills-RANGER.md#poisoned_tips) |
| `skills[28].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[28].type` | `BONUS` |
| `skills[28].unlock` | 42 |
| `skills[28].icon` | `poison` |
| `skills[28].stats[0].stat` | [Шанс отравления](Stats/Stats-HERO.md#stock_poison_chance) |
| `skills[28].stats[0].value` | 10; 30 |
| `skills[28].stats[1].stat` | [Урон от яда](Stats/Stats-HERO.md#stock_poison_damage) |
| `skills[28].stats[1].value` | 10; 30 |
| `skills[29].code` | [Ледяная вспышка](../Skills/Skills-RANGER.md#frost_burst) |
| `skills[29].heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| `skills[29].type` | `TRIGGER` |
| `skills[29].unlock` | 52 |
| `skills[29].icon` | `cold` |
| `skills[29].trigger.on` | `CRIT` |
| `skills[29].trigger.chance` | 15; 30 |
| `skills[29].trigger.hit.targets` | 0 |
| `skills[29].trigger.hit.weapon` | 60; 120 |
| `skills[29].trigger.hit.element` | `COLD` |
| `skills[29].trigger.hit.convert` | 100; 100 |
| `skills[30].code` | [Огненный шар](../Skills/Skills-WITCH.md#fireball) |
| `skills[30].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[30].type` | `SPELL` |
| `skills[30].unlock` | 1 |
| `skills[30].icon` | `fire` |
| `skills[30].mana` | 7; 24 |
| `skills[30].cooldown` | 2 |
| `skills[30].prepare` | 50; 25 |
| `skills[30].condition` | `READY` |
| `skills[30].hit.targets` | 1 |
| `skills[30].hit.spell.element` | `FIRE` |
| `skills[30].hit.spell.min` | 28; 279.6 |
| `skills[30].hit.spell.max` | 38.5; 419.1 |
| `skills[30].hit.ailments[0].ailment` | `IGNITE` |
| `skills[30].hit.ailments[0].chance` | 25; 40 |
| `skills[31].code` | [Ледяной импульс](../Skills/Skills-WITCH.md#freezing_pulse) |
| `skills[31].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[31].type` | `SPELL` |
| `skills[31].unlock` | 6 |
| `skills[31].icon` | `cold` |
| `skills[31].mana` | 8; 26 |
| `skills[31].cooldown` | 3 |
| `skills[31].prepare` | 50; 25 |
| `skills[31].condition` | `READY` |
| `skills[31].hit.targets` | 2 |
| `skills[31].hit.spell.element` | `COLD` |
| `skills[31].hit.spell.min` | 21; 247.3 |
| `skills[31].hit.spell.max` | 35; 370.6 |
| `skills[31].hit.ailments[0].ailment` | `FREEZE` |
| `skills[31].hit.ailments[0].chance` | 20; 35 |
| `skills[32].code` | [Искра](../Skills/Skills-WITCH.md#spark) |
| `skills[32].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[32].type` | `SPELL` |
| `skills[32].unlock` | 12 |
| `skills[32].icon` | `lightning` |
| `skills[32].mana` | 10; 30 |
| `skills[32].cooldown` | 3 |
| `skills[32].prepare` | 50; 25 |
| `skills[32].condition` | `ENEMIES_3` |
| `skills[32].hit.targets` | 4 |
| `skills[32].hit.spell.element` | `LIGHTNING` |
| `skills[32].hit.spell.min` | 8.8; 74.8 |
| `skills[32].hit.spell.max` | 52.5; 451.3 |
| `skills[32].hit.ailments[0].ailment` | `SHOCK` |
| `skills[32].hit.ailments[0].chance` | 25; 45 |
| `skills[33].code` | [Стихийная слабость](../Skills/Skills-WITCH.md#elemental_weakness) |
| `skills[33].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[33].type` | `CURSE` |
| `skills[33].unlock` | 20 |
| `skills[33].icon` | `resist` |
| `skills[33].mana` | 16; 38 |
| `skills[33].cooldown` | 14 |
| `skills[33].prepare` | 30; 10 |
| `skills[33].condition` | `RARE_OR_BOSS` |
| `skills[33].curse.duration` | 8 |
| `skills[33].curse.targets` | 0 |
| `skills[33].curse.stats[0].stat` | [Все сопротивления стихиям](Stats/Stats-HERO.md#stock_resist_all) |
| `skills[33].curse.stats[0].value` | -10; -30 |
| `skills[34].code` | [Иссушение](../Skills/Skills-WITCH.md#essence_drain) |
| `skills[34].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[34].type` | `SPELL` |
| `skills[34].unlock` | 30 |
| `skills[34].icon` | `chaos` |
| `skills[34].mana` | 14; 34 |
| `skills[34].cooldown` | 6 |
| `skills[34].prepare` | 50; 20 |
| `skills[34].condition` | `READY` |
| `skills[34].dot.targets` | 1 |
| `skills[34].dot.element` | `CHAOS` |
| `skills[34].dot.min` | 87.5; 483.6 |
| `skills[34].dot.max` | 133; 731 |
| `skills[34].dot.duration` | 4 |
| `skills[34].dot.ailments[0].ailment` | `POISON` |
| `skills[34].dot.ailments[0].chance` | 100; 100 |
| `skills[35].code` | [Магический барьер](../Skills/Skills-WITCH.md#arcane_ward) |
| `skills[35].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[35].type` | `GUARD` |
| `skills[35].unlock` | 40 |
| `skills[35].icon` | `shield_energy` |
| `skills[35].mana` | 18; 40 |
| `skills[35].cooldown` | 20 |
| `skills[35].prepare` | 40; 15 |
| `skills[35].condition` | `SHIELD_BROKEN` |
| `skills[35].buff.duration` | 6 |
| `skills[35].buff.stats[0].stat` | [Восполнение щита](Stats/Stats-HERO.md#stock_shield_recharge) |
| `skills[35].buff.stats[0].value` | 20; 50 |
| `skills[35].shield` | 15; 40 |
| `skills[36].code` | [Вытягивание силы](../Skills/Skills-WITCH.md#power_siphon) |
| `skills[36].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[36].type` | `SPELL` |
| `skills[36].unlock` | 50 |
| `skills[36].icon` | `chaos` |
| `skills[36].mana` | 10; 28 |
| `skills[36].cooldown` | 3 |
| `skills[36].prepare` | 50; 25 |
| `skills[36].condition` | `READY` |
| `skills[36].hit.targets` | 1 |
| `skills[36].hit.spell.element` | `CHAOS` |
| `skills[36].hit.spell.min` | 30; 300 |
| `skills[36].hit.spell.max` | 45; 450 |
| `skills[36].charges.kind` | `POWER` |
| `skills[36].charges.gain` | 1; 1 |
| `skills[37].code` | [Дисциплина](../Skills/Skills-WITCH.md#discipline) |
| `skills[37].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[37].type` | `AURA` |
| `skills[37].unlock` | 1 |
| `skills[37].icon` | `shield_energy` |
| `skills[37].reserve` | 25 |
| `skills[37].stats[0].stat` | [Энергетический щит](Stats/Stats-HERO.md#stock_energy_shield) |
| `skills[37].stats[0].op` | `INCREASED` |
| `skills[37].stats[0].value` | 10; 40 |
| `skills[38].code` | [Мастер стихий](../Skills/Skills-WITCH.md#elemental_mastery) |
| `skills[38].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[38].type` | `BONUS` |
| `skills[38].unlock` | 5 |
| `skills[38].icon` | `magical` |
| `skills[38].stats[0].stat` | [Урон огнём](Stats/Stats-HERO.md#stock_attack_fire) |
| `skills[38].stats[0].op` | `INCREASED` |
| `skills[38].stats[0].value` | 10; 35 |
| `skills[38].stats[1].stat` | [Урон холодом](Stats/Stats-HERO.md#stock_attack_cold) |
| `skills[38].stats[1].op` | `INCREASED` |
| `skills[38].stats[1].value` | 10; 35 |
| `skills[38].stats[2].stat` | [Урон молнией](Stats/Stats-HERO.md#stock_attack_lightning) |
| `skills[38].stats[2].op` | `INCREASED` |
| `skills[38].stats[2].value` | 10; 35 |
| `skills[39].code` | [Жатва маны](../Skills/Skills-WITCH.md#mana_harvest) |
| `skills[39].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[39].type` | `TRIGGER` |
| `skills[39].unlock` | 10 |
| `skills[39].icon` | `mana` |
| `skills[39].trigger.on` | `SPELL_KILL` |
| `skills[39].trigger.heal.mana` | 3; 8 |
| `skills[40].code` | [Ясность](../Skills/Skills-WITCH.md#clarity) |
| `skills[40].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[40].type` | `AURA` |
| `skills[40].unlock` | 16 |
| `skills[40].icon` | `mana` |
| `skills[40].reserve` | 20 |
| `skills[40].stats[0].stat` | [Восстановление маны](Stats/Stats-HERO.md#stock_mana_regen) |
| `skills[40].stats[0].op` | `INCREASED` |
| `skills[40].stats[0].value` | 30; 120 |
| `skills[41].code` | [Глубокий резерв](../Skills/Skills-WITCH.md#deep_reserve) |
| `skills[41].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[41].type` | `BONUS` |
| `skills[41].unlock` | 24 |
| `skills[41].icon` | `intellect` |
| `skills[41].stats[0].stat` | [Мана](Stats/Stats-HERO.md#stock_mana) |
| `skills[41].stats[0].op` | `INCREASED` |
| `skills[41].stats[0].value` | 8; 25 |
| `skills[42].code` | [Раскол стихий](../Skills/Skills-WITCH.md#elemental_surge) |
| `skills[42].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[42].type` | `TRIGGER` |
| `skills[42].unlock` | 32 |
| `skills[42].icon` | `orb_spark` |
| `skills[42].trigger.on` | `SPELL_CRIT` |
| `skills[42].trigger.chance` | 20; 40 |
| `skills[42].trigger.ailment` | `ELEMENT` |
| `skills[43].code` | [Беглые чары](../Skills/Skills-WITCH.md#swift_casting) |
| `skills[43].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[43].type` | `BONUS` |
| `skills[43].unlock` | 42 |
| `skills[43].icon` | `speed` |
| `skills[43].stats[0].stat` | [Скорость применения](Stats/Stats-HERO.md#stock_cast_speed) |
| `skills[43].stats[0].op` | `INCREASED` |
| `skills[43].stats[0].value` | 6; 18 |
| `skills[44].code` | [Последний оплот](../Skills/Skills-WITCH.md#last_ward) |
| `skills[44].heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| `skills[44].type` | `TRIGGER` |
| `skills[44].unlock` | 52 |
| `skills[44].icon` | `shield_energy` |
| `skills[44].trigger.on` | `SHIELD_BROKEN` |
| `skills[44].trigger.cooldown` | 30 |
| `skills[44].trigger.shield` | 20; 40 |
| `skills[45].code` | [Двойной удар](../Skills/Skills-DUELIST.md#double_strike) |
| `skills[45].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[45].type` | `ATTACK` |
| `skills[45].unlock` | 1 |
| `skills[45].icon` | `doublesword` |
| `skills[45].mana` | 6; 20 |
| `skills[45].cooldown` | 2.5 |
| `skills[45].prepare` | 50; 25 |
| `skills[45].condition` | `READY` |
| `skills[45].hit.targets` | 1 |
| `skills[45].hit.hits` | 2 |
| `skills[45].hit.weapon` | 73.5; 129 |
| `skills[46].code` | [Молниеносный выпад](../Skills/Skills-DUELIST.md#lunge) |
| `skills[46].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[46].type` | `ATTACK` |
| `skills[46].unlock` | 6 |
| `skills[46].icon` | `longsword` |
| `skills[46].mana` | 9; 26 |
| `skills[46].cooldown` | 4 |
| `skills[46].prepare` | 50; 25 |
| `skills[46].condition` | `READY` |
| `skills[46].hit.targets` | 1 |
| `skills[46].hit.weapon` | 208; 355 |
| `skills[46].hit.stats[0].stat` | [Шанс критического удара](Stats/Stats-HERO.md#stock_critical_chance) |
| `skills[46].hit.stats[0].value` | 10; 25 |
| `skills[47].code` | [Вихрь стали](../Skills/Skills-DUELIST.md#steel_whirl) |
| `skills[47].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[47].type` | `ATTACK` |
| `skills[47].unlock` | 12 |
| `skills[47].icon` | `dual` |
| `skills[47].mana` | 14; 36 |
| `skills[47].cooldown` | 8 |
| `skills[47].prepare` | 60; 30 |
| `skills[47].condition` | `ENEMIES_3` |
| `skills[47].hit.targets` | 0 |
| `skills[47].hit.hits` | 3 |
| `skills[47].hit.weapon` | 74; 122 |
| `skills[48].code` | [Вызов](../Skills/Skills-DUELIST.md#challenge) |
| `skills[48].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[48].type` | `WARCRY` |
| `skills[48].unlock` | 20 |
| `skills[48].icon` | `combat` |
| `skills[48].mana` | 12; 30 |
| `skills[48].cooldown` | 12 |
| `skills[48].condition` | `FIGHT_START` |
| `skills[48].buff.duration` | 6 |
| `skills[48].buff.stats[0].stat` | [Урон](Stats/Stats-HERO.md#stock_damage) |
| `skills[48].buff.stats[0].op` | `INCREASED` |
| `skills[48].buff.stats[0].value` | 15; 40 |
| `skills[49].code` | [Бессилие](../Skills/Skills-DUELIST.md#enfeeble) |
| `skills[49].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[49].type` | `CURSE` |
| `skills[49].unlock` | 30 |
| `skills[49].icon` | `dark` |
| `skills[49].mana` | 16; 36 |
| `skills[49].cooldown` | 15 |
| `skills[49].prepare` | 30; 10 |
| `skills[49].condition` | `RARE_OR_BOSS` |
| `skills[49].curse.duration` | 8 |
| `skills[49].curse.targets` | 0 |
| `skills[49].curse.stats[0].stat` | [Урон](Stats/Stats-HERO.md#stock_damage) |
| `skills[49].curse.stats[0].op` | `MORE` |
| `skills[49].curse.stats[0].value` | -15; -35 |
| `skills[50].code` | [Рипост](../Skills/Skills-DUELIST.md#riposte) |
| `skills[50].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[50].type` | `GUARD` |
| `skills[50].unlock` | 40 |
| `skills[50].icon` | `block` |
| `skills[50].mana` | 14; 32 |
| `skills[50].cooldown` | 18 |
| `skills[50].prepare` | 40; 15 |
| `skills[50].condition` | `LIFE_50` |
| `skills[50].buff.duration` | 6 |
| `skills[50].buff.stats[0].stat` | [Шанс блока](Stats/Stats-HERO.md#stock_block_chance) |
| `skills[50].buff.stats[0].value` | 15; 35 |
| `skills[50].buff.counter` | 100; 200 |
| `skills[51].code` | [Заряженный рывок](../Skills/Skills-DUELIST.md#charged_dash) |
| `skills[51].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[51].type` | `ATTACK` |
| `skills[51].unlock` | 50 |
| `skills[51].icon` | `dual` |
| `skills[51].mana` | 14; 34 |
| `skills[51].cooldown` | 6 |
| `skills[51].prepare` | 40; 20 |
| `skills[51].condition` | `READY` |
| `skills[51].hit.targets` | 2 |
| `skills[51].hit.weapon` | 70; 120 |
| `skills[51].charges.kind` | `FRENZY` |
| `skills[51].charges.consume` | Да |
| `skills[51].charges.perCharge` | 30; 60 |
| `skills[52].code` | [Точность](../Skills/Skills-DUELIST.md#precision) |
| `skills[52].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[52].type` | `AURA` |
| `skills[52].unlock` | 1 |
| `skills[52].icon` | `critical` |
| `skills[52].reserve` | 25 |
| `skills[52].stats[0].stat` | [Шанс критического удара](Stats/Stats-HERO.md#stock_critical_chance) |
| `skills[52].stats[0].op` | `INCREASED` |
| `skills[52].stats[0].value` | 15; 45 |
| `skills[52].stats[1].stat` | [Множитель критического удара](Stats/Stats-HERO.md#stock_critical_multiplier) |
| `skills[52].stats[1].value` | 10; 30 |
| `skills[53].code` | [Мастер клинка](../Skills/Skills-DUELIST.md#blade_master) |
| `skills[53].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[53].type` | `BONUS` |
| `skills[53].unlock` | 5 |
| `skills[53].icon` | [sword](Tables/Tables-MODIFIER.md#sword) |
| `skills[53].stats[0].stat` | [Физический урон](Stats/Stats-HERO.md#stock_attack_physical) |
| `skills[53].stats[0].op` | `INCREASED` |
| `skills[53].stats[0].value` | 10; 35 |
| `skills[54].code` | [Натиск](../Skills/Skills-DUELIST.md#onslaught) |
| `skills[54].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[54].type` | `TRIGGER` |
| `skills[54].unlock` | 10 |
| `skills[54].icon` | `speed` |
| `skills[54].trigger.on` | `KILL` |
| `skills[54].trigger.buff.duration` | 4 |
| `skills[54].trigger.buff.stats[0].stat` | [Скорость атаки](Stats/Stats-HERO.md#stock_attack_speed) |
| `skills[54].trigger.buff.stats[0].op` | `INCREASED` |
| `skills[54].trigger.buff.stats[0].value` | 10; 25 |
| `skills[55].code` | [Жажда крови](../Skills/Skills-DUELIST.md#bloodlust) |
| `skills[55].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[55].type` | `AURA` |
| `skills[55].unlock` | 16 |
| `skills[55].icon` | `bleeding` |
| `skills[55].reserve` | 25 |
| `skills[55].stats[0].stat` | [Шанс кровотечения](Stats/Stats-HERO.md#stock_bleed_chance) |
| `skills[55].stats[0].value` | 15; 40 |
| `skills[55].stats[1].stat` | [Урон от кровотечения](Stats/Stats-HERO.md#stock_bleed_damage) |
| `skills[55].stats[1].value` | 10; 30 |
| `skills[56].code` | [Фехтовальщик](../Skills/Skills-DUELIST.md#swordsman) |
| `skills[56].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[56].type` | `BONUS` |
| `skills[56].unlock` | 24 |
| `skills[56].icon` | `block` |
| `skills[56].stats[0].stat` | [Шанс блока](Stats/Stats-HERO.md#stock_block_chance) |
| `skills[56].stats[0].value` | 3; 10 |
| `skills[57].code` | [Второе дыхание](../Skills/Skills-DUELIST.md#second_wind) |
| `skills[57].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[57].type` | `TRIGGER` |
| `skills[57].unlock` | 32 |
| `skills[57].icon` | `regen` |
| `skills[57].trigger.on` | `BLOCK` |
| `skills[57].trigger.heal.life` | 1; 4 |
| `skills[58].code` | [Беспощадность](../Skills/Skills-DUELIST.md#ruthless) |
| `skills[58].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[58].type` | `BONUS` |
| `skills[58].unlock` | 42 |
| `skills[58].icon` | `critical` |
| `skills[58].stats[0].stat` | [Множитель критического удара](Stats/Stats-HERO.md#stock_critical_multiplier) |
| `skills[58].stats[0].value` | 15; 45 |
| `skills[59].code` | [Смертельный удар](../Skills/Skills-DUELIST.md#coup_de_grace) |
| `skills[59].heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| `skills[59].type` | `TRIGGER` |
| `skills[59].unlock` | 52 |
| `skills[59].icon` | `bleeding` |
| `skills[59].stats[0].stat` | [Урон от кровотечения](Stats/Stats-HERO.md#stock_bleed_damage) |
| `skills[59].stats[0].value` | 10; 30 |
| `skills[59].trigger.on` | `CRIT` |
| `skills[59].trigger.ailment` | `BLEED` |
| `skills[60].code` | [Кара](../Skills/Skills-TEMPLAR.md#smite) |
| `skills[60].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[60].type` | `ATTACK` |
| `skills[60].unlock` | 1 |
| `skills[60].icon` | `lightning` |
| `skills[60].mana` | 7; 22 |
| `skills[60].cooldown` | 3 |
| `skills[60].prepare` | 50; 25 |
| `skills[60].condition` | `READY` |
| `skills[60].hit.targets` | 1 |
| `skills[60].hit.weapon` | 158; 271 |
| `skills[60].hit.element` | `LIGHTNING` |
| `skills[60].hit.convert` | 40; 40 |
| `skills[60].hit.ailments[0].ailment` | `SHOCK` |
| `skills[60].hit.ailments[0].chance` | 20; 40 |
| `skills[61].code` | [Очищающее пламя](../Skills/Skills-TEMPLAR.md#purifying_flame) |
| `skills[61].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[61].type` | `SPELL` |
| `skills[61].unlock` | 6 |
| `skills[61].icon` | `fire` |
| `skills[61].mana` | 9; 28 |
| `skills[61].cooldown` | 3 |
| `skills[61].prepare` | 50; 25 |
| `skills[61].condition` | `ENEMIES_3` |
| `skills[61].hit.targets` | 0 |
| `skills[61].hit.spell.element` | `FIRE` |
| `skills[61].hit.spell.min` | 15.8; 163.2 |
| `skills[61].hit.spell.max` | 24.5; 239.7 |
| `skills[62].code` | [Грозовой зов](../Skills/Skills-TEMPLAR.md#storm_call) |
| `skills[62].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[62].type` | `SPELL` |
| `skills[62].unlock` | 12 |
| `skills[62].icon` | `lightning` |
| `skills[62].mana` | 12; 32 |
| `skills[62].cooldown` | 5 |
| `skills[62].prepare` | 50; 20 |
| `skills[62].condition` | `READY` |
| `skills[62].hit.targets` | 1 |
| `skills[62].hit.spell.element` | `LIGHTNING` |
| `skills[62].hit.spell.min` | 37.8; 310.6 |
| `skills[62].hit.spell.max` | 107.1; 933.3 |
| `skills[62].hit.ailments[0].ailment` | `SHOCK` |
| `skills[62].hit.ailments[0].chance` | 100; 100 |
| `skills[63].code` | [Молитва](../Skills/Skills-TEMPLAR.md#prayer) |
| `skills[63].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[63].type` | `HEAL` |
| `skills[63].unlock` | 20 |
| `skills[63].icon` | `light` |
| `skills[63].mana` | 16; 40 |
| `skills[63].cooldown` | 18 |
| `skills[63].prepare` | 40; 15 |
| `skills[63].condition` | `LIFE_50` |
| `skills[63].heal.life` | 15; 40 |
| `skills[63].heal.cleanse` | Да |
| `skills[64].code` | [Проводимость](../Skills/Skills-TEMPLAR.md#conductivity) |
| `skills[64].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[64].type` | `CURSE` |
| `skills[64].unlock` | 30 |
| `skills[64].icon` | `lightning` |
| `skills[64].mana` | 16; 38 |
| `skills[64].cooldown` | 14 |
| `skills[64].prepare` | 30; 10 |
| `skills[64].condition` | `RARE_OR_BOSS` |
| `skills[64].curse.duration` | 8 |
| `skills[64].curse.targets` | 0 |
| `skills[64].curse.stats[0].stat` | [Сопротивление молнии](Stats/Stats-HERO.md#stock_resist_lightning) |
| `skills[64].curse.stats[0].value` | -15; -35 |
| `skills[64].curse.stats[1].stat` | [Сила шока по цели](Stats/Stats-HERO.md#stock_shock_taken) |
| `skills[64].curse.stats[1].value` | 10; 25 |
| `skills[65].code` | [Божественный щит](../Skills/Skills-TEMPLAR.md#divine_shield) |
| `skills[65].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[65].type` | `GUARD` |
| `skills[65].unlock` | 40 |
| `skills[65].icon` | [shield](Tables/Tables-MODIFIER.md#shield) |
| `skills[65].mana` | 18; 42 |
| `skills[65].cooldown` | 24 |
| `skills[65].prepare` | 40; 15 |
| `skills[65].condition` | `LIFE_35` |
| `skills[65].barrier.life` | 15; 35 |
| `skills[65].barrier.duration` | 6 |
| `skills[66].code` | [Земной выброс](../Skills/Skills-TEMPLAR.md#earthen_release) |
| `skills[66].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[66].type` | `ATTACK` |
| `skills[66].unlock` | 50 |
| `skills[66].icon` | `earth` |
| `skills[66].mana` | 16; 38 |
| `skills[66].cooldown` | 6 |
| `skills[66].prepare` | 40; 20 |
| `skills[66].condition` | `READY` |
| `skills[66].hit.targets` | 0 |
| `skills[66].hit.weapon` | 60; 110 |
| `skills[66].hit.stun` | 10; 30 |
| `skills[66].charges.kind` | `ENDURANCE` |
| `skills[66].charges.consume` | Да |
| `skills[66].charges.perCharge` | 40; 80 |
| `skills[67].code` | [Чистота стихий](../Skills/Skills-TEMPLAR.md#purity_of_elements) |
| `skills[67].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[67].type` | `AURA` |
| `skills[67].unlock` | 1 |
| `skills[67].icon` | `resist` |
| `skills[67].reserve` | 25 |
| `skills[67].stats[0].stat` | [Все сопротивления стихиям](Stats/Stats-HERO.md#stock_resist_all) |
| `skills[67].stats[0].value` | 10; 25 |
| `skills[68].code` | [Вера](../Skills/Skills-TEMPLAR.md#faith) |
| `skills[68].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[68].type` | `BONUS` |
| `skills[68].unlock` | 5 |
| `skills[68].icon` | `light` |
| `skills[68].stats[0].stat` | [Здоровье](Stats/Stats-HERO.md#stock_health) |
| `skills[68].stats[0].op` | `INCREASED` |
| `skills[68].stats[0].value` | 3; 10 |
| `skills[68].stats[1].stat` | [Мана](Stats/Stats-HERO.md#stock_mana) |
| `skills[68].stats[1].op` | `INCREASED` |
| `skills[68].stats[1].value` | 5; 15 |
| `skills[69].code` | [Благословение](../Skills/Skills-TEMPLAR.md#blessing) |
| `skills[69].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[69].type` | `TRIGGER` |
| `skills[69].unlock` | 10 |
| `skills[69].icon` | [armour](Tables/Tables-MODIFIER.md#armour) |
| `skills[69].trigger.on` | `HEALED` |
| `skills[69].trigger.buff.duration` | 4 |
| `skills[69].trigger.buff.stats[0].stat` | [Броня](Stats/Stats-HERO.md#stock_armor) |
| `skills[69].trigger.buff.stats[0].op` | `INCREASED` |
| `skills[69].trigger.buff.stats[0].value` | 20; 50 |
| `skills[70].code` | [Гнев небес](../Skills/Skills-TEMPLAR.md#wrath) |
| `skills[70].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[70].type` | `AURA` |
| `skills[70].unlock` | 16 |
| `skills[70].icon` | `lightning` |
| `skills[70].reserve` | 30 |
| `skills[70].stats[0].stat` | [Урон молнией](Stats/Stats-HERO.md#stock_attack_lightning) |
| `skills[70].stats[0].value` | 5; 132 |
| `skills[71].code` | [Святое оружие](../Skills/Skills-TEMPLAR.md#holy_arms) |
| `skills[71].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[71].type` | `BONUS` |
| `skills[71].unlock` | 24 |
| `skills[71].icon` | `magical` |
| `skills[71].stats[0].stat` | [Урон огнём](Stats/Stats-HERO.md#stock_attack_fire) |
| `skills[71].stats[0].op` | `INCREASED` |
| `skills[71].stats[0].value` | 10; 30 |
| `skills[71].stats[1].stat` | [Урон холодом](Stats/Stats-HERO.md#stock_attack_cold) |
| `skills[71].stats[1].op` | `INCREASED` |
| `skills[71].stats[1].value` | 10; 30 |
| `skills[71].stats[2].stat` | [Урон молнией](Stats/Stats-HERO.md#stock_attack_lightning) |
| `skills[71].stats[2].op` | `INCREASED` |
| `skills[71].stats[2].value` | 10; 30 |
| `skills[72].code` | [Праведность](../Skills/Skills-TEMPLAR.md#righteousness) |
| `skills[72].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[72].type` | `TRIGGER` |
| `skills[72].unlock` | 32 |
| `skills[72].icon` | `health` |
| `skills[72].trigger.on` | `KILL` |
| `skills[72].trigger.heal.life` | 1; 4 |
| `skills[72].trigger.heal.mana` | 1; 3 |
| `skills[73].code` | [Стойкость](../Skills/Skills-TEMPLAR.md#fortitude) |
| `skills[73].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[73].type` | `BONUS` |
| `skills[73].unlock` | 42 |
| `skills[73].icon` | `constitution` |
| `skills[73].stats[0].stat` | [Броня](Stats/Stats-HERO.md#stock_armor) |
| `skills[73].stats[0].op` | `INCREASED` |
| `skills[73].stats[0].value` | 10; 30 |
| `skills[73].stats[1].stat` | [Порог оглушения](Stats/Stats-HERO.md#stock_stun_threshold) |
| `skills[73].stats[1].op` | `INCREASED` |
| `skills[73].stats[1].value` | 10; 30 |
| `skills[74].code` | [Мученик](../Skills/Skills-TEMPLAR.md#martyr) |
| `skills[74].heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| `skills[74].type` | `TRIGGER` |
| `skills[74].unlock` | 52 |
| `skills[74].icon` | `regen` |
| `skills[74].trigger.on` | `LOW_LIFE` |
| `skills[74].trigger.cooldown` | 25 |
| `skills[74].trigger.buff.duration` | 4 |
| `skills[74].trigger.buff.stats[0].stat` | [Регенерация здоровья, %](Stats/Stats-HERO.md#stock_life_regen_percent) |
| `skills[74].trigger.buff.stats[0].value` | 2; 5 |
| `skills[75].code` | [Змеиный укус](../Skills/Skills-SHADOW.md#viper_strike) |
| `skills[75].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[75].type` | `ATTACK` |
| `skills[75].unlock` | 1 |
| `skills[75].icon` | `poison` |
| `skills[75].mana` | 6; 20 |
| `skills[75].cooldown` | 2.5 |
| `skills[75].prepare` | 50; 25 |
| `skills[75].condition` | `READY` |
| `skills[75].hit.targets` | 1 |
| `skills[75].hit.weapon` | 152; 278 |
| `skills[75].hit.ailments[0].ailment` | `POISON` |
| `skills[75].hit.ailments[0].chance` | 100; 100 |
| `skills[76].code` | [Шквал клинков](../Skills/Skills-SHADOW.md#blade_flurry) |
| `skills[76].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[76].type` | `ATTACK` |
| `skills[76].unlock` | 6 |
| `skills[76].icon` | [blade](Tables/Tables-MODIFIER.md#blade) |
| `skills[76].mana` | 10; 28 |
| `skills[76].cooldown` | 5 |
| `skills[76].prepare` | 50; 20 |
| `skills[76].condition` | `ENEMIES_3` |
| `skills[76].hit.targets` | 0 |
| `skills[76].hit.hits` | 2 |
| `skills[76].hit.weapon` | 76; 139 |
| `skills[77].code` | [Покров теней](../Skills/Skills-SHADOW.md#shadow_cloak) |
| `skills[77].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[77].type` | `GUARD` |
| `skills[77].unlock` | 12 |
| `skills[77].icon` | `invisible` |
| `skills[77].mana` | 12; 30 |
| `skills[77].cooldown` | 16 |
| `skills[77].prepare` | 40; 15 |
| `skills[77].condition` | `LIFE_50` |
| `skills[77].buff.duration` | 4 |
| `skills[77].buff.stats[0].stat` | [Уклонение](Stats/Stats-HERO.md#stock_evasion) |
| `skills[77].buff.stats[0].op` | `INCREASED` |
| `skills[77].buff.stats[0].value` | 30; 80 |
| `skills[77].buff.nextCrit` | Да |
| `skills[78].code` | [Ядовитое облако](../Skills/Skills-SHADOW.md#toxic_cloud) |
| `skills[78].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[78].type` | `SPELL` |
| `skills[78].unlock` | 20 |
| `skills[78].icon` | `poison` |
| `skills[78].mana` | 14; 36 |
| `skills[78].cooldown` | 7 |
| `skills[78].prepare` | 50; 20 |
| `skills[78].condition` | `ENEMIES_3` |
| `skills[78].dot.targets` | 0 |
| `skills[78].dot.element` | `CHAOS` |
| `skills[78].dot.min` | 66.5; 376.6 |
| `skills[78].dot.max` | 98; 559.3 |
| `skills[78].dot.duration` | 4 |
| `skills[78].dot.ailments[0].ailment` | `POISON` |
| `skills[78].dot.ailments[0].chance` | 100; 100 |
| `skills[79].code` | [Отчаяние](../Skills/Skills-SHADOW.md#despair) |
| `skills[79].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[79].type` | `CURSE` |
| `skills[79].unlock` | 30 |
| `skills[79].icon` | `dark` |
| `skills[79].mana` | 16; 38 |
| `skills[79].cooldown` | 14 |
| `skills[79].prepare` | 30; 10 |
| `skills[79].condition` | `RARE_OR_BOSS` |
| `skills[79].curse.duration` | 8 |
| `skills[79].curse.targets` | 0 |
| `skills[79].curse.stats[0].stat` | [Сопротивление хаосу](Stats/Stats-HERO.md#stock_resist_chaos) |
| `skills[79].curse.stats[0].value` | -15; -35 |
| `skills[79].curse.stats[1].stat` | [Получаемый урон со временем](Stats/Stats-HERO.md#stock_dot_taken) |
| `skills[79].curse.stats[1].value` | 10; 30 |
| `skills[80].code` | [Удар в спину](../Skills/Skills-SHADOW.md#backstab) |
| `skills[80].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[80].type` | `ATTACK` |
| `skills[80].unlock` | 40 |
| `skills[80].icon` | [blade](Tables/Tables-MODIFIER.md#blade) |
| `skills[80].mana` | 16; 38 |
| `skills[80].cooldown` | 10 |
| `skills[80].prepare` | 60; 30 |
| `skills[80].condition` | `READY` |
| `skills[80].hit.targets` | 1 |
| `skills[80].hit.weapon` | 342; 568.8 |
| `skills[80].hit.finisher` | 300; 500 |
| `skills[81].code` | [Разрядка](../Skills/Skills-SHADOW.md#discharge) |
| `skills[81].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[81].type` | `SPELL` |
| `skills[81].unlock` | 50 |
| `skills[81].icon` | `lightning` |
| `skills[81].mana` | 18; 40 |
| `skills[81].cooldown` | 8 |
| `skills[81].prepare` | 40; 20 |
| `skills[81].condition` | `READY` |
| `skills[81].hit.targets` | 0 |
| `skills[81].hit.spell.element` | `LIGHTNING` |
| `skills[81].hit.spell.min` | 10; 90 |
| `skills[81].hit.spell.max` | 20; 160 |
| `skills[81].charges.kind` | `ALL` |
| `skills[81].charges.consume` | Да |
| `skills[81].charges.perCharge` | 60; 120 |
| `skills[82].code` | [Злоба](../Skills/Skills-SHADOW.md#malevolence) |
| `skills[82].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[82].type` | `AURA` |
| `skills[82].unlock` | 1 |
| `skills[82].icon` | `dark` |
| `skills[82].reserve` | 30 |
| `skills[82].stats[0].stat` | [Урон от горения](Stats/Stats-HERO.md#stock_burning_damage) |
| `skills[82].stats[0].value` | 10; 30 |
| `skills[82].stats[1].stat` | [Урон от яда](Stats/Stats-HERO.md#stock_poison_damage) |
| `skills[82].stats[1].value` | 10; 30 |
| `skills[82].stats[2].stat` | [Урон от кровотечения](Stats/Stats-HERO.md#stock_bleed_damage) |
| `skills[82].stats[2].value` | 10; 30 |
| `skills[82].stats[3].stat` | [Длительность состояний](Stats/Stats-HERO.md#stock_ailment_duration) |
| `skills[82].stats[3].value` | 10; 25 |
| `skills[83].code` | [Убийца](../Skills/Skills-SHADOW.md#assassin) |
| `skills[83].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[83].type` | `BONUS` |
| `skills[83].unlock` | 5 |
| `skills[83].icon` | `critical` |
| `skills[83].stats[0].stat` | [Шанс критического удара](Stats/Stats-HERO.md#stock_critical_chance) |
| `skills[83].stats[0].op` | `INCREASED` |
| `skills[83].stats[0].value` | 20; 60 |
| `skills[83].stats[1].stat` | [Множитель критического удара](Stats/Stats-HERO.md#stock_critical_multiplier) |
| `skills[83].stats[1].value` | 10; 30 |
| `skills[84].code` | [Исчезновение](../Skills/Skills-SHADOW.md#vanish) |
| `skills[84].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[84].type` | `TRIGGER` |
| `skills[84].unlock` | 10 |
| `skills[84].icon` | `invisible` |
| `skills[84].trigger.on` | `KILL` |
| `skills[84].trigger.buff.duration` | 2 |
| `skills[84].trigger.buff.stats[0].stat` | [Уклонение](Stats/Stats-HERO.md#stock_evasion) |
| `skills[84].trigger.buff.stats[0].op` | `INCREASED` |
| `skills[84].trigger.buff.stats[0].value` | 50; 100 |
| `skills[85].code` | [Проворство](../Skills/Skills-SHADOW.md#swiftness) |
| `skills[85].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[85].type` | `AURA` |
| `skills[85].unlock` | 16 |
| `skills[85].icon` | `speed` |
| `skills[85].reserve` | 25 |
| `skills[85].stats[0].stat` | [Скорость атаки](Stats/Stats-HERO.md#stock_attack_speed) |
| `skills[85].stats[0].op` | `INCREASED` |
| `skills[85].stats[0].value` | 6; 16 |
| `skills[85].stats[1].stat` | [Уклонение](Stats/Stats-HERO.md#stock_evasion) |
| `skills[85].stats[1].op` | `INCREASED` |
| `skills[85].stats[1].value` | 5; 15 |
| `skills[86].code` | [Ядовед](../Skills/Skills-SHADOW.md#toxicologist) |
| `skills[86].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[86].type` | `BONUS` |
| `skills[86].unlock` | 24 |
| `skills[86].icon` | `poison` |
| `skills[86].stats[0].stat` | [Урон от яда](Stats/Stats-HERO.md#stock_poison_damage) |
| `skills[86].stats[0].value` | 15; 45 |
| `skills[87].code` | [Смертельный яд](../Skills/Skills-SHADOW.md#lethal_venom) |
| `skills[87].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[87].type` | `TRIGGER` |
| `skills[87].unlock` | 32 |
| `skills[87].icon` | `poison` |
| `skills[87].trigger.on` | `CRIT` |
| `skills[87].trigger.ailment` | `POISON` |
| `skills[87].trigger.twice` | Да |
| `skills[88].code` | [Незаметность](../Skills/Skills-SHADOW.md#stealth) |
| `skills[88].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[88].type` | `BONUS` |
| `skills[88].unlock` | 42 |
| `skills[88].icon` | `evasion` |
| `skills[88].stats[0].stat` | [Уклонение](Stats/Stats-HERO.md#stock_evasion) |
| `skills[88].stats[0].op` | `INCREASED` |
| `skills[88].stats[0].value` | 15; 45 |
| `skills[89].code` | [Кража сил](../Skills/Skills-SHADOW.md#siphon) |
| `skills[89].heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| `skills[89].type` | `TRIGGER` |
| `skills[89].unlock` | 52 |
| `skills[89].icon` | `mana` |
| `skills[89].trigger.on` | `EVADE` |
| `skills[89].trigger.heal.mana` | 1; 4 |
| `skills[90].code` | [Стихийный удар](../Skills/Skills-SCION.md#elemental_hit) |
| `skills[90].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[90].type` | `ATTACK` |
| `skills[90].unlock` | 1 |
| `skills[90].icon` | `orb_facet` |
| `skills[90].mana` | 7; 22 |
| `skills[90].cooldown` | 3 |
| `skills[90].prepare` | 50; 25 |
| `skills[90].condition` | `READY` |
| `skills[90].hit.targets` | 1 |
| `skills[90].hit.weapon` | 127; 212 |
| `skills[90].hit.element` | `RANDOM` |
| `skills[90].hit.convert` | 100; 100 |
| `skills[90].hit.ailments[0].ailment` | `ELEMENT` |
| `skills[90].hit.ailments[0].chance` | 25; 40 |
| `skills[91].code` | [Спектральный бросок](../Skills/Skills-SCION.md#spectral_throw) |
| `skills[91].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[91].type` | `ATTACK` |
| `skills[91].unlock` | 6 |
| `skills[91].icon` | `throwing` |
| `skills[91].mana` | 9; 26 |
| `skills[91].cooldown` | 3.5 |
| `skills[91].prepare` | 50; 25 |
| `skills[91].condition` | `READY` |
| `skills[91].hit.targets` | 2 |
| `skills[91].hit.weapon` | 93; 161 |
| `skills[92].code` | [Дуга](../Skills/Skills-SCION.md#arc) |
| `skills[92].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[92].type` | `SPELL` |
| `skills[92].unlock` | 12 |
| `skills[92].icon` | `lightning` |
| `skills[92].mana` | 12; 32 |
| `skills[92].cooldown` | 4 |
| `skills[92].prepare` | 50; 25 |
| `skills[92].condition` | `ENEMIES_3` |
| `skills[92].hit.targets` | 4 |
| `skills[92].hit.spell.element` | `LIGHTNING` |
| `skills[92].hit.spell.min` | 16.1; 145.7 |
| `skills[92].hit.spell.max` | 47.3; 437.2 |
| `skills[93].code` | [Клич единства](../Skills/Skills-SCION.md#rallying_cry) |
| `skills[93].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[93].type` | `WARCRY` |
| `skills[93].unlock` | 20 |
| `skills[93].icon` | `combat` |
| `skills[93].mana` | 14; 34 |
| `skills[93].cooldown` | 13 |
| `skills[93].condition` | `FIGHT_START` |
| `skills[93].buff.duration` | 6 |
| `skills[93].buff.stats[0].stat` | [Урон](Stats/Stats-HERO.md#stock_damage) |
| `skills[93].buff.stats[0].op` | `INCREASED` |
| `skills[93].buff.stats[0].value` | 10; 25 |
| `skills[93].buff.stats[1].stat` | [Все сопротивления стихиям](Stats/Stats-HERO.md#stock_resist_all) |
| `skills[93].buff.stats[1].value` | 10; 20 |
| `skills[94].code` | [Путы времени](../Skills/Skills-SCION.md#temporal_chains) |
| `skills[94].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[94].type` | `CURSE` |
| `skills[94].unlock` | 30 |
| `skills[94].icon` | `dark` |
| `skills[94].mana` | 16; 38 |
| `skills[94].cooldown` | 15 |
| `skills[94].prepare` | 30; 10 |
| `skills[94].condition` | `RARE_OR_BOSS` |
| `skills[94].curse.duration` | 8 |
| `skills[94].curse.targets` | 0 |
| `skills[94].curse.stats[0].stat` | [Скорость атаки](Stats/Stats-HERO.md#stock_attack_speed) |
| `skills[94].curse.stats[0].op` | `MORE` |
| `skills[94].curse.stats[0].value` | -15; -35 |
| `skills[95].code` | [Возрождение](../Skills/Skills-SCION.md#renewal) |
| `skills[95].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[95].type` | `HEAL` |
| `skills[95].unlock` | 40 |
| `skills[95].icon` | `regen` |
| `skills[95].mana` | 18; 42 |
| `skills[95].cooldown` | 20 |
| `skills[95].prepare` | 40; 15 |
| `skills[95].condition` | `LIFE_50` |
| `skills[95].heal.life` | 15; 35 |
| `skills[95].heal.mana` | 10; 20 |
| `skills[96].code` | [Гармония](../Skills/Skills-SCION.md#harmony) |
| `skills[96].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[96].type` | `AURA` |
| `skills[96].unlock` | 1 |
| `skills[96].icon` | `orb` |
| `skills[96].reserve` | 25 |
| `skills[96].stats[0].stat` | [Сила](Stats/Stats-HERO.md#stock_strength) |
| `skills[96].stats[0].value` | 10; 40 |
| `skills[96].stats[1].stat` | [Ловкость](Stats/Stats-HERO.md#stock_agility) |
| `skills[96].stats[1].value` | 10; 40 |
| `skills[96].stats[2].stat` | [Интеллект](Stats/Stats-HERO.md#stock_intellect) |
| `skills[96].stats[2].value` | 10; 40 |
| `skills[97].code` | [Наследие](../Skills/Skills-SCION.md#legacy) |
| `skills[97].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[97].type` | `BONUS` |
| `skills[97].unlock` | 5 |
| `skills[97].icon` | `resist` |
| `skills[97].stats[0].stat` | [Все сопротивления стихиям](Stats/Stats-HERO.md#stock_resist_all) |
| `skills[97].stats[0].value` | 5; 15 |
| `skills[98].code` | [Отзвук](../Skills/Skills-SCION.md#echo) |
| `skills[98].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[98].type` | `TRIGGER` |
| `skills[98].unlock` | 10 |
| `skills[98].icon` | `mirror` |
| `skills[98].trigger.on` | `SKILL_USE` |
| `skills[98].trigger.chance` | 10; 20 |
| `skills[98].trigger.refund` | Да |
| `skills[99].code` | [Резонанс](../Skills/Skills-SCION.md#resonance) |
| `skills[99].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[99].type` | `AURA` |
| `skills[99].unlock` | 16 |
| `skills[99].icon` | `magical` |
| `skills[99].reserve` | 30 |
| `skills[99].stats[0].stat` | [Урон](Stats/Stats-HERO.md#stock_damage) |
| `skills[99].stats[0].op` | `INCREASED` |
| `skills[99].stats[0].value` | 8; 25 |
| `skills[100].code` | [Универсал](../Skills/Skills-SCION.md#adept) |
| `skills[100].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[100].type` | `BONUS` |
| `skills[100].unlock` | 24 |
| `skills[100].icon` | `speed` |
| `skills[100].stats[0].stat` | [Перезарядка умений](Stats/Stats-HERO.md#stock_cooldown_recovery) |
| `skills[100].stats[0].op` | `INCREASED` |
| `skills[100].stats[0].value` | 8; 20 |
| `skills[101].code` | [Эхо фляг](../Skills/Skills-SCION.md#flask_echo) |
| `skills[101].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[101].type` | `TRIGGER` |
| `skills[101].unlock` | 32 |
| `skills[101].icon` | `potion` |
| `skills[101].trigger.on` | `KILL` |
| `skills[101].trigger.chance` | 15; 30 |
| `skills[101].trigger.flaskCharges` | 1 |
| `skills[102].code` | [Жажда силы](../Skills/Skills-SCION.md#hunger_for_power) |
| `skills[102].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[102].type` | `BONUS` |
| `skills[102].unlock` | 42 |
| `skills[102].icon` | `leech` |
| `skills[102].stats[0].stat` | [Здоровье за убийство](Stats/Stats-HERO.md#stock_health_on_kill) |
| `skills[102].stats[0].value` | 2; 8 |
| `skills[102].stats[1].stat` | [Мана за убийство](Stats/Stats-HERO.md#stock_mana_on_kill) |
| `skills[102].stats[1].value` | 1; 4 |
| `skills[103].code` | [Воля к жизни](../Skills/Skills-SCION.md#will_to_live) |
| `skills[103].heroClass` | [Скион (Scion)](../Classes.md#scion) |
| `skills[103].type` | `TRIGGER` |
| `skills[103].unlock` | 52 |
| `skills[103].icon` | `health` |
| `skills[103].trigger.on` | `LOW_LIFE` |
| `skills[103].trigger.cooldown` | 25 |
| `skills[103].trigger.buff.duration` | 4 |
| `skills[103].trigger.buff.stats[0].stat` | [Скорость восстановления](Stats/Stats-HERO.md#stock_recovery_rate) |
| `skills[103].trigger.buff.stats[0].value` | 50; 150 |
| `monsterSkills[0].code` | [Огненный шар](Monster-skills.md#mob_fireball) |
| `monsterSkills[0].icon` | `fire` |
| `monsterSkills[0].mana` | 10 |
| `monsterSkills[0].cooldown` | 5 |
| `monsterSkills[0].spell` | Да |
| `monsterSkills[0].hit.targets` | 1 |
| `monsterSkills[0].hit.weapon` | 150; 150 |
| `monsterSkills[0].hit.element` | `FIRE` |
| `monsterSkills[0].hit.convert` | 100; 100 |
| `monsterSkills[0].hit.ailments[0].ailment` | `IGNITE` |
| `monsterSkills[0].hit.ailments[0].chance` | 30; 30 |
| `monsterSkills[1].code` | [Ледяная стрела](Monster-skills.md#mob_ice_bolt) |
| `monsterSkills[1].icon` | `cold` |
| `monsterSkills[1].mana` | 10 |
| `monsterSkills[1].cooldown` | 5 |
| `monsterSkills[1].spell` | Да |
| `monsterSkills[1].hit.targets` | 1 |
| `monsterSkills[1].hit.weapon` | 140; 140 |
| `monsterSkills[1].hit.element` | `COLD` |
| `monsterSkills[1].hit.convert` | 100; 100 |
| `monsterSkills[1].hit.ailments[0].ailment` | `CHILL` |
| `monsterSkills[1].hit.ailments[0].chance` | 100; 100 |
| `monsterSkills[2].code` | [Разряд](Monster-skills.md#mob_zap) |
| `monsterSkills[2].icon` | `lightning` |
| `monsterSkills[2].mana` | 10 |
| `monsterSkills[2].cooldown` | 5 |
| `monsterSkills[2].spell` | Да |
| `monsterSkills[2].hit.targets` | 1 |
| `monsterSkills[2].hit.weapon` | 130; 130 |
| `monsterSkills[2].hit.element` | `LIGHTNING` |
| `monsterSkills[2].hit.convert` | 100; 100 |
| `monsterSkills[2].hit.ailments[0].ailment` | `SHOCK` |
| `monsterSkills[2].hit.ailments[0].chance` | 30; 30 |
| `monsterSkills[3].code` | [Ядовитый плевок](Monster-skills.md#mob_spit) |
| `monsterSkills[3].icon` | `poison` |
| `monsterSkills[3].mana` | 8 |
| `monsterSkills[3].cooldown` | 5 |
| `monsterSkills[3].spell` | Нет |
| `monsterSkills[3].hit.targets` | 1 |
| `monsterSkills[3].hit.weapon` | 120; 120 |
| `monsterSkills[3].hit.element` | `CHAOS` |
| `monsterSkills[3].hit.convert` | 100; 100 |
| `monsterSkills[3].hit.ailments[0].ailment` | `POISON` |
| `monsterSkills[3].hit.ailments[0].chance` | 50; 50 |
| `monsterSkills[4].code` | [Клич ярости](Monster-skills.md#mob_warcry) |
| `monsterSkills[4].icon` | `combat` |
| `monsterSkills[4].mana` | 12 |
| `monsterSkills[4].cooldown` | 12 |
| `monsterSkills[4].spell` | Нет |
| `monsterSkills[4].buff.duration` | 5 |
| `monsterSkills[4].buff.stats[0].stat` | [Скорость атаки](Stats/Stats-HERO.md#stock_attack_speed) |
| `monsterSkills[4].buff.stats[0].op` | `INCREASED` |
| `monsterSkills[4].buff.stats[0].value` | 25; 25 |
| `monsterSkills[5].code` | [Слабость](Monster-skills.md#mob_enfeeble) |
| `monsterSkills[5].icon` | `dark` |
| `monsterSkills[5].mana` | 12 |
| `monsterSkills[5].cooldown` | 14 |
| `monsterSkills[5].spell` | Да |
| `monsterSkills[5].curse.duration` | 6 |
| `monsterSkills[5].curse.targets` | 1 |
| `monsterSkills[5].curse.stats[0].stat` | [Урон](Stats/Stats-HERO.md#stock_damage) |
| `monsterSkills[5].curse.stats[0].op` | `MORE` |
| `monsterSkills[5].curse.stats[0].value` | -20; -20 |
| `monsterSkills[6].code` | [Уязвимость](Monster-skills.md#mob_vulnerability) |
| `monsterSkills[6].icon` | `bleeding` |
| `monsterSkills[6].mana` | 12 |
| `monsterSkills[6].cooldown` | 14 |
| `monsterSkills[6].spell` | Да |
| `monsterSkills[6].curse.duration` | 6 |
| `monsterSkills[6].curse.targets` | 1 |
| `monsterSkills[6].curse.stats[0].stat` | [Получаемый физический урон](Stats/Stats-HERO.md#stock_physical_taken) |
| `monsterSkills[6].curse.stats[0].value` | 20; 20 |
| `monsterSkills[7].code` | [Удар по земле](Monster-skills.md#mob_slam) |
| `monsterSkills[7].icon` | `earth` |
| `monsterSkills[7].mana` | 10 |
| `monsterSkills[7].cooldown` | 8 |
| `monsterSkills[7].spell` | Нет |
| `monsterSkills[7].hit.targets` | 1 |
| `monsterSkills[7].hit.weapon` | 200; 200 |
| `monsterSkills[7].hit.stun` | 100; 100 |
| `monsterSkills[8].code` | [Исцеление](Monster-skills.md#mob_heal) |
| `monsterSkills[8].icon` | `health` |
| `monsterSkills[8].mana` | 15 |
| `monsterSkills[8].cooldown` | 15 |
| `monsterSkills[8].spell` | Да |
| `monsterSkills[8].heal.life` | 15; 15 |
| `monsterSkills[9].code` | [Каменная кожа](Monster-skills.md#mob_stoneskin) |
| `monsterSkills[9].icon` | [armour](Tables/Tables-MODIFIER.md#armour) |
| `monsterSkills[9].mana` | 12 |
| `monsterSkills[9].cooldown` | 15 |
| `monsterSkills[9].spell` | Да |
| `monsterSkills[9].buff.duration` | 5 |
| `monsterSkills[9].buff.stats[0].stat` | [Получаемый урон](Stats/Stats-HERO.md#stock_damage_taken) |
| `monsterSkills[9].buff.stats[0].value` | -30; -30 |
| `monsterSkills[10].code` | [Кража маны](Monster-skills.md#mob_mana_drain) |
| `monsterSkills[10].icon` | `mana` |
| `monsterSkills[10].mana` | 5 |
| `monsterSkills[10].cooldown` | 10 |
| `monsterSkills[10].spell` | Да |
| `monsterSkills[10].manaBurn` | 10 |
| `monsterSkills[11].code` | [Кольцо хаоса](Monster-skills.md#mob_chaos_ring) |
| `monsterSkills[11].icon` | `chaos` |
| `monsterSkills[11].mana` | 14 |
| `monsterSkills[11].cooldown` | 8 |
| `monsterSkills[11].spell` | Да |
| `monsterSkills[11].hit.targets` | 1 |
| `monsterSkills[11].hit.weapon` | 150; 150 |
| `monsterSkills[11].hit.element` | `CHAOS` |
| `monsterSkills[11].hit.convert` | 100; 100 |
| `monsterSkills[12].code` | [Разрыв](Monster-skills.md#mob_rend) |
| `monsterSkills[12].icon` | `bleeding` |
| `monsterSkills[12].mana` | 8 |
| `monsterSkills[12].cooldown` | 7 |
| `monsterSkills[12].spell` | Нет |
| `monsterSkills[12].hit.targets` | 1 |
| `monsterSkills[12].hit.weapon` | 160; 160 |
| `monsterSkills[12].hit.ailments[0].ailment` | `BLEED` |
| `monsterSkills[12].hit.ailments[0].chance` | 60; 60 |
| `monsterSkills[13].code` | [Паутина](Monster-skills.md#mob_web) |
| `monsterSkills[13].icon` | `poison` |
| `monsterSkills[13].mana` | 10 |
| `monsterSkills[13].cooldown` | 14 |
| `monsterSkills[13].spell` | Да |
| `monsterSkills[13].curse.duration` | 5 |
| `monsterSkills[13].curse.targets` | 1 |
| `monsterSkills[13].curse.stats[0].stat` | [Скорость атаки](Stats/Stats-HERO.md#stock_attack_speed) |
| `monsterSkills[13].curse.stats[0].op` | `INCREASED` |
| `monsterSkills[13].curse.stats[0].value` | -20; -20 |
| `monsterSkills[14].code` | [Ледяное дыхание](Monster-skills.md#mob_frost_breath) |
| `monsterSkills[14].icon` | `cold` |
| `monsterSkills[14].mana` | 12 |
| `monsterSkills[14].cooldown` | 8 |
| `monsterSkills[14].spell` | Да |
| `monsterSkills[14].hit.targets` | 1 |
| `monsterSkills[14].hit.weapon` | 140; 140 |
| `monsterSkills[14].hit.element` | `COLD` |
| `monsterSkills[14].hit.convert` | 100; 100 |
| `monsterSkills[14].hit.ailments[0].ailment` | `CHILL` |
| `monsterSkills[14].hit.ailments[0].chance` | 100; 100 |
| `monsterSkills[14].hit.ailments[1].ailment` | `FREEZE` |
| `monsterSkills[14].hit.ailments[1].chance` | 15; 15 |
| `monsterSkills[15].code` | [Огненное дыхание](Monster-skills.md#mob_flame_breath) |
| `monsterSkills[15].icon` | `fire` |
| `monsterSkills[15].mana` | 12 |
| `monsterSkills[15].cooldown` | 8 |
| `monsterSkills[15].spell` | Да |
| `monsterSkills[15].hit.targets` | 1 |
| `monsterSkills[15].hit.weapon` | 150; 150 |
| `monsterSkills[15].hit.element` | `FIRE` |
| `monsterSkills[15].hit.convert` | 100; 100 |
| `monsterSkills[15].hit.ailments[0].ailment` | `IGNITE` |
| `monsterSkills[15].hit.ailments[0].chance` | 40; 40 |
| `monsterSkills[16].code` | [Раскат грома](Monster-skills.md#mob_thunderclap) |
| `monsterSkills[16].icon` | `lightning` |
| `monsterSkills[16].mana` | 12 |
| `monsterSkills[16].cooldown` | 9 |
| `monsterSkills[16].spell` | Нет |
| `monsterSkills[16].hit.targets` | 1 |
| `monsterSkills[16].hit.weapon` | 160; 160 |
| `monsterSkills[16].hit.element` | `LIGHTNING` |
| `monsterSkills[16].hit.convert` | 60; 60 |
| `monsterSkills[16].hit.ailments[0].ailment` | `SHOCK` |
| `monsterSkills[16].hit.ailments[0].chance` | 40; 40 |
| `monsterSkills[16].hit.stun` | 60; 60 |
| `monsterSkills[17].code` | [Костяная броня](Monster-skills.md#mob_bone_armour) |
| `monsterSkills[17].icon` | [armour](Tables/Tables-MODIFIER.md#armour) |
| `monsterSkills[17].mana` | 12 |
| `monsterSkills[17].cooldown` | 15 |
| `monsterSkills[17].spell` | Да |
| `monsterSkills[17].buff.duration` | 6 |
| `monsterSkills[17].buff.stats[0].stat` | [Броня](Stats/Stats-HERO.md#stock_armor) |
| `monsterSkills[17].buff.stats[0].op` | `INCREASED` |
| `monsterSkills[17].buff.stats[0].value` | 60; 60 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
