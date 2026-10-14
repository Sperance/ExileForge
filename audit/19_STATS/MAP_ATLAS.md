# 19_STATS / MAP_ATLAS — характеристики карт и Атласа (группа `map`)

Охват: все 144 характеристики `stats.json` с `group = MAP` (72) и `group = ATLAS` (72); модификаторы карт (префиксы/суффиксы `tags: map`, HANDCRAFTED, ALCHEMY), строки Атласа (`atlas.json`, 616 узлов, потолок 132 очка), печати (`rules.json → seals`, имплисит печати стража), угрозы тиров (`campaign.maps.tiers.perils`), строки тиров (`tiers.effects`), риск карт (`maps.risk`), `MapEffects` клиента, алтари (`ALTAR`-семейства, `features.altar`), скарабеи и зелья (`rules.json → brews`), мутаторы и проклятия Разлома, башня (как потребители тех же кодов).

Версии: сервер/правила `ktor-bestgame` @ 2619f0f (контент), клиент `ExileForge` с сабмодулем `backend` @ 3ec40bd2. Тесты гонялись в копии `/home/user/audit/work/stats-map/EF` (контент сабмодуля).

## Сводка

| | всего | работает (тест) | работает (только код) | частично | мёртвая |
|---|---|---|---|---|---|
| MAP | 72 | 40 | 20 | 8 | 4 |
| ATLAS | 72 | 9 | 52 | 6 | 5 |
| **итого** | **144** | **49** | **72** | **14** | **9** |

- **Частично** (14): MAP_MONSTER_DAMAGE, MAP_BOSS_POWER, MAP_ABYSS_DAMAGE, MAP_ABYSS_LEADER, ATLAS_MONSTER_DAMAGE, ATLAS_INFLUENCE_POWER, ATLAS_GUARDIAN_POWER, ATLAS_ABYSS_POWER (урон чар монстров `ATTACK_MAGICAL` не растёт, STAT-MAP-03); MAP_MONSTER_SPEED, ATLAS_MONSTER_SPEED (скорость чар не растёт, STAT-MAP-05); MAP_MONSTER_ARMOUR (54% монстров без базы, STAT-MAP-06); MAP_HERO_REGEN (обходит `Recovery`, STAT-MAP-07); MAP_SEALED_GUARDIAN и ATLAS_MAP_EFFECT (печать × атлас, STAT-MAP-01/04).
- **Мёртвые** (9): ATLAS_ALTAR_CHANCE, ATLAS_ALTAR_PACTS, ATLAS_ALTAR_BOONS (8 узлов Атласа), ATLAS_ORB_CHANCE, MAP_MONSTER_MODS, MAP_REINFORCEMENTS (только алтари, выключенные на картах) — STAT-MAP-02; MAP_HARSH_BARGAINS, MAP_FOUNTAINS, ATLAS_SKILL_LEVEL (нет ни одного источника) — STAT-MAP-08.
- «Работает (код)» — потребитель найден и прочитан (добыча, окна, объекты карты); метаморфного теста нет (см. «Что не проверено»).
- Имена ru: у всех 144 серверное `enum.EnumStatStock.*` есть; клиентское `enum.stat.*` есть у 117 и совпадает с сервером дословно; у остальных клиент берёт серверное (фолбэк `Text.rawStatTitle`). `stat.template.*` нет у 4: MAP_HERO_DAMAGE, MAP_MONSTER_MODS, MAP_REINFORCEMENTS, ATLAS_ORB_CHANCE. `stat.desc.*` нет ни у одной.
- `stats.json` у этих групп не несёт `kind/unit/section` (только code/group/order); единица в таблице — по коду потребителя. Клиент решает «%»/«шт» на карточке узла Атласа списком `AtlasEffects.flat` — он неполон (STAT-MAP-09).

### Как это сворачивается (общая схема)
- Строки карты-предмета → `LootRoller.mapEffects` (сумма по стату, плюс качество → `MAP_QUANTITY`, плюс `tiers.effects × тир`, **всё × (1 + ATLAS_MAP_EFFECT/100)**, `Loot.kt:255-266`) → `atlasPowers` (ATLAS_MONSTER_* добавляются в MAP_MONSTER_*) → + скарабеи → `activeMap` (влияние: +INFLUENCE_POWER к жизни/урону; риск по `maps.risk` → количество, опыт, доля в редкость). Вход — `CampaignService.kt:204`.
- Монстры: `MapEffects.buffs` (клиент) превращает MAP_MONSTER_* в `MonsterEffect` и кладёт их через `Spawns.packs` в `MonsterRoller.fold`: `(база + ΣADD) × (1 + ΣINCREASED/100) × Π MORE`. INCREASED на нулевой базе = 0.
- Герой: `MapEffects.hero` (клиент) правит лист боя: свои формулы на каждую строку; дары Атласа (`AtlasEffects.hero`) — прибавки к листу до «меньше» карты.
- Спавн/добыча: `Run` (правила) читает `context.active.effects` + сделки алтаря (`mapEffect`) и `context.atlas` (`context[...]`).
- Атлас: `AtlasBonuses.of` — сумма строк взятых узлов; в `RunContext.atlas` кладётся **на каждом заходе** (не только по карте-предмету), вместе с древом гильдии и зельем.

## Таблица характеристик

Обозначения: «T:» — проверено тестом `AuditMapStatsTest` (вывод — ниже), «код» — проверено чтением кода. Источники: значения — диапазон тиров `[низ … верх]` пула карт; Σ — сумма всех узлов Атласа (реально меньше: 132 очка из 616 узлов). «алтарь (мёртв)» — источник есть в контенте, но недостижим (STAT-MAP-02).

