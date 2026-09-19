# CLAUDE.md

Guidance for AI assistants working in this repository.

## What this project is

ExileForge is an **Android Compose client** (version 2.0.0, `versionCode` 13) for the
**ktor-bestgame** RPG server (0.9.1), pinned in
`core/.../contract/Contract.kt` as `SERVER_COMMIT = 64b3577822e0f4b5d710ca8b9d4e250e65269359`
on the server branch `claude/tender-pasteur-a36kj2`.

The client is deliberately **thin**: the server owns items, stats, modifier rolls and inventory.
This client renders server state, sends commands, and never recomputes game numbers locally.

## Repository layout

```
build.gradle.kts, settings.gradle.kts   Gradle 9.7.1, AGP 9.4.0, Kotlin 2.4.20, modules :app and :core
core/                                   Pure JVM library (java-library + kotlin-jvm, toolchain 17)
  contract/      Contract.kt            Wire JSON, validation, editable/creation fields, templates, requireId
                 CharacterContract.kt   Character document validation
  network/       GameApi.kt             The single HTTP client (OkHttp) for every server route
                 ItemRepository.kt      CRUD interface implemented by GameApi (lets tests fake it)
                 ApiFailure/FailureState/RequestJournal/RequestLog/ItemPage/HttpPayload
  model/         Catalog, EntitySource, CatalogFilter, EquipmentKind
                 command/Commands.kt    UserProfile, ItemStack, UseRecipeCommand, RouteInfo, ApiCapabilities
                 hero/HeroModels.kt     CharacterSummary, EquipmentInstance, OrbOutcome, CharacterItem, Recipe*, HeroView
                 modifier/Modifiers.kt  ModifierDefinition, ModifierTier, Modifier, effects and sources
                 currency/Orbs.kt       CurrencyItem, CurrencyOrb and the CURRENCY category
                 character/CharacterStats.kt  The server's stat enum names
  i18n/          Loc.kt                 Lang (RU/EN), `tr(ru, en)` and the global `uiLanguage`
  editor/        EditorSchema.kt        Declarative form schemas (FormField/InputSpec) used by the editor
  display/       ItemPresentation.kt    Display-only projections and bilingual titles
  verification/  CrudScenario.kt        Admin-only self-check run from the Checks screen
app/                                    Android application (minSdk 26, compile/target SDK 37)
  MainActivity.kt, ForgeApplication.kt  Entry points; Application owns RequestJournal + ServerStore
  presentation/  ForgeRuntime.kt        Shared coroutine scope, GameApi instance, MutableStateFlow<ForgeState>
                 ForgeViewModel.kt      Lifecycle owner and thin facade delegating to feature models
                 features/              Catalog, Editor, Hero, Session, Checks view models
                 state/ForgeState.kt    One immutable state object for the whole app
  ui/            ForgeApp.kt            Scaffold, banner with RU/EN switch, bottom navigation, tab dispatch
                 screens/               catalog, editor, hero, checks, server
                 components/            ItemCard, PropertyRow, InfoCard, spinners and Ornament.kt
                 forms/, icons/ (ForgeGlyphs vector set, ItemEmblem, ItemIcon/PropertyIcon), theme/
  data/settings/ServerStore.kt          DataStore Preferences: base URL, saved filters, language
docs/                                   Russian reference docs (API_CONTRACT, VALIDATION)
scripts/client_server_test.py           Boots the real backend + MongoDB and runs ServerIntegrationTest
.github/workflows/android.yml           `build` job (unit/lint/APK/emulator UI) and `client-server` job
```

`core` must stay free of Android and Compose imports — it is a plain JVM library and is
compiled by unit tests without an Android SDK. Put anything reusable and UI-independent there.

## Build and verification

Requires **JDK 17** (Gradle toolchain, pinned in `gradle/gradle-daemon-jvm.properties`) and
**Android SDK 37** for `:app` tasks. `gradlew` is committed without the executable bit, so invoke it
as `bash gradlew` (CI does `chmod +x gradlew` first).

