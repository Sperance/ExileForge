# 13. Тесты, CI и архитектурные инварианты (разделы 4, 21, 22)

Источники: ExileForge `claude/sharp-bardeen-6i3dkz` @ e304983f (сабмодуль `backend` → 3ec40bd), ktor-bestgame `claude/tender-pasteur-a36kj2` @ 2619f0f.
Все прогоны — в копиях `/home/user/audit/work/tests/{server,client}`; оригиналы не тронуты. Временные аудит-тесты лежат только в копиях (`rules/src/test/kotlin/audit/`, `core/src/test/kotlin/audit/`).

## 1. Перечень тестов

| Репо / модуль | Класс | @Test | Что ловит (кратко) |
|---|---|---|---|
| server `:rules` | RulesInvariantsTest | 27 | дно/потолок редкости после сфер, аффиксы по уровню, сферы и `orbs.affixCounts`, награда захода один раз, уникалки/мифики по уровню района, руны, законы тронов, сигнатуры боссов, пулы карт |
| | FateTest | 7 | Предначертание: офферы, выбор, Осквернение, Равновесие, Щедрость |
| | RiftTest | 5 | неделя, план, связи, счёт, дары |
| | ContentLoadTest | 4 | загрузка/валидация контента, текст модификаторов ru/en, достижения, детерминизм ролла |
| | MightTest | 4 | Мощь: клиент=сервер, умения, стойкость, фляга |
| | CraftingTest | 3 | печати, качество/катализатор, Знамение выбора |
| | QuestProgressTest | 3 | счёт заданий |
| | ShardYieldTest / TrialChoicesTest | 2 / 2 | осколки; испытания |
| | BaseVarianceTest / TreeTotalsTest | 1 / 1 | база; дерево |
| **итого `:rules`** | 11 классов | **59** | |
| server root | AuthTest | 18 | `ModerationPolicy` |
| | LocalizationTest | 8 | ключи словарей по контенту, одинаковые ключи, шаблоны модификаторов, плейсхолдеры |
| | IconsTest | 5 | иконки предметов/статов/умений, уникальные спрайты, SVG |
| | CollectionSchemasTest | 5 | схемы коллекций, `GlobalSerials` |
| | TreeLayoutTest / AtlasLayoutTest | 3 / 2 | раскладки |
| | PlausibilityTest / SoftDeleteTest | 2 / 2 | темп боя; мягкое удаление |
| | KoinGraphTest / GrantedTest / CommandMarkTest | 1 / 1 / 1 | граф Koin; `GR_001`; метка Idempotency-Key |
| **итого server root** | 11 классов | **48** | |
| client `:core` | UiStringsTest | 11 | словари клиента, ключи из исходников и перечислений |
| | StartGateTest / UpdatePolicyTest | 6 / 6 | окно запуска, обновления |
| | RoadLifeTest | 5 | здоровье на карте |
| | CommandRunnerWatchdogTest / ContentSmokeTest / RiftCombatTest | 4 / 4 / 4 | |
| | ModifierCoverageTest / GuardianAndAutoTest / SheetExplainTest | 3 / 3 / 3 | |
| | BossPhaseTest / LifeFlowTest | 2 / 2 | |
| | CombatGoldenTest / ExpeditionGoldenTest / GatherTest / SceneSightTest / CommandQueueRoutesTest | 1 каждый | |
| | ServerIntegrationTest | 1 | живой контракт (только с `EF_LIVE_URL`) |
| **итого client `:core`** | 18 классов (+`TestContent`) | **59** | |
| client `:app` androidTest | ForgeSheetTest 6, DesignPreviewTest, HeroPanelTest, NarrowScreenTest, ReleaseSmokeTest, StartWindowTest по 1 | **11** | эмулятор, только перед релизом |

## 2. Что реально запускает CI