| код | имя ru (сервер = клиент) | ед. | ops / свёртка | источники / макс | потребители | тест | капы | показ | статус |
|---|---|---|---|---|---|---|---|---|---|
| MAP_MONSTER_LIFE | Здоровье монстров карты | % | INCREASED STOCK_HEALTH; MapEffects.buffs → MonsterRoller.fold | префикс 8–40; тир +8/тир (T16: 128); ATLAS_MONSTER_LIFE (Σ126.7); влияние; Разлом/башня; всё ×ATLAS_MAP_EFFECT | бой монстров (клиент), риск 0.4 | T: HEALTH↑ у 76/76 | нет | «больше» = increased (сводится аддитивно с атласом) | работает |
| MAP_MONSTER_DAMAGE | Урон монстров карты | % | INCREASED STOCK_ATTACK_{PHYS,FIRE,COLD,LIGHTNING,CHAOS}; MapEffects.buffs → MonsterRoller.fold | префикс 6–32; тир +5/тир (T16: 80); ATLAS_MONSTER_DAMAGE Σ67.4; влияние | бой, риск 0.6 | T: ATTACK_* ↑, ATTACK_MAGICAL нет; удар чар ×1.192 вместо ×1.32 | нет | «больше урона» — для чар неверно | частично → STAT-MAP-03 |
| MAP_MONSTER_SPEED | Скорость атаки монстров карты | % | INCREASED ATTACK_SPEED и CAST_SPEED; MapEffects.buffs → MonsterRoller.fold | префикс 5–25; ATLAS_MONSTER_SPEED Σ24.4; Разлом | бой, риск 0.8 | T: скорость атаки ↑ у 76/76, CAST_SPEED не изменился у 76/76 | castSpeed кап боя | ru: «скорости атаки» (en: attack and cast) | частично → STAT-MAP-05 |
| MAP_MONSTER_RESIST | Сопротивления монстров карты | % | ADD RESIST_FIRE/COLD/LIGHTNING/CHAOS; MapEffects.buffs → MonsterRoller.fold | префикс 8–40; Разлом 20 | бой | T: +40 у 76/76 (бой физ. героя не меняется — ожидаемо) | потолок сопр. монстров боя | ок | работает |
| MAP_PACK_SIZE | Число монстров карты | % | % к числу жетонов (Run.count), сумма с ATLAS_PACK_SIZE | префикс 6–35; ALC 20–25; тир +2/тир; скарабей 15 | Run.count (правила) | T: 58.6→78.6 жетонов/заход (+34%) | нет | ок | работает |
| MAP_MONSTER_RARITY | Волшебные и редкие монстры карты | % | множитель весов волшебных и редких | префикс 15–80 | Run.rarityRule | T: magic .146→.232, rare .019→.029 | нет | ок | работает |
| MAP_HERO_RESIST | Ослабление сопротивлений на карте | % | −v к 4 сопротивлениям (вкл. хаос); MapEffects.hero (лист героя) | суффикс 6–30; штраф акта resistPenalty; Разлом; алтарь (мёртв) | бой героя, сводка «на карте» | T: лист 0→−30 ×4 | сопр. боя (кап/дно Combatant) | ок | работает |
| MAP_HERO_REGEN | Ослабление регенерации на карте | % | ×(1−v/100) HEALTH_REGEN и LIFE_REGEN_PERCENT — своя формула мимо Recovery | суффикс 20–80; алтарь (мёртв) | бой, RoadLife | T: дно POWER_ETERNITY_SEAM обходится (2/с вместо 5/с) | max(0,…) | ru: «меньше восстановление здоровья» — вампиризм не режет | частично → STAT-MAP-07 |
| MAP_CHESTS | Сундуки карты | шт | +N сундуков окна (сервер, .toInt) | суффикс 1–3; HC 1–2; ALC 2–3; скарабей +1 | CampaignService (вход) | код | .toInt | ок | работает (код) |
| MAP_QUANTITY | Количество предметов на карте | % | +к количеству ActiveMap | тир +3/тир (T16 48); качество карты (+1/%) | Run.boost через ActiveMap.quantity | код | нет | ок | работает (код) |
| MAP_RARITY | Редкость предметов на карте | % | +к редкости ActiveMap | тир +5/тир (T16 80); скарабей 20 | ActiveMap.rarity | код | нет | ок | работает (код) |
| MAP_EXPERIENCE | Опыт на карте | % | +к опыту ActiveMap | суффикс 3–14; HC 2–8 | ActiveMap.experience | код | нет | ок | работает (код) |
| MAP_MAGIC_MONSTERS | Волшебные монстры карты | % | множитель веса волшебных | префикс 8–30; ALC 40–50 | Run.rarityRule | T: magic .146→.205 | нет | ок | работает |
| MAP_RARE_MONSTERS | Редкие монстры карты | % | множитель веса редких (+ATLAS_RARE_MONSTERS) | префикс 6–25; ALC 40–50 | Run.rarityRule | T: rare .019→.027 | нет | ок | работает |
| MAP_HERO_DEGEN | Карта: потеря здоровья героя | % зд./с | ADD LIFE_DEGEN_PERCENT; MapEffects.hero (лист героя) | только мутатор Разлома MUT_BLEEDING 0.5 | бой, RoadLife | T: бой изменился (dur 13.18→12.43) | нет | ок | работает (только Разлом) |
| MAP_FLASK_CHARGES | Карта: заряды фляг | % | ADD FLASK_CHARGES_GAINED −v; MapEffects.hero (лист героя) | суффикс 20–40 | бой (фляги) | T: лист 0→−40; бой без фляг не проверен | нет | ок | работает (лист) |
| MAP_HERO_MANA_REGEN | Карта: регенерация маны | % | «увеличение» MANA_REGEN: (100+x)(1−v)−100; MapEffects.hero (лист героя) | суффикс 30–50; Разлом 20 | Recovery.manaRegen | T: лист 0→−50 | max(0,…) | ок (по RULES — в «увеличении») | работает (лист) |
| MAP_HERO_NO_MANA_REGEN | Карта: мана не восстанавливается | флаг | ADD NO_MANA_REGEN; MapEffects.hero (лист героя) | угроза тиров (perils) T11+, 10%; риск 20 | Recovery.share | T: флаг в листе; риск ×атлас | — | ок | работает; → STAT-MAP-04 |
| MAP_MONSTER_CAST | Колдовство монстров карты | % | ADD COOLDOWN_RECOVERY; MapEffects.buffs → MonsterRoller.fold | префикс 20–40 | Combatant.recovery | T: +40 у 76/76 (бой этой стаи не изменился) | recoveryMax | ок | работает (стат) |
| MAP_SKILL_COST | Карта: цена умений | % | ADD SKILL_COST −v; MapEffects.hero (лист героя) | суффикс 15–30 | Combatant.skillCost | T: 1.0→1.3 | max(0,…) | ок | работает |
| MAP_HERO_RECOVERY | Карта: восстановление героя | % | ADD RECOVERY_RATE −v; MapEffects.hero (лист героя) | суффикс 10–40; Разлом 20 | бой | T: лист 0→−40 | — | ок | работает (лист) |
| MAP_HERO_MAX_RESIST | Карта: максимум сопротивлений | % | ADD RESIST_MAX_ALL и RESIST_MAX_CHAOS −v | суффикс 2–10; Разлом 10 | Combatant.resistTo | T: лист | resistHardCap/0 | ок | работает |
| MAP_HERO_DEFENCES | Карта: защита героя | % | ×(1−v) ARMOR/EVASION/ES | суффикс 10–40 | бой | T: evasion 26→15.6 | max(0,…) | «меньше» ок | работает |
| MAP_HERO_BLOCK | Карта: блок героя | % | ADD BLOCK_CHANCE −v | суффикс 6–25 | бой | T: 0→−25 (отрицат. в листе) | кламп в бою (не проверен) | ок | работает |
| MAP_HERO_CRIT | Карта: крит героя | % | ×(1−v) CRIT и SPELL_CRIT (если есть в листе) | суффикс 15–50; Разлом 10 | бой | T: 5→2.5, dealt 23.4→22.3 | max(0,…) | ок | работает |
| MAP_MONSTER_PENETRATION | Пробивание монстров карты | % | ADD PENETRATE_ELEMENTAL | префикс 4–20; Разлом 15 | бой | T: стат +20 (бой физ.) | — | ок | работает (стат) |
| MAP_MONSTER_REFLECT | Отражение монстров карты | % | ADD REFLECT | префикс 3–15 | бой | T: taken 192→194.4 | — | ок | работает |
| MAP_MONSTER_CRITICAL | Крит монстров карты | % | ADD CRITICAL_CHANCE | префикс 4–20; Разлом 10 | бой | T: бой изменился | крит-кап боя | ок | работает |
| MAP_MONSTER_AILMENTS | Состояния от монстров карты | % | ADD 5 шансов недугов | префикс 8–35 | бой | T: бой изменился | — | ок | работает |
| MAP_MONSTER_ARMOUR | Защита монстров карты | % | INCREASED ARMOR, EVASION на нулевой базе | префикс 15–80; башня | бой; риск 0.2 | T: броня не изм. у 49/76, уклонение у 53/76; в контенте 135/251 без обоих | — | «больше брони и уклонения» | частично → STAT-MAP-06 |
| MAP_MONSTER_LEECH | Вампиризм монстров карты | % | ADD LEECH_ALL | префикс 1–6 | бой | T: стат (бой без изменений — герой не ранит) | — | ок | работает (стат) |
| MAP_MONSTER_STUN | Стойкость монстров карты | % | ADD AVOID_STUN | префикс 30–100 | бой | T: стат | — | ок | работает (стат) |
| MAP_MONSTER_MAGIC_MIN | Карта: монстры не ниже волшебных | флаг | вес обычных = 0 | префикс (ур. 35) 1; риск 12 | Run.rarityRule | T: magic .894, rare .019→.106 (×5.6) | — | текст молчит о редких | работает; вопрос |
| MAP_HERO_HASTE | Карта: скорость героя | % | ADD MOVEMENT_SPEED | комната карты с силой уникалки SECRET_HASTE (MapFeatures) | ExpeditionWorld.heroSpeed | T: лист 0→20 | предел скорости листа | ок | работает |
| MAP_HERO_ATTACK_SPEED | Карта: скорость атаки героя | % more | ×(1+v) ATTACK_SPEED листа | мутатор Разлома MUT_BRISK 10; алтарь (мёртв) | бой | T: 1.5→1.65; без оружия — 0 (нет ключа) | attackSpeedMin/Max | ru «больше», en «increased» | работает (кроме безоружного) |
| MAP_HERO_LIFE | Карта: здоровье героя | % more | ×(1+v) HEALTH | сбор узлов GATHER_MIGHT; Разлом ±10/15; алтарь (мёртв) | бой | T: 192→220.8 | max(0,…) | отрицательное в Разломе печатается «-10% больше» | работает |
| MAP_HERO_DAMAGE | Карта: урон героя | % more | DAMAGE «больше» | Разлом MUT_EMPOWERED 15; сбор узлов; spite алтаря; алтарь (мёртв) | бой | T: dealt 23.4→29.2 | — | нет stat.template (фолбэк «имя: +15») | работает |
| MAP_MONSTER_MODS | Карта: лишние модификаторы монстров | шт | строк сверх броска (empowered) | только ALTAR_CURSE_MODS (алтари выключены) | RunFeatures (pacts) | — | — | нет stat.template | мёртвая → STAT-MAP-02 |
| MAP_REINFORCEMENTS | Карта: редкие стаи подкрепления | шт | стаи подкрепления | только ALTAR_CURSE_HORDE (алтари выключены) | Run.reinforcements | — | ≥0 | нет stat.template | мёртвая → STAT-MAP-02 |
| ATLAS_ORB_CHANCE | Шанс сфер на карте | % | ×(1+v) доли сфер | только ALTAR_BOON_ORBS (алтари выключены) | Run (сферы) | — | ≥0 | нет stat.template | мёртвая → STAT-MAP-02 |
| ATLAS_ALTAR_CHANCE | Атлас: шанс алтаря сделки | % | шанс алтаря | узлы ALTAR_A01/A03/A04/A07 Σ45 | MapFeatures.altars — rule.max=0 | T: 0 алтарей за 300 семян при +1000% | — | обещает алтари | мёртвая → STAT-MAP-02 |
| ATLAS_ALTAR_PACTS | Атлас: пары алтаря | шт | +пары | узел A03 +1 | MapFeatures.altars | T: см. выше | — | показ «+1%» | мёртвая → STAT-MAP-02, 09 |
| ATLAS_ALTAR_BOONS | Атлас: сила даров алтаря | % | сила даров | узлы A02/A05/A06/A08 Σ72 | MapFeatures.altars | T: см. выше | — | ок | мёртвая → STAT-MAP-02 |
| ATLAS_MERCHANT_CHANCE | Атлас: шанс странствующего торговца | % | ×шанс торговца | 4 узла Σ78 | MapFeatures.merchant | код | шанс ≤1 | ок | работает (код) |
| ATLAS_MERCHANT_STOCK | Атлас: товары странствующего торговца | шт | +вещи | 1 узел +1 | MapFeatures.merchant | код | ≥0 | показ «+1%» | работает (код); → STAT-MAP-09 |
| ATLAS_MERCHANT_DISCOUNT | Атлас: скидка странствующего торговца | % | скидка | 4 узла Σ17 | merchant | код | maxDiscount 90 | ок | работает (код) |
| ATLAS_TRAPS | Атлас: ловушки на картах | % | ×число ловушек | 4 узла Σ55 | MapFeatures.traps | код | — | ок | работает (код) |
| ATLAS_TRAP_REWARD | Атлас: редкость за ловушки | % | редкость за ловушку | 5 узлов Σ10 | MapFeature.Trap → ATLAS_RARITY | код | — | ок | работает (код) |
| ATLAS_SECRET_CHANCE | Атлас: шанс тайных комнат | % | ×шанс комнат | 4 узла Σ49 | MapFeatures.room | код | ≤1 | ок | работает (код) |
| ATLAS_TREASURE_QUALITY | Атлас: добыча сокровищниц | % | количество сокровищницы | 4 узла Σ72 | сокровищница | код | — | ок | работает (код) |
| ATLAS_NODES | Атлас: узлы ремёсел | шт | +узлы ремёсел | 2 узла Σ3 | MapFeatures.nodes | код | ≥0 | показ «+2%» | работает (код); → STAT-MAP-09 |
| ATLAS_GATHER_DOUBLE | Атлас: двойной сбор | % | шанс двойного сбора | 7 узлов Σ40 | узел сбора | код | — | ок | работает (код) |
| MAP_DEADLY_TRAPS | Карта: смертоносные ловушки | % | FeatureStat TRAPS и TRAP_DAMAGE | префикс 15–60; риск 0.3 | MapFeatures.traps | код | scale ≥0 | ок | работает (код) |
| MAP_HARSH_BARGAINS | Карта: тяжкие сделки | % | FeatureStat ALTAR_CURSES | нет: семейство удалено в 189b680 | MapFeatures.altars (алтарей нет) | — | — | ключ локали остался | мёртвая → STAT-MAP-08 |
| MAP_GREEDY_MERCHANT | Карта: жадный торговец | % | MERCHANT_MARKUP | префикс 20–80; риск 0.15 | торговец карты | код | ≥0 | ок | работает (код) |
| MAP_TREASURE_GUARDS | Карта: стражи сокровищ | флаг | GUARDS | префикс 1; риск 12 | Run.guards | код; риск ×атлас (T) | — | ок | работает; → STAT-MAP-04 |
| MAP_SEALED_GUARDIAN | Карта: печать стража | % редких | доля редких для снятия печати | имплисит печати T6+ 15%, T11+ 30%: 60 | Run.seal/sealOpen (сервер, клиент) | T: при ATLAS_MAP_EFFECT ≥+67% босс не открывается 40/40 | нет (>100% допускается) | карта показывает 60%, действует 60×(1+e) | частично → STAT-MAP-01 |
| MAP_HERO_LEECH | Карта: вампиризм героя | % | ADD LEECH_ALL | мутатор Разлома MUT_LEECHING 1 | бой | T: бой изменился | — | ок | работает (только Разлом) |
| MAP_GOLD | Золото карты | % | +к золоту Run.boost | суффикс 8–40; HC 6–25; ALC 25–30 | Run.boost, LootBreakdown | код | — | ок | работает (код) |
| MAP_FOUNTAINS | Источники карты | шт | +источники | нет источника | MapEffects.fountains | — | ≥0 | ключ есть | мёртвая → STAT-MAP-08 |
| MAP_BOSS_POWER | Сила босса карты | % more | MORE HEALTH+ATTACK_* боссу; + к добыче босса | префикс 10–50; ALC 30–40; алтарь (мёртв) | bossBuffs; Run bossLoot | T: HEALTH/ATTACK_* ↑; ATTACK_MAGICAL нет | — | ок | частично → STAT-MAP-03 |
| MAP_ITEM_LEVEL | Уровень предметов карты | шт | +к уровню вещей (.toInt) | суффикс 3–5 (ур. 40+) | Run.itemLevel | T: ×1.96 → 9.8 | maxItemLevel | показ 5, действует до 9 | работает; → STAT-MAP-04 |
| MAP_CRYSTALS | Кристаллы эссенций карты | шт | +кристаллы окна | суффикс 1; ALC 1–2 | CampaignService | код | .toInt | ок | работает (код) |
| MAP_BOOKS | Книги умений карты | % | шанс книг | суффикс 30–60; ALC 50–100 | Run (книги) | код | — | ок | работает (код) |
| MAP_ABYSS_CRACKS | Расщелины Бездны карты | шт | +расщелины | суффикс 1; влияние Бездны | CampaignService | код | .toInt | ок | работает (код) |
| MAP_ABYSS_DEPTH | Глубина Бездны карты | шт | +глубина | суффикс 1 | RunAbyss (клиент) | код | — | ок | работает (код) |
| MAP_ABYSS_HOARD | Награда Расщелины карты | % | вещи награды | суффикс 15–50 | Run.abyss | код | — | ок | работает (код) |
| MAP_ABYSS_UNIQUE | Уникалки Бездны карты | % | шанс уникалки | суффикс 20–80 | Run.abyss | код | — | ок | работает (код) |
| MAP_ABYSS_ORBS | Сферы Бездны карты | % | сферы награды | суффикс 20–80 | Run.abyss | код | — | ок | работает (код) |
| MAP_ABYSS_RARE | Редкие вещи Бездны карты | % | доля редких | суффикс 5–20 | Run.abyss | код | — | ок | работает (код) |
| MAP_ABYSS_LIFE | Здоровье монстров Бездны | % | INCREASED HEALTH волн | префикс 12–50 | AbyssWaves.buffs | код | — | ок | работает (код) |
| MAP_ABYSS_DAMAGE | Урон монстров Бездны | % | INCREASED ATTACK_* волн (без MAGICAL) | префикс 10–40 | AbyssWaves.buffs | код | — | ок | частично → STAT-MAP-03 |
| MAP_ABYSS_SWARM | Орда Бездны | % | размер волны | префикс 15–50 | AbyssWaves | код | — | ок | работает (код) |
| MAP_ABYSS_LEADER | Сила вожаков Бездны | % more | MORE HEALTH+ATTACK_* вожакам (без MAGICAL) | префикс 15–50 | AbyssWaves.leaderBuffs | код | — | ок | частично → STAT-MAP-03 |
| ATLAS_QUANTITY | Атлас: количество предметов на картах | % | +к количеству | 32 узла Σ91.2; гильдия; алтарь (мёртв, в т.ч. −20) | Run.boost (каждый заход, не только карта) | код | — | «на картах» | работает (код); вопрос STAT-MAP-12 |
| ATLAS_RARITY | Атлас: редкость предметов на картах | % | +к редкости | 20 узлов Σ130.8; гильдия; зелья 10–30; ловушки | Run.boost | код | — | «на картах» | работает (код) |
| ATLAS_EXPERIENCE | Атлас: опыт на картах | % | +к опыту | 10 узлов Σ26.8; гильдия; зелья 10–30 | Run (опыт), LootBreakdown | код | — | «на картах» | работает (код) |
| ATLAS_MAP_DROP | Атлас: шанс выпадения карт | % | ×шанс карты | 16 узлов Σ151 | Run (mapDrop) | код | — | ок | работает (код) |
| ATLAS_VAAL_CHANCE | Атлас: шанс портала Ваал | % | ×шанс портала | 17 узлов Σ239 | Run.portal, клиент portalChance | код | шанс ≤1 | ок | работает (код) |
| ATLAS_VAAL_REWARD | Атлас: награда Ваал-зон | % | награда Ваал-зоны | 27 узлов Σ191 | VaalZones | код | — | ок | работает (код) |
| ATLAS_VAAL_MIN_MODS | Атлас: минимум модификаторов Ваал-зоны | шт | +минимум строк | 4 узла Σ5 | VaalZones | код | — | ок | работает (код) |
| ATLAS_CHESTS | Атлас: сундуки на картах | шт | +сундуки | 4 узла Σ5 | Chests.window (сервер) | код | — | ок | работает (код) |
| ATLAS_FOUNTAINS | Атлас: источники на картах | шт | +источники | 4 узла Σ5 | AtlasEffects.fountains (клиент) | код | ≥0 | ок | работает (код) |
| ATLAS_PACK_SIZE | Атлас: число монстров | % | % к жетонам | 14 узлов Σ62.4 | Run.count | T: 58.6→93.3 (+59%) | — | «на картах» | работает |
| ATLAS_RARE_MONSTERS | Атлас: редкие монстры | % | вес редких | 10 узлов Σ60.5 | Run.rarityRule | T: rare .019→.029 | — | ок | работает |
| ATLAS_BOSS_RESPAWN | Атлас: скорость возвращения босса | % | время возрождения ×(1−v) | 15 узлов Σ180 | bossRespawnHours | код | atlas.respawnCap 90 (Σ узлов 180) | «на v% быстрее» = на v% короче | работает (код); вопрос STAT-MAP-11 |
| ATLAS_BOSS_UNIQUE | Атлас: уникальные предметы с босса | % | ×шанс уникалки босса | 16 узлов Σ174.4 | Run (босс) | код | — | ок | работает (код) |
| ATLAS_MAP_NEXT | Атлас: карты выше | % | ×шанс карты следующей зоны | 17 узлов Σ257 | LootRoller.mapDrop | код | — | ок | работает (код) |
| ATLAS_MAP_RARE | Атлас: редкие карты | % | вес редкой карты | 14 узлов Σ321 | mapRarity | код | — | ок | работает (код) |
| ATLAS_MAP_AFFIX | Атлас: лишний модификатор карты | % | шанс лишней строки | 13 узлов Σ162.2 | Run (дроп карты) | код | percent: >100 = 100 | ок | работает (код); вопрос STAT-MAP-11 |
| ATLAS_MAP_EFFECT | Атлас: эффект модификаторов карты | % | × все строки карты-предмета (вкл. тир, качество, флаги, печать) | 19 узлов Σ145 (≈49 очков → +96%) | LootRoller.mapEffects (сервер, вход) | T: см. STAT-MAP-01/04 | нет | «к действию модификаторов карт» | частично → STAT-MAP-01, 04 |
| ATLAS_VAAL_UNIQUE | Атлас: уникальные предметы Ваал | % | ×уникалки Ваал | 13 узлов Σ285 | Run | код | — | ок | работает (код) |
| ATLAS_BOSS_LOOT | Атлас: добыча с боссов | % | +количество босса | 29 узлов Σ408 | Run | код | — | ок | работает (код) |
| ATLAS_CHEST_LOOT | Атлас: добыча из сундуков | % | +количество сундуков | 15 узлов Σ223; гильдия | Run.chest | код | — | ок | работает (код) |
| ATLAS_RECIPE | Атлас: рецепты | % | ×шанс рецепта | 8 узлов Σ291.5 | Run | код | — | ок | работает (код) |
| ATLAS_GOLD | Атлас: золото | % | +золото | 14 узлов Σ195.2; гильдия; зелья 15–45; алтарь (мёртв) | Run.boost | код | — | ок | работает (код) |
| ATLAS_MONSTER_MODS | Атлас: модификаторы редких монстров | шт | +строк редким | 1 узел +1 | Run/MonsterRoller extraRareMods | код | — | ок | работает (код) |
| ATLAS_CRYSTAL_CHANCE | Атлас: шанс кристаллов | % | шанс лишнего кристалла | 32 узла Σ274 | Windows (crystals) | код | percent: >100 = 100 | ок | работает (код); STAT-MAP-11 |
| ATLAS_CRYSTAL_ESSENCES | Атлас: лишняя эссенция | % | шанс лишней эссенции | 17 узлов Σ367 | Windows | код | >100 = 100 | ок | работает (код); STAT-MAP-11 |
| ATLAS_CRYSTAL_TIER | Атлас: эссенции выше | % | шанс эссенции выше | 14 узлов Σ195 | Windows | код | >100 = 100 | ок | работает (код); STAT-MAP-11 |
| ATLAS_CRYSTALS | Атлас: кристаллы | шт | +кристаллы | 3 узла Σ3 | Windows | код | — | ок | работает (код) |
| ATLAS_CRYSTALS_MORE | Атлас: больше кристаллов | % | больше кристаллов | 1 узел 100 | Windows (через AtlasBonuses) | код | — | ок | работает (код) |
| ATLAS_GUARDIAN_POWER | Атлас: сила стражей | % more | MORE HEALTH+ATTACK_* стражам (без MAGICAL) | 3 узла Σ27 | MapEffects.guardianBuffs | код | — | ок | частично → STAT-MAP-03 |
| ATLAS_BOOKS | Атлас: книги умений | % | шанс книг | 8 узлов Σ136.5 | Run | код | — | ок | работает (код) |
| ATLAS_BOOKS_OWN | Атлас: книги своего класса | % | доля своего класса | 3 узла Σ70 | Run | код | share ≤1 | ок | работает (код) |
| ATLAS_FLASK_CHARGES | Атлас: заряды фляг | % | ADD FLASK_CHARGES_GAINED (AtlasEffects.hero) | 13 узлов Σ260 | лист героя в заходе | T: лист 0→100 | — | ок | работает (код) |
| ATLAS_FLASK_DURATION | Атлас: длительность фляг | % | ADD FLASK_DURATION | 14 узлов Σ154 | лист | T: лист | — | ок | работает (код) |
| ATLAS_FLASK_RARE | Атлас: заряды за редких | шт | заряды за редкого и уникального | 2 узла Σ2 | BattleLand (ключ ATLAS_FLASK_RARE в листе) | T: ключ в листе | — | ru «редкого» (и уникального тоже) | работает |
| ATLAS_MANA_REGEN | Атлас: регенерация маны | % | ADD MANA_REGEN до «меньше» карты | 9 узлов Σ93 | Recovery.manaRegen | T: лист 0→93 | — | ок | работает (код) |
| ATLAS_SKILL_LEVEL | Атлас: уровень умений | шт | ADD SKILL_LEVEL | нет узлов | AtlasEffects.hero | T: лист 0→1 (код живой) | — | ок | мёртвая → STAT-MAP-08 |
| ATLAS_ABYSS_CHANCE | Атлас: шанс расщелины | % | +шанс расщелины | 13 узлов Σ77.5 | Windows (abyss) | код | percent | ок | работает (код) |
| ATLAS_ABYSS_EXTRA | Атлас: вторая расщелина | % | шанс второй | 3 узла Σ60 | Windows | код | — | ок | работает (код) |
| ATLAS_ABYSS_DEPTH | Атлас: глубина Бездны | шт | +глубина | 3 узла Σ4 | AbyssRifts | код | — | ок | работает (код) |
| ATLAS_ABYSS_HOARD | Атлас: награда Расщелины | % | вещи награды | 18 узлов Σ271 | Run.abyss | код | — | ок | работает (код) |
| ATLAS_ABYSS_UNIQUE | Атлас: уникальные предметы Бездны | % | шанс уникалки | 1 узел 80 | Run.abyss | код | — | ок | работает (код) |
| ATLAS_ABYSS_ORBS | Атлас: сферы Бездны | % | сферы | 13 узлов Σ201 | Run.abyss | код | — | ок | работает (код) |
| ATLAS_ABYSS_RARE | Атлас: редкие вещи Бездны | % | доля редких | 9 узлов Σ59.5 | Run.abyss | код | — | ок | работает (код) |
| ATLAS_ABYSS_POWER | Атлас: сила Бездны | % more | MORE HEALTH+ATTACK_* волнам (без MAGICAL) | 3 узла Σ27 | AbyssWaves.buffs | код | — | ок | частично → STAT-MAP-03 |
| MAP_HERO_ENFEEBLE | Игрок ослаблен | % less | DAMAGE «меньше» | суффикс 6–25; Разлом 10 | бой | T: dealt 23.4→14.6 | max(0,…) | ок | работает |
| MAP_HERO_VULNERABILITY | Игрок уязвим | % | ADD PHYSICAL_TAKEN | суффикс 8–30 | бой | T: бой изменился | — | ок | работает |
| MAP_HERO_FLASK_EFFECT | Слабые фляги | % | ADD FLASK_EFFECT −v | суффикс 8–30; алтарь (мёртв) | фляги | T: лист | — | ок | работает (лист) |
| MAP_HERO_BUFF_DURATION | Короткие усиления | % | ADD BUFF_DURATION −v | суффикс 10–50 | баффы | T: лист | — | ок | работает (лист) |
| MAP_MONSTER_ONSLAUGHT | Монстры под Натиском | флаг | ADD ONSLAUGHT_ALWAYS | префикс (ур. 30) 1; риск 15 | бой | T: бой изменился | — | ок | работает; → STAT-MAP-04 |
| MAP_MONSTER_FORTIFY | Монстры укреплены | флаг | ADD FORTIFY_ALWAYS | префикс (ур. 40) 1; риск 12 | бой | T: dealt 23.4→18.7 | — | ок | работает; → STAT-MAP-04 |
| MAP_MONSTER_EXTRA_ELEMENTAL | Стихийный урон монстров | % | ADD PHYSICAL_AS_EXTRA_{FIRE,COLD,LIGHTNING} по v/3 | префикс 6–25; Разлом 20 | бой | T: бой изменился | — | ок | работает |
| ATLAS_OMENS | Атлас: знамения | % | вес знамений | 11 узлов Σ215 | Run | код | — | ок | работает (код) |
| ATLAS_CATALYSTS | Атлас: катализаторы | % | вес катализаторов | 12 узлов Σ210 | Run | код | — | ок | работает (код) |
| ATLAS_QUALITY_ORBS | Атлас: сферы качества | % | вес сфер качества | 12 узлов Σ180 | Run | код | — | ок | работает (код) |
| ATLAS_VEILED | Атлас: скрытые строки | % | шанс скрытой строки | 12 узлов Σ38 | Run.kill | код | percent | ок | работает (код) |
| ATLAS_MONSTER_LIFE | Атлас: здоровье монстров | % | сливается в MAP_MONSTER_LIFE (INCREASED) + риск | 15 узлов Σ126.7 | LootRoller.atlasPowers | T: 20+50=70 | — | ru «больше», en «more» — по факту increased | работает |
| ATLAS_MONSTER_DAMAGE | Атлас: урон монстров | % | → MAP_MONSTER_DAMAGE | 13 узлов Σ67.4 | atlasPowers | код | — | en «more» | частично → STAT-MAP-03 |
| ATLAS_MONSTER_SPEED | Атлас: скорость монстров | % | → MAP_MONSTER_SPEED | 7 узлов Σ24.4 | atlasPowers | код | — | ок | частично → STAT-MAP-05 |
| ATLAS_MAP_TIER | Атлас: тир карт | % | ×шанс подъёма тира | 26 узлов Σ217.7 | LootRoller.mapTier | код | heroCap | ок | работает (код) |
| ATLAS_MAP_INFLUENCE | Атлас: захваченные карты | % | +шанс захвата | 23 узла Σ64 | mapInfluence | код | percent | ок | работает (код) |
| ATLAS_INFLUENCE_ITEMS | Атлас: вещи влияния | % | +шанс влияния вещи | 25 узлов Σ96.9 | Run | код | percent | ок | работает (код) |
| ATLAS_INFLUENCE_POWER | Атлас: сила захваченных карт | % | +к MAP_MONSTER_LIFE/DAMAGE захваченной | 16 узлов Σ79 | LootRoller.influenced | код | — | «больше … (и больше награда)» | частично (MAGICAL) → STAT-MAP-03 |
| ATLAS_INFLUENCE_SHAPER | Атлас: только Создатель | флаг | пул влияния | 1 узел | InfluenceRule.pool | код | — | показ «+1%» | работает (код); → STAT-MAP-09 |
| ATLAS_INFLUENCE_ELDER | Атлас: только Древний | флаг | пул влияния | 1 узел | InfluenceRule.pool | код | — | показ «+1%» | работает (код); → STAT-MAP-09 |
| MAP_BLIGHT_CHANCE | Карта: очаги Скверны | % | FeatureStat BLIGHT_CHANCE | суффикс 50–200 | MapFeatures.blight | код | шанс ≤1 | ок | работает (код) |
| MAP_BLIGHT_SPREAD | Карта: расползание Скверны | % | FeatureStat BLIGHT_SPREAD | суффикс 15–60 | MapFeatures.blight | код | ≤1 | ок | работает (код) |
| MAP_MONSTER_UNSUPPRESSED | Неподавимость монстров карты | % | ADD SUPPRESSION_AVOID | префикс 20–60 | бой | T: стат | — | ок | работает (стат) |
| ATLAS_SUPPRESSION | Атлас: подавление восстановления | % | ADD RECOVERY_SUPPRESSION | 1 узел 15 | лист героя | T: лист 0→15 | — | ок | работает (код) |
| MAP_MONSTER_WITHER | Изранение и иссушение от монстров карты | % | ADD WOUND_CHANCE, PARCH_CHANCE | префикс 8–35; ATLAS_MONSTER_WITHER 10 | бой | T: бой изменился | — | ок | работает |
| ATLAS_MONSTER_WITHER | Атлас: изранение и иссушение от монстров | % | → MAP_MONSTER_WITHER | 1 узел 10 | atlasPowers | T: 10 | — | ок | работает (код) |

