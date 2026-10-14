# 06 — Предметы, уникальные/мифические, контент, модификаторы

Источник: копия сервера `/home/user/audit/work/items` (ktor-bestgame `2619f0f`, правила `rules/`, контент `src/main/resources/content`). Production-репозитории не менялись. Временные тесты лежат только в копии: `rules/src/test/kotlin/audit/*.kt`, их вывод — в `/home/user/audit/work/items/out/`.

Контракт (цитаты RULES.md, «Общее»):
- «Любая сущность с редкостью … держит дно и потолок модификаторов … на каждом пути … у вещи это одна точка `ItemFactory.hold`, через неё идут все пути.»
- «Мифические предметы падают только с 80 уровня района … `dropLevel` … действует на всех путях: добыча, сфера удачи, сундуки, боссы, торговцы, ремесло; путь, что поднимает уровень вещи над районом, сверяет его с уровнем района (4.9.0, `ContentIndex.uniquePool` с `area`).»
- «У уникального предмета одна или две силы (строки `POWER_`)…»
- «Ни одного балансного числа в Kotlin…»; сервер: «Контент валидируется `ContentLoader` на старте; битый контент — падение старта, тихих фоллбэков нет.»

## Сводка находок

| ID | Категория | P | Уверенность | Суть |
|---|---|---|---|---|
| ITEM-01 | BUG / ECONOMY | P2 | высокая | Обмен трофеев босса (`Fates.trophyPool`) фильтрует по уровню **героя**: мифики (dl 80–84) и уникалки выше района за трофеи с боссов районов 65–70; мифик — 1 из 2 записей пула |
| ITEM-02 | BUG | P2 | высокая | `LootRoller.pickFrom` сверяет с районом только мифики: уникалки с `dropLevel` выше района падают из сокровищницы (+10), сундуков Скверны (+2/4/6), монстров карты (±2) и Скверны (+шаг, Мать +3); 61 сочетание район × сундук |
| ITEM-03 | BUG | P2 | высокая | Мифический шаблон создаётся с редкостью `UNIQUE` (`create(…, Rarity.UNIQUE)` + `rarityFor` не закрепляет редкость шаблона); `reconcile` не чинит. Клад Бездны и клад башни при районе ≥85 (VEIN_OF_THE_FIRST_WORLD, около 3,7 % уникалок клада), раш |
| ITEM-04 | ARCHITECTURE / TEST_GAP | P3 | высокая (код) | Сферы (`OrbApplier`) меняют редкость и аффиксы мимо `ItemFactory.hold`; сейчас дно и потолок держатся только благодаря контенту (154 170 применений — 0 нарушений) |
| ITEM-05 | BALANCE / DESIGN | P3 | высокая | Качество базы умножается **после** локального «увеличения» (931 против 854 по формуле PoE, +9 %) |
| ITEM-06 | DESIGN (вопрос) | P3 | высокая | Сфера удачи смотрит на ilvl, а он поднимается над районом (+5 с монстра, ремесло +10, закалка, сундук-предмет); у 9 семейств вне `chanceVariety` долгие диапазоны с одной уникалкой |
| ITEM-07 | TEST_GAP | P3 | средняя | `FlaskSheet` читает свойства фляги по жёсткой паре «стат+операция» без проверки контента; `FlaskReadout.uses` = 0 при расходе 0 |
| CONTENT-01 | CONTENT | P3 | высокая | 4 уникалки с тремя `POWER_`, у DEATHBED_DRAUGHT ни одной; валидатор считает и `FLASK_`, верхней границы нет |
| CONTENT-02 | CONTENT / ARCHITECTURE | P2 | высокая | `ignoreUnknownKeys` + значения по умолчанию Kotlin: опечатка в ключе контента молча берёт число из кода (опыт: `chanceVariety` отключается, ContentLoadTest зелёный); дубль ключа JSON — побеждает последний |
| CONTENT-03 | CONTENT | P3 | высокая | 19 полей `rules.json` берутся из умолчаний Kotlin (`quality`, `itemLevelBonus`, `craftItemLevelBonus`, `maxItemLevel`, `experienceWindow`, `auction.*`…) |
| CONTENT-04 | CONTENT | P3 | высокая | 6 натуральных аффиксов не входят ни в одну таблицу (мёртвые записи) |
| CONTENT-05 | TEST_GAP | P3 | высокая | Пробелы валидатора: `unique:chance` целиком с весом 0 проходит (сфера удачи мертва), `itemLevelSpread` со сдвигом −50 проходит |

---

