# 19_STATS · группа `misc`: HERO (OUTSIDE / NONE / без раздела), FLASK, AURA

Аудитор: агент STAT-MISC. Дата: 2026-10-13. Репозитории не менялись; все прогоны — в копии `/home/user/audit/work/stats-misc/EF` (клиент с сабмодулем `backend/`, совпадает с `/home/user/ktor-bestgame` побайтно по `rules/src` и `content`).

Охват: 68 характеристик `stats.json` — 41 HERO (раздел `OUTSIDE`, `NONE` или только `partOf`), 20 FLASK, 7 AURA.

Обозначения столбца «тест»: A, A2, B, C, D, D2, E, F, G, H — методы `StatMiscAuditTest`; P — `StatMiscProbeTest`; «код» — проверено чтением кода, тестом не гонялось.

## Сводка

| Итог | Кол-во | Характеристики |
|---|---|---|
| Работает (проверено тестом) | 44 | почти все FLASK, бонусы героя к флягам, добыча, ауры, сопротивления, регенерация, свет, скорость, сундуки, часть работы |
| Работает (проверено только кодом) | 17 | BORROW_SKILLS, WORK_YIELD/EXPERIENCE/FIND/DOUBLE/SAVE/RARITY/ITEM_LEVEL/UNIQUE, VEILED_LOOT, FATE_ASH_LOOT, SKILL_EXPERIENCE, WORN_EMPTY_SLOTS/UNIQUES/CORRUPTED, FLASK_HITS_CURSE, FLASK_NO_CHARGE_CHANCE (лист проверен тестом, бросок — кодом) |
| Работает частично | 5 | STOCK_MOVEMENT_SPEED (баффы сил в бою отбрасываются), STOCK_FLASK_RECOVERY и STOCK_FLASK_CHARGES_GAINED (SET-штрафы сил обходятся), STOCK_FLASK_LIFE_RECOVERY (обходит штраф «Неутолимости»), STOCK_HEALTH_ON_KILL_PERCENT (в бою работает, в сводке подписан неверно) |
| Мёртвые | 2 | STOCK_CURSE_WHEN_HIT (нет ни одного источника), STOCK_VEILED (заглушка скрытой строки по замыслу, без потребителя) |

Самое важное:
1. **STAT-MISC-01**: бонусы к флягам с пояса и из 54 строк дерева подписаны «этой фляги», хотя действуют на все фляги.
2. **STAT-MISC-02**: «Неутолимость» на мифическом поясе (фляги не лечат) обходится свойствами самой фляги и узлами дерева: восстановление фляги остаётся на уровне 84% и даже 228%.
3. **STAT-MISC-03**: «Утонувшие фляги» (фляги не получают зарядов) тоже обходятся.
4. **STAT-MISC-04**: сила «Прокладка пути» на двух уникалках мёртвая.

## Таблица характеристик

Ед. = `StatDef.unit` (в скобках `kind`, если он отличается). «Макс» — лучший тир и потолок уникалки, «дерево Σ» — сумма всех узлов дерева с этой строкой (верхняя граница, не реальный билд).

