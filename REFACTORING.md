# REFACTORING.md - план рефакторинга клиента

Временный документ: этапы и прогресс. Читай в начале каждой задачи по рефакторингу, отмечай сделанное, удали по завершении. Пуш и подъём версии - по завершённым этапам; клиент закрепляет сервер после его этапа. Решения зафиксированы в `RULES.md`.

## Этап 0 - защита
- [x] Golden-тест боя в `:core` (`CombatGoldenTest`, снимки в `core/src/test/resources/golden/`): четыре боя на фиксированных seed; перезапись `GOLDEN_UPDATE=1` только при осознанной смене правил.
- [x] Golden-тест забега в `:core` (`ExpeditionGoldenTest`, снимки `run-*.txt`): три автозабега на фиксированных seed и шаге - смерть, сундуки, босс и выход; перезапись `GOLDEN_UPDATE=1`.

## Этап 1 - Koin
- [x] `koin-android`, `koin-androidx-compose`; `di/AppModule.kt`: сторы DataStore, журнал, область приложения, `ForgeViewModel`, `UpdateViewModel`.
- [x] Модули network (`Transport`, `GameApi`, клиенты) и repositories - с этапом 2: репозитории - синглы Koin; `Transport` и клиенты маршрутов собирает `GameApi` (3.80.45).
- [x] `ForgeApplication` стартует Koin; `MainActivity` без фабрик; `DraftStore` и `GuideStore` приходят через `koinInject`.
- [x] `GameApi` - session-scope: его создаёт `SessionActions.newApi` на каждый сервер и публикует через синглтон `ServerConnection`; `OkHttpClient` процесса (`ForgeHttp.client`) - синглтон Koin, его получают сессия и обновления (3.80.45).

## Этап 2 - репозитории :core
- [x] Истина в `:core`: `StateFlow`/`Flow`, без Android. С 3.80.32 `ForgeState` - только проекция (`combine` правок всех источников в `ForgeRuntime.projection()`), в неё никто не пишет; логика читает источники.
  - [x] `SessionRepository` (сервер, аккаунт, герои аккаунта, здоровье сервера) и `WorldRepository` (контент, словарь, иконки, портреты).
  - [x] `FeedbackRepository` (предложения, отчёты, почта); `Notices` (тосты) и `GameEvents` (сигнал «герой изменился») в `:core`.
  - [x] `HeroRepository` (id героя и `HeroView`; кошелёк и сумка правятся только через него), `QuestRepository`, `ContentLoader` (делегат `ensureContent`).
  - [x] `MarketRepository` (витрина, свои лоты, сделки, полка торговца).
  - [x] `GuildRepository` (своя гильдия, поиск, журнал, хранилище).
  - [x] `CraftsRepository` (ремёсла, итоги сеанса, предсказанные циклы); `Buzzes` (вибрации) в `:core`.
  - [x] `HeroRepository` держит владельца, свежесть чтения, предмет под кузницей, её фразу и вскрытый сундук; `HeroSync` (части, снимки, лист, копия на устройстве) и `HeroActions` (все команды героя) - в `presentation/hero`; `HeroViewModel` из `features` удалён.
  - [x] Команды и чтения (`task`/`read`, busy/loading/failure, строка отказа) - `CommandRunner` в `:core`; `Phrase` тоже в `:core`.
  - [x] Текущий `GameApi` - `ServerConnection` в `:core`; `Reads` (ключи чтений) там же.
  - [x] `LanguageRepository` (язык интерфейса вместе с `uiLanguage`), `LinkRepository` (связь и очередь команд), `AdminRepository` (коды наград, тестовые учётки) в `:core`; `AppModes` (игрок/администратор) в приложении; фильтр журнала боя - в `PreferencesRepository`; прогрев - поток `WarmupViewModel` (3.80.32).
  - [x] Черновики - состояние экранов, не общее: адрес сервера (`ServerPage`), класс нового героя (`CharactersViewModel`), редкость и слот выдачи (отладочная панель) (3.80.32).
