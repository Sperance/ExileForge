# Инвентаризация ExileForge (аудит 4.8.x)

Состояние на начало аудита: клиент `Sperance/ExileForge@claude/sharp-bardeen-6i3dkz` HEAD `1c7c0c82`, сервер
`Sperance/ktor-bestgame@claude/tender-pasteur-a36kj2` HEAD `e3515f2`; сабмодуль `backend/` закреплён ровно на `e3515f2`
(совпадает с веткой сервера). Рабочие деревья чистые, ветки свежие относительно `origin`.

## Размер кода

| Часть | Файлов `.kt` | Строк |
|---|---|---|
| Сервер `src/` + общий модуль `rules/` | 281 (из них `rules/` 91) | ~38 тыс. |
| Клиент `:core` (JVM) | 168 | |
| Клиент `:app` (Compose) | 323 | ~77 тыс. вместе с `:core` |
| Тесты: сервер / `rules` / `:core` / `:app` | 8 + `CommandMarkTest`, `PlausibilityTest` / 10 / 16 + `CommandQueueRoutesTest` / 7 | |

## Контент (`backend/src/main/resources/content`, грузит `ContentLoader`, проверяет на старте)

| Файл | Что | Объём |
|---|---|---|
| `equipment.json` | шаблоны вещей: 539 уникальных, 29 мифических, 261 редких, 51 волшебных, 103 базовых; 23 слота | 983 |
| `modifiers.json` | семейства модификаторов (всего описаний 3891) | 893 |
| `items.json` | стопки: сферы, печати, руны, яйца, материалы | 608 |
| `campaign.json` | монстры 251, 9 регионов, 150 зон, сундуки, механики карт, Ваал, Бездна, испытания | |
| `atlas.json` / `tree.json` | узлы Атласа / дерева умений | 616 / 810 |
| `skills.json` | 104 умения 7 классов, 18 умений монстров, 18 рун | |
| `powers.json` | силы уникальных (`POWER_`) | 811 |
| `stats.json` | характеристики | 1289 |
| `tables.json` | таблицы добычи и бросков | 405 |
| `pets.json`, `fates.json`, `quests.json`, `professions.json`, `essences.json`, `guilds.json`, `achievements.json`, `rules.json`, `classes.json` | питомцы 65 видов, 29 даров, сюжет 9 глав, 7 профессий, сущности, гильдии, 38 достижений, правила движка, 7 классов | |
| `locale/{ru,en,common}.json` | словари сервера: по 10 946 строк ru/en, 1591 общих | |

## Карта подсистем