| код | имя ru (сервер / клиент) | ед. | ops | источники / макс | потребители | тест | капы | показ | статус |
|---|---|---|---|---|---|---|---|---|---|
| STOCK_RESIST_ALL | Все сопротивления стихиям / то же (+«, %» — срезается) | PERCENT (NUMBER) | ADD | суффикс 24, крафт 13, имплисит 14, порча 18 (проклятие −12), самоцвет 8, Shaper 19, HC 15; уникалки до 48 (−20 у штрафных); дерево Σ 221 (50 узлов); закон; зелье; фляга CRYSTAL_INFUSION 10 в глотке; монстры MOB_WARDED 62, BOSS_SIEGE 62, MOB_CELESTIAL 35; силы (PER/HEX/BUFF) | Combatant.rawResist (герой и монстр), Might, StatLimits, MapEffects, ResistBalance | G: огонь по герою 491→246 при 50%, по монстру 980→490 | resistCap 75, hardCap 90, пол −50 (`resistFloor`) | раздел NONE: своей строки нет, входит в строки огня/холода/молнии (`FightReading`, `StatLimits`) | работает |
| STOCK_MOVEMENT_SPEED | Скорость передвижения / **Скорость движения** | PERCENT | INCREASED | ботинки 46, крафт 29, имплисит 17, порча 30 (проклятие −12), самоцвет 5, Shaper 18; уникалки до 55; дерево Σ 100; фляги (ртутная 40, серебряная 20, уник. 50) в глотке; карта HERO_HASTE; силы PER/BUFF | ExpeditionWorld.heroSpeed (карта); в бою — только конверсии сил (TAILWIND, DEVOTED_SWIFTNESS) | F; P (TRAILBLAZE) | ×0.5..×2.5 (`coerceIn` в heroSpeed), в сводке не виден | «30.00%» (preciseStats), имя расходится с сервером | частично: баффы сил в бою срезаются (MISC-04) |
| STOCK_MANA_REGEN | Скорость регенерации маны / то же | PERCENT | INCREASED | суффикс 71, уникалки до 60 (−30); дерево Σ 237; закон; атлас ATLAS_MANA_REGEN; карта (меньше регенерации маны); сила MOONWELL_CALM | Recovery.manaRegen (бой, Мощь, сводка) | G: 5→8.55 при +71%, при −150% → 0 | max(0, 1+v/100) | часть строки MANA_REGEN_FLAT, «увеличение N%» — верно | работает |
| STOCK_LIFE_REGEN_PERCENT | Регенерация здоровья, % | PERCENT (NUMBER) | ADD | суффикс фляги MENDING 3 (в глотке), Бездна 0.54, условный 0.72, уникалки до 0.75; дерево Σ 1.5; дар; закон; монстры MOB_REGENERATING и др. до 2.0 | Combatant.regen(LIFE) (герой и монстр), Might, RoadLife | G: 2% от 1000 = 20/с, в бою 500→581 за 5 с; у монстра 20/с | max(0); `Recovery.share` | часть HEALTH_REGEN «N% от максимума» — верно | работает |
| STOCK_ENERGY_REGEN_PERCENT | Регенерация энергощита, % | PERCENT (NUMBER) | ADD | шлем 1.5, самоцвет 0.5 | Combatant.regen(SHIELD), EnergyShield | G: 2% → 20/с | max(0) | часть ENERGY_REGEN «от максимума» — верно | работает |
| STOCK_GOLD | Золото / то же | PERCENT | INCREASED | кольцо 33, порча 30 (проклятие −25), уникалка 45; дерево Σ 28; сила GILDED_GREED (доля редкости → золото) | Powers.loot → RunContext → Run.boost.gold (карты, испытания, Разлом, башня, Бездна) | E: tower gold 28 894→43 341 (+50%); −150% → 0; GREED 50% от 100 редкости → +50 | `coerceAtLeast(0)` у множителя | сводка = Powers.loot (лист + силы) — как сервер | работает |
| STOCK_EXPERIENCE | Опыт / то же | PERCENT | INCREASED | префикс 7, порча 6, самоцвет 3, уникалки до 21; дерево Σ 17 | Powers.loot → RarityBonus.experience → Run (убийства, башня, Бездна) | E: 117 720→176 579 (+50%) | нет | как сервер | работает (вопрос MISC-13) |
| STOCK_RARITY | Редкость добычи / то же (+«, %») | PERCENT | INCREASED | суффикс 33, префикс 30, порча 28, имплисит 16, самоцвет 10, Shaper 30, уникалки до 100; дерево Σ 34; силы PER | Run.boost.rarity, LootRules.uniqueRarity | E: r=100 → множитель уникалки ×1.154 | уникалка насыщается формулой `1+2r/(r+1200)` | как сервер | работает |
| STOCK_QUANTITY | Количество добычи / то же | PERCENT | INCREASED | суффикс 13, порча 12, Elder 9, уникалки до 39; дерево Σ 1 узел | Run.boost.quantity, LootBoost.count | E: q=50 проходит в boost | count ≥ 0 | как сервер | работает |
| STOCK_LIGHT_RADIUS | Радиус света / то же | NUMBER | INCREASED | база класса 7 (`sharedBase`); суффикс 35, проклятие −30, уникалки +35 и −40 | ExpeditionWorld.lightRadius | F: +35% → 9.5; −40% → 4.2; +500% → 14 (кап) | 2..14 (`min/maxLight`); ≤ −100% → снова 7 (MISC-12) | сводка печатает лист без капа | работает |
| STOCK_CHEST_QUANTITY | Количество сундуков / то же | PERCENT | INCREASED | суффикс 35, крафт 21, уникалки 30; дерево Σ 48 | сервер CampaignService → Chests.window (при пересчёте окна сундуков) | F: средний счёт 0.98→1.33→1.98→3.48 при 0/35/100/250% | bonus ≥ 0 | сводка = лист; действует только при новом окне | работает |
| STOCK_LOW_LIFE_SPEED | Скорость при ранении / то же | NUMBER | ADD | только монстры: MOB_ESS_RAGE 12..31 | BattleTick.foeLines (+INCREASED скорости атаки ниже 50% здоровья) | P: скорость атаки 1.0→1.31, удары за 20 с 20→26 | нет | NONE | работает (только монстр) |
| STOCK_BORROW_SKILLS | Чужие умения / то же | NUMBER | ADD | только монстры: MOB_ESS_INSANITY 1 | Spawns.skilled (+1 умение монстров и 40 маны) | код | — | NONE | работает (код) |
| AURA_RESIST | Аура: сопротивления | NUMBER | ADD | монстры RARE+: WITHERING 39, BOSS_DREAD 39, ESS_LOATHING 19, ENTROPIC 31 | Combatant.under → герой: −v к RESIST_ALL и RESIST_CHAOS | H: 75→45, хаос −30; гибель носителя снимает ауру | нет; пол сопротивления −50 | журнал (LineKind.AURA) | работает |
| AURA_DAMAGE_TAKEN | Аура: получаемый урон | NUMBER | ADD | CRUSHING 31, BOSS_DREAD 39 | +v к DAMAGE_TAKEN героя | H | нет | журнал | работает |
| AURA_SLOW | Аура: замедление | NUMBER | ADD | SLOWING 31, BOSS_WARLORD 39, ESS_DELIRIUM 25 | скорость атаки героя ×max(0.1, 1−v/100); чары не трогает (см. CAST-02) | H: 2.0→1.4 | ≥0.1× | журнал | работает |
| AURA_RECOVERY | Аура: восстановление | NUMBER | ADD | DRAINING 62, ABYSS_DRAINING 55 | −v к RECOVERY_RATE (регенерация, вампиризм, здоровье за удар и убийство, щит); фляги не трогает (по RULES) | H: recoveryRate 1.0→0.7 | recoveryRate ≥ 0 | журнал | работает |
| AURA_WEAKEN | Аура: слабость | NUMBER | ADD | ESS_FEAR 12, ESS_SCORN 25, WHISPERING 22 | DAMAGE героя ×max(0, 1−v/100) | H: −30; 5 аур по 25 → −100 (герой без урона) | ≥0 (MISC-07) | журнал | работает |
| AURA_CRIT | Аура: сомнение | NUMBER | ADD | ESS_DOUBT 38 | шанс крита героя ×(1−v/100) | H: 40%→28% | ≥0 | журнал | работает |
| AURA_COOLDOWN | Аура: несчастье | NUMBER | ADD | ESS_MISERY 38 | −v к COOLDOWN_RECOVERY | H: recovery 1.0→0.7 | 0.1..recoveryMax | журнал | работает |
| STOCK_WORK_SPEED | Скорость работы | PERCENT | INCREASED | инструменты 28/30, имплисит, порча 15; дерево Σ 18; дар; гильдия | Work.cycleMillis | F: 300 000→200 000 мс при +50% | max(0) | сводка — лист без инструмента (сервер добавляет инструмент) | работает |
| STOCK_WORK_YIELD | Лишняя добыча | PERCENT (NUMBER) | ADD | инструменты 50, порча 20; дерево Σ 31 | Work.settle extraUnits | код | max(0) | то же | работает (код) |
| STOCK_WORK_LUCK | Удача в работе | PERCENT (NUMBER) | ADD | инструменты 50; дерево Σ 54 | Work.nothingChance | F: 25%→12.5%→2.5% (кап 90) | luckCap 90 | то же | работает |
| STOCK_WORK_EXPERIENCE | Опыт профессии | PERCENT | INCREASED | инструменты 36; дерево Σ 60 | Work.settle xp | код | max(0) | то же | работает (код) |
| STOCK_WORK_FIND | Побочные находки | PERCENT | INCREASED | инструменты 36; дерево Σ 50 | Work.findChance | код | итог ≤ 100 | то же | работает (код) |
| STOCK_FLASK_CHARGES_GAINED | Заряды фляг | PERCENT | INCREASED | пояс 41, фляга REPLENISHING 72 (своя), уникалки +25 и −35; дерево Σ 212; атлас; карта (−); сила DROUGHT (SET −100) | FlaskSheet.gained (убийства) | A: +41% → ×1.41, своя +72% → ×1.72; A2 | итог ≥ 0 | сводка (герой); на фляге — «этой фляги» | частично: SET силы обходится (MISC-03); текст пояса и дерева неверен (MISC-01) |
| STOCK_FLASK_DURATION | Длительность фляг | PERCENT | INCREASED | пояс 35, фляга LASTING 64, эссенция 30, уникалки до 54; дерево Σ 180; атлас; сила FLOW MORE 1000 | FlaskSheet.duration | A: 3→4.05 / 4.92 с; C | ≥ minDuration 0.5 с | то же | работает; MISC-01, MISC-08 |
| STOCK_FLASK_EFFECT | Эффект фляг | PERCENT | INCREASED | пояс 18, фляга CONCENTRATED 36, порча 10, уникалки до 35; дерево Σ 80; карта (−) | FlaskSheet.effect (строки глотка; у полезной фляги + качество) | A: ×1.18 / ×1.36; строка ртутной 40→54.4 | ≥0 | то же | работает; MISC-01 |
| STOCK_FLASK_CHARGES_USED | Расход зарядов | PERCENT | ADD | пояс 18, фляга ECONOMICAL 36; дерево Σ 40 | FlaskSheet.perUse | A: 10→8.2 / 6.4 | расход ≥ 0; потолка снижения нет (реальный максимум ≈94%) | «Расход зарядов 30%» означает −30% (MISC-11) | работает; MISC-01 |
| STOCK_FLASK_LIFE_RECOVERY | Восстановление флягами жизни | PERCENT | INCREASED | пояс 47; дерево Σ 144 | FlaskSheet.recovery (только фляги здоровья) | A: 1800→2646; A2: на ману не действует | ≥0 | то же | частично: обходит «Неутолимость» (MISC-02); MISC-01 |
| STOCK_FLASK_RECOVERY | Восстановление фляги | PERCENT | INCREASED | фляга SATURATED 64 (своя); сила THIRST (SET −100) | FlaskSheet.recovery (здоровье и мана) | A: 1800→2952, 380→623; на полезной фляге не действует (её нет в её таблице) | ≥0 | то же | частично: SET силы не перекрывает свои строки фляги (MISC-02) |
| STOCK_FLASK_CHARGES_PER_KILL | Заряды за убийство | NUMBER | ADD | фляга BOUNTIFUL 3 (своя), эссенция 1, уник. фляга 1 | FlaskSheet.gained | A: 1→4; A2: +30 → 31 | итог ≥ 0 | «этой фляги» на фляге — верно | работает |
| FLASK_CHARGES | Заряды | NUMBER | ADD | база 30/40; CAPACIOUS 58 | maxCharges (≥1), бой, ёмкость Разлома | A: 30→88 | ≥1 | карточка (лист фляги) | работает |
| FLASK_CHARGES_PER_USE | Зарядов за глоток | NUMBER | ADD | база 8/10/15/20 | perUse | A | — | карточка | работает |
| FLASK_DURATION | Длительность | NUMBER | ADD | база 3–5 с; порча +1; уникалки +2 / −1 | duration | A: 3→4 / 5 / 2 | ≥0.5 с | карточка | работает |
| FLASK_LIFE | Восстанавливает здоровья | NUMBER | ADD | база 60..2000 | draught, вид фляги LIFE | A, D2 | — | карточка | работает |
| FLASK_MANA | Восстанавливает маны | NUMBER | ADD | база 30..420 | draught, вид MANA | A | — | карточка | работает |
| FLASK_INSTANT | Восстановление сразу | NUMBER (по смыслу %) | ADD | BUBBLING 55, SEETHING 100, уникалки 50/100 | FlaskSheet.instant | A: 0→0.55 / 1.0 | 0..1 | текст «N% восстановления сразу» верен; unit NUMBER (MISC-14) | работает |
| FLASK_CHARGE_ON_CRIT | Заряд при крите | NUMBER | ADD | SURGEONS 36 | Battle.flaskCharge | D: 0→5 зарядов за 10 с (14 критов) | — | текст | работает |
| FLASK_CHARGE_WHEN_HIT | Заряд от удара | NUMBER | ADD | RESPONSIVE 36 | flaskCharge | D: 0→8 | — | текст | работает |
| FLASK_LIFE_TO_SHIELD | Здоровье в щит | NUMBER | ADD | SHIMMERING 72 (только фляги здоровья) | draught.shield | D2: 1296 щита сразу (72% от всех 1800) | щит ≤ максимума | текст «восстановленного» (MISC-09) | работает |
| FLASK_LIFE_TO_MANA | Здоровье в ману | NUMBER | ADD | TWIN 36, уник. 20 | draught.mana | D2: 648 сразу | — | MISC-09 | работает |
| FLASK_LOW_LIFE_RECOVERY | Восстановление при ранении | NUMBER | ADD | DESPERATE 86, уник. 40 | draught (ниже 35%) | D2: 600→1116/с при 200/1000 | — | текст | работает |
| FLASK_DURATION_PER_KILL | Длительность за убийство | NUMBER | ADD | LINGERING 1.7 (только полезные) | BattleLand: продлевает `until` глотка | D: 5.02→13.52 с за 5 убийств | — | текст | работает (лечение за время не продлевается; у полезных его нет) |
| FLASK_NO_CHARGE_CHANCE | Глоток без зарядов | NUMBER | ADD | порча 15 | BattleSkills.drink | A (лист 15); бросок — код | — | текст | работает |
| FLASK_SIP_MANA | Мана за глоток | NUMBER | ADD | порча 5, уник. 10 | draught.mana | D2: 10 из 200 | — | текст | работает |
| FLASK_SIP_SHIELD | Щит за глоток | NUMBER | ADD | уник. 30 | draught.shield | D2: 150 из 500 | — | текст | работает |
| FLASK_AUTO_LOW_LIFE | Глоток сам | NUMBER | ADD | уникалки 20/30 | useFlasks | D: пьётся при 15%, не пьётся при 35% (для порога 20%) | — | текст | работает |
| FLASK_USES_ALL | Тратит все заряды | NUMBER | ADD | уник. 1 | drink | D: 40→0 | — | текст | работает |
| FLASK_INVULNERABLE | Неуязвимость | NUMBER | ADD | уник. 2 с | drink → invulnerableUntil | D: true | — | текст | работает |
| FLASK_SKILLS_FREE | Умения без маны | NUMBER | ADD | уник. 1 | Battle.skillsFree | D: true в глотке | — | текст | работает |
| FLASK_HITS_CURSE | Проклинающие удары | NUMBER | ADD | уник. 1 | BattleLand.hexing → hex (проклятия класса kit.curses) | D (флаг на листе); наложение — код | — | текст | работает (код) |
| STOCK_CURSE_WHEN_HIT | Проклятие при попадании / нет у клиента (берётся сервера) | PERCENT | — | **нет ни одного источника** в контенте | BattleLand:118, FightReading.CURSE_WHEN_HIT | код | — | часть CURSE_ON_HIT | **мёртвая** (MISC-06) |
| STOCK_WORN_EMPTY_SLOTS | Пустые слоты экипировки | NUMBER | считает WornCount | 0..11 мест | силы PER/MORE (HOLLOW_*) | код | — | NONE | работает (код) |
| STOCK_WORN_UNIQUES | Надетые уникальные предметы | NUMBER | WornCount | 0..N | LOYAL_BANNER и др. | код | капы сил | NONE | работает (код) |
| STOCK_WORN_CORRUPTED | Надетые осквернённые | NUMBER | WornCount | 0..N | ALTAR_*, CARESS, PROTOTYPE | код | капы сил | NONE | работает (код) |
| STOCK_WORN_JEWELS | Надетые самоцветы | NUMBER | WornCount | 0..N | RESISTANCES_PER_JEWEL (perStat), силы | P: 3×2 = 6, 3×5 = 15 к RESIST_ALL | — | NONE | работает |
| STOCK_FLASKS_AUTO | Бесконечные фляги | FLAG | ADD | сила WORLDBLOOD_FLOW | BattleTick: все фляги в начале боя, бесплатно | D: пьётся при 0 зарядов, заряды не тратятся; C | — | ✓ | работает |
| STOCK_HEALTH_ON_KILL_PERCENT | Восстановление здоровья при убийстве | PERCENT | ADD | суффикс 6, крафт 4, уник. 5; дар | BattleLand.kill: maxLife·v/100·recoveryRate | G: 4 убийства по 5% → +200 | — | **«увеличение 5%»** вместо «5% от максимума» (MISC-05) | частично (показ) |
| STOCK_WORK_DOUBLE | Двойной выход | PERCENT | ADD | инструмент 10, порча 6, уник. 12; дар | Work.settle | код | — | — | работает (код) |
| STOCK_WORK_SAVE | Экономия материалов | PERCENT | ADD | 12, порча 8 | Work.settle | код | — | — | работает (код) |
| STOCK_WORK_RARITY | Редкость изделий | PERCENT | ADD | 15 | CraftsService (WorkBonus.raise) | код | до редкой | — | работает (код) |
| STOCK_WORK_ITEM_LEVEL | Уровень изделий | NUMBER | ADD | 5 | CraftsService craftedItemLevel | код | ≥0 | — | работает (код) |
| STOCK_WORK_UNIQUE | Шанс шедевра | PERCENT | INCREASED | кузнец 50 | CraftsService uniqueChance | код | ≥0 | — | работает (код) |
| STOCK_VEILED | Скрыто | NUMBER | ADD 0 | VEILED_PREFIX/SUFFIX (0..0) | нет | код | — | NONE | мёртвая по замыслу (заглушка) |
| STOCK_VEILED_LOOT | Скрытая добыча | NUMBER | ADD | монстры MOB_VEILED 1 | Run.kill → veiled; не ложится сверх броска (LOOT_STATS) | код | — | NONE | работает (код) |
| STOCK_FATE_ASH_LOOT | Пепельный / нет у клиента | NUMBER | ADD | монстры MOB_ASHEN 20..30 | Run.kill (Пепел судьбы с шансом v%) | код | — | NONE | работает (код) |
| STOCK_SKILL_EXPERIENCE | Опыт умений / нет у клиента (берётся сервера) | PERCENT | ADD | суффикс 20 (шлем, амулет); сила SCHOLARS_INSIGHT | SkillGrowth.gain | код | — | OUTSIDE, «+N%» | работает (код); вопрос MISC-13 |

