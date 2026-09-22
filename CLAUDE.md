# CLAUDE.md

Guidance for AI assistants working in this repository.

## What this project is

ExileForge is an **Android Compose client** (version 2.4.0, `versionCode` 17) for the
**ktor-bestgame** RPG server (0.18.0), pinned in
`core/.../contract/Contract.kt` as `SERVER_COMMIT = 6026b1a709fb6a50609332e8414cae948c8082ed`
on the server branch `claude/tender-pasteur-a36kj2`.

The client is deliberately **thin**: the server owns items, stats, modifier rolls and inventory.
This client renders server state, sends commands, and never recomputes game numbers locally.

## Standing rules (never skip, whatever the task)

These two were set by the owner of the project and outrank convenience. They apply to every
change, in this repository and in `ktor-bestgame`, whether or not the task mentions them.

1. **Anything with a name is born translated.** Adding an item, a piece of equipment, a class, a
   currency orb, a tree node, a modifier, an enum value or an error code means adding its strings
   to **every** language `locale/index.json` lists — today `ru`, `en` and `zh`, and whatever it
   lists tomorrow. A code without a name in all of them is an unfinished change, not a change with
   a follow-up. The keys are `<section>.<CODE>.<field>` exactly as `core/i18n/LocaleKey` and the
   server's own `LocaleKey` build them, so never hand-write one. The server's `LocalizationTest`
   is what catches a miss: it checks that every dictionary covers every key the code asks for,
   that the languages hold identical key sets, that no string is empty, and that a placeholder
   never disappears in translation. Run it before calling such a change done.

   Client-side labels are the other half of the same rule, and they work the same way. A
   user-facing string is never written in the source: it is a key in
   `core/src/main/resources/i18n/ui_{ru,en,zh}.json`, read with `ui("screen.thing")`, and the
   three files must hold identical key sets. `:core`'s `UiStringsTest` enforces that, refuses an
   empty string, refuses a lost `{0}`, walks every enum the client names codes from, and reads
   the sources to fail on a `ui(...)` key that is in no dictionary. Adding a language means a
   `Lang` entry, three-language coverage of a fourth file, and the server serving it — the picker
   is built from the server's manifest, because an interface in a language the server cannot name
   items in is half a translation.

2. **Every finished change ends with a changelog entry and a version.** Once the checks have
   passed and the branches are pushed, write what changed into `CHANGELOG.md` — a new entry at
   the **top**, dated, under the version number, brief: what moved, and for a fix, what the cause
   was. Each repository keeps its own file and they cross-reference each other, so the history of
   dates answers "what changed and when" without reading commits. Then report the same list in
   the reply, **for the application and for the server separately** — even when only one of them
   moved, say so. The version numbers are the ones in `app/build.gradle.kts`
   (`versionName`/`versionCode`) and in the server's `SERVER_VERSION`, and they are bumped as
   part of the change rather than left for later. This is the last step of the work, not a
   courtesy: a change that is pushed but not written up is not delivered.

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
  i18n/          Loc.kt                 Lang (RU/EN/ZH), `ui(key)`/`uiOr`/`plural`, the global `uiLanguage`
                 ServerLocale.kt        LocaleManifest/LocaleBundle/LocaleKey, the global `serverLocale`, loc/locOr/locError
  editor/        EditorSchema.kt        Declarative form schemas (FormField/InputSpec) used by the editor
  display/       ItemPresentation.kt    Display-only projections and code-to-title tables
                 ServerIcons.kt         IconManifest/IconBundle/IconKey, the global `serverIcons`
  verification/  CrudScenario.kt        Admin-only self-check run from the Checks screen
  src/main/resources/i18n/              ui_{ru,en,zh}.json — every label the client wrote itself