- [x] `ForgeState` распадается на срезы этих репозиториев; `sliced()` удалён (3.80.40): ни один экран не читает общее состояние.
  - [x] Аккаунт и настройки (`ServerScreen`, `SettingsScreen`, страницы тестера, отзывов и почты, `LoginForm`): срез `AccountUi` из `ServerViewModel` (3.80.33).
  - [x] Общий срез `GameUi` (команда в полёте, язык, мир, сессия, герой, связь, режим, настройки устройства) от синглтона `GameSlice`; модели экранов отдают его как `game`. Помощники «контент + герой» (`view`, `sellPrice`, `unmetFor`, `wearDelta`, `manaReserve`, `passiveShares`, сферы, валюты, атлас, древо) - один раз в `HeroLens`; `ForgeState.game` - мост для ещё не переведённых экранов (3.80.34).
  - [x] Город (площадь, квесты, торговец, аукцион, гильдия): `game: GameUi` из своих моделей, здание - параметр маршрута; общие `StackInfoSheet`, `StackIcon`, `WearPreview`, `ListingSheet` - на срезе (3.80.34).
  - [x] Ремёсла, кузница и хаб «Развитие»: `game` из `CraftsViewModel`, `SmithyViewModel`, `ProgressViewModel`; места развития (зверинец, испытания, хроника) пока на общем состоянии - переезжают с героем и походом (3.80.35).
  - [x] Древо и гримуар: `game` из `TreeViewModel` и `GrimoireViewModel` (3.80.36).
  - [x] Герой (шапка, сводка, снаряжение, сундук, сумка, зверинец, инкубатор, хроника, разбор характеристик, сферы администратора): `game` из `HeroViewModel`; `StatExplainer`/`TraceExplainer` - над `GameUi`; тесты панелей героя строят срез напрямую (3.80.37).
  - [x] Поход, испытания и атлас (карта мира, карточка зоны, бег, арена, отчёты, добыча, атлас): `game` из `ExpeditionViewModel`, своё состояние похода (карточка, добыча, атлас, журнал) - из её `state`; тосты (`notice`, `refusal`) - в срезе; места «Развития» - на срезе `ProgressViewModel` (3.80.38).
  - [x] Вход и меню героев (`game` из `SessionViewModel` и `CharactersViewModel`), почта и плашка пути изгнанника - на срезе (3.80.39). На общем состоянии остались только оболочка (`ForgeApp`, шапка, отчёт об ошибке) и отладочные экраны администратора.
  - [x] Оболочка (`ForgeApp`, нижняя панель, шапка, отчёт жука): `ShellViewModel` отдаёт срез, маршрут, прогрев, поход, испытание, журнал запросов и вибрации и сам запускает рантайм; отладочные экраны администратора - на `AdminViewModel`; `sliced()`/`common`/`toasts` удалены (3.80.40).
  - [x] `ForgeState`, `AccountState`, `PlayState`, `AdminState`, проекция в `ForgeRuntime` и их помощники удалены; типы режима, фазы, вкладок, зданий и сортировки - в `state/GameTypes.kt` (3.80.41).
  - [x] `ForgeViewModel` удалён (3.80.42): возврат, уход, сброс журнала похода и проверку обновлений ведёт `ShellViewModel` - один экземпляр у активности и композиции. Рантайм-синглтон больше не закрывается вместе с активностью (закрытый не запускался снова).
  - [x] `WorldLoader` (3.80.43): контент, словарь, иконки, портреты и смена языка - синглтон с внедрением через конструктор; рантайм лишь делегирует. Мёртвый флаг `contentStale` удалён.
  - [x] `ForgeRuntime` и `FeatureViewModel` удалены (3.80.44): фичи - сервисы `presentation/app` с внедрением через конструктор над общим `AppService` (`SessionActions` с созданием сервера и сбросом сессии, `ConnectionActions`, `CharacterActions`, `WarmupActions`, `RedemptionActions`); взаимные ссылки сессии, связи и героев - ленивые; запуск - `AppStartup`, его зовёт `ShellViewModel`.

## Этап 3 - навигация
- [x] Navigation 3 (`navigation3-runtime/ui` 1.2.0, вместо Navigation Compose: стек у приложения, без NavController): `presentation/nav/Route` - типизированные ключи Auth, Characters, Hero, Tree, Grimoire, Expedition, Crafts, Progress, Forge, Pets, Trials, Chronicle, City, Quests, Merchant, Auction, Guild, Account, Settings, Admin, Redemption, Atlas; `Navigator` - стек (вкладка сбрасывает до корня, подэкран ложится над корнем, аккаунт и настройки - поверх любого). Прогрев, поход и испытание - состояния игры поверх стека, не маршруты (их открывают и закрывают команды, не игрок). Разделы гильдии и квестов - состояние своих моделей, не стек.
- [x] Фичи не пишут `phase`/`tab`/`building` - просят навигатор; `ForgeState` лишь отражает его верх для экранов, что ещё читают номера вкладок. `settingsReturn` и `BackHandler` атласа удалены; системный «назад» снимает экран со стека.
- [x] Гейтинг уровня - `Navigator.gate`: закрытый экран не открывается, тост говорит, с какого уровня.
- [x] Оставшиеся `BackHandler` - локальные окна экранов (карточка зоны, окно профессии, карточка узла) и оверлеи похода, испытания и отчёта: остаются локальным состоянием экранов и их моделей - это окна поверх экрана, а не экраны стека (3.80.45). `BackRow` - кнопка «назад» внутри экрана и для его внутренних страниц.

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
  - [x] Progress: своя модель `ProgressViewModel` (срез «игра» и свежий герой) (3.80.35); выдачи тестера - у `HeroViewModel` (3.80.30), черновик выдачи - на отладочной панели (3.80.32).
  - [x] Expedition/Atlas/Trials, ядро: `ExpeditionRepository` (карточка зоны, добыча похода, окно атласа, счётчики журнала) в `:core`; `ExpeditionActions` и `TrialActions` в `presentation/expedition`; `ExpeditionViewModel`/`TrialViewModel` из `features` удалены.
  - [x] Expedition/Atlas/Trials, экраны: `ExpeditionViewModel` над действиями похода, испытаний и героя; карта мира, карточка зоны, доска испытаний и листы снаряжения/добычи только на ней, оверлеи похода/испытания/атласа - ещё и на `ForgeViewModel` ради фильтра журнала, тостов и вибрации.