| Подсистема | Общие правила (`rules/`) | Сервер (`src/main/.../features`) | Клиент (`core`/`app`) | Тесты |
|---|---|---|---|---|
| Контент и версии | `content/Content.kt` (`RULES_VERSION`, хэш), `ContentLoader`, `EngineRules` | `config/ContentStore`, манифест `/static/index.json`, `/content/{file}` | `WorldLoader`, `GameApi.manifest` (ETag) | `ContentLoadTest`, `LocalizationTest`, `UiStringsTest`, `ContentSmokeTest` |
| Предметы, редкость, сферы | `roll/ItemFactory`, `Orbs`, `AffixRoller`, `Veils`, `Desecrations`, `Bench`, `Quality`, `AtlasSeals` | `logic/inventory`, `HeroRoute` (`/orb`, `/seal`, `/essence`, `/craft`, `/unveil`, `/choose`) | `HeroClient`, кузница `ui/screens/forge` | `RulesInvariantsTest`, `CraftingTest`, `BaseVarianceTest` |
| Добыча и забеги | `run/Run.kt`, `roll/Loot.kt`, `LootChests`, `MapFeatures`, `RewardDraws` | `logic/campaign/CampaignService`, `Plausibility`, `MapFeatureEvents`, `PendingLoot` | `core/campaign/run` (`ExpeditionRun`, `RunJournal`), `ExpeditionActions` | `ExpeditionGoldenTest`, `PendingLootTest`, `ServerIntegrationTest` |
| Бой | лист `sheet/*`, `Might`, `FightQueue`, `EnrageRule` | только правдоподобие журнала | `core/campaign/combat` (`Battle`, `BattleTick`) | `CombatGoldenTest`, `BossPhaseTest`, `RiftCombatTest` |
| Испытания (башня, раш, Разлом) | `content/Trials.kt`, `rift/RiftRun.kt` | `TrialService`, `RiftService`, `RiftAwards` | `TrialActions`, `RiftActions` | `RiftTest`, `TrialChoicesTest` |
| Атлас, дерево, законы тронов | `ThroneLaws`, `content/Atlas.kt`, `Tree.kt` | `logic/atlas`, `logic/tree`, `logic/quests` | экраны дерева и Атласа | `AtlasLayoutTest`, `TreeLayoutTest`, `QuestProgressTest` |
| Экономика | `AuctionRules`, `StashTakePrice`, `SellPrice` | `trade/AuctionService`, `MerchantService`, `guild/*`, `mail/*`, `redemption` | `AuctionClient`, `GuildClient`, `FeedbackClient` | `ServerIntegrationTest` |
| Питомцы, ремёсла | `roll/Menagerie`, `PetForge`, `Work` | `logic/pets`, `logic/crafts` | `CraftsActions` | `IncubatorTest`, `CraftingTest` |
| Аутентификация, модерация | — | `logic/auth` (`AccessPolicy`, `HeroGate`, `ProgressGate`, `SessionCache`), `logic/moderation` (`ModerationPolicy`) | `GameApi.login*`, `ModerationClient` | `AuthTest` |
| Мутации и надёжность | — | `HeroLocks`, `MongoFactory.transactionExecute`, `server/addons/Idempotency`, `IdempotentReplyStore`, `base/repository` (версии) | `CommandQueue`, `Transport`, `CommandRunner` | `CommandMarkTest`, `CommandQueueRoutesTest` |
| Схемы MongoDB | — | `config/CollectionSchemas`, `DatabaseSeeder` | — | `CollectionSchemasTest` |

## Граф влияния (что затрагивает больше всего)

```
content/*.json ──> ContentLoader ──> ContentIndex ──┬─> ItemFactory/Orbs/Loot ──> сервер (выдача) и клиент (показ, превью)
   (RULES_VERSION в хэше)                            ├─> Run (seed) ───────────> клиент: автозабег/бой; сервер: повтор журнала, награды
                                                     └─> Sheet/Might ──────────> клиент: бой, сводка; сервер: правдоподобие (DPS)
HeroLocks + transactionExecute + Idempotency ──> каждая мутация героя (≈150 POST-маршрутов)
BaseRepository (версии, EntityCodec $set по полям) ──> все записи Hero/Guild/AuctionLot/Mail/User
Transport/CommandQueue (клиент) ──> каждая команда и повтор
```

Узлы с наибольшим радиусом: `ContentIndex`, `Run`, `ItemFactory`, `BaseRepository`, `Idempotency`, `Transport`.

## Сетевые маршруты

Полный реестр маршрутов с авторизацией, замком, транзакцией и идемпотентностью - `API_TRAFFIC.md`, раздел «Реестр API».

## Инструменты аудита (в репозитории клиента, вне CI)

- `./gradlew :core:loadRun -Purl=http://localhost:8081/ -Pplayers=N -Pseconds=S -Pramp=R -Pprofile=mixed|fight|browse|idle -Pspeedup=K` -
  виртуальные игроки на настоящем `GameApi` и автозабеге правил (`core/src/load/.../LoadRun.kt`).
- `./gradlew :core:probeRun -Padmin=<пароль> -Pdb=<база>` - пробы эксплойтов и гонок (`ExploitProbes.kt`).
- Оба отказываются работать не против `localhost`.
