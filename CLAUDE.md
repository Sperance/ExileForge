# CLAUDE.md

Guidance for AI assistants working in this repository.

## What this project is

ExileForge is an **Android Compose client** (version 1.10.0, `versionCode` 11) for the
**ktor-bestgame** RPG server (0.12.0, **API revision 3**), pinned in
`core/.../contract/Contract.kt` as `SERVER_COMMIT = 24ed09b867fc559504333d8ee6e3b03e133ecfb6`
on the server branch `refactor/compact-rpg-architecture`.

The client is deliberately **thin**: the server owns items, stats, crafting, combat and the
passive tree. This client renders server state, sends commands, and never recomputes game
numbers locally.

## Repository layout

```
build.gradle.kts, settings.gradle.kts   Gradle 9.7.1, AGP 9.4.0, Kotlin 2.4.20, modules :app and :core
core/                                   Pure JVM library (java-library + kotlin-jvm, toolchain 17)
  contract/      Contract.kt            Wire JSON, validation, editable/protected fields, templates, requireId
                 CharacterContract.kt   Character document validation
  network/       GameApi.kt             The single HTTP client (OkHttp) for every server route
                 ItemRepository.kt      CRUD interface implemented by GameApi (lets tests fake it)
                 ApiFailure/FailureState/RequestJournal/RequestLog/ItemPage/HttpPayload
  model/         Catalog, EntitySource, CatalogFilter, EquipmentKind
                 command/Commands.kt    Serializable commands, EquipmentSlot, CalculatedStats, EquipmentView, ApiCapabilities
                 hero/, combat/, passives/, modifier/, character/
  i18n/          Loc.kt                 Lang (RU/EN), `tr(ru, en)` and the global `uiLanguage`
  editor/        EditorSchema.kt        Declarative form schemas (FormField/InputSpec) used by the admin editor
                 conflict/ThreeWayMerge.kt
  generation/    ItemGenerator, ModifierSelection, PresetDefinitions
  display/       ItemPresentation.kt    Display-only projections and Russian titles
  verification/  CrudScenario.kt        Admin-only self-check run from the Checks screen
app/                                    Android application (minSdk 26, compile/target SDK 37)
  MainActivity.kt, ForgeApplication.kt  Entry points; Application owns RequestJournal + ServerStore
  presentation/  ForgeRuntime.kt        Shared coroutine scope, GameApi instance, MutableStateFlow<ForgeState>
                 ForgeViewModel.kt      Lifecycle owner and thin facade delegating to feature models
                 features/              Catalog, Editor, Hero, Session, Passive, Combat, Checks view models
                 state/ForgeState.kt    One immutable state object for the whole app
  ui/            ForgeApp.kt            Scaffold, banner with RU/EN switch, bottom navigation, tab dispatch
                 screens/               catalog, editor, inventory (Hero/Forge), combat, passives, checks, server
                 components/            ItemCard, PropertyRow, InfoCard, spinners and Ornament.kt (ForgePanel/ScreenHeader/OrnateDivider/StatGlobe)
                 forms/, icons/ (ForgeGlyphs vector set, ItemEmblem), theme/
  data/settings/ServerStore.kt          DataStore Preferences: base URL, saved filters, pending commands
docs/                                   Russian reference docs (API_CONTRACT, COMBAT, PASSIVES, VALIDATION)
scripts/client_server_test.py           Boots the real backend + MongoDB and runs ServerIntegrationTest
.github/workflows/android.yml           `build` job (unit/lint/APK/emulator UI) and `client-server` job
```

`core` must stay free of Android and Compose imports — it is a plain JVM library and is
compiled by unit tests without an Android SDK. Put anything reusable and UI-independent there.

## Build and verification

Requires **JDK 17** (Gradle toolchain) and **Android SDK 37** for `:app` tasks.
`gradlew` is committed without the executable bit, so invoke it as `bash gradlew` (CI does
`chmod +x gradlew` first).