app/                                    Android application (minSdk 26, compile/target SDK 37)
  MainActivity.kt, ForgeApplication.kt  Entry points; Application owns RequestJournal + ServerStore
  presentation/  ForgeRuntime.kt        Shared coroutine scope, GameApi instance, MutableStateFlow<ForgeState>, locale + device sign-in
                 ForgeViewModel.kt      Lifecycle owner and thin facade delegating to feature models
                 features/              Catalog, Editor, Hero, Session, Character, Checks, Auction view models
                 state/ForgeState.kt    One immutable state object for the whole app
  ui/            ForgeApp.kt            Scaffold, banner with RU/EN switch, bottom navigation, tab dispatch
                 screens/               session (auth + character menu), admin, catalog, editor, hero, tree, craft, auction, checks, server
                 components/            ItemCard, ItemRow, PropertyRow, InfoCard, ConfirmDialog, spinners and Ornament.kt
                 forms/, icons/ (ForgeGlyphs vector set, ItemEmblem, ItemIcon/PropertyIcon, ServerSprite), theme/
  data/settings/ServerStore.kt          DataStore Preferences: base URL, saved filters, language, locale bundles, icon set, device-session flag
                 DeviceId.kt            UUID v5 over the hardware fingerprint plus ANDROID_ID
CHANGELOG.md                            Dated entries per version; the server keeps its own
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

Inside `GAME` navigation is an `Int` tab in state, named in `ForgeState` and dispatched by a
`when` in `ForgeApp`: `TAB_CATALOG`, `TAB_EDITOR`, `TAB_CHECKS`, `TAB_ACCOUNT`, `TAB_HERO`,
`TAB_TREE`, `TAB_AUCTION`, `TAB_CRAFT`, `TAB_ADMIN`. The bottom bar carries `PLAYER_TABS` — hero,
tree, auction, account — and, for an administrator only, `TAB_ADMIN` on top of them. That one tab
holds every administrator tool as a button: the catalogue, the editor, the checks, granting items
and the switch that drops the tools to see the app as a player sees it. `ADMIN_TABS` is what
`ForgeRuntime.tab` refuses without them, so a player cannot reach any of those screens at all.
The forge is still a tab a button opens, from the Hero tab. The bar stays on those screens and is
the way back out of them.
`ForgeApp` re-`key`s the whole tree on `server`, `sessionEpoch` and `lang`, so a logout, a server
change or a language switch discards per-screen Compose state.

## Server contract rules (do not violate)

These are enforced by tests and are the point of the client's design:

1. **The server is authoritative.** Never compute damage, stats, prices or modifier rolls locally.
   `GET /api/v1/character/inventory/stats` is the character sheet; the client prints it. Since
   0.10.0 it answers a `CharacterSheet` object: the numbers, the level, and the server's verdict on
   every worn item (`active` / `inactive` with the requirement each one misses). Since 0.17.0 it
   also carries `unwearable`: every *template* the character cannot currently meet, with reasons.
   The verdict is on the template because that is where a requirement lives, so one answer marks a
   stash line and a stranger's lot alike — and the client looks a template up rather than working
   a requirement out. A requirement is never re-checked here, and an attribute conversion
   (`perStat`/`perAmount`) is never resolved here: the server applies the class's own conversions
   (strength to life, intelligence to mana and three more) in the same pass as the tree, which is
   what makes an attribute from a worn item feed one.
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
8. **Template vs. instance: the base is a reference, never a copy.** A template carries
   `modifierIds` — a pool of `ModifierDefinition` ids — and `baseParams`, the armour, damage and
   attack speed every copy of it has. What lands on a copy, in which tier and with which value, is
   rolled by the server in `itemToInventory`. Since 0.16.0 an instance carries **only** what it
   rolled: the base lives in the catalogue in a single copy and reaches an item through its
   `equipmentId`, so rebalancing a base reaches every copy already in the world and no card can
   print a property twice. That makes the catalogue a prerequisite rather than a nicety —
   `ForgeRuntime.ensureEquipment()` reads `GET /api/v1/equipment` whole, once per session, and
   `requireWorkbench` names the route so a stale server is reported instead of drawing half an
   item. `validateModifierPool` rejects rolled `params` in a template write, and
   `inventoryDocument` is a display-only projection — base first, then the rolls — that must never
   be posted back.
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
    nothing, and a reset is a respec that leaves it in place — a new tree is never empty. The
    screen draws the seeded coordinates and sends one node code. `reachableFrom` highlights
    neighbours so 299 nodes stay navigable — it reads `connections`, it does not decide: a
    highlighted node can still be refused. Since 0.16.0 the graph is a file,
    `resources/skilltree/tree.json` on the server: seven class areas that meet through their own
    branches with no shared ring, and `SkillTreeSeeder` only reads it. Giving a node back is no
    longer free — a refund spends one **Orb of Regret** per node and a full reset one per node
    returned (`ST_015` when the bag is short) — and a tapped node opens a sheet with what it gives
    and what it would cost, while «Подробно» prints `SkillTreeState.totals` — a list of
    `StatContribution`, one line per characteristic and operation, which the server sums over the
    whole allocated tree rather than the client adding modifiers up. It is the tree's
    *contribution*, not the character's total: there is no base under it, so a percentage stays a
    percentage. It used to be a total over an empty base, where every INCREASED collapsed to zero
    and was then dropped, which is why no percentage node ever showed. Taking a node, giving one
    back and resetting the tree all ask first, and the question names what it costs. A `JEWEL_SOCKET`
    node holds nothing itself: taking it opens a socket, and a jewel — an ordinary equipment
    instance of slot `JEWEL` — is put in it by `POST /api/v1/characterequipment/socket` naming the
    node code, which is why `EquipmentInstance` has `socketCode` beside `equippedSlot`: the slot
    says what it is, the code says where it is worn. A jewel's modifiers count globally while its
    socket is taken, and a socket with a jewel still in it refuses to be refunded (`ST_014`)
    rather than quietly dropping the jewel.