## [ITEM-01] Обмен трофеев выдаёт мифики и уникалки выше района — по уровню героя
- **Категория:** BUG / ECONOMY. **Приоритет:** P2 (P1 — если владелец считает «мифик только с 80 района» жёстким инвариантом и при герое 80+). **Уверенность:** высокая. **Подсистема:** Предначертание «Трофейщик», торговец.
- **Файлы:** `rules/.../content/Fates.kt:586` (`trophyPool`), `:593-601` (`trade`); `src/.../logic/trade/MerchantService.kt:161-172` (`trophy`, передаёт `hero.level`); начисление трофеев — `src/.../logic/campaign/CampaignService.kt:446,487`, `TrialService.kt:218` (уровень района не учитывается).
- **Инвариант:** «Мифические предметы падают только с 80 уровня района … dropLevel действует на всех путях: … торговцы …; путь, что поднимает уровень вещи над районом, сверяет его с уровнем района».
- **Фактически:** `trophyPool = templatePool(boss.tables).filter { unique && dropsAt(heroLevel) }`. Трофей начисляется за победу над стражем зоны (`CampaignService:446`) в районе любого уровня. Все пять боссов с мификами стоят в зонах 65–70.
- **Доказательство** (тест `audit.ItemsAuditTest.trophyPool`, `out/trophy_pool.txt`):
  ```
  boss=BOSS_STAR_SPAWN zone=C6_FALLEN_STAR@65 heroLevel=85 pool=2 mythic=[DUST_OF_THE_CAST_DOWN_STAR@81]
  boss=BOSS_MOTHER_BLIGHT zone=C6_ROTTEN_HEART@69 heroLevel=80 pool=2 mythic=[TEARS_OF_THE_STONE_FOREMOTHER@80]
  boss=BOSS_VEIL_WEAVER zone=C6_VEIL_OF_SHADOWS@69 heroLevel=85 pool=2 mythic=[RING_OF_THE_OTHERGLASS@83]
  boss=BOSS_ENDLESS_WARDEN zone=C6_ENDLESS_STAIR@69 heroLevel=85 pool=2 mythic=[FINAL_ECHO_OF_THUNDER@82]
  boss=BOSS_ABYSS_KING zone=C6_ABYSSAL_THRONE@70 heroLevel=85 pool=4 mythic=[COLOURLESS_FLAME@84]
  ```
  Веса в таблицах `boss:*` равны (по 100), так что мифик выпадает в 50 % обменов (у ABYSS_KING — в 25 %). Цена обмена — `TROPHY_HUNTER.TROPHIES = 10` (с ростом дара уменьшается), трофей можно удвоить (`TROPHY_DOUBLE`) и получать со стражей Ваал (`TROPHY_GUARDS`).
- **Воспроизведение:** герой 85 уровня с даром «Трофейщик» бьёт BOSS_STAR_SPAWN в C6_FALLEN_STAR (район 65) без карты, набирает 10 трофеев → `POST merchant/trophy?boss=BOSS_STAR_SPAWN` → с вероятностью 1/2 получает DUST_OF_THE_CAST_DOWN_STAR (мифик, dl 81). Проверено кодом и тестом пула; сквозной HTTP-сценарий не запускался.
- **Последствия:** мифики добываются в районах 65–70 вместо карт 80+; частота задаётся таймером возрождения босса и ценой в трофеях, а не правилами добычи мификов. Уникалки боссов с `dropLevel` выше зоны — тем же путём.
- **Масштаб:** все игроки с даром «Трофейщик» и уровнем героя от 80 до 84 и выше.
- **Варианты решения:** (1) `trophyPool(…, area)`: уровень района, где получен трофей, хранить вместе с трофеем (например, `trophies[boss]` → наибольший район) и звать `uniquePool`-подобный фильтр с `area`. (2) Исключить мифики из обмена (`rarity == UNIQUE`), уникалки фильтровать по уровню зоны босса (`zone.level`/`topAreaLevel`). (3) Мифик за трофеи разрешить только за трофеи с карт с районом ≥ `mythicalDropLevel`.
- **Побочные эффекты:** (1) меняет форму документа героя (`trophies`) → схема/вайп; (2) без изменения схемы.
- **Регрессионные тесты:** `trophyPool` для каждого босса: нет мифика при районе ниже `mythicalDropLevel`, нет уникалки с `dropLevel` выше района.
- **Вопросы владельцу:** должен ли обмен трофеев давать мифики? Если да — с какого района должны быть трофеи?
- **Рекомендация:** вариант 2 (минимальный, без схемы), затем решить про мифики. **Статус:** открыто.