Свёртка: у героя — `Sheet`/`ModifierMath` (у PERCENT увеличение прибавляется как есть, у NUMBER умножает базу; базы ненулевые: свет 7, скорость 3.2 вне листа). У монстров здесь только ADD (ауры, LOW_LIFE_SPEED, BORROW, регенерация, сопротивления), поэтому баг «INCREASED на нулевой базе у монстров» эту группу не задевает. `StatLines.fold`: INCREASED скорости атаки при ранении ложится на ненулевую базу, проверено (P).

---

## Карточки дефектов

## [STAT-MISC-01] Бонусы к флягам с пояса и из дерева подписаны «этой фляги»
- **Категория:** LOCALIZATION / BUG. **Приоритет:** P2. **Уверенность:** высокая (проверено тестом B).
- **Подсистема:** текст модификаторов (`rules/text/ModifierText`), клиент и сервер.
- **Файлы:** `rules/.../sheet/FlaskSheet.kt:116` (`local = Slot.FLASK.tag in def.tags && stat.startsWith("STOCK_FLASK_")`), `rules/.../text/ModifierText.kt:32`; контент `modifiers.json`: семейства `INCREASED_FLASK_CHARGES_GAINED/DURATION/EFFECT/LIFE_RECOVERY`, `REDUCED_FLASK_CHARGES_USED` с тегом `flask`, таблица `belt`.
- **Инвариант (RULES.md, «Фляги»):** «Бонус к флягам на самой фляге (`STOCK_FLASK_*`, `FlaskSheet.local`) ложится в её лист, а не на героя: работает только на неё и печатается «этой фляги»».
- **Как работает:** признак «на самой фляге» берётся из тега описания, а не из места вещи. У пяти поясных семейств тег `flask`, поэтому на поясе и в 54 строках узлов дерева (13 длительность, 11 эффект, 14 заряды, 9 лечение, 7 расход) текст говорит «+N% к эффекту этой фляги», хотя бонус идёт в лист героя и действует на все фляги.
- **Доказательство (B):** `INCREASED_FLASK_EFFECT src=PREFIX tags=[flask] tables=[belt] local=[true] text=«+{0}% к эффекту этой фляги»`; `tree flask lines: {INCREASED_FLASK_DURATION=13, INCREASED_FLASK_EFFECT=11, INCREASED_FLASK_CHARGES_GAINED=14, INCREASED_FLASK_LIFE_RECOVERY=9, REDUCED_FLASK_CHARGES_USED=7}`. Клиент печатает тот же `ModifierText` (`core/display/Text.kt`, `Comparison.kt`).
- **Воспроизведение:** открыть карточку редкого пояса с «эффектом фляг» или узел дерева «Длительность фляг».
- **Последствия:** игрок думает, что бонус пояса или дерева действует на одну флягу; вводящий в заблуждение текст на частом аффиксе.
- **Варианты решения:**
  1. Снять тег `flask` с пяти поясных семейств (у них `tags` нужны только для таблиц и катализаторов). Проверить, что катализатор и сфера по тегу их не теряют.
  2. Решать `local` по месту носителя: `ModifierText.template(def, onFlask: Boolean)`, место передают карточка вещи и дерево.
  3. Ввести отдельный тег `flask-local` для фляжных префиксов (`FLASK_ECONOMICAL/REPLENISHING/LASTING/CONCENTRATED/SATURATED/BOUNTIFUL/CORRUPT_EFFECT`).