13. **The auction's goods live in the lot.** While a lot is listed the instance has left
    `CharacterEquipment` and the stack has left the bag, which is why a worn item cannot be listed
    (`AU_010`). Prices are counted in currency orbs alone (`AU_007`), a seller cannot buy their own
    lot (`AU_006`), and the level the auction opens at is a server constant the client never copies:
    it asks, and turns `AU_002` into the screen's explanation. A lot's line is the same `ItemRow`
    the stash draws — icon, name, what it is, its properties — with the price and the seller
    underneath, and the card behind it adds when the lot was listed: the server writes `createdAt`
    in UTC, so the client is free to show it in the device's zone. Every auction tab refreshes by
    a pull, as the hero does, and buying asks first, saying so when the item cannot be worn yet.
14. **Gold is the merchant's, not the client's.** `POST /api/v1/characterequipment/sell` destroys
    the instance and pays for it; the price is the template's base times the copy's rarity times
    how many affixes rolled, times `STOCK_GOLD` — a characteristic that exists, that nothing
    grants yet, and that therefore costs nothing until something does. A worn or socketed item is
    refused (`CH_014`, `CH_015`), as the auction refuses one. The client sends the pair and prints
    what came back; it never works a price out, here or anywhere.
15. **An item has no stat fields, and no number is printed raw.** Armour, damage and attack speed
    are fixed modifiers in `baseParams` (values, no tier); `durability` is the only number left as
    a field. Every `Double` the server sends is counted in full and *displayed* through
    `statNumber(stat, value)`: whole, without a point, except the five rates where a fraction is
    the whole point — `STOCK_ATTACK_SPEED`, `STOCK_CAST_SPEED`, `STOCK_CRITICAL_CHANCE`,
    `STOCK_CRITICAL_MULTIPLIER`, `STOCK_MOVEMENT_SPEED` — which keep two decimals. Rounding is
    display and never travels back to the server. Rarity is never written out either: it is the
    colour of the frame and of the name, in `ItemRow` and in the auction's rows alike. A row lists
    its modifiers one per line — base first, then what rolled — up to `ROW_PROPERTIES`, and counts
    what does not fit rather than dropping it. Requirements
    (`requiredLevel`, `requiredStrength`, `requiredDexterity`, `requiredIntelligence`) are printed,
    never enforced here — the server checks them twice and the two checks are different rules:
    `equip` refuses an item out of reach outright (`CH_013`), while one already worn keeps its slot
    and only stops counting, landing in the sheet's `inactive`. Never disable a control on a
    requirement the client worked out itself; send the command and show the refusal.
16. **Release builds require HTTPS** (`usesCleartextTraffic=false`); only the debug manifest
    permits cleartext for local servers. This matters more than usual: the password travels as a
    query parameter, because that is the route the server exposes.