## Карточки дефектов

## [STAT-MAP-01] «Эффект модификаторов карт» Атласа растит порог печати стража — с +67% босс запечатанной карты не встаёт никогда
- **Категория:** BUG. **Приоритет:** P1. **Уверенность:** высокая (воспроизведено тестом реальным `Run`).
- **Подсистема:** правила (`LootRoller.mapEffects`, `Run.seal/sealNeed/sealOpen`), сервер (вход в карту, приём босса), клиент (`RunSeal`).
- **Файлы:строки:** `rules/.../roll/Loot.kt:255-266` (строка 265: каждое значение × `atlasEffect`); `rules/.../run/Run.kt:505,514,517,520`; `rules/.../roll/Loot.kt:218-224` (`mapSeal`, имплисит 60); `src/main/kotlin/.../campaign/CampaignService.kt:204` (вход: `mapEffects(it, bonuses.mapEffect)`), `:439`, `:606` (без снятой печати босс и бой с ним отклоняются); `content/campaign.json → maps.tiers` (sealFrom 6, sealChance 15, sealDouble 11 → 30%); `atlas.json` — 19 узлов `ATLAS_MAP_EFFECT`, Σ145.
- **Ожидание:** текст `stat.template.MAP_SEALED_GUARDIAN.ADD` — «портал откроется, когда падут {v}% редких монстров карты»; печать — доля, ≤100%. `ATLAS_MAP_EFFECT` — «+{v}% к действию модификаторов карт» (усиливать строки, а не ломать карту).
- **Факт:** `mapEffects` множит на `(1 + ATLAS_MAP_EFFECT/100)` все строки, включая имплисит печати. 60 × 1.67 = 100.2% → `sealNeed = ceil(rareTotal × 1.002) = rareTotal + 1`; `raresKilled` считает только жетоны зоны (`key / PACK_SLOTS < count`), подкрепления и стражи не в счёт → печать не снимается ни при какой зачистке. Сервер не принимает ни `Engage(boss)`, ни `Boss`: нет убийства босса, очка Атласа `BOSS`, `completions/cleared`, добычи босса. Карта при этом показывает «60%».
- **Доказательство (тест `AuditMapStatsTest.seal`, зона ур. ≥68, тир 6, 40 семян):**
  ```
  atlasEffect=1.0  MAP_SEALED_GUARDIAN=60.0  | rareTotal=5 sealNeed=3 openAfterKillingEverything=true  | blocked 0/40
  atlasEffect=1.3  MAP_SEALED_GUARDIAN=78.0  | rareTotal=5 sealNeed=4 openAfterKillingEverything=true  | blocked 0/40
  atlasEffect=1.67 MAP_SEALED_GUARDIAN=100.2 | rareTotal=5 sealNeed=6 openAfterKillingEverything=false | blocked 40/40
  atlasEffect=1.96 MAP_SEALED_GUARDIAN=117.6 | rareTotal=5 sealNeed=6 openAfterKillingEverything=false | blocked 40/40
  ```