- **Побочные эффекты:** вариант 1 может поменять пулы сфер и катализаторов, если тег `flask` там читается.
- **Регрессионный тест:** для каждого описания с `STOCK_FLASK_*` из таблицы, отличной от `flask*`, и для каждой строки дерева текст не содержит «этой фляги».
- **Рекомендация:** вариант 2 или 3. **Статус:** открыт.

## [STAT-MISC-02] «Неутолимость» (фляги не лечат) обходится свойствами фляги и деревом
- **Категория:** BUG / EXPLOIT. **Приоритет:** P2. **Уверенность:** высокая (тест C).
- **Подсистема:** силы уникалок × лист фляги.
- **Файлы:** `powers.json:7832` (`POWER_WORLDBLOOD_THIRST`: `SET STOCK_FLASK_RECOVERY −100`), мифический пояс `VEIN_OF_THE_FIRST_WORLD` (ур. 80, вместе с `WORLDBLOOD_FLOW`); `FlaskSheet.kt:60` (`recovery`).
- **Ожидание:** текст силы — «Ваши фляги не восстанавливают здоровье и ману».
- **Как работает:** SET обнуляет только `STOCK_FLASK_RECOVERY` листа героя. В `recovery()` к нему прибавляются собственное увеличение фляги (`FLASK_SATURATED` до 64), качество (до 20) и `STOCK_FLASK_LIFE_RECOVERY` героя: это отдельная характеристика, SET её не трогает, в дереве её до 144. `FLASK_SIP_MANA` не зависит от восстановления совсем.
- **Доказательство (C):** `THIRST plain divine life: life=0.0` · `+ SATURATED T1 + q20: life=1512.0 (recovery=0.84)` · `+ 47% life recovery: life=846.0`. С узлами дерева на 144% восстановление выходит 1 + (−100 + 144 + 20 + 64)/100 = **2.28**, то есть выше, чем без пояса.
- **Последствия:** у пояса с сильнейшим плюсом (все фляги бесплатно в каждом бою и на 1000% дольше) штраф снимается вложением в дерево или аффиксом фляги. Фляга здоровья лечит каждый бой даром и долго.
- **Варианты:**
  1. Сделать запрет флагом (`STOCK_NO_FLASK_RECOVERY`), который `FlaskSheet.recovery` читает до сложения и возвращает 0.
  2. SET на обе характеристики плюс множитель «меньше 100%» поверх (`MORE −100` вместо SET).
  3. Оставить штраф частичным, но переписать текст («−100% восстановления фляг от героя»).