**deploy.yml (сервер)**, job `checks`, только `push`:
`ktlintCheck :rules:ktlintCheck :rules:test test --tests LocalizationTest --tests AuthTest --tests IconsTest --tests CollectionSchemasTest --tests KoinGraphTest --tests AtlasLayoutTest --tests TreeLayoutTest --tests CommandMarkTest --tests PlausibilityTest --tests GrantedTest --tests SoftDeleteTest`.
- Все 11 классов фильтра существуют (проверено `find`). Серверных тестов вне фильтра нет: в `src/test` ровно эти 11 классов. `:rules:test` гоняет все 59.
- `workflow_dispatch` (деплой от релиза клиента) проверки не гоняет, а выкатывает уже помеченный `:<sha>` образ головы ветки — по коду это соответствует `RULES.md` («на VPS уезжает только проверенный»).

**android.yml (клиент)**:
- job `core` (`:core:ktlintCheck :core:test`) — только при `needs.changes.outputs.core == 'true'`, т.е. изменены `core/**`, `backend`, `scripts/client_server_test.py`, `gradle/**`, `*.gradle.kts`, `gradle.properties`, сам workflow. Релиз это условие **не** включает (строка 56) — см. TEST-01.
- `ServerIntegrationTest` в job `core` пропускается (`assumeTrue`, отчёт «skipped»); реально идёт только в job `client-server` (`--tests com.sperance.exileforge.core.ServerIntegrationTest`), при изменении core или перед релизом. Класс существует.
- androidTest и `ReleaseSmokeTest` — только перед релизом (`emulator`). Класс `com.sperance.exileforge.ui.ReleaseSmokeTest` существует.
- Несуществующих классов в фильтрах нет ни в одном workflow.

## 3. Прогоны (реально запущено)

| Команда (в копии) | Результат |
|---|---|
| server: `./gradlew --offline :rules:test --rerun-tasks` | BUILD SUCCESSFUL 2m55s; **59 tests, 0 failures, 0 errors, 0 skipped** |
| server: `./gradlew --offline :rules:test --tests 'com.sperance.*' --rerun` (повтор на флейки) | **59/59** снова |
| server: `./gradlew --offline test --tests <11 классов deploy.yml> --rerun-tasks --continue` | BUILD SUCCESSFUL 2m38s; **48 tests, 0 failures** (Mongo не нужен) |
| client (копия с сабмодулем `backend` 3ec40bd): `./gradlew --offline :core:test --rerun-tasks --continue` | BUILD SUCCESSFUL 4m32s; **59 tests: 58 passed, 1 skipped (`ServerIntegrationTest.realServerClientContract`), 0 failures** |

Логи: `/home/user/audit/work/tests/{rules_test.log,rules_test_run2.log,server_test.log,core_test.log}`; XML-отчёты — в `build/test-results` копий.
Не запускалось: `ktlintCheck`, `:app:*` (lint, сборка APK), androidTest на эмуляторе, `client_server_test.py` с живым сервером и Mongo.

**Флейки от случайности:** не найдено. Все броски в тестах — с фиксированным семенем (`Dice(n)`, `Random(seed)` в бою клиента, `Streams(42)`), часы — виртуальные (`runTest`/`advanceTimeBy`) или константы; повторный прогон `:rules:test` — 59/59. Хрупкость есть другого рода: статистические утверждения на фиксированных семенах завязаны на поток костей и сдвинутся при правке контента (это не флейк, а ожидаемая поломка).

---