- **Воспроизведение:** герой с ≥+67% `ATLAS_MAP_EFFECT` (жадный обход графа: ≈49 очков дают +96%, потолок 132) входит в карту тира ≥6 с имплиситом печати (15% карт T6–T10, 30% T11+); убить всех — портал не открывается.
- **Последствия:** запечатанные карты непроходимы для героев с сильной ветвью «эффекта карт» — ровно у тех, кто вкладывается в эндгейм карт; трата карты-предмета; при +30…+66% печать просто требует 78–100% редких вместо 60%.
- **Масштаб:** все печати стража на картах-предметах; все герои с ≥67% ветви.
- **Варианты:** (1) не масштабировать `MAP_SEALED_GUARDIAN` (список «строк-правил», что `atlasEffect` не трогает: печать, флаги, счётные — см. STAT-MAP-04); (2) масштабировать, но `seal.coerceAtMost(100)` в `Run.seal` (печать становится «все редкие»); (3) масштабировать обратно (эффект атласа *ослабляет* печать как вредную строку).
- **Побочные эффекты:** вариант 1/2 меняет `sealNeed` у уже открытых заходов — заход заморожен в `RunContext.active`, сервер и клиент считают из одного значения; правка правил → `RULES_VERSION`.
- **Регрессионные тесты:** `sealOpen(все жетоны)` = true при любом `atlasEffect`; значение печати на карточке = значение в заходе.
- **Вопросы владельцу:** см. «Вопросы» → MAP_SEALED_GUARDIAN.
- **Рекомендация:** вариант 1 вместе со STAT-MAP-04 (одна точка «что усиливает эффект карт»). **Статус:** открыт.

