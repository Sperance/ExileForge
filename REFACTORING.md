# REFACTORING.md - план рефакторинга клиента

Временный документ: этапы и прогресс. Читай в начале каждой задачи по рефакторингу, отмечай сделанное, удали по завершении. Пуш и подъём версии - по завершённым этапам; клиент закрепляет сервер после его этапа. Решения зафиксированы в `RULES.md`.

## Этап 0 - защита
- [x] Golden-тест боя в `:core` (`CombatGoldenTest`, снимки в `core/src/test/resources/golden/`): четыре боя на фиксированных seed; перезапись `GOLDEN_UPDATE=1` только при осознанной смене правил.
- [ ] Golden-тест забега (`ExpeditionRun`) - перед распилом `ExpeditionRun.kt` (этап 5).

## Этап 1 - Koin
- [x] `koin-android`, `koin-androidx-compose`; `di/AppModule.kt`: сторы DataStore, журнал, область приложения, `ForgeViewModel`, `UpdateViewModel`.
- [ ] Модули network (`Transport`, `GameApi`, клиенты) и repositories - с этапом 2.
- [x] `ForgeApplication` стартует Koin; `MainActivity` без фабрик; `DraftStore` и `GuideStore` приходят через `koinInject`.
- [ ] `GameApi` - session-scope (пересоздаётся при смене сервера), `ForgeHttp` - singleton через Koin.

## Этап 2 - репозитории :core
- [ ] Истина в `:core`: `StateFlow`/`Flow`, без Android. ForgeState лишь отражает потоки для ещё не переведённых экранов.
  - [x] `SessionRepository` (сервер, аккаунт, герои аккаунта, здоровье сервера) и `WorldRepository` (контент, словарь, иконки, портреты).
  - [ ] `HeroRepository` (герой, снимки, синк), `MarketRepository`, `GuildRepository`, `QuestRepository`, `FeedbackRepository`.
  - [ ] Команды и чтения (`task`/`read`, busy/loading/failure, смена `GameApi`) - в `:core` (`CommandRunner`), чтобы модели экранов не зависели от `ForgeRuntime`.
- [ ] `ForgeState` распадается на срезы этих репозиториев; `sliced()` удалён.

## Этап 3 - навигация
- [ ] Navigation Compose, типизированные маршруты (`@Serializable`): Auth, Characters, Game{Hero, Expedition, Crafts, Progress, Tree, Grimoire, City{Quests, Merchant, Auction, Guild{…}}, Server, Settings, Admin, Redemption}, Run, Trial, Atlas, Warmup. Вложенные графы для гильдии и квестов.
- [ ] Гейтинг `Feature.ofTab` - guard при переходе. Фичи не пишут `tab`/`building`, а просят навигатор. Системный back работает везде; `settingsReturn` и ручные `BackHandler` удалены.

## Этап 4 - экраны
- [ ] Каждый экран - свой androidx `ViewModel` из Koin + `UiState`; экран не получает `ForgeViewModel`/`ForgeState`. Порядок (решение владельца): простые сначала - Settings → Server → Feedback/Mail → City (Quests, Merchant, Auction, Guild) → Crafts/Progress → Tree/Grimoire → Hero → Expedition/Atlas → Session/Characters; пуш после каждого экрана.
  - [x] Server (аккаунт/сервер/клиент): `ServerViewModel` над `SessionRepository` и `WorldRepository`; команды пока через общую модель.
  - [x] Settings: `PreferencesRepository` (единственный источник настроек устройства) + `SettingsViewModel`; `ForgeState.settings` лишь отражает поток для ещё не переведённых экранов.
- [ ] По ходу: файлы UI не длиннее ~400 строк (SkillTreeScreen, CraftsScreen, MapStyles, GrimoireScreen, ExpeditionScene, WorldArt, AtlasScreen, AuctionTabs).
- [ ] `ForgeViewModel`, `ForgeRuntime`, `FeatureViewModel` удалены.

## Этап 5 - бой
- [x] `Combat.kt` → пакет `core/campaign/combat/`: модель (Combatant, Foe, Ally, эффекты), `Battle` по секциям (views, tick, strike/land, skills, monsters, reach, log).
- [ ] Константы боя (`Battle.Companion`, файловые константы секций) - из контента.
- [x] `ArenaOverlay.kt` → пакет `ui/screens/expedition/arena/` (FightPalette, FightMarks, ArenaOverlay, FoeCard, HeroCard, ScoutPanel, FightFeed, Controls, VitalBars, ActionBar, StateTiles, FightLog общий с Report/CombatDetail).
- [ ] `ExpeditionRun.kt`, `ExpeditionWorld.kt` - по тем же правилам.

## Этап 6 - баланс и чистка
- [x] Правила сервера 1.74.5: окно «недавнего» события, пределы скорости атаки и потолок уровня умения читаются из контента (`CombatRules`, `SkillBookRules`), констант в `rules` больше нет.
- [ ] Константы боя и мира (LUNGE, ENTRY, STAGGER, HERO_SPEED, GATHER_RADIUS, LOW_LIFE, AUTO-RUN волны, …) - из `content/rules.json` через `rules`.
- [ ] Удалить неиспользуемое: `MailButton`, `classDescription`, `itemSources`, `rankIndexOf`.
- [ ] Удалить этот файл.