## [TEST-01] UiStringsTest не запускается при правке только `app/`, в том числе на релизе
- **Категория:** TEST_GAP. **Приоритет:** P2. **Уверенность:** высокая (проверено кодом workflow).
- **Подсистема:** CI клиента.
- **Файлы:** `ExileForge/.github/workflows/android.yml:33-41` (фильтр `core`), `:56` (`if: needs.changes.outputs.core == 'true'`), `:372` (release не ждёт пропущенных); `core/src/test/.../UiStringsTest.kt:132-150` (сканирует `../app/src/main/kotlin`).
- **Инвариант:** «Оба словаря … `LocalizationTest` и `UiStringsTest` зелёные»; «Игрок нигде не видит сырой код или ключ».
- **Факт:** `every_key_the_sources_ask_for_exists` читает исходники `app/`, но job `core` запускается только при изменении `core/**`/`backend`/сборки. Коммит, добавивший `ui("new.key")` в `app/` без строки в `core/src/main/resources/i18n/ui_*.json`, проходит CI и даже релиз: «Пропущенные по фильтру проверки … релиз не держат».
- **Доказательство:** фильтр `core:` не содержит `app/**`; условие job `core` не включает `release == 'true'`.
- **Воспроизведение:** пуш только с правкой `app/.../*.kt`, добавляющей `ui("x.y")` → job `core` skipped → `release` публикует.
- **Последствия:** сырой ключ у игрока (у `ui()` фоллбэк — сам ключ, тест `a_missing_key_is_shown_as_itself`).
- **Варианты:** (1) добавить `app/src/main/**` в фильтр `core`; (2) запускать `core` всегда перед релизом (`|| release == 'true'`); (3) вынести проверку ключей `app` в job `app`.
- **Побочные эффекты:** +~4 мин CI на правках app.
- **Регрессионный тест:** не нужен — правка workflow.
- **Вопрос владельцу:** приемлемо ли, что `core` всегда идёт перед релизом?
- **Рекомендация:** вариант 2 (минимум) или 1.
- **Статус:** открыто.

## [TEST-02] UiStringsTest не перечисляет ~45 семейств динамических ключей клиента
- **Категория:** TEST_GAP. **Приоритет:** P2 (из-за реального пропуска L10N-02). **Уверенность:** высокая (скрипт по исходникам + аудит-тест).
- **Подсистема:** локализация клиента.
- **Файлы:** `UiStringsTest.kt:158-246`; примеры вызовов: `CombatDetail.kt:261,268` (`trace.note.value.${kind}`), `EffectShelf.kt:171` (`fight.buff.`), `SkillText.kt:71,76` (`fight.charge.`), `FlaskText.kt:9`, `StackLore.kt` (`source.`, `stash.rarity_many.`, `essence.slot.`), `LevelUpScreen.kt` (`unlock.about.`), `TraceExplain.kt` (`trace.rarity.`) и др.
- **Инвариант:** «Полноту таких ключей по контенту проверяют `UiStringsTest` … новый ключ из кода — сразу в их перечень».
- **Факт:** из ~95 префиксов `ui("…$…")` в `core`+`app` около 45 в тесте не упомянуты (`moderation.target.`, `fight.charge.`, `moderation.role.`, `stash.rarity_many.`, `filter.sort.`, `fight.buff.`, `source.`, `guild.faction.`, `enum.catalyst.`, `term.`, `stat.kind.`, `trace.*`, `unlock.about.`, `rift.rest.`, `quest.rarity.`…). Также `fight.fate_state.*` перечислен строковым списком, а не из перечисления.
- **Доказательство:** аудит-тест `client/core/src/test/kotlin/audit/UiKeysAuditTest.kt` → `out/ui_dynamic_keys.tsv`: для `ChargeKind`, `BuffKind`, `FlaskMeasure`, `SourceKind`, `Rarity`, `FactorKey`, `MonsterRarity` ключи есть на ru/en; для `NoteKind` нет 15 из 22 (см. L10N-02). `unlock.about.*` (11 из `rules.json → unlocks`) и `guild.faction.*` (3) — есть.
- **Воспроизведение:** `./gradlew --offline :core:test --tests 'audit.*'` в копии клиента.
- **Варианты:** (1) дописать перечисления в `every_code_the_client_enumerates_is_named`; (2) сделать ключи типизированными (`ui(NoteKind)` с таблицей) — тогда тест по `entries` пишется один раз.
- **Регрессионный тест:** расширение `every_code_the_client_enumerates_is_named` (обязательно `NoteKind`, `BuffKind`, `ChargeKind`, `FactorKey`, `SourceKind`, `FateLever`-состояния из перечисления).
- **Рекомендация:** (1) сейчас, (2) по мере правок.
- **Статус:** открыто.