- **Регрессионный тест:** лист с силой → `recovery()` = 0 при любых строках фляги и герою; sip mana = 0.
- **Вопросы владельцу:** см. раздел вопросов. **Статус:** открыт.

## [STAT-MISC-03] «Утонувшие фляги» (фляги не получают зарядов) обходится
- **Категория:** BUG. **Приоритет:** P2. **Уверенность:** высокая (тесты C и D, код).
- **Файлы:** `powers.json:7925` (`POWER_UMBILICUS_DROUGHT`: `SET STOCK_FLASK_CHARGES_GAINED −100`), уникальный пояс `CORD_OF_THE_SUNKEN_GOD` (ур. 28); `FlaskSheet.kt:67` (`gained`); `BattleSkills.kt:363` (`flaskCharge`), `:399` (заряды из ответа умения), `BattleReach.kt:19` (`chargeFlasks`).
- **Как работает:** SET режет только увеличение героя. Своё увеличение фляги (`FLASK_REPLENISHING` 72) складывается с −100 и даёт ещё 0.72 базы. Плоские `+N за убийство` (фляга до 3, эссенция и пояс героя) прибавляются после множителя. Заряды при крите и при получении удара, от умений и сил, а также стартовые заряды не смотрят на характеристику вообще.
- **Доказательство (C):** `DROUGHT ... gained(base=1): plain=0.0 replenishing=0.72 bountiful=3.0; belt +3/kill: 3.0`; D: `FLASK_SURGEONS` даёт заряды при крите независимо от листа.
- **Варианты:**
  1. Флаг «фляги не получают зарядов», который проверяют `gained`, `flaskCharge`, `chargeFlasks` и ответ умения.
  2. `MORE −100` на весь итог `gained` и единый путь начисления зарядов (`Battle.chargeFlasks`) с этим множителем.
  3. Сузить текст до «−100% получаемых зарядов фляг за убийство».