## [ITEM-02] `pickFrom` пропускает уникалки с уровнем падения выше района при поднятом уровне добычи
- **Категория:** BUG. **Приоритет:** P2. **Уверенность:** высокая. **Подсистема:** добыча забега.
- **Файлы:** `rules/.../roll/Loot.kt:128-132` (фильтр по району только у `MYTHICAL`); вызывающие с `level > areaLevel`: `run/Run.kt:633-634` (сокровищница, `features.treasure.levelBonus = 10`), `:654-656` (сундук Скверны, +2/+4/+6), `:570-574` (`monster.level` — монстры карты `areaLevel ± maps.levelSpread(2)`, Скверна `+ step.level` до +4, Мать +3 — `:491-495`, `:532-536`).
- **Инвариант:** «путь, что поднимает уровень вещи над районом, сверяет его с уровнем района (4.9.0, `ContentIndex.uniquePool` с `area`)».
- **Фактически:** `templatePoolUpTo(tags, level).filter { rarity != MYTHICAL || dropsAt(area) }` — у UNIQUE `dropsAt(level)` идёт по поднятому уровню.
- **Доказательство** (реальные `LootRoller.pickFrom` и `templatePoolUpTo`; `out/raised_chest.txt`, `out/raised_chest_all.txt`):
  ```
  zone=C6_ABYSSAL_THRONE area=70 chest level=80 N=200000 seed=81: uniques=3017 dropLevel>area=19 [CHARRED_VOW@72]
  zone=C7_SHATTERED_CROWN area=75 chest level=85 N=200000 seed=86: uniques=2967 dropLevel>area=20 [STAFF_OF_THE_UNBROKEN_WALL@80]
  area=41 C4_SERPENT_SHRINE treasure(+10): 11 [CARVERS_CIRCLET@45, ...]
  area=53 C5_ASHFALL_VALLEY treasure(+10): 17 [...]
  area=39 C4_ORCHID_GLADE blight:RARE(+6): 10 [...@40]
  TOTAL combos=61
  ```
  Мифики отсечены правильно (`mythic=0`).
- **Воспроизведение:** открыть сундук сокровищницы в районе 53 → кандидаты добычи включают 17 уникалок с dl 55–60. Тест: `./gradlew --offline :rules:test --tests 'audit.ItemsAuditTest.raisedChest*'`.
- **Последствия:** уникалки следующего акта раньше времени; в сочетании с `uniqueReach` (+10 к требуемому уровню) — вещи, которые герой ещё не наденет. Доля невелика (около 0,6 % уникалок сундука при районе 70), но в серединных актах кандидатов до 17.
- **Масштаб:** все заходы с сокровищницей и Скверной; на картах — монстры уровня выше района.
- **Варианты решения:** (1) в `pickFrom` фильтровать **все** уникальные по `dropsAt(minOf(level, area))` (как `uniquePool`); (2) отдавать в `grant` уровень района отдельно и тянуть шаблон на `area`, а ilvl — на поднятом уровне.
- **Побочные эффекты:** сдвиг костей добычи → поднять `RULES_VERSION` (обрыв незаконченных забегов).
- **Регрессионные тесты:** для каждой зоны и бонуса сундука — ни одного UNIQUE с `dropLevel > areaLevel` в пуле `pickFrom`.
- **Вопросы владельцу:** монстр карты на +2 к району и сундук +10 — повод открывать уникалки выше района или нет?
- **Рекомендация:** вариант 1. **Статус:** открыто.

## [ITEM-03] Мифический шаблон создаётся с редкостью UNIQUE
- **Категория:** BUG. **Приоритет:** P2. **Уверенность:** высокая. **Подсистема:** `ItemFactory`, награды забега.
- **Файлы:** `rules/.../roll/ItemFactory.kt:20-24` (`rarityFor`: для закреплённой редкости возвращает `wanted`), `:138-143` (`reconcile` проверяет только `rarityFor`); вызовы с жёстким `Rarity.UNIQUE` по пулам, где есть мифики: `run/Run.kt:748` (клад Бездны и клад башни — `abyss.uniques = ["abyss"]` содержит VEIN_OF_THE_FIRST_WORLD, dl 85), `:778-782` (раш: `ownPool` собственных таблиц боссов, в `boss:*` пять мификов), `:670`, `:601`, `:832`, `MapFeatures.kt:383`, `LootChests.kt:107`, `CraftsService.kt:319`.
- **Инвариант:** у копии уникального и мифического шаблона редкость закреплена шаблоном (`Rarity.fixed`, KDoc `grantable`: «уникальный и мифический — только свою»); облик и цена — по редкости копии.
- **Доказательство** (`audit.ItemsAuditTest.mythicCreatedAsUnique`, `out/mythic_as_unique.txt`):
  ```
  template=VEIN_OF_THE_FIRST_WORLD template.rarity=MYTHICAL created(UNIQUE).rarity=UNIQUE reconcileChanged=false after=UNIQUE
  abyss uniques area=85 N=100000 seed=90: mythical drawn=3722
  ```
- **Последствия:** мифик виден и считается как уникалка: не растёт счётчик `MYTHICS` (`HeroRules.kt:66`), цена торговца ×8 вместо ×20 (`sell.rarity`), облик `UNIQUE` вместо `MYTHICAL`, фильтр витрины аукциона (`lot.rarity`) и автопродажа по редкости работают как для уникалки (герой, который автопродаёт уникалки, может продать мифик).
- **Масштаб:** клады Бездны и башни при районе от 85, раш при районе не ниже `dropLevel` мифика босса.
- **Варианты решения:** (1) `rarityFor`: `if (template.rarity.fixed) template.rarity` — тогда и `create`, и `reconcile` чинят все пути разом; (2) на местах вызова передавать `template.rarity` вместо `Rarity.UNIQUE`.
- **Побочные эффекты:** вариант 1 исправит и сохранённые копии при следующем `reconcile` (без вайпа); `RULES_VERSION` не обязателен (кости не сдвигаются), но меняется результат → по правилу RULES поднять.
- **Регрессионные тесты:** `create(mythic, UNIQUE).rarity == MYTHICAL`; `reconcile` исправляет копию с неверной редкостью.
- **Рекомендация:** вариант 1. **Статус:** открыто.