## [TEST-03] Нет проверки, что числа в рукописных текстах сил совпадают с `powers.json`
- **Категория:** TEST_GAP. **Приоритет:** P2. **Уверенность:** высокая.
- **Подсистема:** локализация уникалок.
- **Факт:** текст силы (`stat.template.POWER_*`) пишет числа длительностей, вторых строк, порогов руками; `LocalizationTest` проверяет только наличие ключа и плейсхолдеров. В итоге 11 сил расходятся с контентом (L10N-01).
- **Варианты:** (1) тест: каждое число `value/upto/duration/stacks/cooldown/cap` силы встречается в шаблоне на каждом языке (или явный белый список); (2) подставлять эти числа плейсхолдерами `{d}`, `{s}` из контента (как `{v}`).
- **Рекомендация:** (2) — устраняет класс ошибок; (1) — как сторож до перехода.
- **Статус:** открыто.

## [TEST-04] Инварианты RULES.md без единого теста
- **Категория:** TEST_GAP. **Приоритет:** P3. **Уверенность:** высокая (grep по тестам обоих репо).
- **Факт:** ни один тест не ссылается на: `loot.currencyCap`, `EnrageRule`, `MailRules.gate`, `StashTakePrice`, `ProgressGate`, `HeroLocks`/`transactionExecute` (повторяемость тела), `HeroRepository.recordUniques`, `fight.minWaveSeconds`, `orbs.chanceVariety` (проверка контента есть в загрузчике, не в тесте), `ShortCodes`, текст фляги «во время действия»/«этой фляги» (`FlaskSheet.draught/local` в тексте). Часть из них — проверки контента в `ContentLoader.validate` (тест `contentLoadsAndValidates` их косвенно гоняет).
- **Аудит-проверка вместо теста:** текст фляг проверен дампом (см. 18_LOCALIZATION_UI): все строки с `draught` несут «во время действия», все `local` — «этой фляги».
- **Варианты:** точечные тесты правил (`MailRules.gate`, `StashTakePrice`, `currencyCap`, `EnrageRule` — чистые функции `rules`), без Mongo.
- **Вопрос владельцу:** какие из них «ловят критичную поломку» по его мерке (RULES: тест живёт, только если ловит критичное).
- **Статус:** открыто.

## [TEST-05] Дублирующие тесты текста модификаторов
- **Категория:** TEST_GAP (качество). **Приоритет:** P3. **Уверенность:** высокая.
- **Файлы:** `rules/.../ContentLoadTest.kt:33-44` ≡ `src/test/.../LocalizationTest.kt:287-296` (одна проверка `ModifierText.template != null` по всем определениям); `ExileForge/core/.../ModifierCoverageTest.kt:45-53` — третья копия, и слабее: принимает подпись стата `enum.EnumStatStock.<stat>` (т.е. общий шаблон-заглушку), тогда как `LocalizationTest.every_stat_used_by_a_modifier_has_its_own_template` требует свой шаблон.
- **Инвариант:** «Дублирующий … тест удаляй в той же задаче».
- **Прочее:** `ContentLoadTest` перечисляет языки руками `listOf("en", "ru")` — новый язык не проверится там (LocalizationTest берёт языки из манифеста — корректно).
- **Варианты:** оставить серверный `LocalizationTest`, удалить две копии; или оставить клиентскую как контракт сабмодуля, но со строгим условием.
- **Статус:** открыто.