```bash
bash gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Note: a bare container without a JDK 17 toolchain or Android SDK (as in cloud sessions) cannot
run these — the Gradle toolchain download fails and `:app` tasks need the SDK. In that case
rely on CI and keep changes reviewable by reading, rather than claiming a local build passed.

- `:core:test` — JUnit + `kotlin-test` + **MockWebServer**; no device or SDK needed.
- `:app:connectedAndroidTest` equivalents run in CI on an **API 35 emulator** via
  `adb shell am instrument`; the workflow greps for `OK (n tests)`.
- `ServerIntegrationTest` is **opt-in**: it is skipped (`assumeTrue`) unless `EF_LIVE_URL` is
  set. The `client-server` CI job builds the pinned backend, starts an isolated MongoDB replica
  set and runs it through `scripts/client_server_test.py`.
- `DesignPreviewTest`, `EquipmentPanelTest` and `PassiveTreeTest` capture screenshots into the
  app's external files dir (`design/arsenal.jpg`, `hero.jpg`, `passives.jpg`); CI pulls them and
  base64-prints them into the log. Do not remove those captures — the workflow fails without the files.

## Architecture and state

**One state object.** `ForgeState` (a single `data class`) holds everything: session, catalog
page, editor draft, hero/inventory, combat, passives, failures. Derived permissions are computed
properties on it: `isAdmin`, `adminTools`, `canEdit`, `ownsCharacter`.

**ForgeRuntime** owns the `SupervisorJob` scope, the `MutableStateFlow<ForgeState>`, the current
`GameApi`, and instantiates the seven feature view models. It also provides shared helpers:

- `task(writing = false) { ... }` — the standard action wrapper. It refuses to start while
  `busy`, clears previous message/error/failure, runs the block on the runtime scope, maps
  exceptions to `FailureState` + a Russian user message, and always clears `busy`. **Every
  user-triggered server call goes through `task`.** Pass `writing = true` for mutations so that
  IO/5xx failures are classified as `UncertainWrite` rather than `Offline`.
- `loadPage`, `setEditor`, `pinnedDefinitions`, `clearSession`, `newApi`.

**Feature view models** (`presentation/features/*`) hold no state of their own; they read
`runtime.state.value` and `mutable.update { it.copy(...) }`. They are written as
`fun x() { with(runtime) { task { ... } } }`.

**ForgeViewModel** is only a lifecycle owner plus a compatibility facade: every public method
forwards to a feature model. Add new actions to the feature model **and** expose a one-line
delegate here, because screens receive `ForgeViewModel`.

**Navigation** is an `Int` tab in state, dispatched by a `when` in `ForgeApp`:
`0` catalog/characters, `1` editor, `2` checks (admin only — `ForgeRuntime.tab` blocks it
otherwise), `3` account/server, `4` hero, `5` forge (`InventoryForge(forgeOnly = true)`),
`6` combat, `7` passive tree (not in the bottom bar; reached from Hero/Combat).
`ForgeApp` re-`key`s the whole tree on `server` and `sessionEpoch`, so a logout or server change
discards per-screen Compose state.

## Server contract rules (do not violate)

These are enforced by tests and are the point of the client's design:

1. **The server is authoritative.** Never compute damage, stats, loot chance or passive bonuses
   locally. Unsupported effects are listed explicitly (`CalculatedStats.unsupported`,
   `Battle.unsupportedStats`) rather than approximated.
2. **Identity comes from the server.** POST sends documents without `_id`; `requireId` demands
   24 hex chars before any request is built.
3. **Optimistic concurrency everywhere.** PUT sends `{expectedVersion, changes}`, DELETE sends
   `{expectedVersion}`; versions are Kotlin `Long` and must survive values above JavaScript's
   safe-integer range. A mutation is **never** silently retried with a fresher version.
4. **409 opens conflict review.** The draft is preserved, `conflict = true`, and
   `ThreeWayMerge.review` transfers disjoint changes while overlapping fields require an explicit
   choice. Arrays (modifiers) are atomic — never merged element-wise.
5. **401 clears the session.** `GameApi(onUnauthorized = ...)` → `clearSession()`, which drops
   the token, journal and all account-specific UI state, and bumps `sessionEpoch`.
6. **Uncertain writes are surfaced, not retried.** `FailureState.UncertainWrite` (IO error or
   5xx on a write) tells the user to refresh. The exception: craft/drop, combat actions and
   passive changes carry a stable `requestId` and are persisted to DataStore **before** sending,
   so "Подтвердить действие" replays the identical payload. Pending keys are scoped
   `"$server:$profileId:$characterId"` so switching account or character never replays someone
   else's command. Only explicit client rejections (400/403/404/409/422) clear the pending record.
7. **Capabilities gate features.** `ApiCapabilities.requireWorkbench()` before login;
   `capabilities().combat` and `.passiveTree` are checked before those screens load.
8. **Secrets never reach the journal.** `request(sensitive = true)` for login and password
   change; bodies are stored as `[скрыто]`. Tokens live in memory only.
9. **Template vs. instance.** Rolled instance modifiers are changed only through server crafting
   commands; `validateReferenceWrite` rejects inline definitions in equipment writes.
   `inventoryDocument` is a display-only projection and must never be posted back.
10. **Release builds require HTTPS** (`usesCleartextTraffic=false`); only the debug manifest
    permits cleartext for local servers.

## Conventions

- **Language split:** code, comments, commit messages and test names are English; every
  user-facing string (including `require`/`check` messages, which surface in snackbars) is
  **bilingual**: write it as `tr("русский текст", "English text")` from `core/i18n/Loc.kt`.
  `tr` reads the global `uiLanguage`, which defaults to **RU**, so tests that assert Russian
  keep passing. Compose refreshes because `ForgeApp` keys the whole tree on `s.lang`; helpers
  that take an explicit language (`slotTitle`, `rarityTitle`, `Catalog.title`,
  `EquipmentSlot.title`) default to `uiLanguage`. Never add a user-facing literal in one
  language only.
- **Style:** dense, low-ceremony Kotlin — one-line bodies, `when` expression tables, few
  comments. Comments exist only where a rule is non-obvious (idempotency, atomic arrays,
  display-only projections). Match the surrounding density instead of expanding it.
- Validation is done with `require`/`check` at the boundary, before any network call.
- Serialization goes through the shared `WireJson` (`prettyPrint`, `ignoreUnknownKeys`);
  unknown server fields are preserved in documents rather than dropped.
- Typed models (`@Serializable data class`) for read models; raw `JsonObject` only at the admin
  editor's form boundary and for catalogs whose shape is server-defined.
- Compose screens are `@Composable fun Screen(s: ForgeState, vm: ForgeViewModel)`; they read
  state and call `vm::action`. Enable/disable controls with `!s.busy` plus the relevant
  ownership/pending guard, as in `CombatScreen`'s `controls` value.
- Theme: dark only, Path of Exile palette in `ui/theme/Theme.kt` (Ink/Abyss/Panel stone,
  Gold/Bronze frames, PoE rarity colours, cut-corner shapes). Build screens from
  `ScreenHeader`, `ForgePanel`, `OrnateDivider`, `Engraved`, `StatGlobe` and `PropertyRow`
  instead of ad-hoc cards, and use `rarityColor`/`ItemEmblem`/`ForgeGlyphs` rather than new
  ad-hoc colors or network images (all icons are offline vectors).
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`
  (docs-only commits often append `[skip ci]`).

## Common tasks

**Adding a server route:** add the typed model under `core/model/...`, add one `suspend fun` on
`GameApi` (reuse the private `request(...)`, pass `authenticated = true`), cover it with a
MockWebServer test asserting path, bearer header and exact body, then expose it through a
feature view model + a `ForgeViewModel` delegate.

**Adding a screen:** create `ui/screens/<feature>/`, add the tab index to the `when` in
`ForgeApp` and to the `destinations`/`icons` maps if it belongs in the bottom bar, and guard
admin-only tabs in `ForgeRuntime.tab`.

**Adding a durable command:** follow `CombatViewModel.submit` — persist to `ServerStore` under a
`server:user:character` key before sending, keep the `requestId` stable across retries, clear
the record only on success or an explicit 4xx rejection, and clear visible state in
`clearSession`/`characterId` so account and hero switches cannot leak it.

**Adding an editor field:** extend `schemaFields` in `EditorSchema.kt` and, if it is writable,
`editableFields` in `Contract.kt`; the form UI is generated from the schema.

## Docs to keep in sync

`README.md` and `docs/*.md` are Russian, versioned against the server, and referenced by the app.
When behavior changes, update the matching doc: `docs/API_CONTRACT.md` (routes and payloads),
`docs/COMBAT.md`, `docs/PASSIVES.md`, `docs/VALIDATION.md` (what CI verifies). If the pinned
server commit or API revision changes, update `SERVER_COMMIT`, `ApiCapabilities.require*`, the
README header and the `client-server` job's checkout ref together.

`ExileForge-debug.apk` at the repo root is a committed build artifact; CI publishes fresh APKs as
workflow artifacts. Don't regenerate it as part of ordinary changes.