## [STAT-MAP-02] Ветка алтарей Атласа (8 узлов) и все строки алтарей мертвы: алтари с карт сняты, а коммит баланса вернул узлам алтарные строки
- **Категория:** BUG / CONTENT. **Приоритет:** P2. **Уверенность:** высокая (тест + история git).
- **Подсистема:** контент Атласа, объекты карты.
- **Файлы:строки:** `content/campaign.json:10432` (`"altar": {"chance": 0.0, "extraChance": 0.0, "max": 0, …}`); `rules/.../run/MapFeatures.kt:335` (`if (rule.max <= 0) return emptyList()`); `content/atlas.json:16-23` (`ATLAS_ALTAR_A01..A08`: ATLAS_ALTAR_CHANCE Σ45, ATLAS_ALTAR_BOONS Σ72, ATLAS_ALTAR_PACTS +1); `content/modifiers.json` — 17 семейств `source: ALTAR` (`ALTAR_BOON_*`, `ALTAR_CURSE_*`), их единственные источники для `MAP_MONSTER_MODS`, `MAP_REINFORCEMENTS`, `ATLAS_ORB_CHANCE`.
- **История:** `189b680 feat: remove altars from campaign maps` (8 окт.) — алтари сняты, узлы ветки переписаны на `ATLAS_SECRET_CHANCE/ATLAS_TREASURE_QUALITY` («the altar atlas branch now boosts secret rooms and treasure vaults»). `c814e9f feat: passive tree and atlas balance pass from the build audit` (13 окт.) вернул в эти же 8 узлов `ATLAS_ALTAR_CHANCE/BOONS/PACTS` (и поднял A08 до 48) — по-видимому, баланс правился от старой копии `atlas.json`.
- **Факт:** узлы берутся за очки Атласа и ничего не дают; Разлом свои алтари строит из `trials.rift.boons/curses`, атласные строки туда не переходят (`RiftService.ALTAR` — только силы уникалок). Тексты узлов: «+10% к шансу алтаря сделки на картах», «Дары алтарей сделки на 48% сильнее».
- **Доказательство:** тест `AuditMapStatsTest.altars` — `altar rule max=0 chance=0.0; altars over 300 seeds = 0` при `ATLAS_ALTAR_CHANCE` +1000%.
- **Последствия:** до 8 очков Атласа впустую (ветка стоит между VAAL_C04 и далее); игрок обманут текстом. Мёртвый контент: 17 ALTAR-семейств, MAP_MONSTER_MODS, MAP_REINFORCEMENTS, ATLAS_ORB_CHANCE, локаль `modifier.ALTAR_*`.
- **Масштаб:** все герои, взявшие ветку; уже взятые узлы у живых героев.
- **Варианты:** (1) вернуть узлам строки `189b680` (SECRET_CHANCE/TREASURE_QUALITY) — как задумано; (2) вернуть алтари на карты (`altar.max > 0`) — тогда ветка снова живая; (3) убрать ветку и вернуть очки (респек) — требует миграции взятых узлов.
- **Побочные эффекты:** (1) меняет бонусы уже взятых узлов без перевыбора — допустимо, коды узлов те же; (3) — серверная миграция `atlas`.
- **Регрессионные тесты:** проверка контента: каждая строка узла Атласа имеет живого потребителя (для `FeatureStat` с `atlas` — ненулевой `rule.chance/max` механики).
- **Рекомендация:** вариант 1 + проверка контента. **Статус:** открыт.