## [TEST-06] Слабые утверждения: тесты, проходящие при неверном результате
- **Категория:** TEST_GAP. **Приоритет:** P3. **Уверенность:** высокая (код), средняя (последствия).
- `ModifierCoverageTest.everyRolledStatIsReadByTheEngine` (`ModifierCoverageTest.kt:26-41`): «читается движком», если код начинается с `STOCK_RESIST_`, `STOCK_ATTACK_`, `STOCK_DAMAGE_VS_`… или кончается на `_FIRE`/`_COLD`… — новый мёртвый стат с таким именем пройдёт.
- `PlausibilityTest` (`PlausibilityTest.kt:30,43`) берёт `speed = 4` литералом, а не высшую из `fight.speeds`: при смене скоростей в контенте тест останется зелёным, проверяя не то.
- `IconsTest.every_hero_stat_has_its_own_sprite` не проверяет стиль GLASS из правила (данные сейчас верны: 298/298 статов HERO/FLASK/AURA — `glass`, проверено скриптом).
- `RulesInvariantsTest.aUniqueNeverDropsAboveTheAreaLevel` (`:631-645`) не имеет защиты от пустого прогона (в соседних тестах она есть: «the check proves nothing»). Замер аудит-тестом `VacuityAuditTest`: сейчас проверяется 837 уникалок за 4000 сундуков (`CHEST_VAAL`, tier 3, floor 21) — тест содержателен, но при снижении `uniqueChance` молча станет пустым.
- `RulesInvariantsTest.MAP_HERO_BUFFS/MAP_THREATS` — ручные списки: новый «помогающий» стат карты вне списка не будет пойман `mapPoolsNeverHelpTheHero`.
- **Варианты:** анти-пустые проверки (`assertTrue(checked > 0)`), чтение порогов из контента, классификация статов по полю `stats.json`, а не по списку.
- **Статус:** открыто.

---

# Архитектурные инварианты (раздел 22)

| Инвариант | Итог | Как проверено |
|---|---|---|
| `rules/` без `kotlin.random.Random` мимо `Dice` | соблюдено с оговоркой (ARCH-04) | grep: `Random` только в `Dice.kt` и `Work.cycleDice` (`Dice(Random(seed xor …))`) |
| `rules/` без `System.currentTimeMillis` | соблюдено | grep: 0 вхождений (`now` передаётся параметром) |
| `rules/` без Ktor/Mongo/Android | соблюдено | импорты: только `com.sperance`, `kotlinx.serialization`, `java.security/io` |
| балансные числа только в `content/*.json` | **нарушено** (ARCH-01, ARCH-02) | аудит-тест `DefaultsAuditTest`, grep |
| `:core` без `android.*` | соблюдено | 0 импортов `android.`/`androidx.` в `core/src/main` |
| `:app` без OkHttp | **нарушено по букве** (ARCH-03) | 3 файла импортируют `okhttp3.OkHttpClient` |
| мутации героя через `CommandQueue` с `Idempotency-Key` | соблюдено | `Transport.request`: каждый авторизованный POST получает ключ; очередь — кроме ROLLED/OWN_RETRY/READS/ADMIN (по замыслу) |

## [ARCH-01] Сотни балансных чисел берутся из значений по умолчанию Kotlin, а не из контента
- **Категория:** ARCHITECTURE. **Приоритет:** P2. **Уверенность:** высокая (проверено аудит-тестом).
- **Подсистема:** `rules/content` (`EngineRules.kt`, `Campaign.kt`, `Buffs.kt`, `Rift.kt`…).
- **Инвариант:** «Ни одного балансного числа в Kotlin: шансы, веса, цены, лимиты, таймеры, коэффициенты — только в `content/*.json` с типизированным чтением».
- **Факт:** `rules.json`/`campaign.json` не содержат многих полей; движок читает их из значений по умолчанию data-классов. Аудит-тест декодирует файл, кодирует с `encodeDefaults = true` и сравнивает: **209 путей** берут значение из Kotlin (10 в `rules.json`, 199 в `campaign.json`; `guilds.json`, `quests.json` — 0). Существенные:
  - `campaign.json.combat.accuracy` — вся формула попадания `{base 20, perLevel 3, perDexterity 0.5, hitScale 1.25, evasionDivisor 5, evasionPower 0.9, minHit 0.05}`;
  - `combat.buffs` (Натиск: +20% скор. атаки, +20 каста/движения 4 с; Нечестивая мощь 30%; Чародейский всплеск; Укрепление −20%), `combat.defence` (`suppressionCap 75`, `suppressed 40`, `deflectionCap 50`, `deflected 40`, `recoup 4`), `combat.buildup.*` (оглушение/заморозка/электрошок, пулы по редкости), `combat.critical.damage 100`;
  - `rarities[1|2].signaturePower` = **1.3 / 1.7** (RULES прямо ссылается на `rarities[].signaturePower`);
  - `maps.riskRarity 0.6`, `maps.tiers.sealFrom/sealDouble/sealChance 15/sealRares`, `maps.influence {chance 8, items 15, power 20, abyssCracks 2}`;
  - Разлом: `trials.rift.score`, `echo`, `nodes` (цены лавки, лечение отдыха 50%…), `champion`, все параметры стражей (`warden/devourer/lord`), веса даров/проклятий (100);
  - `rules.json`: `loot.experienceWindow {3, 16, 2.5, 1.5, 0.01}`, `loot.maxItemLevel 100`, `itemLevelBonus`, `craftItemLevelBonus 10`, `quality {max 20, steps, unveilChoices 3}`, `auction.historyDays/extendWindowHours/priceSales/priceLevelSpread`, `run.idleMinutes 60`;
  - `features.merchant.maxDiscount 90`, `features.traps.maxStrike 90`.