- **Регрессионный тест:** при силе ни один путь начисления не меняет `charges`.
- **Статус:** открыт.

## [STAT-MISC-04] «Прокладка пути» мёртвая, часть «Броска монеты» про скорость передвижения мёртвая
- **Категория:** BUG / CONTENT. **Приоритет:** P3. **Уверенность:** высокая (тест P).
- **Файлы:** `core/.../campaign/BattlePowers.kt:309` (`BUFF` отбрасывает строки `STOCK_MOVEMENT_SPEED`: «Movement speed matters only on the map»); `powers.json:4323` (`POWER_TRAILBLAZE`: по убийству +v% скорости передвижения на 4 с), `:7708` (`POWER_LUCKDASH_GAMBLE`); уникалки `WORN_ROADS` (ур. 60), `RUNAWAYS_BOOTS` (ур. 34), `DICERS_SHOES`.
- **Как работает:** убийства бывают только в бою, а бой срезает строку скорости передвижения. Бафф не ложится и не переносится на карту.
- **Доказательство (P):** `POWER_TRAILBLAZE kills=4 heroEffects=[]`; `POWER_LUCKDASH_GAMBLE heroEffects=[BUFF:...:[STOCK_ATTACK_SPEED=7.14]]` — строки скорости передвижения нет.
- **Варианты:** (1) переносить бафф скорости передвижения на карту через `HeroCarry` на остаток длительности; (2) заменить силу на боевую (скорость атаки или уклонение); (3) убрать силу или переписать текст.
- **Статус:** открыт.

## [STAT-MISC-05] Сводка подписывает долю здоровья за убийство как «увеличение»
- **Категория:** BUG (показ). **Приоритет:** P3. **Уверенность:** высокая (тест P).
- **Файлы:** `core/.../display/Text.kt:114` (`partFigure`: часть вида `PERCENT` → «увеличение …»); `stats.json`: `STOCK_HEALTH_ON_KILL_PERCENT` `kind=PERCENT`, `partOf=STOCK_HEALTH_ON_KILL`.
- **Инвариант (RULES.md, сводка):** «строка с частями … «12 + 1.5% от максимума» (доля максимума) или «3 + увеличение 40%» (часть вида `PERCENT`)».
- **Как работает:** бой читает характеристику как долю максимума здоровья (`lifeOnKillShare`, тест G: 5% × 4 убийства = +200 при 1000 здоровья), а сводка печатает «увеличение 5%».
- **Доказательство (P):** `partFigure row=STOCK_HEALTH_ON_KILL part=STOCK_HEALTH_ON_KILL_PERCENT -> «увеличение 5%»`; у `STOCK_LIFE_REGEN_PERCENT` (`kind NUMBER`, `unit PERCENT`) печатается правильно: «5% от максимума».
- **Варианты:** (1) в `stats.json` сменить `kind` на NUMBER и `unit` на PERCENT, как у регенерации (свёртка не изменится: источники только ADD); (2) в `partFigure` различать «долю» явным признаком `StatDef` (например, `share: true`).
- **Регрессионный тест:** у каждой пары `partOf`, где бой читает долю максимума, текст «от максимума».
- **Статус:** открыт.

## [STAT-MISC-06] STOCK_CURSE_WHEN_HIT — мёртвая характеристика
- **Категория:** CONTENT. **Приоритет:** P3. **Уверенность:** высокая (дамп всех описаний, grep контента).
- **Файлы:** `BattleLand.kt:118` (читает `CURSE_ON_HIT + CURSE_WHEN_HIT`), `CombatSheet.kt:64` (`FightReading.CURSE_WHEN_HIT`); комментарий в коде ссылается на «эссенцию 1.57.0», но ни одно описание `modifiers.json`, `essences.json`, уникалок и сил её не даёт. У клиента нет `enum.stat.STOCK_CURSE_WHEN_HIT` (подставляется серверное имя).
- **Варианты:** (1) вернуть источник (строку эссенции «Ужаса» на нагруднике); (2) удалить характеристику, `FightReading` и ключи локали; (3) оставить заделом.
- **Статус:** открыт.

## [STAT-MISC-07] Ауры монстров складываются без потолка: «слабость» до −100% урона героя
- **Категория:** BALANCE / DESIGN. **Приоритет:** P3. **Уверенность:** высокая по механике (тест H), низкая по частоте (не мерилась).
- **Файлы:** `Battle.kt:300` (`auras()` суммирует по живым врагам), `Combatant.kt:215–232`.
- **Как работает:** `AURA_WEAKEN` суммируется и даёт `DAMAGE = (100+d)·max(0, 1−Σ/100) − 100`; при Σ ≥ 100 герой не наносит урона. `AURA_RESIST` уводит сопротивления к полу −50. `AURA_DAMAGE_TAKEN` ничем не ограничена. `AURA_SLOW` ограничена 0.1×. Ауры есть только у RARE+ (эссенции до 25, Бездна «Шепчущий» до 22 с весом 280).
- **Доказательство (H):** `5x(WEAKEN 25, RESIST 39): hero DAMAGE=-100.0 RESIST_ALL=-120.0 fireResist=-0.5`.
- **Варианты:** (1) брать максимум, а не сумму одной ауры (как в PoE — один источник одной ауры); (2) потолок суммы в `rules.json` (например, 75%); (3) оставить как есть.
- **Статус:** вопрос владельцу.

## [STAT-MISC-08] «Бесконечный ток» (+1000% длительности) не умножает своё увеличение фляги
- **Категория:** DESIGN. **Приоритет:** P3. **Уверенность:** высокая (тест C).
- **Как работает:** MORE 1000 ложится на процент героя `STOCK_FLASK_DURATION` (`(100+35)·11−100 = 1385`), а `duration()` складывает его со своим увеличением фляги. В итоге `LASTING` (+64%) даёт 77.45 с вместо 109.45 с при честном «больше».
- **Варианты:** (1) отдельный множитель «больше длительности фляг» в `FlaskSheet.duration`; (2) оставить: свойство фляги в 11 раз слабее, чем у героя.
- **Статус:** вопрос владельцу.