```bash
bash gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Note: a bare container without a JDK 17 toolchain or Android SDK (as in cloud sessions) cannot
run the `:app` tasks — `dl.google.com` is usually unreachable there. `:core` can still be compiled
and tested locally by temporarily pointing the daemon JVM criteria at the available JDK; revert that
change before committing. Otherwise rely on CI and keep changes reviewable by reading.

- `:core:test` — JUnit + `kotlin-test` + **MockWebServer**; no device or SDK needed.
- `:app:connectedAndroidTest` equivalents run in CI on an **API 35 emulator** via
  `adb shell am instrument`; the workflow greps for `OK (n tests)`.
- `ServerIntegrationTest` is **opt-in**: it is skipped (`assumeTrue`) unless `EF_LIVE_URL` is
  set. The `client-server` CI job builds the pinned backend, starts an isolated MongoDB replica
  set and runs it through `scripts/client_server_test.py`.
- `DesignPreviewTest` and `HeroPanelTest` capture screenshots into the app's external files dir
  (`design/arsenal.jpg`, `hero.jpg`); CI pulls them and base64-prints them into the log. Do not
  remove those captures — the workflow fails without the files.

## Architecture and state

**One state object.** `ForgeState` (a single `data class`) holds everything: session, catalog
page, editor draft, hero, checks, failures. Derived permissions are computed properties on it:
`isAdmin`, `adminTools`, `canEdit`, `ownsCharacter`.

**ForgeRuntime** owns the `SupervisorJob` scope, the `MutableStateFlow<ForgeState>`, the current
`GameApi`, and instantiates the five feature view models. It also provides shared helpers:

- `task(writing = false) { ... }` — the standard action wrapper. It refuses to start while
  `busy`, clears previous message/error/failure, runs the block on the runtime scope, maps
  exceptions to `FailureState` + a bilingual user message, and always clears `busy`. **Every
  user-triggered server call goes through `task`.** Pass `writing = true` for mutations so that
  IO/5xx failures are classified as `UncertainWrite` rather than `Offline`.
- `loadPage`, `setEditor`, `ensureDefinitions`, `recipeDocument`, `clearSession`, `newApi`.

**Feature view models** (`presentation/features/*`) hold no state of their own; they read
`runtime.state.value` and `mutable.update { it.copy(...) }`. They are written as
`fun x() { with(runtime) { task { ... } } }`.

**ForgeViewModel** is only a lifecycle owner plus a compatibility facade: every public method
forwards to a feature model. Add new actions to the feature model **and** expose a one-line
delegate here, because screens receive `ForgeViewModel`.

**Navigation** is an `Int` tab in state, dispatched by a `when` in `ForgeApp`:
`0` catalog/characters, `1` editor, `2` checks (admin only — `ForgeRuntime.tab` blocks it
otherwise), `3` account/server, `4` hero.
`ForgeApp` re-`key`s the whole tree on `server`, `sessionEpoch` and `lang`, so a logout, a server
change or a language switch discards per-screen Compose state.

## Server contract rules (do not violate)

These are enforced by tests and are the point of the client's design:

1. **The server is authoritative.** Never compute damage, stats, prices or modifier rolls locally.
   `GET /api/v1/character/inventory/stats` is the character sheet; the client prints it.
2. **Identity comes from the server.** POST sends a JSON array of documents without `_id`;
   `requireId` demands 24 hex chars before any request is built.
3. **The server owns versioning.** PUT sends only the changed fields and DELETE sends no body —
   the server reads the stored document, checks its own `version` and rejects a racing write. A
   mutation is **never** silently retried, and a rejection is surfaced with its message.
4. **There is no token.** `GET /api/v1/user/login` answers with the account document, which is the
   whole session and lives in memory only. `logout()` drops it; `authenticated = true` requires it.
   A 401 still clears the session through `GameApi(onUnauthorized = ...)` → `clearSession()`.
5. **Uncertain writes are surfaced, not retried.** `FailureState.UncertainWrite` (IO error or 5xx
   on a write) tells the user to refresh. There is no durable replay: this server has no
   `requestId`, so a repeat would create a second instance.
6. **Capabilities gate features.** `ApiCapabilities` is built from the server's own `/system/routes`
   table, and `requireWorkbench()` runs before login so a stale server is named, not guessed at.
7. **Secrets never reach the journal.** `request(sensitive = true)` for login and password change;
   the query string and body are stored as `[скрыто]`. The account document lives in memory only.
8. **Template vs. instance.** A template carries `modifierIds` — a pool of `ModifierDefinition` ids.
   What lands on a copy, in which tier and with which value, is rolled by the server in
   `itemToInventory`. `validateModifierPool` rejects rolled `params` in a template write, and
   `inventoryDocument` is a display-only projection that must never be posted back.
9. **Lists are read whole and paged here.** The server's `/paged` route passes `page` straight to
   the repository as the offset instead of `page * size`, so every page but the first is off by all
   but one record; it also offers no filter. The client reads `GET /api/v1/{collection}` and slices
   it, comparing only fields the server already wrote — filtering is display, never a game
   calculation. Move paging back once the server fixes the offset.
10. **Orbs are the server's rules.** A currency orb is an `items` document of category `CURRENCY`;
    `POST /api/v1/characterequipment/applyOrb` spends one and answers with the item plus a sentence
    saying what happened. The client sends the pair and prints that sentence — it never decides what
    an orb did, and `CurrencyOrb` is a translation table, not a rule table. `rarity` and `corrupted`
    belong to the instance, so the instance wins over its template in `inventoryDocument`.
11. **Release builds require HTTPS** (`usesCleartextTraffic=false`); only the debug manifest
    permits cleartext for local servers. This matters more than usual: the password travels as a
    query parameter, because that is the route the server exposes.
12. **No network or raster images.** Entities carry at most an `image` URL, which the client stores
    but never fetches. Every picture is a bundled vector (`ItemEmblem`, `ForgeGlyphs`).

## Conventions

- **Language split:** code, comments, commit messages and test names are English; every
  user-facing string (including `require`/`check` messages, which surface in snackbars) is
  **bilingual**: write it as `tr("русский текст", "English text")` from `core/i18n/Loc.kt`.
  `tr` reads the global `uiLanguage`, which defaults to **RU**, so tests that assert Russian
  keep passing. Compose refreshes because `ForgeApp` keys the whole tree on `s.lang`; helpers
  that take an explicit language (`slotTitle`, `rarityTitle`, `weaponTitle`, `statTitle`,
  `Catalog.title`) default to `uiLanguage`. Never add a user-facing literal in one language only.
- **Style:** dense, low-ceremony Kotlin — one-line bodies, `when` expression tables, few
  comments. Comments exist only where a rule is non-obvious (who rolls, what is display-only,
  why a POST carries an empty body). Match the surrounding density instead of expanding it.
- Validation is done with `require`/`check` at the boundary, before any network call.
- Serialization goes through the shared `WireJson` (`prettyPrint`, `ignoreUnknownKeys`);
  unknown server fields are preserved in documents rather than dropped.
- Typed models (`@Serializable data class`) for read models; raw `JsonObject` only at the
  editor's form boundary and for catalogs whose shape is server-defined.
- Compose screens are `@Composable fun Screen(s: ForgeState, vm: ForgeViewModel)`; they read
  state and call `vm::action`. Enable/disable controls with `!s.busy` plus the relevant
  ownership guard, as in `AdminGrantPanel`'s `enabled` value.
- Theme: dark only, Path of Exile palette in `ui/theme/Theme.kt` (Ink/Abyss/Panel stone,
  Gold/Bronze frames, rarity colours matching the server enum, cut-corner shapes). Build screens
  from `ScreenHeader`, `ForgePanel`, `OrnateDivider`, `Engraved`, `StatGlobe` and `PropertyRow`
  instead of ad-hoc cards, and use `rarityColor` rather than new ad-hoc colors.
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`
  (docs-only commits often append `[skip ci]`).

## Common tasks

**Adding a server route:** add the typed model under `core/model/...`, add one `suspend fun` on
`GameApi` (reuse the private `request(...)`, pass `authenticated = true` when it needs an account),
cover it with a MockWebServer test asserting path, query and exact body, then expose it through a
feature view model + a `ForgeViewModel` delegate.

**Adding a screen:** create `ui/screens/<feature>/`, add the tab index to the `when` in
`ForgeApp` and to the `destinations`/`icons` maps if it belongs in the bottom bar, and guard
admin-only tabs in `ForgeRuntime.tab`.

**Adding an editor field:** extend `schemaFields` in `EditorSchema.kt` and, if it is writable,
`editableFields` (or `creationFields`) in `Contract.kt`; the form UI is generated from the schema.

**Reading the server:** the backend is a separate repository. Clone it and read
`base/route/BaseRoute.kt` for the shared CRUD shape, then the `*Route.kt` of the feature — the
route segment is the entity's class name, lower-cased.

## Docs to keep in sync

`README.md` and `docs/*.md` are Russian (README is bilingual), versioned against the server, and
referenced by the app. When behavior changes, update the matching doc: `docs/API_CONTRACT.md`
(routes and payloads) and `docs/VALIDATION.md` (what CI verifies). If the pinned server commit
changes, update `SERVER_COMMIT`, `SERVER_BRANCH`, `SERVER_VERSION`, `ApiCapabilities.requireWorkbench`,
the README header and the `client-server` job's checkout ref together.

`ExileForge-debug.apk` at the repo root is a committed build artifact; CI publishes fresh APKs as
workflow artifacts. Don't regenerate it as part of ordinary changes.
