# Android: производительность, жизненный цикл, сеть клиента

**Ограничение окружения:** эмулятора и устройства в аудите не было - профилировщики CPU/Memory, Macrobenchmark, StrictMode и
Perfetto не запускались, FPS и пропуски кадров не измерены. Всё ниже - чтение кода (`CONFIRMED` - видно в коде, `PLAUSIBLE` -
влияние на кадры требует профилирования). Сеть клиента - `API_TRAFFIC.md`.

## Главное по влиянию

| ID | Приор. | Статус | Где | Суть |
|---|---|---|---|---|
| AP-1 | P2 | CONFIRMED код, влияние PLAUSIBLE | `core/campaign/run/ExpeditionRun.kt` 348-367, `RunSnapshot.kt`, `ui/screens/expedition/ExpeditionPlay.kt:72` | `update(dt)` каждый vsync делает `state.value = snapshot()`, а `RunHud` меняется каждый кадр (`tally.seconds += dt`, `FightHud.hits[].age`) - равенство не срабатывает, `collectAsState` перекомпонует весь `ExpeditionPlay` (карта, арена, лента, панели) 60-120 раз/с. То же `TrialScreen.kt` 44-53, `RiftScreen.kt` 623-632 |
| AP-2 | P2 | CONFIRMED | `core/campaign/FightViews.kt` 107, 155, 179; `RunSnapshot.kt`; `arena/FightFeed.kt:35`, `FightLog.kt:43` | Каждый кадр - O(длина журнала боя): фильтр всех событий ради ударов последней секунды, полная копия журнала `toList().asReversed()`, `heroCast()` с `indexOfLast`, сводки `summary`, фильтр ленты. Стоимость кадра растёт с длиной боя - долгие бои с боссом на ×4 |
| AP-3 | P3 | CONFIRMED (по дизайну) | `scene/ExpeditionScene.kt:51`, `combat/Battle.kt` 442, 584, `BattleTick.kt` | Бой шагает в `withFrameNanos` на главном потоке, фиксированный шаг 1/60: на ×4 - 4 шага за кадр плюс п. 1-2; `everyone.sortedBy` аллоцирует каждый шаг; при FPS < 20 бой замедляется (`dt.coerceAtMost(.05)`) |
| AP-4 | P3 | CONFIRMED аллокации | `scene/ScenePainter.kt` 60-260, `FightStage.kt`, `arena/BossBand.kt`, `components/Relic.kt` (`relicCorners`) | В каждом кадре - списки лямбд с упакованными `Double` на клетку, `sortedBy`, 6+ промежуточных `filter`, новые `Path()` и по 8 `Brush` | 
| AP-5 | P3 | **ИСПРАВЛЕН** | `scene/PortraitMotion.kt` (`portraitClock`), `ReportScreen.kt` (`FieldHead`) | Нарушение `RULES.md` (часы декора одни - `DecorClock`): `portraitClock` вёл свой цикл `withFrameNanos`; дыхание портрета в отчёте боя шло на `rememberClock` без `LocalMotion` - декор не останавливался при выключенных анимациях. Теперь оба читают `decorSeconds()` (общие часы, покой без `LocalMotion`). `rememberClock` остался только у боя (`ArenaOverlay`), бой анимируется всегда |
| AP-6 | P3 | CONFIRMED | `arena/FightLog.kt:45` (+ `GuildLog.kt:26`, `GuildStash.kt:112`, `CraftsScreen.kt:141`, `StatBreakdownSheet.kt:95`) | `itemsIndexed` без `key` при «новые сверху»: каждое событие сдвигает индексы - перекомпонуются все видимые строки, прокрутка прыгает |
| AP-7 | P3 | CONFIRMED | `crafts/CraftsScreen.kt:323` | `CycleBar` - `while(true) withFrameMillis` ради полосы цикла длиной в десятки секунд: экран ремёсел держит полный FPS | 
| AP-8 | P3 | CONFIRMED код | `presentation/state/SheetView.kt:105`, `ui/ForgeApp.kt` 344-345 | `treeState()` создаёт `SheetCalculator` и раскрывает дерево при каждом вызове - из нижней навигации на каждую отдачу `GameUi`; так же `game.bench`, `orbs`, `currencies`, `progress` без кэша |
| AP-9 | P3 | CONFIRMED | `components/Motion.kt:49` (`DecorClockHost`) | `withInfiniteAnimationFrameNanos` просит каждый vsync, пока анимации включены, даже без читателей декора - Choreographer активен 60-120 Гц, состояние меняется 30 раз/с (батарея, PLAUSIBLE) |
| AP-10 | P3 | CONFIRMED | `NeonSkillIcon.kt:82`, `Relic.kt` 323, 375, `RollMarks.kt:54` и др. | `motionClock()` читается в композиции - перекомпоновка 30 раз/с вместо перерисовки; `motionPhase` (чтение в draw) дешевле |

## Жизненный цикл и потоки

- `collectAsState` без lifecycle в `ExpeditionPlay`, `TrialScreen`, `RiftScreen`, `TrialsBoard` - источники горячие `StateFlow` без
  работы вверх по потоку: утечки нет, влияние низкое.
- `StallWatchdog` - поток-демон постит в главный Looper раз в секунду и в фоне (мелкие пробуждения).
- Синглтонные `CoroutineScope` (`SecretVault`, `AppModule`) - на время процесса, утечек не найдено. Тикеры `delay` в
  `LaunchedEffect`/`produceState` отменяются с композицией.
- Блокирующего ввода-вывода и разбора контента на главном потоке не найдено: `WorldLoader` - `Dispatchers.Default`, `ServerStore` - `IO`.
- Мёртвые импорты анимаций (`SkillTreeScreen`, `TreeDetails`, `TreeSearch`, `SceneDecor`, `SceneSpots`) - мусор, не нагрузка.

## Что нужно, чтобы закрыть п. AP-1…AP-4 измерением

1. Macrobenchmark сценария «поход → бой с боссом 3 мин на ×4» с `FrameTimingMetric` на эталонном устройстве.
2. Perfetto-трасса того же сценария: доля `Recomposer`, `ExpeditionRun.update`, `Battle.hud`.
3. Исправление AP-1 (разделить данные кадра и HUD: данные кадра читаются в draw как `State`, HUD отдаётся только при изменении,
   секунды в `MapTally` - с шагом 1 с) - крупная переделка экрана боя, не делалась в аудите без замера «до».
