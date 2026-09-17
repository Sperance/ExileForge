# ExileForge 1.11.0

Android Compose client for **ktor-bestgame 0.13.0**, API revision 4.
Server: `master`, commit `f0d88446254b1f3d3ff1a06a6e609471ba99f97e`.
Client development branch: `master`.

## Язык интерфейса · Interface language

Каждый элемент интерфейса доступен на русском и английском. Переключатель `RU / EN` стоит в шапке приложения и во вкладке «Аккаунт»; выбор сохраняется в DataStore и применяется ко всем экранам, подсказкам, сообщениям об ошибках и проверкам контракта. Русский остаётся языком по умолчанию.

Every label, hint, error and contract-validation message exists in Russian and English. The `RU / EN` switch sits in the app banner and on the Account tab; the choice is stored in DataStore and applies to the whole interface. Russian stays the default.

## Оформление

Тёмная тема в духе Path of Exile: чернёный камень, бронзовые рамки с косыми углами, гравированные заголовки с ромбовидным разделителем, рамки предметов в цветах редкости PoE (обычный, магический, редкий, уникальный) и сферы здоровья, маны и щита. Собственный набор векторных глифов (наковальня, скрещённые клинки, сфера, самоцвет, флакон, шлем, щит, тайник, свиток, портал, созвездие, череп, сигил, изгнанник, атлас, том, факел, весы, цепь, знамя) остаётся для элементов самого приложения.

## Иконки предметов и сущностей

Картинки игровых сущностей теперь рисует сервер: набор `forge-vector` приходит одним спрайтом и покрывает оружие, броню, украшения, сферы, характеристики, аффиксы, редкости, узлы дерева и бой. Клиент разбирает SVG сам и рисует его на Canvas — растровых и сетевых изображений по-прежнему нет. Набор кэшируется по `iconSetVersion`, обновляется условным запросом и полностью необязателен: сервер без набора оставляет приложение на встроенных эмблемах. [Подробности](docs/ICONS.md).

## Древо навыков

[Общее дерево и личные навыки героя](docs/PASSIVES.md): 115 узлов, связи, крупные и ключевые навыки, масштабирование, поиск, изучение и безопасный сброс. Вход из «Герой» и «Поход».

## Поход и бой

Выбор зон, обычные мобы и боссы, пошаговые действия по характеристикам, журнал и таблицы лута. Победа автоматически выдаёт опыт, золото, предметы и сферы. Потерянный ответ восстанавливается повтором сохранённой команды. [Правила и управление](docs/COMBAT.md).

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
- A 409 preserves the editor draft and opens explicit conflict review. No mutation silently retries with a newer version. A 401 clears the session and private UI state.
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

## Workbench release — 1.11.0 / server 0.13.0

This release targets **API revision 4**. Deploy the server from `master` at `f0d88446254b1f3d3ff1a06a6e609471ba99f97e` before updating the app.

- Icons for items, equipment, currency, stats, passive nodes, zones and monsters come from the server set. The client reads `icon` from every response, falls back to the published binding tables for documents written before the set existed, and keeps its own emblems only for servers that ship no icons.
- The whole set arrives as one public sprite, is stored per server against `iconSetVersion`, and is refreshed with a conditional request. Account changes never drop it; switching servers does.
- Administrators can set `icon` on equipment and items. The editor offers the ids of the loaded set and refuses anything outside it before a request is built.

- The interface ships in Russian and English. `RU / EN` in the banner switches every label, snackbar and validation message at once, including the ones raised inside `:core`.
- Login starts in **Player** mode. Players have Characters, Hero, Forge and Account. An administrator can switch to the administration workspace with catalog editors and diagnostic checks. Switching modes is disabled while an editor has an open draft.
- Hero shows the character summary, icon-based equipment slots, inventory cards and server stats. Comparing an item calls the read-only server endpoint; confirming equips with the same character version. Slot compatibility and requirements are checked on the server.
- Forge shows currency counts, eligibility and rejection reasons. The engine's eligibility check does not predict random rolls or guarantee a later transaction: current ownership/version and equipped-item requirements are checked again during crafting. Before/after cards show the last completed craft.
- Catalog search and pickers search server-side before pagination. Filters support slot, rarity, item-level bounds, basic numeric equipment properties and modifier definition references. Filters are stored per server/catalog and restored when reopening that catalog. This is not a search over computed character stats or every raw PoE stat.
- On 409 the editor offers three-way review. Disjoint changes transfer automatically; overlapping fields require an explicit choice. Modifier arrays remain atomic. The merged result remains a draft until the user saves; a second concurrent edit still returns 409.
- `ForgeViewModel` is a lifecycle facade. Catalog, editor, hero, session and checks actions live in `presentation/features`; `ForgeRuntime` owns the shared coroutine lifetime, transport and cross-screen coordination. Read models for characters, instances, PoE rolls, recipes, comparisons and currency eligibility are typed. Arbitrary admin template/definition editors retain JSON only at their form boundary.
- Shared `FailureState` distinguishes offline reads, uncertain writes, expired sessions, forbidden operations and version conflicts. Uncertain writes never retry with a fresh version automatically.

The `client-server` CI job builds the pinned backend, starts it with an isolated MongoDB replica set and exercises the actual Kotlin GameApi. Credentials are generated per run. The default unit job skips this opt-in live test; the dedicated job runs it with the server.