- [x] Файлы UI не длиннее ~400 строк: SkillTreeScreen, CraftsScreen, GrimoireScreen, AuctionTabs, AtlasScreen, ExpeditionPlay, ZoneCard, CraftScreen, CombatDetail, ForgeApp (шапка в `ui/Banner.kt`) разбиты.
- [x] Файлы сцены похода: стили биомов (StoneStyles, WildStyles, SkyStyles), `ScenePainter` с секциями декора и объектов карты (SceneDecor, SceneSpots), наброски пергамента (`WorldSketches`) - в своих файлах.
- [x] `ForgeViewModel`, `ForgeRuntime`, `FeatureViewModel` удалены (3.80.42-3.80.44). Сделано (3.80.30): ни один экран не получает `ForgeViewModel` - оболочка (вкладки, здания, настройки, тосты, журнал, настройки устройства) в `ShellViewModel`, выдачи и код награды в `HeroViewModel`, тестовые учётки и связь в `SessionViewModel`; фасад остался только корню `ForgeApp`, `MainActivity` и отладочным экранам администратора.

## Этап 5 - бой
- [x] `Combat.kt` → пакет `core/campaign/combat/`: модель (Combatant, Foe, Ally, эффекты), `Battle` по секциям (views, tick, strike/land, skills, monsters, reach, log).
- [x] Константы боя из контента (сервер 1.74.6, клиент 3.80.31): `campaign.combat` - lunge, entry, stagger, minDot, petMendEvery, lifeDelay, selfBurn; технические STEP/TICK/NEVER/FOREVER и визуальный HIT_LIFETIME остаются кодом.
- [x] `ArenaOverlay.kt` → пакет `ui/screens/expedition/arena/` (FightPalette, FightMarks, ArenaOverlay, FoeCard, HeroCard, ScoutPanel, FightFeed, Controls, VitalBars, ActionBar, StateTiles, FightLog общий с Report/CombatDetail).
- [x] `ExpeditionRun.kt`, `ExpeditionWorld.kt`, `AutoRun.kt` → пакет `core/campaign/run/`: виды и команды (RunViews, RunCommand), `ExpeditionRun` по секциям (commands/answers, abyss, walk, fights, snapshot), `ExpeditionWorld` по секциям (agents, monsters, grid); golden-тесты забега не сдвинулись.

## Этап 6 - баланс и чистка
- [x] Правила сервера 1.74.5: окно «недавнего» события, пределы скорости атаки и потолок уровня умения читаются из контента (`CombatRules`, `SkillBookRules`), констант в `rules` больше нет.
- [x] Константы мира из контента (сервер 1.74.6, клиент 3.80.31): `campaign.expedition` (шаг и радиусы героя и монстров, дистанции, сундуки и источники, свет, волны и шаг автозабега, aftermath и stagePause) через `ExpeditionRules`; `ExpeditionWorld` и `AutoPilot` получают их конструктором.
- [x] Удалено неиспользуемое: `MailButton`, `classDescription`, `itemSources`, `rankIndexOf`.
- [ ] Удалить этот файл.

## Где остановились (3.80.45, сервер 1.74.6)
- Сделано: golden-тест забега, распил `ExpeditionRun`/`ExpeditionWorld`, файлы сцены, мёртвый код, константы боя и мира в контент, срезы `AccountUi`/`GameUi` для всех экранов; `ForgeState`, `ForgeViewModel`, `ForgeRuntime`, `FeatureViewModel` удалены - сервисы `presentation/app` и `WorldLoader`.
- Дальше по плану: серверный этап 3 (value-классы кодов, типизированные статы, sealed-иерархии, `API_REVISION` 43).