## [ITEM-04] Сферы обходят `ItemFactory.hold`
- **Категория:** ARCHITECTURE / TEST_GAP. **Приоритет:** P3. **Уверенность:** высокая (проверено кодом), сейчас не воспроизводится.
- **Файлы:** `rules/.../roll/Orbs.kt:245-251` (`upgrade`), `:253-260` (`reroll`), `:275-282` (`regal`, только `normalize`), `:288-299`, `:373-407` (`vaal`, исход RARE), `:415-433` (`chance`, не-уникальная ветка); `hold` зовёт только эссенция (`:238`). `LootChests.kt:97-99` (осквернённая вещь сундука). `ItemFactory.kt:58-67`: KDoc `floored` обещает «вещь той, что дал create», а код возвращает `null`. `AffixRoller.normalize:104-108`: расколотые не снимаются, поэтому `over` может остаться выше потолка.
- **Инвариант:** «у вещи это одна точка `ItemFactory.hold`, через неё идут все пути: ролл, сферы, ремесло…».
- **Фактически:** сферы сами выставляют редкость и вызывают `rollAffixes`; дно держится, только если пул базы на её ilvl открывает достаточно групп. Потолок держат лимиты сторон (prefixes + suffixes = ceiling во всех записях `rarities`), а не `hold`.
- **Доказательство:** (а) `audit.ItemsAuditTest.floorAfterOrbs` — по всем не-уникальным базам и ilvl от `level−40` до `level` нет ни одного сочетания, где открытых групп меньше дна; (б) `floorSampling`, seed 99: цепочки алхимия → хаос → очищение → трансмутация → царей → хаос → ваал (у фляг — трансмутация и изменение) на ilvl 1 и на уровне базы: `applied=154170 outOfLimits=0`.
- **Последствия:** сегодня их нет. Новая база с узким пулом или запись `rarities`, где `ceiling < prefixes + suffixes`, даст редкую ниже дна или выше потолка через сферу. `reconcile` на следующем чтении героя такую вещь понизит — игрок «потеряет» редкость.
- **Варианты решения:** (1) `OrbApplier.apply` в конце зовёт `factory.hold` (и `trimmed`) для той же копии; (2) проверка контента: `ceiling == prefixes + suffixes` и пул каждой базы на ilvl 1 держит дно.
- **Регрессионные тесты:** оставить `floorSampling` как постоянный тест правил.
- **Рекомендация:** вариант 1 + исправить KDoc `floored`. **Статус:** открыто.

## [ITEM-05] Качество базы умножается после локального «увеличения»
- **Категория:** BALANCE / DESIGN. **Приоритет:** P3. **Уверенность:** высокая.
- **Файлы:** `rules/.../sheet/Sheet.kt:161-192` (`foldItem`: `based(kept + folded)` умножает уже свёрнутое значение на `1 + q/100`).
- **Ожидание:** «База вещи — как в PoE (4.8.0)»; в PoE качество — ещё одно локальное «increased»: `база × (1 + лок% + кач%)`.
- **Доказательство** (`audit.ItemsAuditTest.qualityVsLocal`, `out/quality_local.txt`):
  ```
  ASCENDANT_HAUBERK INCREASED_ARMOUR@LOCAL base=388 local+100%: q0=776 q20=931.2 additive(PoE)=853.6
  ASCENDANT_HAUBERK INCREASED_ENERGY_SHIELD@LOCAL base=132 local+100%: q0=264 q20=316.8 additive(PoE)=290.4
  ```
- **Последствия:** при 20 % качества и +100 % локального бонуса защита на 9 % выше, чем по формуле PoE; разница растёт вместе с локальным процентом.
- **Варианты:** (1) сложить качество с локальным INCREASED внутри `raw(start, localOps)`; (2) оставить и записать в RULES как намеренное.
- **Побочные эффекты:** меняется Мощь и лист → `RULES_VERSION`.
- **Вопрос владельцу:** умножение задумано? **Статус:** вопрос.