17. **No network or raster images; a drawing is outlines, not a picture.** No image file is ever
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
18. **The server owns every name; the client owns its own labels.** Since 0.14.0 no document
    carries text: equipment, items, modifiers, tree nodes and classes store a `code`, and the
    strings are static files — `locale/index.json` (languages with a hash each) and
    `locale/{ru,en,zh}.json`, keyed `<section>.<CODE>.<field>` exactly as `core/i18n/LocaleKey`
    builds it. Both sides must compute the same key, so never hand-write one. The bundle is
    global (`serverLocale`), loaded by `ForgeRuntime.loadLocale` per server and language and
    stored by hash in `ServerStore`; a missing key returns itself so a hole is visible. A
    modifier's text is a **template** with `{0}`, `{1}` per effect — never assemble a sentence by
    appending numbers to a label. `messageArgs` from the server are themselves keys, so resolve
    them through the bundle before substituting. The bundle also has an `enum.` section, but the
    client keeps its own tables (`slotTitle`, `rarityTitle`, `statTitle`, `CurrencyOrb`): they
    take an explicit language, while the dictionary answers in whichever one is loaded — and
    exactly one is loaded. Since 2.4.0 those tables are not two-argument enums either: they are
    keys in the client's own dictionary, `core/src/main/resources/i18n/ui_{ru,en,zh}.json`, read
    with `ui(lang, key)`. That is the whole of the client's half — one mechanism, keyed the same
    way as the server's, and shipped in the jar so a label exists before any server has answered.
    Which languages may be picked is still the server's to say: the picker is built from
    `locale/index.json`, so a language the server cannot name items in is never offered.
    A name is shown in the chosen language and in no other; the English
    twin the showcase briefly carried is gone, and so is the second bundle that fed it. Switching
    language is still cheap because `ServerStore` keeps a bundle per server *and* language, so the
    other one is usually already on disk. A refusal is translated by filling the client's own
    template with `ApiFailure.args` — since 0.17.0 the error envelope carries the arguments it
    interpolated, so all 114 codes translate rather than the 17 whose template had no hole. A
    template still holding `{0}` after substitution means the server sent fewer arguments than it
    wants, and then the server's finished sentence wins: half a sentence is worse than one in the
    wrong language. See `locError`.

19. **A session is made, never restored; a character is chosen once per session.** The server
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
20. **The hero is re-read on a reason, never on a timer and never on request.** Reading it whole
    is five requests, so nothing asks the player to press anything: a command re-reads it because
    the command changed it, and everything changed *elsewhere* — a trade, an administrator, the
    same account on another device — is caught by `heroReadAt` going cold. `ensureHero()` refreshes
    when a character tab opens and the last reading is older than `FRESH_FOR`; a trade sets the
    stamp to 0 because the auction patches only the bag and the inventory, never the character
    document. The manual path is a pull-to-refresh on the stash, not a button competing with the
    content. This server has no change feed and `version` belongs to the character alone, so a
    cheap "did anything change" question cannot be asked — the stamp is the answer instead.

## Conventions

- **Language split:** code, comments, commit messages and test names are English; **no
  user-facing string is written in the source at all** — including `require`/`check` messages,
  which surface in snackbars. It is a key in `core/src/main/resources/i18n/ui_{ru,en,zh}.json`,
  read with `ui("screen.thing")` from `core/i18n/Loc.kt`. `ui` reads the global `uiLanguage`,
  which defaults to **RU** in `:core`, so tests that assert Russian keep passing; `:app` sets it
  from the store, or from the device's own language on a first run. Compose refreshes because
  `ForgeApp` keys the whole tree on `s.lang`. Helpers that need an explicit language
  (`slotTitle`, `rarityTitle`, `weaponTitle`, `statTitle`, `nodeTypeTitle`, `Catalog.title`,
  `CurrencyOrb.title`) take one and pass it to `ui(lang, key)`; `uiOr` gives a fallback where a
  code the client cannot know might arrive. A sentence with a number in it is a template with
  `{0}`, never a label with a value appended — word order differs, and in Chinese it differs
  most; a counted noun goes through `plural(key, n)`. The **name of a thing in the game** is not
  a client string at all: it comes from `serverLocale` through `loc`/`locOr` (rule 18).
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
