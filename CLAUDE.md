# CLAUDE.md

Guidance for AI assistants working in this repository.

## What this project is

ExileForge is an **Android Compose client** (version 2.1.0, `versionCode` 14) for the
**ktor-bestgame** RPG server (0.15.1), pinned in
`core/.../contract/Contract.kt` as `SERVER_COMMIT = a7fbe483499c1c660111ede3ff66ddf2afec06d4`
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
                 hero/HeroModels.kt     CharacterSummary, CharacterSheet, EquipmentInstance, OrbOutcome, Recipe*, HeroView
                 modifier/Modifiers.kt  ModifierDefinition, ModifierTier, Modifier, effects and sources
                 currency/Orbs.kt       CurrencyItem, CurrencyOrb and the CURRENCY category
                 progression/Progression.kt   CharacterClass, ExperienceLevel, StatValue
                 skilltree/SkillTree.kt CharacterSkillNode, SkillTreeNode, SkillTreeState, reachableFrom
                 auction/Auction.kt     AuctionLot, AuctionFilter, AuctionPage, lot kinds and states
                 character/CharacterStats.kt  The server's stat enum names
  i18n/          Loc.kt                 Lang (RU/EN), `tr(ru, en)` and the global `uiLanguage`
                 ServerLocale.kt        LocaleManifest/LocaleBundle/LocaleKey, the global `serverLocale`, loc/locOr/locError
  editor/        EditorSchema.kt        Declarative form schemas (FormField/InputSpec) used by the editor
  display/       ItemPresentation.kt    Display-only projections and bilingual titles
                 ServerIcons.kt         IconManifest/IconBundle/IconKey, the global `serverIcons`
  verification/  CrudScenario.kt        Admin-only self-check run from the Checks screen
app/                                    Android application (minSdk 26, compile/target SDK 37)
  MainActivity.kt, ForgeApplication.kt  Entry points; Application owns RequestJournal + ServerStore
  presentation/  ForgeRuntime.kt        Shared coroutine scope, GameApi instance, MutableStateFlow<ForgeState>, locale + device sign-in
                 ForgeViewModel.kt      Lifecycle owner and thin facade delegating to feature models
                 features/              Catalog, Editor, Hero, Session, Character, Checks, Auction view models
                 state/ForgeState.kt    One immutable state object for the whole app
  ui/            ForgeApp.kt            Scaffold, banner with RU/EN switch, bottom navigation, tab dispatch
                 screens/               session (auth + character menu), catalog, editor, hero, tree, craft, auction, checks, server
                 components/            ItemCard, PropertyRow, InfoCard, spinners and Ornament.kt
                 forms/, icons/ (ForgeGlyphs vector set, ItemEmblem, ItemIcon/PropertyIcon, ServerSprite), theme/
  data/settings/ServerStore.kt          DataStore Preferences: base URL, saved filters, language, locale bundles, icon set, device-session flag
                 DeviceId.kt            UUID v5 over the hardware fingerprint plus ANDROID_ID
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

**Navigation** has two levels. Above the tabs is `AppPhase` (`AUTH` → `CHARACTERS` → `GAME`),
dispatched first by `ForgeApp`: the sign-in screen and the character menu are full-screen, with no
banner and no bottom bar. Only `GAME` builds the scaffold. Nothing below the gate writes
`characterId` — `CharacterViewModel` owns it, so a tab can never move the player onto another hero.

Inside `GAME` navigation is an `Int` tab in state, dispatched by a `when` in `ForgeApp`:
`0` catalog/characters, `1` editor, `2` checks (admin only — `ForgeRuntime.tab` blocks it
otherwise), `3` account/server, `4` hero, `5` skill tree, `6` auction, `TAB_CRAFT` (`7`) the forge.
The bottom bar carries five destinations for everyone (`0, 4, 5, 6, 3`). Everything else is a tab
reached by a button: the editor and the checks from the Account tab, the forge from the Hero tab.
The bar stays on those screens and is the way back out of them.
`ForgeApp` re-`key`s the whole tree on `server`, `sessionEpoch` and `lang`, so a logout, a server
change or a language switch discards per-screen Compose state.

## Server contract rules (do not violate)

These are enforced by tests and are the point of the client's design:

1. **The server is authoritative.** Never compute damage, stats, prices or modifier rolls locally.
   `GET /api/v1/character/inventory/stats` is the character sheet; the client prints it. Since
   0.10.0 it answers a `CharacterSheet` object: the numbers, the level, and the server's verdict on
   every worn item (`active` / `inactive` with the requirement each one misses). A requirement is
   never re-checked here, and an attribute conversion (`perStat`/`perAmount`) is never resolved here.
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
9. **Catalogue lists are read whole and paged here; the auction is not.** `/paged` was fixed in
   0.13.1, but the generic route still takes no filter, so moving the catalogue onto it would cost
   every catalogue filter. The catalogue reads `GET /api/v1/{collection}` and slices it, comparing
   only fields the server already wrote — filtering is display, never a game calculation. The
   auction is the exception: `auctionlot/search` filters and pages server-side, and its results are
   never narrowed again here.
10. **Orbs are the server's rules.** A currency orb is an `items` document of category `CURRENCY`;
    `POST /api/v1/characterequipment/applyOrb` spends one and answers with the item plus a sentence
    saying what happened. The client sends the pair and prints that sentence — it never decides what
    an orb did, and `CurrencyOrb` is a translation table, not a rule table. `rarity` and `corrupted`
    belong to the instance, so the instance wins over its template in `inventoryDocument`.
11. **A class is the base, and it is chosen once.** A character carries `classId`, not stats:
    `CharacterClass` holds the level-1 base, the per-level growth and the attribute conversions.
    It is a creation field with no update route — moving a character between classes would rewrite
    their history — so it is offered in the editor only while `original == null`.
12. **The tree is the server's graph.** `skilltreenode` is the shared seeded tree; what one
    character took is a snapshot inside their own document (`Character.skillNodes`), reached through
    `/api/v1/character/skilltree/*`. Allocate, refund and reset each answer with the whole
    `SkillTreeState`; adjacency, cost, the point balance and whether a refund would detach the rest
    are all checked server-side. The class's start node arrives with the character and costs
    nothing, and a reset is a respec that leaves it in place — a new tree is never empty. The screen draws the seeded coordinates and sends one node code.
    `reachableFrom` highlights neighbours so 122 nodes stay navigable — it reads `connections`, it
    does not decide: a highlighted node can still be refused.
13. **The auction's goods live in the lot.** While a lot is listed the instance has left
    `CharacterEquipment` and the stack has left the bag, which is why a worn item cannot be listed
    (`AU_010`). Prices are counted in currency orbs alone (`AU_007`), a seller cannot buy their own
    lot (`AU_006`), and the level the auction opens at is a server constant the client never copies:
    it asks, and turns `AU_002` into the screen's explanation.
14. **An item has no stat fields.** Armour, damage and attack speed are fixed modifiers in
    `baseParams` (values, no tier); `durability` is the only number left as a field. Requirements
    (`requiredLevel`, `requiredStrength`, `requiredDexterity`, `requiredIntelligence`) are printed,
    never enforced here — the server checks them twice and the two checks are different rules:
    `equip` refuses an item out of reach outright (`CH_013`), while one already worn keeps its slot
    and only stops counting, landing in the sheet's `inactive`. Never disable a control on a
    requirement the client worked out itself; send the command and show the refusal.
15. **Release builds require HTTPS** (`usesCleartextTraffic=false`); only the debug manifest
    permits cleartext for local servers. This matters more than usual: the password travels as a
    query parameter, because that is the route the server exposes.
16. **No network or raster images; a drawing is outlines, not a picture.** No image file is ever
    downloaded, and no entity names one: the `image` URL both catalogues used to carry was removed
    in 0.15.1, because nothing had ever fetched it. The *shape* of an icon does come from the
    server: `icons/
    index.json` carries a fingerprint the server computes from the file, `icons/icons.json` holds
    the drawings and a `code → drawing` table, and the client paints the path data itself. A
    sprite carries alpha and no colour, so `ItemIcon` and `PropertyIcon` tint it by rarity exactly
    as they tint the bundled set. Everything the server does not cover — and every sprite whose
    path data will not parse — falls back to `ItemEmblem`/`ForgeGlyphs`, which is why a missing set
    is invisible rather than broken and why the Account tab reports how much of it arrived. Keys
    mirror `LocaleKey`'s sections (`equipment.<CODE>`, `item.<CODE>`, `stat.<STAT>`) so one code
    answers both what a thing is called and how it is drawn. Modifiers are deliberately out: their
    text is a template with substitutions and an icon cannot stand in for a sentence.