## [ITEM-06] Сфера удачи по уровню вещи; разнообразие вне `chanceVariety`
- **Категория:** DESIGN (вопрос). **Приоритет:** P3. **Уверенность:** высокая.
- **Файлы:** `rules/.../roll/Orbs.kt:413` (`chanceUniques(item.level)`), `content/Content.kt:394`, `EngineRules.kt:444` (ilvl = район + `itemLevelBonus[UNIQUE]=3` + сдвиг до +2), `:461` (ремесло +10), `InventoryService.temper` (ilvl + до `maxLevels`).
- **Факты** (`out/chance_variety.txt`, `out/chance_raised_ilvl.txt`): в `unique:chance` **мификов нет** — сфера удачи мифик не делает ни на каком ilvl (проверено перебором 1..100). Уникалку выше района она даёт: с монстра района 77 ilvl доходит до 82. Семейства из `chanceVariety` (FLASK_LIFE, FLASK_MANA) везде ≥ 2 — правило выполнено. Вне списка одна уникалка на длинных диапазонах: BOW 10–34, SWORD 10–34, SHIELD 20–39, QUIVER 10–24, DOUBLEAXE 30–44, BLADE 25–34, DOUBLESWORD 25–29, BOOTS/GLOVES 10–14.
- **Вопросы владельцу:** считать ли ilvl «уровнем района» для сферы удачи (KDoc `ItemTemplate.dropLevel` говорит «у сферы удачи — уровня предмета»)? Добавлять ли семейства в `chanceVariety` по мере наполнения? **Статус:** вопрос, не баг.

## [ITEM-07] Лист фляги: связка «стат + операция» без проверки
- **Категория:** TEST_GAP. **Приоритет:** P3. **Уверенность:** средняя.
- **Файлы:** `rules/.../sheet/FlaskSheet.kt:135-156` (`of`: INCREASED → `increased`, всё прочее, включая MORE и SET, → `added`), `:48-67` (читатели: `FLASK_CHARGES_USED` — только из `added`, длительность, эффект, лечение, заряды за убийство — только из `increased`), `:176` (`uses = 0` при `perUse <= 0`).
- **Факты:** сейчас контент совпадает со всеми читателями (`STOCK_FLASK_DURATION/EFFECT/RECOVERY/CHARGES_GAINED` — INCREASED, `CHARGES_USED/CHARGES_PER_KILL` — ADD; проверено перебором `modifiers.json`). Строка `STOCK_FLASK_DURATION` с ADD на фляге молча пропадёт. MORE на свойстве фляги сложится как плоская прибавка. При 100 % снижения расхода карточка покажет «0 глотков» вместо «без ограничения».
- **Смешение свойств фляги и героя:** не найдено — фляги исключены из листа героя (`Sheet.kt:261`), `local/draught/shown` разделены по тегу слота и приставке.
- **Варианты:** проверка контента «op каждого `STOCK_FLASK_*`/`FLASK_*` на фляге — та, что читает `FlaskSheet`»; явная ветка для `perUse == 0`. **Статус:** открыто.

## [CONTENT-01] Число сил у уникалок
- **Категория:** CONTENT. **Приоритет:** P3. **Уверенность:** высокая.
- **Файлы:** `content/equipment.json` (`DEATHBED_DRAUGHT` ~стр. 14977, `DEATHLESS_RHYTHM` 25102, `PINIONS_OF_THE_CAST_DOWN` 25294, `BURNING_CHIEFTAINS_HILT` 26433, `GEMCUTTERS_GLOVES` 28453); `rules/.../content/Equipment.kt:240` (`uniqueEffects` считает `POWER_` и `FLASK_`), `Content.kt:459` (только нижняя граница).
- **Инвариант:** «У уникального предмета одна или две силы (строки `POWER_`)».
- **Факт:** из 568 уникальных и мифических шаблонов 534 имеют 2 силы, 16 — одну. Нарушают правило: DEATHLESS_RHYTHM (MYTHICAL), PINIONS_OF_THE_CAST_DOWN (MYTHICAL), BURNING_CHIEFTAINS_HILT, GEMCUTTERS_GLOVES — по 3 `POWER_`; DEATHBED_DRAUGHT — 0 `POWER_` (три `FLASK_`). Опыт E06: четвёртая сила валидатором не ловится.
- **Варианты:** (1) свести к 1–2 силам и добавить в валидатор `POWER_ in 1..2`; (2) записать в RULES, что `FLASK_` у уникальных фляг считается силой, и что мификам можно три.
- **Вопрос владельцу:** мификам разрешено 3 силы? `FLASK_`-строка — это сила? **Статус:** вопрос.