- **Доказательство:** `/home/user/audit/work/tests/out/kotlin_defaults.tsv` (209 строк, путь → значение).
- **Воспроизведение:** `server/rules/src/test/kotlin/audit/DefaultsAuditTest.kt`, `./gradlew --offline :rules:test --tests audit.DefaultsAuditTest`.
- **Последствия:** баланс правится только релизом кода (клиент+сервер), а не контентом; `RULES_VERSION`/хеш контента не меняются при правке такого дефолта в Kotlin — клиент и сервер могут расходиться, если сабмодуль отстал (сейчас — нет).
- **Масштаб:** все остальные файлы контента (`equipment`, `skills`, `tree`…) не проверялись этим методом.
- **Варианты:** (1) выписать дефолты в JSON и сделать поля обязательными (без `= …`); (2) проверка контента: «поле обязано присутствовать» для числовых полей правил; (3) оставить дефолты только для нейтральных значений (0/1/пусто).
- **Побочные эффекты:** правка JSON меняет хеш контента → сброс кэша клиента; результат роллов не меняется (если значения те же) — `RULES_VERSION` не нужен.
- **Регрессионный тест:** тот же приём «encodeDefaults-диф» как проверка контента.
- **Вопрос владельцу:** считать ли «нейтральные» дефолты (вес 100, множитель 1.0) нарушением?
- **Рекомендация:** (1)+(2) для `campaign.json.combat.*`, `rarities`, `rift`.
- **Статус:** открыто.

## [ARCH-02] Числа баланса и пороги прямо в коде клиента и сервера
- **Категория:** ARCHITECTURE. **Приоритет:** P3. **Уверенность:** высокая (код).
- **Факт (выборка):**
  - `ExileForge/core/.../campaign/Loadout.kt:328` `const val LOW_LIFE = 0.35` — порог «низкого здоровья» фляг и событий `LOW_LIFE` (`BattleTick.kt:165`); текст `stat.template.FLASK_LOW_LIFE_RECOVERY.ADD` дублирует «ниже 35%».
  - `BattleTick.kt:179,202` — «низкое здоровье» пассивок и `STOCK_LOW_LIFE_SPEED` = `maxLife / 2` (второй, другой порог); текст «пока здоровья меньше половины».
  - `Loadout.kt:308` `own("FLASK_LOW_LIFE_RECOVERY")` — код стата строкой мимо реестра (`Stats.kt` KDoc: «без строк `STOCK_*` по месту»).
  - `Spawns.kt:96` `BORROWED_MANA = 40.0`; `VaalZones.kt:13-14` `SHARE = 0.6`, `MIN_SIZE = 32`; `MapGenerator.kt:282-298` доли пропастей/вод.
  - `rules/roll/LootChests.kt:172` `top * .9` — «главные атрибуты класса» для подбора добычи сундука (в `rules/`!).
  - Сервер: `Plausibility.kt:42-64` (`BOSS_SECONDS 8`, `MARGIN 3`, `SLACK_SECONDS 10`, `AUDIT_SCORE 3`…), `TrialService.kt:325` `FLOOR_SECONDS 3`; `HeroService.kt:53` `MAX_NAME = 24` срезает `rules.json → inputs.heroName` (`minOf(MAX_NAME, …)`, `:92`) — поднять длину имени контентом нельзя.
