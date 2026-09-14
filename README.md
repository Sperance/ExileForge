# ExileForge 1.6.0

Android Compose client for **ktor-bestgame 0.9.0**, API revision 2.
Server: `refactor/compact-rpg-architecture`, commit `5d477ffd42f65ad1fe5e056602b670181ffae516`.
Client development branch: `master`.

## Connect

1. Start the updated server and its MongoDB replica set.
2. In **Сервер**, save the server root URL (without `/api/v1`). Emulator: `http://10.0.2.2:8080/`; physical device: the computer's LAN address.
3. Log in with an existing account. An administrator creates accounts on the server. The application checks API capabilities before login.
4. In **Каталог**, select characters and create your character. The server assigns the authenticated owner automatically.
5. In **Герой**, select a character and refresh the arsenal. Administrators can grant equipment/currency through searchable selectors or request a random drop for their own character.

Debug allows HTTP for local development. Release requires HTTPS. Tokens stay in memory; passwords and token responses are excluded from the request journal. Password changes require login again.

## What changed for API 0.9

- Catalog, selectors, CRUD and character commands send Bearer authentication. UI permissions come from the current server profile.
- POST uses server-generated IDs. Character creation/editing exposes name and description only; owner, inventory, money and stats are server controlled.
- PUT sends `{expectedVersion, changes}`; DELETE sends `{expectedVersion}`. Versions remain Kotlin Long, including values above JavaScript's safe integer range.
- A 409 preserves the editor draft and requires explicit reload confirmation. No mutation silently retries with a newer version. A 401 clears the session and private UI state.
- Equipment templates select immutable modifier definitions and revisions. Rolled instance modifiers are changed using server crafting commands, not arbitrary template JSON.
- The Hero tab uses EquipmentView: instance snapshots, equipped slots, item stacks and server-calculated stats. Rings have separate slots. The server validates level/attribute requirements and hand compatibility.
- The Hero tab supports equip, unequip, redemption, instantaneous recipes, administrator grants and stack adjustments. IDs are selected by searchable pickers.
- Recipes with time or skill requirements remain unavailable until implemented by the server. Refresh a recipe after use to obtain its new version.
- All displayed stats come from the server; unsupported effects are listed explicitly. This client does not implement a second combat calculator.
- Craft/drop retain the same requestId after an ambiguous network error. Other commands require refresh after uncertain outcomes; do not automatically repeat them.
- CRUD checks are administrator-only and clean up only a confirmed server-generated ID with its known version. If the POST response is lost, the journal/report identifies the unique test name for manual review.

## Build and verification

JDK 17, Android SDK 37, Gradle wrapper:

```bash
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

GitHub Actions also runs Compose checks on an API 35 emulator. APKs and UI reports are attached to each successful workflow run.
The original fantasy cards, property/item icons, bottom navigation, reference pickers and server-side modifier search remain available.