## [CONTENT-02] Опечатка в ключе контента молча берёт значение по умолчанию
- **Категория:** CONTENT / ARCHITECTURE. **Приоритет:** P2. **Уверенность:** высокая.
- **Файлы:** `rules/.../RulesJson.kt:9-14` (`ignoreUnknownKeys = true`), `content/Content.kt:235-268` (`ContentLoader.load` разбирает контент тем же JSON).
- **Инвариант:** «битый контент — падение старта, тихих фоллбэков нет».
- **Доказательство** (`audit.CorruptContentAuditTest`, `out/corrupt_results.txt`; плюс настоящий `ContentLoadTest` на испорченной копии):
  ```
  E05_rules_typo_chanceVariety: LOADED (not caught) chanceVariety=ChanceVariety(minimum=1, families=[])
  E16_campaign_typo_levelBonus: LOADED (not caught) treasure.levelBonus=10
  E11_json_duplicate_key: LOADED (not caught) weight(first unique:chance)=HUSHED_SPRING_CIRCLET=100   (дубль 999999 потерян)
  ContentLoadTest с E05 в rules.json: BUILD SUCCESSFUL (4/4 зелёные)
  ```
  Опечатка `chanceVarity` отключает проверку разнообразия сферы удачи; опечатка `levelBonuss` возвращает число из Kotlin. Обязательные поля без значения по умолчанию (`dropLevel` уникалки, `mythicalDropLevel`) ловятся.
- **Последствия:** любая правка `rules.json`, `campaign.json`, `features` с опечаткой незаметно меняет баланс на значение из кода.
- **Варианты:** (1) отдельный `ContentJson` с `ignoreUnknownKeys = false` для `ContentLoader` (документы героя и чанки клиента — оставить как есть); (2) проверка «каждый ключ JSON известен сериализатору» по дереву `JsonElement`; (3) запрет дублей ключей (свой парсер либо прогон через `Json` с `allowDuplicateKeys`-аналогом, либо проверка в CI скриптом).
- **Побочные эффекты:** при включении вскроются старые лишние ключи — их надо будет вычистить. Клиент читает те же чанки — ему нужен свой (терпимый) JSON.
- **Регрессионные тесты:** корпус испорченных вариантов из `out/make_corrupt.py` как параметризованный тест.
- **Рекомендация:** вариант 1. **Статус:** открыто.

## [CONTENT-03] Балансные числа в значениях по умолчанию Kotlin
- **Категория:** CONTENT. **Приоритет:** P3. **Уверенность:** высокая.
- **Факт** (`audit.DefaultsAuditTest`, `out/rules_defaults.txt`): 19 полей `rules.json` не заданы в контенте и берутся из кода: `quality` (max 20, шаги 5/2/1/1/1, `unveilChoices`), `loot.itemLevelBonus` {1,2,3}, `loot.craftItemLevelBonus` 10, `loot.maxItemLevel` 100, `loot.experienceWindow`, `auction.historyDays/extendWindowHours/priceSales/priceLevelSpread`, `run.idleMinutes`, пустые пределы `rarities` COMMON/UNIQUE/MYTHICAL. Файлы: `EngineRules.kt:397-425`, `Crafting.kt:86-92`.
- **Инвариант:** «Ни одного балансного числа в Kotlin … только в `content/*.json`».
- **Вариант:** перенести в `rules.json` и убрать умолчания, тогда отсутствие поля станет ошибкой загрузки (это заодно закрывает CONTENT-02 для этих полей). **Статус:** открыто.

## [CONTENT-04] Мёртвые аффиксы
- **Категория:** CONTENT. **Приоритет:** P3. **Уверенность:** высокая.
- **Факт** (`audit.ItemsAuditTest.unusedContent`): натуральные PREFIX/SUFFIX `INCREASED_CRITICAL_DAMAGE`, `DAMAGE_PER_CORRUPTED_ITEM`, `LIFE_PER_UNIQUE_ITEM`, `RESISTANCES_PER_JEWEL`, `COND_DAMAGE_LOW_LIFE`, `COND_DAMAGE_VS_UNIQUE` не входят ни в одну таблицу модификаторов. В таблицах есть только их варианты `@CORRUPTED`, `JEWEL_`, `ELDER_` и узлы дерева. `VEILED_PREFIX/SUFFIX` вне таблиц намеренно (`Veils.kt`). Шаблонов без таблиц нет (кроме `MAP`); все уникалки достижимы (`dropLevel` ≤ верхнего уровня района 100).
- **Вопрос:** удалить натуральные варианты или вернуть в пулы? При удалении — grep локали и второго репозитория (CLAUDE.md). **Статус:** вопрос.

## [CONTENT-05] Пробелы валидатора контента
- **Категория:** TEST_GAP. **Приоритет:** P3. **Уверенность:** высокая.
- **Опыты** (`out/corrupt_results.txt`): **не ловятся** — E14 `unique:chance` целиком с весом 0 (`validateChanceVariety` допускает 0 кандидатов: `count in 1 until minimum`, `Content.kt:560`, — сфера удачи перестаёт работать везде); E20 `itemLevelSpread` со сдвигом −50 (`EngineRules.kt:450` проверяет только веса и уникальность); E06 четвёртая `POWER_`; E03 `grid.top` с min>max молча переставляется (`Modifiers.kt:182`), вреда нет. **Ловятся:** дубль шаблона и предмета, неизвестный шаблон в таблице, отрицательный вес, `affixes` min>max у редкости, `dropLevel` 0 у уникалки и <80 у мифика, строка уникалки min>max, уникалка без силы, опечатка семейства, `baseVariance` min>max.
- **Вариант:** у семейств `chanceVariety` требовать ≥ `minimum` начиная с первого уровня, где есть база; ограничить сдвиг ilvl; добавить верхнюю границу сил (CONTENT-01). **Статус:** открыто.