## [STAT-MAP-03] Строки «урон монстров» карт и Атласа не растят урон чар монстров (`STOCK_ATTACK_MAGICAL`)
- **Категория:** BUG. **Приоритет:** P2. **Уверенность:** высокая (тест).
- **Подсистема:** клиент `MapEffects`, `AbyssWaves`; правила (для сравнения) `MonsterRoller`.
- **Файлы:строки:** `core/.../campaign/MapEffects.kt:53` (`damage = DamageType.entries.map { it.attack }` — PHYSICAL/FIRE/COLD/LIGHTNING/CHAOS), `:63` (MONSTER_DAMAGE), `:87` (guardianBuffs), `:91` (bossBuffs); `core/.../campaign/AbyssWaves.kt:103,104,108`; `core/.../combat/BattleMonsters.kt:70` и `MonsterSkillDamage.kt:20` (заклинание кладёт `ATTACK_MAGICAL × share` в урон); правила `MonsterRoller.kt:291-297` (`STRIKES` включает `ATTACK_MAGICAL` — кривая силы боссов его растит).
- **Ожидание:** «Монстры: {v}% больше урона», «Босс: {v}% больше здоровья и урона», «Стражи кристаллов: {v}% больше … урона», «Монстры Бездны: …урона» — весь урон.
- **Факт:** у 50/251 монстров контента есть `ATTACK_MAGICAL` (медиана 0.77 от суммы ударов, до 2.0); его доля в ударе чар не растёт ни от одной из 8 строк: MAP_MONSTER_DAMAGE, MAP_BOSS_POWER, ATLAS_MONSTER_DAMAGE и ATLAS_INFLUENCE_POWER (через MAP_MONSTER_DAMAGE), ATLAS_GUARDIAN_POWER, MAP_ABYSS_DAMAGE, MAP_ABYSS_LEADER, ATLAS_ABYSS_POWER.
- **Доказательство (тест `caster`):** `CRYPT_WRAITH MOB_ICE_BOLT ATTACK_MAGICAL 10.45->10.45 spellHit 36.57 -> 43.59 (x1.192, expected x1.32)` при MAP_MONSTER_DAMAGE 32.
- **Последствия:** заклинатели на картах слабее, чем обещает строка; риск строки (0.6/%) платится полностью. Тексты врут.
- **Варианты:** (1) в `MapEffects`/`AbyssWaves` брать список ударов из правил (`GuardianFloor.STRIKES`, сделать публичным) — одна точка «что такое урон монстра»; (2) добавить `ATTACK_MAGICAL` в локальный список клиента.
- **Регрессионные тесты:** метаморфный: удар чар заклинателя × (1 + v/100) под MAP_MONSTER_DAMAGE v.
- **Рекомендация:** вариант 1. **Статус:** открыт.

## [STAT-MAP-04] «Эффект модификаторов карт» удваивает награду за риск у флаговых строк, не усиливая их, и растит счётные строки
- **Категория:** ECONOMY. **Приоритет:** P2. **Уверенность:** высокая (тест).
- **Файлы:строки:** `rules/.../roll/Loot.kt:265` (всё × atlasEffect), `:153` (`risk = Σ value × вес`), `:156-183` (`activeMap`: риск → количество и опыт); `content/campaign.json → maps.risk` (флаги: ONSLAUGHT 15, FORTIFY 12, MAGIC_MIN 12, TREASURE_GUARDS 12, NO_MANA_REGEN 20); потребители флагов проверяют только `> 0` (`MapEffects.kt:77-78`, `Run.kt:546`, `Run.guards`).
- **Факт:** флаг 1 → 1.96: эффект тот же, риск-награда ×1.96. Счётные: `MAP_ITEM_LEVEL` 5 → 9.8 (+9 к уровню вещей), `MAP_CHESTS` 3 → 5.9 (`.toInt` → 5), `MAP_TREASURE_GUARDS` 2.0. Также умножаются строки тира (`tiers.effects`: QUANTITY/RARITY/PACK/MONSTER_LIFE/DAMAGE) и качество карты (→ QUANTITY).
- **Доказательство (тест `extras`):** карта только из 5 флагов: `atlasEffect=1.0 risk=71 quantity=71 experience=71`; `1.5 → 106.5`; `1.96 → 142.0`.
- **Последствия:** карта «из флагов» при +96% даёт +71% количества и опыта сверху без роста опасности; MAP_ITEM_LEVEL выше потолка своего тира (+9 вместо +5, в пределах `maxItemLevel`).
- **Варианты:** (1) масштабировать только «числовые» строки (список флагов/счётных — в правилах, рядом с `MapFeatureStats.COUNTS`); (2) масштабировать всё, но риск считать по немасштабированным значениям флагов; (3) оставить как есть (решение владельца: «эффект карт» — мультипликатор награды).
- **Рекомендация:** (1), заодно решить, входят ли тир и качество в «модификаторы карт». **Статус:** открыт.

## [STAT-MAP-05] Скорость монстров карты не растит скорость чар: INCREASED на процент-стате `CAST_SPEED`
- **Категория:** BUG (+LOCALIZATION). **Приоритет:** P3. **Уверенность:** высокая (тест).
- **Файлы:строки:** `core/.../campaign/MapEffects.kt:64` (`INCREASED` к `ATTACK_SPEED` и `CAST_SPEED`); `combat/Combatant.kt:46` (`CAST_SPEED` — проценты: `1 + (cooldown + cast)/100`).
- **Факт:** у 200/251 монстров `CAST_SPEED` нет (база 0 → 0), у остальных 0.7–1.0 (→ +0.25 п.п.). Тест: `MAP_MONSTER_SPEED=25 … unchanged-target STOCK_CAST_SPEED=76/76`. Касается MAP_MONSTER_SPEED, ATLAS_MONSTER_SPEED (сливается в неё), проклятий Разлома CURSE_SWIFT, MUT_FRENZIED. Ru-текст и имя говорят только о скорости атаки («Скорость атаки монстров карты», «больше скорости атаки»), en — «Attack and Cast Speed».
- **Решено владельцем (CAST-01):** процент-статы монстрам — как у героя. Значит: `CAST_SPEED` строкой карты — `ADD`, не `INCREASED`.
- **Варианты:** (1) `MonsterEffect(CAST_SPEED, Op.ADD, v)`; (2) убрать чары из строки и поправить en-текст.
- **Рекомендация:** (1) + ru «скорости атаки и сотворения чар». **Статус:** открыт.