## [STAT-MISC-09] «Здоровье в щит» и «в ману» берут всю номинальную порцию фляги сразу
- **Категория:** DESIGN / LOCALIZATION. **Приоритет:** P3. **Уверенность:** высокая (тест D2).
- **Как работает:** `Loadout.kt:316–317` даёт `lifeAmount · %` маной или щитом сразу при глотке, даже при полном здоровье и хотя само здоровье идёт за длительность. Текст: «{v}% **восстановленного** здоровья…».
- **Доказательство:** `FLASK_SHIMMERING`: `life=0 lifeRate=600 shield=1296` при здоровье 900/1000.
- **Варианты:** (1) считать от фактически вылеченного (в `lifeBack`/`recoveries`); (2) переписать текст: «… от лечения фляги».
- **Статус:** вопрос владельцу.

## [STAT-MISC-10] Скорость передвижения: расходятся имена, лишние знаки
- **Категория:** LOCALIZATION. **Приоритет:** P3. **Уверенность:** высокая (тест P).
- **Как работает:** сервер называет характеристику «Скорость передвижения», шаблоны говорят «к скорости передвижения», а клиент `ui_ru.json` пишет «Скорость движения». `preciseStats` печатает «30.00%».
- **Варианты:** одно имя (убрать клиентский ключ), убрать `MOVEMENT_SPEED` из `preciseStats`.
- **Статус:** открыт.

## [STAT-MISC-11] «Расход зарядов 30%» в сводке означает снижение
- **Категория:** LOCALIZATION. **Приоритет:** P3. **Уверенность:** высокая (тест P).
- **Как работает:** характеристика — снижение расхода (`−{v}% к расходу`), а строка сводки «Расход зарядов 30%» читается как «тратит 30%».
- **Варианты:** переименовать в «Экономия зарядов фляг» или печатать со знаком «−30%».
- **Статус:** открыт.

## [STAT-MISC-12] Потолки света и скорости не видны; свет ≤ −100% возвращает базу
- **Категория:** BUG (P3) + показ. **Уверенность:** высокая (тест F, код `ExpeditionWorld.kt:453/456`).
- **Как работает:** радиус ограничен 2..14, скорость — ×0.5..×2.5, а сводка печатает лист без потолка (+500% света при пределе 14). `lightRadius` берёт `takeIf { it > 0 } ?: defaultLight`: лист с ≤ −100% света (проклятия порчи −30 и уникалка −40) получает радиус 7, а не 2.
- **Варианты:** (1) `coerceAtLeast(0)` вместо возврата к базе; показ потолка, как у сопротивлений (`StatLimits`); (2) оставить.
- **Статус:** открыт.

## [STAT-MISC-13] Опыт героя повышает и опыт умений (двойной счёт?)
- **Категория:** DESIGN (вопрос). **Приоритет:** P3. **Уверенность:** высокая по коду.
- **Как работает:** `HeroRules.kt:54` передаёт `reward.experience`, уже умноженный на `STOCK_EXPERIENCE` (`Run.experienceFor`), в `SkillGrowth.gain`, где сверху ещё `× (1 + STOCK_SKILL_EXPERIENCE)`.
- **Варианты:** (1) так задумано — оставить и описать в `stat.desc`; (2) кормить умения опытом без бонуса героя.
- **Статус:** вопрос владельцу.

## [STAT-MISC-14] У процентных характеристик FLASK unit по умолчанию NUMBER
- **Категория:** DATA. **Приоритет:** P3. **Уверенность:** средняя (сейчас нет пути показа через `statValue`).
- **Как работает:** `FLASK_INSTANT`, `CHARGE_ON_CRIT/WHEN_HIT`, `LIFE_TO_*`, `LOW_LIFE_RECOVERY`, `NO_CHARGE_CHANCE`, `SIP_*`, `AUTO_LOW_LIFE` — проценты, но `unit=NUMBER` («Восстановление сразу 30»). Флаги `USES_ALL`, `SKILLS_FREE`, `HITS_CURSE` печатаются числом 1. Карточка берёт текст шаблона и сейчас верна.
- **Варианты:** проставить `unit` в `stats.json` (для флагов — расширить `FlagStats` или ввести `kind BOOL`), либо оставить до первого экрана, который их напечатает.
- **Статус:** открыт.

## [STAT-MISC-15] Атлас и карта пишут «больше», а складывают как увеличение
- **Категория:** LOCALIZATION. **Приоритет:** P3. **Уверенность:** высокая по коду (`AtlasFog.kt:70–77`, `MapEffects.kt:118, 127`).
- **Как работает:** `ATLAS_FLASK_CHARGES` («{v}% больше получаемых зарядов фляг»), `ATLAS_FLASK_DURATION` и `MAP_HERO_HASTE` («{v}% больше скорость передвижения») прибавляются к процентам героя, то есть работают как увеличение, а не как множитель.
- **Варианты:** текст «+{v}% к …» или настоящий множитель.
- **Статус:** открыт.

---

## Вопросы владельцу

**STOCK_FLASK_RECOVERY / STOCK_FLASK_LIFE_RECOVERY — «Неутолимость» (MISC-02)**
- Что должна делать сила на поясе «Жила первого мира»?
  - а) Полный запрет восстановления флягами здоровья и маны (флаг, режет всё, включая «глоток маны»).
  - б) −100% только к бонусам героя (поправить текст).
  - в) Запрет только здоровья: фляги маны пусть работают.

**STOCK_FLASK_CHARGES_GAINED — «Утонувшие фляги» (MISC-03)**
- Какие заряды режет сила?
  - а) Все пути: убийства, крит, удар, умения, Разлом.
  - б) Только заряды за убийство, включая плоские +N.
  - в) Только процент героя (как сейчас; поправить текст).

**STOCK_MOVEMENT_SPEED — «Прокладка пути», «Бросок монеты» (MISC-04)**
- Как быть с баффами скорости передвижения из боя?
  - а) Переносить бафф на карту на остаток длительности.
  - б) Заменить «Прокладку пути» боевой силой.
  - в) Убрать силу или строку из «Броска монеты».