---

## Пути создания и изменения предмета (раздел 5)

| Путь | Где | Через `hold` | Замечания |
|---|---|---|---|
| Старт героя (фляга, снаряжение, магическое оружие, инструмент) | `HeroService.kt:147-173` | да (`create`) | — |
| Добыча монстров, боссов, сундуков зоны | `Run.grant` → `pickFrom` → `create(template.rarity)` `Run.kt:961-962` | да | ITEM-02 |
| Сокровищница, сундук Скверны (редкие наверняка) | `Run.kt:633-658` → `floored` | да | ITEM-02 |
| Уникалки механик, Мать | `Run.kt:600-601,666-670` | да | `Rarity.UNIQUE` (ITEM-03 — в таблицах сейчас мификов нет) |
| Клад Бездны и клад башни | `Run.kt:738-750` | да | ITEM-03 (мифик как UNIQUE) |
| Раш, Разлом | `Run.kt:761-833` | да | раш: ITEM-03; Разлом: `unique:rift` без мификов |
| Боссы: таблицы карт, тиров, атласа | `Run.kt:686-691` → `pooled` → `uniquePool(area)` | да | мифики с района ✓ |
| Блуждающий торговец | `MapFeatures.kt:369-391` | да (`floored`, `unique(area)`) | ✓ |
| Торговец города, полки | `MerchantService.kt:90-110` → `floored` | да | таблицы торговца без уникалок ✓ |
| Обмен трофеев | `Fates.trade` → `create(template.rarity)` | да | **ITEM-01** |
| Сундуки-предметы | `LootChests.open` | да | уникалка — `uniquePool(area = floorOf(tier))`, только UNIQUE ✓; осквернённая — Ваал без `hold` (ITEM-04) |
| Ремесло (кузнец, самоцвет, инструмент, фляга, карта) | `CraftsService.kt:294-365` | да | ручные строки `HANDCRAFTED` — не аффиксы, мест не занимают; `unique:smith` без мификов, dl ≤ 55 |
| Сферы, знамения, верстак, гравёр | `InventoryService.applyOrb/craft` → `OrbApplier` | **нет** (кроме эссенции) | ITEM-04, сейчас без нарушений |
| Сфера удачи («перерождение» в уникалку) | `Orbs.kt:415-426` → `create(...).inheriting` | да | `granted`, качество и `resale` наследуются; мификов нет (ITEM-06) |
| Зеркало | `Orbs.kt:436` | копия | `granted` едет ✓ |
| Закалка | `InventoryService.temper` | не нужно (аффиксы не меняются) | поднимает ilvl (ITEM-06) |
| Аукцион | `AuctionService.kt:243-262` → `reconcile` | да | кости `Dice.system()` (в отличие от героя); на витрине (`search`) лоты не сверяются до старта или покупки — P3, не карточка |
| Склад гильдии, почта игроков | перенос копий | на чтении героя (`HeroRepository.reconcile`) | выданное — отказ `GR_001` ✓ |
| Письмо администратора, промокод, тестировщик | `MailService.kt:59-77`, `RedemptionService.kt:67-80`, `TesterGrants.kt:55-88` | да | редкость — только `grantable` ✓, метка `granted` ✓ |
| Загрузка из базы | `HeroRepository.kt:125-136` → `reconcile` (кости от id) | да + `trimmed` (потолок) | ITEM-03: редкость шаблона не чинится |
| Квесты, достижения, награды Разлома | — | — | предметов экипировки нет (только стопки) |

Прямых `ItemInstance(` вне `ItemFactory.create` нет (grep). Редкость меняют только `ItemFactory` и `Orbs.kt`.

## Модификаторы и статы (раздел 6) — проверено кодом
- Свод: `(база + ΣADD) × (1 + ΣINCREASED/100) × Π(1 + MORE/100)`, SET последним, у процентного стата INCREASED складывается; округление до десятых на каждый стат (`Sheet.kt:86-95`). Конверсии `perStat` — по порядку реестра, валидатор запрещает обратный порядок (`Content.kt:432`).
- Локальные строки сворачиваются в вещи (`foldItem`); мёртвых локальных строк в пулах и имплиситах нет (тест `deadLocalAffixes`: 0).
- `Slot.numericBase` ↔ наличие `base`, имплисит у бижутерии — проверяется валидатором. `Slot.topAffixRarity` (фляга — MAGIC) соблюдают `rarityFor`, Ваал (`isFlask` → ничего), эссенции (у фляги нет гарантии), `flasks.orbs`.
- `QualityKind`: смена вида обнуляет качество (`Orbs.kt:205`, `QualityKind.lost`), очищение сбрасывает в базу, закалка снимает катализатор, сфера удачи переносит качество только если вид работает на уникалке; клиент предупреждает (`core/display/QualityText.kt:21-23`).
- Числа: веса тяг суммируются в `Long`; `TierLadder` — `Long`; вес тира 0 заменяется номером позиции (не мёртвый тир). Деление на ноль: `SellPrice.quality` и `FlaskReadout.uses` защищены; `applyEssence` при `essences.tiers.size == 1` даст `0/0 = NaN` → худший тир (латентно, сейчас 8 тиров). Отрицательных MORE ≤ −50 в контенте нет. Переполнений `Int` в путях предмета не найдено (качество ≤ 20, ilvl ≤ 100).

