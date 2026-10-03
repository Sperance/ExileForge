# REFACTORING.md - план рефакторинга клиента

Временный документ: этапы и прогресс. Читай в начале каждой задачи по рефакторингу, отмечай сделанное, удали по завершении. Пуш и подъём версии - по завершённым этапам; клиент закрепляет сервер после его этапа. Решения зафиксированы в `RULES.md`.

## Этап 0 - защита
- [ ] Golden-тесты в `:core`: лог боя (`Battle`) и забега (`ExpeditionRun`) на фиксированных seed и контенте; перезаписываются только при смене `RULES_VERSION`.

## Этап 1 - Koin
- [ ] `koin-core`, `koin-android`, `koin-androidx-compose`. Модули: stores (DataStore), network (`Transport`, `GameApi`, клиенты), repositories, viewModels.
- [ ] `ForgeApplication` стартует Koin; `MainActivity` без фабрик. `GameApi` - session-scope (пересоздаётся при смене сервера), `ForgeHttp` - singleton через Koin, `DraftStore` не создаётся в composable.

## Этап 2 - репозитории :core
- [ ] Истина в `:core`: `SessionRepository` (аккаунт, сервер, язык), `HeroRepository` (герой, снимки, синк), `ContentRepository` (манифест, индекс, локаль), `MarketRepository`, `GuildRepository`, `QuestRepository`, `FeedbackRepository` - `StateFlow`/`Flow`, без Android.
- [ ] `ForgeState` распадается на срезы этих репозиториев; `sliced()` удалён.

## Этап 3 - навигация
- [ ] Navigation Compose, типизированные маршруты (`@Serializable`): Auth, Characters, Game{Hero, Expedition, Crafts, Progress, Tree, Grimoire, City{Quests, Merchant, Auction, Guild{…}}, Server, Settings, Admin, Redemption}, Run, Trial, Atlas, Warmup. Вложенные графы для гильдии и квестов.
- [ ] Гейтинг `Feature.ofTab` - guard при переходе. Фичи не пишут `tab`/`building`, а просят навигатор. Системный back работает везде; `settingsReturn` и ручные `BackHandler` удалены.

## Этап 4 - экраны
- [ ] Каждый экран - свой androidx `ViewModel` из Koin + `UiState`; экран не получает `ForgeViewModel`/`ForgeState`. Порядок: Session/Characters → Hero → Expedition/Atlas → Crafts/Progress → Tree/Grimoire → City (Quests, Merchant, Auction, Guild) → Server/Settings → Admin/Redemption.
- [ ] По ходу: файлы UI не длиннее ~400 строк (SkillTreeScreen, CraftsScreen, MapStyles, GrimoireScreen, ExpeditionScene, WorldArt, AtlasScreen, AuctionTabs).
- [ ] `ForgeViewModel`, `ForgeRuntime`, `FeatureViewModel` удалены.

## Этап 5 - бой
- [ ] `Combat.kt` → пакет `core/campaign/combat/`: модель (Combatant, Foe, Ally, эффекты), `Battle` по секциям (skills, tick, strike/land, traits, powers, log), константы - из контента.
- [ ] `ArenaOverlay.kt` → компоненты (FoeCard, HeroCard, FightFeed, Controls, FightLog общий с Report/CombatDetail).
- [ ] `ExpeditionRun.kt`, `ExpeditionWorld.kt` - по тем же правилам.

## Этап 6 - баланс и чистка
- [ ] Константы боя и мира (LUNGE, ENTRY, STAGGER, HERO_SPEED, GATHER_RADIUS, LOW_LIFE, AUTO-RUN волны, …) - из `content/rules.json` через `rules`.
- [ ] Удалить неиспользуемое: `MailButton`, `classDescription`, `itemSources`, `rankIndexOf`.
- [ ] Удалить этот файл.
