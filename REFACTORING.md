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
  - [x] `FeedbackRepository` (предложения, отчёты, почта); `Notices` (тосты) и `GameEvents` (сигнал «герой изменился») в `:core`.
  - [x] `HeroRepository` (id героя и `HeroView`; кошелёк и сумка правятся только через него), `QuestRepository`, `ContentLoader` (делегат `ensureContent`).
  - [x] `MarketRepository` (витрина, свои лоты, сделки, полка торговца).
  - [x] `GuildRepository` (своя гильдия, поиск, журнал, хранилище).
  - [x] `CraftsRepository` (ремёсла, итоги сеанса, предсказанные циклы); `Buzzes` (вибрации) в `:core`.
  - [x] `HeroRepository` держит владельца, свежесть чтения, предмет под кузницей, её фразу и вскрытый сундук; `HeroSync` (части, снимки, лист, копия на устройстве) и `HeroActions` (все команды героя) - в `presentation/hero`; `HeroViewModel` из `features` удалён.
  - [x] Команды и чтения (`task`/`read`, busy/loading/failure, строка отказа) - `CommandRunner` в `:core`; `Phrase` тоже в `:core`.
  - [x] Текущий `GameApi` - `ServerConnection` в `:core`; `Reads` (ключи чтений) там же.
- [ ] `ForgeState` распадается на срезы этих репозиториев; `sliced()` удалён.

## Этап 3 - навигация
- [x] Navigation 3 (`navigation3-runtime/ui` 1.2.0, вместо Navigation Compose: стек у приложения, без NavController): `presentation/nav/Route` - типизированные ключи Auth, Characters, Hero, Tree, Grimoire, Expedition, Crafts, Progress, Forge, Pets, Trials, Chronicle, City, Quests, Merchant, Auction, Guild, Account, Settings, Admin, Redemption, Atlas; `Navigator` - стек (вкладка сбрасывает до корня, подэкран ложится над корнем, аккаунт и настройки - поверх любого). Прогрев, поход и испытание - состояния игры поверх стека, не маршруты (их открывают и закрывают команды, не игрок). Разделы гильдии и квестов - состояние своих моделей, не стек.
- [x] Фичи не пишут `phase`/`tab`/`building` - просят навигатор; `ForgeState` лишь отражает его верх для экранов, что ещё читают номера вкладок. `settingsReturn` и `BackHandler` атласа удалены; системный «назад» снимает экран со стека.
- [x] Гейтинг уровня - `Navigator.gate`: закрытый экран не открывается, тост говорит, с какого уровня.
- [ ] Оставшиеся `BackHandler` - локальные окна экранов (карточка зоны, окно профессии, карточка узла): оставить как локальное состояние или сделать маршрутами.

## Этап 4 - экраны
- [ ] Каждый экран - свой androidx `ViewModel` из Koin + `UiState`; экран не получает `ForgeViewModel`/`ForgeState`. Порядок (решение владельца): простые сначала - Settings → Server → Feedback/Mail → City (Quests, Merchant, Auction, Guild) → Crafts/Progress → Tree/Grimoire → Hero → Expedition/Atlas → Session/Characters; пуш после каждого экрана.
  - [x] Server (аккаунт/сервер/клиент): `ServerViewModel` над `SessionRepository` и `WorldRepository`; команды пока через общую модель.
  - [x] Settings: `PreferencesRepository` (единственный источник настроек устройства) + `SettingsViewModel`; `ForgeState.settings` лишь отражает поток для ещё не переведённых экранов.
  - [x] Feedback/Mail: `FeedbackViewModel` над `FeedbackRepository`, `ServerConnection`, `CommandRunner`; листы и админ-страницы берут модель из Koin, `ForgeViewModel` фидбэка не знает.
  - [x] Quests (доска Города и вкладка гильдии): `QuestActions` (общие действия, зовут их и прогрев, и конец похода) + `QuestViewModel`; раздел доски - состояние модели экрана.
  - [x] Merchant и Auction: `MarketActions` + `MarketViewModel`; продажа из сундука героя пока через `ForgeViewModel` → `MarketActions`, `ensureHero`/`autoSell` - до переноса экрана героя.
  - [x] Guild (зал и все разделы): `GuildActions` + `GuildViewModel`; `ensureHero` на входе - до переноса экрана героя.
  - [x] Crafts: `CraftsActions` (будильник цикла, посадка ответа) + `CraftsViewModel` (окно профессии); экран разбит на четыре файла ≤300 строк.
  - [x] Tree: `TreeViewModel` (узел под курсором, поиск) над `HeroActions`; экран разбит на четыре файла ≤~400 строк.
  - [x] Hero (сундук, снаряжение, сумка, зверинец, хроника): `HeroViewModel` над `HeroActions`/`HeroSync`/`MarketActions`; переходы и выбор кузницы пока через `ForgeViewModel`.
  - [x] Forge (кузница): `SmithyViewModel` (сфера, эссенция, предзнаменование, раздел; сфера по умолчанию - из контента) над `HeroActions`; `PlayState` больше не хранит выбор кузницы.
  - [x] Grimoire: `GrimoireViewModel` над `HeroActions`; экран разбит на три файла.
  - [x] Session/Characters: `SessionViewModel` и `CharactersViewModel` над `ForgeRuntime` (он теперь Koin-single, один на процесс); сама логика входа и фаз уедет с навигацией.
  - [ ] Progress: своя модель; выдачи тестера (`grant*`) - в модель отладочной панели.
  - [x] Expedition/Atlas/Trials, ядро: `ExpeditionRepository` (карточка зоны, добыча похода, окно атласа, счётчики журнала) в `:core`; `ExpeditionActions` и `TrialActions` в `presentation/expedition`; `ExpeditionViewModel`/`TrialViewModel` из `features` удалены.
  - [x] Expedition/Atlas/Trials, экраны: `ExpeditionViewModel` над действиями похода, испытаний и героя; карта мира, карточка зоны, доска испытаний и листы снаряжения/добычи только на ней, оверлеи похода/испытания/атласа - ещё и на `ForgeViewModel` ради фильтра журнала, тостов и вибрации.
- [x] Файлы UI не длиннее ~400 строк: SkillTreeScreen, CraftsScreen, GrimoireScreen, AuctionTabs, AtlasScreen, ExpeditionPlay, ZoneCard, CraftScreen, CombatDetail, ForgeApp (шапка в `ui/Banner.kt`) разбиты.
- [ ] Файлы сцены похода (MapStyles, ExpeditionScene, WorldArt) - с распилом `ExpeditionWorld` (этап 5).
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