## Контент-целостность (раздел 13) — итог
- Дублей ключей JSON нет ни в одном файле (`object_pairs_hook`); дублей тегов таблиц нет; дублей кодов шаблонов, предметов и описаний нет (валидатор). Висячих ссылок нет (валидатор). Отрицательных весов нет. Диапазоны min>max: в контенте нет; у сеток со знаком минус «вершина ниже основания» — это норма для отрицательных статов (53 сетки).
- Шаблоны без таблиц — только `MAP` (выдаётся кодом). Уникалки достижимы все.

## Что проверено
Файлы (точечно): `rules/.../roll/ItemFactory.kt`, `AffixRoller.kt`, `Orbs.kt`, `Loot.kt`, `LootChests.kt`, `Quality.kt`, `Veils.kt`, `Rolls.kt`; `content/Content.kt` (`ContentLoader`, `validate`, `CampaignValidator.validateChanceVariety/validateUniqueLevels`), `Equipment.kt`, `Modifiers.kt`, `EngineRules.kt`, `Essences.kt`, `Fates.kt` (trophy), `Features.kt`, `Crafting.kt`; `run/Run.kt` (grant, treasure, blight, hoard, rush, rift, mechanic), `run/MapFeatures.kt` (merchant); `sheet/Sheet.kt`, `sheet/FlaskSheet.kt`; `table/Tables.kt`; `RulesJson.kt`. Сервер: `InventoryService.kt`, `MerchantService.kt`, `CraftsService.kt`, `MailService.kt`, `RedemptionService.kt`, `TesterGrants.kt`, `HeroService.kt`, `HeroRepository.kt`, `AuctionService.kt`, `HeroRules.kt`, `CampaignService.kt` (трофеи), `ContentStore.kt`. Клиент: только grep `QualityKind`/`ItemFactory`.

## Запущенные тесты и опыты
| Команда (в копии) | Результат |
|---|---|
| `./gradlew --offline :rules:test --tests '*RulesInvariantsTest' --tests '*ContentLoadTest' --rerun-tasks` | BUILD SUCCESSFUL; RulesInvariantsTest 27/27, ContentLoadTest 4/4 |
| `--tests 'audit.ItemsAuditTest'` (`chanceVarietyAllFamilies`, `mythicTables`, `chanceOnRaisedItemLevel`, `floorAfterOrbs`, `raisedChestUniqueDropLevel`, `raisedChestCandidatesAllZones`, `unusedContent`, `floorSampling`, `trophyPool`, `mythicCreatedAsUnique`, `deadLocalAffixes`, `qualityVsLocal`) | все прошли; вывод — `work/items/out/*.txt` (seed и N — в строках вывода) |
| `--tests 'audit.CorruptContentAuditTest'` по 20 испорченным копиям (`out/make_corrupt.py`, `out/corrupt/E01..E20`) | ловятся 12, не ловятся 8 (E03, E05, E06, E11, E14, E16, E17, E20; E17 — ноль у веса тира — по задумке, E03 — безвредная перестановка) |
| Настоящий `ContentLoadTest --rerun-tasks` с E02 (неизвестный шаблон в `tables.json`) в копии | 4/4 FAILED `ContentException` — поймано |
| Настоящий `ContentLoadTest --rerun-tasks` с E05 (опечатка `chanceVariety`) в копии | BUILD SUCCESSFUL — **не поймано** (CONTENT-02); копия контента восстановлена, `diff -rq` с оригиналом пуст |
| `--tests 'audit.DefaultsAuditTest'` | 19 полей из умолчаний Kotlin (CONTENT-03) |

## Не проверено
- Сквозные HTTP-сценарии (обмен трофеев, клад Бездны на 85+, сокровищница) на поднятом сервере с Mongo — сделаны на функциях `rules/` и подтверждены разбором кода вызывающих мест.
- Достижимость района ≥ `dropLevel` мифика в раше (уровень района раша) — не считал.
- Таймер возрождения боссов (насколько быстро копятся трофеи) — не считал.
- Клиентские карточка и фильтры при мифике с редкостью UNIQUE — не открывал, вывод сделан по RULES («облик задаёт только редкость»).
- Силы уникалок (`powers.json`, `Powers.applySheet`) по числам — вне этого раздела.