## [STAT-MAP-06] «Монстры: больше брони и уклонения» не действует на 54% монстров (INCREASED на нулевой базе), а риск платит всегда
- **Категория:** BUG / BALANCE. **Приоритет:** P2. **Уверенность:** высокая (тест + подсчёт контента).
- **Файлы:строки:** `core/.../campaign/MapEffects.kt:72`; `MonsterRoller.fold` (`(база+ADD)×(1+INC)`); `content/campaign.json → monsters` (база); `maps.risk.MAP_MONSTER_ARMOUR = 0.2`.
- **Факт:** у 135/251 монстров нет ни брони, ни уклонения. В тестовой стае: броня не изменилась у 49/76, уклонение у 53/76; при этом бой изменился (dealt 23.4→17.1) — строка работает только на «бронированных». Верхний ролл 80% → +16% количества и опыта за строку, половине карты безвредную.
- **Варианты:** (1) MORE/INCREASED к броне/уклонению + плоская прибавка по уровню монстра (ADD от `growth`), чтобы у всех было что растить; (2) превратить строку в «меньше получаемого физ. урона»/`PHYSICAL_REDUCTION` ADD; (3) снизить вес риска до доли монстров с базой.
- **Вопрос владельцу** — какая защита задумана (см. ниже). **Статус:** открыт.

## [STAT-MAP-07] «Меньше регенерации здоровья» карты — своя формула мимо `Recovery`: обходит дно регенерации и не трогает вампиризм
- **Категория:** ARCHITECTURE / BUG. **Приоритет:** P3. **Уверенность:** высокая (тест на уровне листа).
- **Файлы:строки:** `core/.../campaign/MapEffects.kt:101`; `rules/.../sheet/Recovery.kt:20-26`; `content/Campaign.kt:74-77` (`RecoveryPool.LIFE(LIFE_REGEN_LESS, NO_LIFE_REGEN, LIFE_REGEN_FLOOR)`); сила `POWER_ETERNITY_SEAM` → `STOCK_LIFE_REGEN_FLOOR` («Регенерация и вампиризм здоровья не снижаются ниже {v}%»).
- **Инвариант (RULES, «Восстановление запасов»):** «`Recovery.share` (лист: «меньше регенерации», запрет, дно …) множит регенерацию и вампиризм здоровья … Своих формул «меньше регенерации» ни клиент, ни сервер не держат».
- **Факт / доказательство (тест `extras`):** реген 10/с, дно 50%, MAP_HERO_REGEN 80: строка карты → `HEALTH_REGEN 2.0 × share 1.0 = 2.0/с`; через `LIFE_REGEN_LESS 80` → `10 × 0.5 = 5.0/с`. Вампиризм строкой не режется, хотя ru-текст «меньше восстановление здоровья» (en: «Life Regeneration»).
- **Варианты:** (1) строка карты пишет `STOCK_LIFE_REGEN_LESS` (+v), как остальные «меньше регенерации»; (2) оставить формулу, но поправить текст и учесть дно.
- **Рекомендация:** (1). **Статус:** открыт.

## [STAT-MAP-08] Характеристики без единого источника: MAP_FOUNTAINS, ATLAS_SKILL_LEVEL, MAP_HARSH_BARGAINS
- **Категория:** CONTENT (мёртвый код). **Приоритет:** P3. **Уверенность:** высокая (скан всего контента).
- **Факт:** ни модификатор, ни узел, ни таблица, ни сила не дают эти коды; потребители живы (`MapEffects.fountains`, `AtlasEffects.hero[SKILL_LEVEL]`, `FeatureStat.ALTAR_CURSES`). MAP_HARSH_BARGAINS — семейство удалено в `189b680`, но стат, `MapStat`, локаль и `FeatureStat.map` остались. ATLAS_SKILL_LEVEL — семейство `ATLAS_SKILL_LEVEL` есть, узлов нет.
- **Варианты:** (1) удалить стат/перечисление/локаль (проверка `StatRegistry.validate` требует согласованно); (2) дать источник (узел Атласа «+1 к уровню умений на картах», строку «+источник»).
- **Статус:** открыт; решение — владелец.

## [STAT-MAP-09] Карточка узла Атласа печатает «%» у счётных и флаговых строк
- **Категория:** LOCALIZATION / UI. **Приоритет:** P3. **Уверенность:** высокая (код).
- **Файлы:строки:** `core/.../atlas/AtlasFog.kt:81` (`AtlasEffects.flat`), `app/.../expedition/AtlasNode.kt:136` (`unit = if (stat in flat) "" else "%"`).
- **Факт:** в `flat` нет ATLAS_NODES (+2 узла), ATLAS_MERCHANT_STOCK (+1 вещь), ATLAS_ALTAR_PACTS (+1 пара), ATLAS_INFLUENCE_SHAPER/ELDER (флаги) → «+2% Узлы ремёсел», «+1% Только Создатель».
- **Варианты:** (1) единица — из правил (флаг/счёт у `AtlasStat`, как `MapFeatureStats.COUNTS`); (2) дописать коды в `flat`.
- **Статус:** открыт.

## [STAT-MAP-10] Тексты строк расходятся с действием (мелкие)
- **Категория:** LOCALIZATION. **Приоритет:** P3. **Уверенность:** высокая.
- MAP_MONSTER_SPEED ru: «скорости атаки» (действует и на чары; en верно) — см. STAT-MAP-05.
- MAP_HERO_REGEN ru: «меньше восстановление здоровья» — режет только регенерацию (и грамматика: «восстановления»); en — «Life Regeneration».
- MAP_HERO_DAMAGE, MAP_MONSTER_MODS, MAP_REINFORCEMENTS, ATLAS_ORB_CHANCE — нет `stat.template.*` (ru/en): на полке «на весь бой» MAP_HERO_DAMAGE (Разлом MUT_EMPOWERED, сбор узлов) печатается фолбэком «Карта: урон героя: +15».
- MAP_HERO_ATTACK_SPEED, MAP_HERO_LIFE: en «increased», действие — «more» (множитель итога); MAP_HERO_LIFE с отрицательным значением (проклятие Разлома −10) — «-10% больше максимум здоровья» (нет `.negative`).
- ATLAS_MONSTER_LIFE/DAMAGE en «more», сворачиваются как increased (в MAP_MONSTER_*).
- ATLAS_FLASK_RARE ru «за убийство редкого» — действует на редких и уникальных (`BattleLand.kt:341`, `rarity >= RARE`; en верно).
- ATLAS_BOSS_RESPAWN «возвращается на {v}% быстрее» — фактически время короче на v% (90% = в 10 раз быстрее).
- **Статус:** открыт.

## [STAT-MAP-11] Переполнение строк Атласа над потолком (мёртвые очки)
- **Категория:** BALANCE (вопрос). **Приоритет:** P3. **Уверенность:** высокая (код), величина — оценка обходом графа.
- ATLAS_BOSS_RESPAWN: Σ узлов 180, кап `atlas.respawnCap` 90 — половина ветки после капа не даёт ничего (≈45 очков до капа).
- Шансы через `dice.percent` (>100 = 100): ATLAS_MAP_AFFIX Σ162 (≈45 очков до 100), ATLAS_CRYSTAL_CHANCE Σ274 (≈45), ATLAS_CRYSTAL_ESSENCES Σ367 (≈40), ATLAS_CRYSTAL_TIER Σ195 (≈33). Дальше узлы этих строк мертвы; карточка/сумма этого не показывает.
- **Статус:** вопрос владельцу.

## [STAT-MAP-12] Бонусы Атласа действуют на каждом заходе (и вне карт-предметов), тексты — «на картах»
- **Категория:** DESIGN (вопрос). **Приоритет:** P3. **Уверенность:** средняя (код; намерение неизвестно).
- **Факт:** `CampaignService.context` (`:275-281`) кладёт `atlasBonuses + guild` в `RunContext.atlas` всегда; `Run` читает `context[ATLAS_PACK_SIZE/RARE_MONSTERS/QUANTITY/RARITY/GOLD/EXPERIENCE/...]` без проверки `active`. Клиент `ExpeditionRun` (`AtlasEffects.map(context.active?.effects.orEmpty(), context.atlas)`) кладёт дары героя Атласа (реген маны, фляги, подавление) тоже на любой заход. Часть механик сама проверяет `active != null` (уникалки тиров, следопыт, разброс уровня). Тексты: «на картах», «Герой на картах: …».
- **Статус:** вопрос владельцу (если «карта» = любая зона Похода — дефекта нет, только уточнить тексты).