- **Варианты:** вынести в `rules.json` (`fight.lowLife`, `fight.passiveLowLife`, `plausibility.*`), убрать `MAX_NAME`.
- **Вопрос владельцу:** 35% и 50% — два разных «низких здоровья» намеренно?
- **Статус:** открыто.

## [ARCH-03] `:app` держит OkHttp
- **Категория:** ARCHITECTURE. **Приоритет:** P3. **Уверенность:** высокая.
- **Файлы:** `app/.../di/AppModule.kt:86,116` (`single<OkHttpClient> { ForgeHttp.client }`), `presentation/app/SessionActions.kt:42,59,301` (`http.connectionPool.evictAll()`), `presentation/features/UpdateViewModel.kt:36,72,78`; `core/build.gradle.kts:12` `api("com.squareup.okhttp3:okhttp:5.5.0")` отдаёт OkHttp наружу.
- **Инвариант:** «`:app` — только Compose и Android; без OkHttp и расчётов».
- **Факт:** логики HTTP в app нет — только тип в DI и сброс пула; но зависимость и тип протекают.
- **Варианты:** спрятать за интерфейсом core (`Network.resetConnections()`), `api` → `implementation`.
- **Статус:** открыто.

## [ARCH-04] Недетерминированные кости в `rules` и разные кости сверки
- **Категория:** ARCHITECTURE. **Приоритет:** P3. **Уверенность:** средняя (трактовка инварианта).
- **Факт:** `rules/roll/Dice.kt:43` `Dice.system() = Dice(Random.Default)`; сервер катит им всё «по команде»: сферы/ремесло (`InventoryService.kt:262,286,320,338`), сундук (`:359`), задания (`QuestService.kt:52,139`), выдачи, ремёсла (`CraftsService.kt:184`). Сид не хранится → результат не воспроизводим (RULES: «роллится детерминированно в `rules/` через `Dice`/seed и подтверждается сервером» — для заходов это так: `Streams(seed)`; для команд — нет). `AuctionService.kt:260` сверяет вещь лота `Dice.system()`, тогда как `HeroRepository.kt:135` — `RECONCILE.of(item.id)` (RULES: «катит те же кости, что запись — от id копии»).
- **Последствия:** нет эксплойта (ролл на сервере); нельзя переиграть спорный ролл по логам; копия на аукционе сверяется иначе, чем у героя.
- **Варианты:** сид команды = хеш(Idempotency-Key, heroId) → `Dice(seed)`; лоты — `RECONCILE.of(item.id)`.
- **Вопрос владельцу:** требуется ли воспроизводимость роллов сфер?
- **Статус:** открыто.

---

## Что проверено
- Файлы: оба `RULES.md`, оба workflow, все тестовые классы (заголовки и утверждения выборочно: `LocalizationTest`, `IconsTest`, `UiStringsTest`, `ModifierCoverageTest`, `ContentLoadTest`, `PlausibilityTest`, `CommandMarkTest`, `ServerIntegrationTest`, `RiftTest`, части `RulesInvariantsTest`), `CommandQueue.kt`, `Transport.kt`, `Dice.kt`, `EngineRules.kt`, `Loadout.kt`, `BattleTick.kt`, `AuctionService.kt`, `HeroRepository.kt`.
- Аудит-тесты (только в копиях): `server/rules/src/test/kotlin/audit/{LocaleAuditTest,DefaultsAuditTest,VacuityAuditTest,SnapshotCostAuditTest}.kt`, `client/core/src/test/kotlin/audit/UiKeysAuditTest.kt`; выводы — `/home/user/audit/work/tests/out/`.

## Что не проверено
- `ktlintCheck`, сборка `:app`, lint, androidTest на эмуляторе, `client_server_test.py` (живой контракт с Mongo).
- Дефолты Kotlin в остальных файлах контента (кроме `rules/campaign/guilds/quests`).
- `01_PROJECT_INVENTORY.md` на момент работы не существовал — карта механик не использовалась.