**AURA_* — сложение аур (MISC-07)**
- Как складывать одинаковые ауры нескольких монстров?
  - а) Сумма без потолка (как сейчас).
  - б) Сумма с потолком в правилах (например, 75%).
  - в) Берётся сильнейшая аура каждого вида.

**STOCK_FLASK_DURATION — «Бесконечный ток» (MISC-08)**
- Должны ли +1000% умножать и свойства самой фляги?
  - а) Да — отдельный множитель «больше».
  - б) Нет — оставить.

**FLASK_LIFE_TO_SHIELD / FLASK_LIFE_TO_MANA (MISC-09)**
- От чего считать процент?
  - а) От фактически вылеченного здоровья.
  - б) От номинального лечения фляги (как сейчас; переписать текст).

**STOCK_CURSE_WHEN_HIT (MISC-06)**
- Что делать с характеристикой без источника?
  - а) Вернуть источник (эссенция).
  - б) Удалить характеристику и её чтение.
  - в) Оставить заделом.

**STOCK_EXPERIENCE × STOCK_SKILL_EXPERIENCE (MISC-13)**
- Должен ли бонус опыта героя повышать и опыт умений?
  - а) Да (как сейчас).
  - б) Нет — умения получают опыт без бонуса героя.

**STOCK_FLASK_CHARGES_USED (без потолка)**
- Реальный максимум снижения — около 94% (дерево 40, пояс 18, фляга 36): фляга на 10 зарядов тратит 0.6 за глоток. Нужен ли потолок?
  - а) Нет.
  - б) Потолок в правилах (например, 80%).
  - в) Минимум 1 заряд за глоток.

**STOCK_LIGHT_RADIUS / STOCK_MOVEMENT_SPEED — показ потолка (MISC-12)**
- Показывать ли в сводке итог с потолком (как у сопротивлений)?
  - а) Да.
  - б) Нет.

**STOCK_CHEST_QUANTITY**
- Бонус к сундукам читается только при пересчёте окна сундуков (раз в `refreshHours`): надетая после этого вещь не действует до следующего окна. Так и задумано?
  - а) Да.
  - б) Пересчитывать дополнительные сундуки при входе на карту по текущему листу.

---

## Проверено и не проверено

**Запущенные тесты** (копия `/home/user/audit/work/stats-misc/EF`, Gradle `--offline`):
- `./gradlew --offline :core:test --tests "*audit.StatMiscDumpTest*"` — успешно, дамп всех описаний по характеристикам группы: `/home/user/audit/work/stats-misc/dump_mods.txt`.
- `./gradlew --offline :core:test --tests "*audit.StatMiscAuditTest*" -i --rerun-tasks` — 9 тестов (A, A2, B, C, D, D2, E, F, G, H), все прошли (печатают факты, без утверждений); вывод — `/home/user/audit/work/stats-misc/run1.log`.
- `./gradlew --offline :core:test --tests "*audit.StatMiscProbeTest*"` — 4 пробы (скорость монстра при ранении, `partFigure` и `statValue`, самоцветы perStat, силы скорости передвижения) — успешно.
- Файлы тестов: `/home/user/audit/work/stats-misc/EF/core/src/test/kotlin/com/sperance/exileforge/core/audit/StatMisc{Dump,Audit,Probe}Test.kt`. Сиды фиксированы: Battle `Random(7)`/`Random(3)`, `Run` seed 42, сундуки `Dice(Random(0..1999))` (N = 2000 на точку).

**Проверено только кодом:** STOCK_BORROW_SKILLS (Spawns.skilled); STOCK_WORK_YIELD/EXPERIENCE/FIND/DOUBLE/SAVE/RARITY/ITEM_LEVEL/UNIQUE (Work.settle, CraftsService; инструмент складывается с листом суммой — корректно, у этих характеристик нет MORE и SET); STOCK_VEILED_LOOT и STOCK_FATE_ASH_LOOT (Run.kill, исключены из строк сверх броска); STOCK_SKILL_EXPERIENCE (SkillGrowth.gain); STOCK_WORN_EMPTY_SLOTS/UNIQUES/CORRUPTED (WornCount.of); наложение проклятия при FLASK_HITS_CURSE (нужны `kit.curses`); статистика броска FLASK_NO_CHARGE_CHANCE.

**Не проверено:**
- Мощь (`Might.hero`) с фляжными штрафами сил.
- Иконки (`IconsTest` сервера).
- Экраны приложения (эмулятор не запускался).
- Частота встреч нескольких RARE с аурами в одном бою.
- Серверные интеграционные пути (Mongo не поднималась).
- Резерв маны аурами героя (`STOCK_RESERVATION`, `STOCK_AURA_EFFECT`) — раздел RESERVE, вне этой группы; монстровые `AURA_*` с резервом не связаны.

**Просмотренные файлы:** `rules/.../content/Stats.kt`, `sheet/FlaskSheet.kt`, `sheet/SheetFold.kt`, `sheet/Sheet.kt` (SheetCalculator, ModifierMath, WornCount), `sheet/Recovery.kt`, `content/Powers.kt` (loot, WorldKind), `run/Run.kt` (boost, worth, kill), `roll/Work.kt`, `roll/Windows.kt` (Chests), `roll/MonsterRoller.kt` (fold, LOOT_STATS), `roll/Veils.kt`, `text/ModifierText.kt`; клиент `campaign/Loadout.kt`, `combat/{Battle,BattleSkills,BattleLand,BattleTick,Combatant}.kt`, `BattlePowers.kt`, `MapEffects.kt`, `CombatSheet.kt`, `run/ExpeditionWorld.kt`, `run/ExpeditionRun.kt`, `Spawns.kt`, `atlas/AtlasFog.kt`, `display/{Text,ItemView,StatLimits}.kt`; сервер `CampaignService.kt`, `CraftsService.kt`, `HeroRules.kt`; контент `stats.json`, `modifiers.json`, `powers.json`, `equipment.json`, `tables.json`, `tree.json`, `classes.json`, `campaign.json`, `rules.json`, `professions.json`; словари `locale/ru.json`, `core/.../i18n/ui_ru.json`.