17. **The server owns every name; the client owns its own labels.** Since 0.14.0 no document
    carries text: equipment, items, modifiers, tree nodes and classes store a `code`, and the
    strings are static files — `locale/index.json` (languages with a hash each) and
    `locale/{ru,en}.json`, keyed `<section>.<CODE>.<field>` exactly as `core/i18n/LocaleKey`
    builds it. Both sides must compute the same key, so never hand-write one. The bundle is
    global (`serverLocale`), loaded by `ForgeRuntime.loadLocale` per server and language and
    stored by hash in `ServerStore`; a missing key returns itself so a hole is visible. A
    modifier's text is a **template** with `{0}`, `{1}` per effect — never assemble a sentence by
    appending numbers to a label. `messageArgs` from the server are themselves keys, so resolve
    them through the bundle before substituting. The bundle also has an `enum.` section, but the
    client keeps its own tables (`slotTitle`, `rarityTitle`, `statTitle`, `CurrencyOrb`): they
    take an explicit language and one dictionary is loaded at a time. An error is translated only
    when `error.<code>` has no placeholder, because the error envelope carries the finished
    sentence and the code but never the arguments — see `locError`.

18. **A session is made, never restored; a character is chosen once per session.** The server
    issues no token, so a relaunch signs in again — silently by device when `ServerStore`'s
    `deviceSession` flag says the last session was played that way, and an explicit sign-out
    clears it. Registration *is* the sign-in: `GET /user/login/byDeviceId` answering `US_015`
    means "never seen", and that becomes `POST /user/byDeviceId`; any other refusal is reported,
    never registered around. The identifier is derived in `:app` (`deviceId()`) as a UUID v5 over
    `MANUFACTURER|MODEL|DEVICE|HARDWARE|ANDROID_ID` — the `Build.*` parts describe the *model*, so
    `ANDROID_ID` is what actually makes it unique and hardware alone would hand two owners of the
    same phone one account. After signing in everyone passes through the character menu, an
    administrator included: `GET /character/byUser` lists the account's characters, at most
    `MAX_CHARACTERS`. The menu enters the game directly when there is exactly one, and opens the
    creation form when there are none — an empty list is not a choice. The server address lives on
    the sign-in screen because behind the gate there is no way back to it. Entering is not the
    same as coming back: `readCharacters(autoEnter = true)` is passed exactly once, by the
    sign-in, because the menu is also where a player goes *to leave* a character — entering the
    only one again there would make the screen unreachable for anyone who owns one.
19. **The hero is re-read on a reason, never on a timer and never on request.** Reading it whole
    is five requests, so nothing asks the player to press anything: a command re-reads it because
    the command changed it, and everything changed *elsewhere* — a trade, an administrator, the
    same account on another device — is caught by `heroReadAt` going cold. `ensureHero()` refreshes
    when a character tab opens and the last reading is older than `FRESH_FOR`; a trade sets the
    stamp to 0 because the auction patches only the bag and the inventory, never the character
    document. The manual path is a pull-to-refresh on the stash, not a button competing with the
    content. This server has no change feed and `version` belongs to the character alone, so a
    cheap "did anything change" question cannot be asked — the stamp is the answer instead.

## Conventions

- **Language split:** code, comments, commit messages and test names are English; every
  user-facing string (including `require`/`check` messages, which surface in snackbars) is
  **bilingual**: write it as `tr("русский текст", "English text")` from `core/i18n/Loc.kt`.
  `tr` reads the global `uiLanguage`, which defaults to **RU**, so tests that assert Russian
  keep passing. Compose refreshes because `ForgeApp` keys the whole tree on `s.lang`; helpers
  that take an explicit language (`slotTitle`, `rarityTitle`, `weaponTitle`, `statTitle`,
  `Catalog.title`) default to `uiLanguage`. Never add a user-facing literal in one language only.
  The **name of a thing in the game** is not a client string: it comes from `serverLocale` through
  `loc`/`locOr` (rule 17), so never write `tr` for one.
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
  from `ScreenHeader`, `ForgePanel`, `OrnateDivider`, `Engraved`, `StatBar`, `SectionHeader`,
  `ItemRow` and `PropertyRow` instead of ad-hoc cards, and use `rarityColor` rather than new
  ad-hoc colors. A stash is a list of `ItemRow`s with the full `ItemCard` one tap behind each,
  because a card is a page about one item and a line is a stash you can read down.
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`
  (docs-only commits often append `[skip ci]`).

## Common tasks

**Adding a server route:** add the typed model under `core/model/...`, add one `suspend fun` on
`GameApi` (reuse the private `request(...)`, pass `authenticated = true` when it needs an account),
cover it with a MockWebServer test asserting path, query and exact body, then expose it through a
feature view model + a `ForgeViewModel` delegate.

**Adding a screen:** create `ui/screens/<feature>/`, add the tab index to the `when` in
`GameScaffold` and to the `destinations`/`icons` maps if it belongs in the bottom bar, and guard
admin-only tabs in `ForgeRuntime.tab`. A screen above the gate goes in the `when (s.phase)` of
`ForgeApp` instead and builds its own scaffold.

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