## Не дефекты, но стоит знать
- MAP_HERO_ATTACK_SPEED безоружному герою ничего не даёт (в листе нет `ATTACK_SPEED`, бой берёт `unarmed.speed`): `armed 1.5 → 1.65; unarmed → 0.0 (фолбэк)`. Источник — только мутатор Разлома.
- MAP_MONSTER_MAGIC_MIN («Все монстры не ниже волшебных»): вес обычных = 0, и доля редких растёт с 1.9% до 10.6% (×5.6) — тест `spawnLines`. Текст об этом молчит; риск 12.
- MAP_HERO_BLOCK уводит блок в минус в листе (0 → −25); кламп в бою не проверен.
- MAP_HERO_DEGEN, MAP_HERO_LEECH — источники только в мутаторах Разлома (в пуле карт их нет).
- Клиентский `AtlasEffects.map` добавляет ATLAS_PACK_SIZE/ATLAS_RARE_MONSTERS в MAP_* эффекты захода; спавн их не читает (`Run` берёт атлас отдельно) — двойного счёта в спавне нет; в показе не проверено.

## Вопросы владельцу

**MAP_SEALED_GUARDIAN / ATLAS_MAP_EFFECT (STAT-MAP-01, 04)**
1. Должен ли «эффект модификаторов карт» трогать печать стража? а) нет — печать вне масштаба; б) да, но не выше 100%; в) да, наоборот — ослаблять (печать — вред).
2. Что ещё вне масштаба: а) только печать; б) печать + флаги (Натиск, Укрепление, «не ниже волшебных», стражи, без маны) + счётные (уровень вещей, сундуки, кристаллы, расщелины); в) масштабируется всё, но риск флагов — по исходному значению; г) как сейчас.
3. Входят ли строки тира (`tiers.effects`) и качество карты в «модификаторы карт»? а) да (как сейчас); б) нет — только аффиксы предмета.

**ATLAS_ALTAR_* (STAT-MAP-02)**
4. Ветка алтарей: а) вернуть строки из `189b680` (тайники/сокровищницы); б) вернуть алтари на карты; в) убрать ветку с возвратом очков.
5. 17 ALTAR-семейств, MAP_MONSTER_MODS, MAP_REINFORCEMENTS, ATLAS_ORB_CHANCE: а) удалить; б) оставить до возвращения алтарей.

**MAP_MONSTER_DAMAGE и др. (STAT-MAP-03)**
6. Урон чар монстров (`ATTACK_MAGICAL`) — часть «урона монстров» для всех строк карт/Атласа/Бездны/стражей? а) да (как в правилах `STRIKES`); б) нет, чары отдельной строкой карты.

**MAP_MONSTER_ARMOUR (STAT-MAP-06)**
7. Что значит строка для монстров без брони и уклонения? а) дать им базу (плоская прибавка от уровня), а строку оставить INCREASED; б) строка становится «меньше получаемого физ. урона» для всех; в) риск строки уменьшить (половина монстров её не несёт); г) оставить.

**MAP_HERO_REGEN (STAT-MAP-07)**
8. «Меньше регенерации здоровья» карты: а) через `LIFE_REGEN_LESS` (режет и вампиризм, уважает дно); б) только регенерация, но с дном; в) как есть, поправить текст.

**MAP_FOUNTAINS, ATLAS_SKILL_LEVEL, MAP_HARSH_BARGAINS (STAT-MAP-08)**
9. а) удалить коды; б) дать источники (узел «+1 к уровню умений на картах», суффикс «+источник»); в) оставить про запас.

**Переполнение Атласа (STAT-MAP-11)**
10. ATLAS_BOSS_RESPAWN (Σ180 при капе 90) и шансы Σ>100 (MAP_AFFIX, CRYSTAL_*): а) срезать сумму узлов до потолка; б) сверх 100% — второй бросок (вторая строка/кристалл); в) показывать «потолок достигнут» на карточке; г) оставить.

**Атлас вне карт-предметов (STAT-MAP-12)**
11. а) Атлас действует на всех заходах — поправить тексты («в Походе»); б) только на картах-предметах (`active != null`) — правка `Run`/клиента.

**MAP_MONSTER_MAGIC_MIN**
12. Рост доли редких ×5.6 при «не ниже волшебных» — задумано? а) да; б) нет: убирать обычных, перекладывая вес только в волшебных.

## Запущенные тесты

Копия клиента `/home/user/audit/work/stats-map/EF` (сабмодуль `backend` @ 3ec40bd2, его контент). Временный тест: `core/src/test/kotlin/com/sperance/exileforge/core/audit/AuditMapStatsTest.kt` (в репозитории не лежит).

Команда: `./gradlew --offline :core:test --tests "com.sperance.exileforge.core.audit.AuditMapStatsTest"` — 3 прогона, EXIT 0 (тест печатающий, без assert). Полный вывод: `/home/user/audit/work/stats-map/out.txt`.

Что делает тест (всё — реальные функции клиента и правил, фиксированные seed):
1. `monsters` — `Spawns(index, Run(zone C1_BRINE_CAVES, seed 1234)).packs(false, MapEffects.buffs({код: верхний ролл}) + bossBuffs)` против пустых баффов: 18 монстровых строк × 76 членов стай; какие статы изменились и у скольких цель осталась прежней; бой `Battle` (герой класса 0, ур. 12, seed 1234) с баффом и без. Счёт по контенту: 135/251 монстров без брони и уклонения, 200/251 без `CAST_SPEED`, 50/251 с `ATTACK_MAGICAL`.
2. `hero` — 27 строк героя (MAP_HERO_*, MAP_SKILL_COST, MAP_FLASK_CHARGES, дары Атласа): `MapEffects.hero(лист, {код: v})` → разница листа, `Combatant` (maxLife, attackSpeed, skillCost), бой.
3. `caster` — `monsterSkillDamage` заклинателя с MAP_MONSTER_DAMAGE 32 и без: ×1.192 вместо ×1.32.
4. `seal` — `LootRoller.mapEffects(карта с имплиситом печати, atlasEffect ∈ {1, 1.3, 1.67, 1.96})` → `Run.sealNeed/rareTotal/sealOpen(все жетоны)` на 40 семенах: при 1.67 и 1.96 босс заперт 40/40.
5. `altars` — `Run(...).features.all` при ATLAS_ALTAR_CHANCE +1000% на 300 семенах: 0 алтарей.
6. `extras` — дно регенерации против MAP_HERO_REGEN; MAP_HERO_ATTACK_SPEED с оружием/без; риск флаговой карты при atlasEffect 1/1.5/1.96 (71/106.5/142).
7. `spawnLines` — `Run.count` и доли редкости на 200 семенах: MAP_PACK_SIZE 35 (58.6→78.6 жетонов), ATLAS_PACK_SIZE 60 (→93.3), MAP_MONSTER_RARITY 80, MAP_MAGIC_MONSTERS 50, MAP_RARE_MONSTERS 50, ATLAS_RARE_MONSTERS 60, MAP_MONSTER_MAGIC_MIN; `atlasPowers`.

Ограничение боёв: тестовый герой (ур. 12, только стартовый узел) проигрывает каждый бой против стаи зоны C1; строки, чей эффект проявляется только при стихийном уроне героя, лечении, фляге или долгом бое (сопротивления монстров, пробивание, вампиризм монстров, стойкость к оглушению, перезарядка, фляги), подтверждены на уровне стата/листа, исход боя не менялся — это не доказательство «не работает».

## Что проверено, что нет

Проверено: весь контент на источники 144 кодов (скрипт по всем `content/*.json`), потребители в `rules/src/main`, `src/main/kotlin` сервера, `core/src/main`, `app/src/main`; локаль сервера ru/en и клиента ru (имена, шаблоны, описания); `MapEffects` целиком; `LootRoller.mapEffects/activeMap/risk/atlasPowers/mapPerils/mapSeal`; `Run` (спавн, печать, добыча-атлас); `MapFeatures` (алтари, торговец, ловушки, комнаты, Скверна, узлы); `AtlasEffects`; `AbyssWaves`; карточка узла Атласа; история `atlas.json`/`modifiers.json` (189b680, c814e9f).

Не проверено:
- метаморфные тесты добычи для «работает (код)» (ATLAS_QUANTITY/RARITY/GOLD/EXPERIENCE/BOSS_LOOT/CHEST_LOOT/OMENS/…, MAP_GOLD/BOOKS/ABYSS_*): потребитель найден и прочитан, исход добычи не сравнивался;
- показ строк карты в окне запуска/сводке (видит ли игрок значения после ×ATLAS_MAP_EFFECT) — кроме печати (показ 60%, действует ×);
- кламп отрицательного блока/сопротивлений в бою; эффект MAP_HERO_FLASK_EFFECT/BUFF_DURATION/FLASK_CHARGES в бою с флягами и баффами;
- угрозы тиров (`mapPerils`) — бросок только прочитан кодом (одна угроза в контенте: MAP_HERO_NO_MANA_REGEN T11+, 10%); флаг работает (тест листа);
- `seals` (`rules.json`): печати алхимии/влияния только прочитаны (`SealEffect`), строки `alchemy:map` — источник 8 ALC-семейств в таблице; отдельного теста печатей нет;
- серверная часть Мощи и правдоподобия под строками карт (не моя группа).
