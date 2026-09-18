# ExileForge 2.0.0

Android Compose client for **ktor-bestgame 0.9.0**.
Server: branch `claude/tender-pasteur-a36kj2`, commit `e8e9ae824dda7484622462da892c636c6369be6b`.

## Язык интерфейса · Interface language

Каждый элемент интерфейса доступен на русском и английском. Переключатель `RU / EN` стоит в шапке приложения и во вкладке «Аккаунт»; выбор сохраняется в DataStore и применяется ко всем экранам, подсказкам, сообщениям об ошибках и проверкам контракта. Русский остаётся языком по умолчанию.

Every label, hint, error and contract-validation message exists in Russian and English. The `RU / EN` switch sits in the app banner and on the Account tab; the choice is stored in DataStore and applies to the whole interface. Russian stays the default.

## Оформление

Тёмная тема в духе Path of Exile: чернёный камень, бронзовые рамки с косыми углами, гравированные заголовки с ромбовидным разделителем, рамки предметов в цветах редкости и сферы здоровья, маны и щита. Картинки игровых сущностей рисует сам клиент: набор векторных эмблем покрывает оружие, броню, украшения и расходники, а глифы приложения — навигацию и свойства. Растровых и сетевых изображений нет.

## Экраны

- **Персонажи / Каталог** — поиск и постраничный просмотр персонажей, экипировки и предметов. Фильтры по слоту, редкости, типу оружия, уровню и модификатору в пуле.
- **Герой** — сводка персонажа, надетые слоты, характеристики сервера, инвентарь и сумка, промокоды и рецепты.
- **Редактор** — шаблоны экипировки и предметов, персонажи и их базовые характеристики.
- **Проверки** (администратор) — CRUD-сценарий и журнал запросов.
- **Аккаунт** — сервер, вход, смена пароля и язык.

## Получить предмет с рандомными роллами

Администратор на вкладке «Герой» выбирает редкость и категорию (слот) и жмёт **«Получить предмет с рандомными роллами»**. Клиент берёт случайный шаблон с такой редкостью и слотом и просит сервер создать его экземпляр. Всё остальное — дело сервера: он выбирает префиксы и суффиксы в количестве, которое задаёт редкость, добавляет постоянные источники (implicit, enchant, corruption, unique), роллит тир каждого модификатора и значение внутри его диапазона. Клиент не роллит ничего и не предсказывает результат.

Рядом остаются выдача конкретного шаблона по справочнику и изменение количества простых предметов в сумке.

## Connect

1. Start the server and its MongoDB replica set (the backend pins `mongodb://localhost:27017`, database `mongobase`).
2. In **Аккаунт**, save the server root URL (without `/api/v1`). Emulator: `http://10.0.2.2:8080/`; physical device: the computer's LAN address.
3. Log in with an existing account. The application reads `/system/routes` and refuses a server that is missing a route it needs.
4. In **Каталог**, create your character. The current account becomes the owner; the server enforces the per-account limit.
5. In **Герой**, select a character and refresh it.

Debug allows HTTP for local development; release requires HTTPS. **This server has no token**: the login response is the session and lives in memory only. The password travels as a query parameter, so HTTPS is not optional outside a local network. Login and password changes are recorded as `[скрыто]` in the request journal.

## What changed for server 0.9.0

- **No token.** `GET /api/v1/user/login` answers with the account document; the client keeps it in memory and drops it on sign-out. Permissions come from the server's `role`.
- **No client version on writes.** `PUT` sends the changed fields and `DELETE` sends nothing: the server reads the stored document and rejects a racing write itself. A rejected write is reported, never retried silently.
- **Equipment is a pool of references.** A template carries `modifierIds`; which of them land on a copy, in which tier and with which value, is rolled by the server when the instance is created.
- **Inventory is its own collection.** One item in a character's bag is one `CharacterEquipment` document with its own rolls; the slot comes from the template, so equipping takes an instance id and nothing else.
- **Stats come from the server.** `GET /api/v1/character/inventory/stats` returns the summed sheet; the client prints it and implements no second calculator.
- **Search is a display concern.** The server pages but does not filter, so a filtered search reads the collection once and narrows it on the client. Nothing game-related is computed.
- **Removed with the features the server no longer has:** passive tree, combat and the battle arena, orbs and crafting, the server icon set, JWT, cursor-paged inventory and three-way conflict review.

## Build and verification

JDK 17, Android SDK 37, Gradle wrapper:

```bash
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

GitHub Actions also runs Compose checks on an API 35 emulator. The `client-server` job builds the pinned backend, starts it against an isolated MongoDB replica set and exercises the real `GameApi`. APKs and UI reports are attached to each successful workflow run.
