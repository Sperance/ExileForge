# ExileForge 2.0.0

Android Compose client for **ktor-bestgame 0.14.0**.
Server: branch `claude/tender-pasteur-a36kj2`, commit `3a07a4f8d3e65a1365f088b4ab710e1c609ee257`.

## Язык интерфейса · Interface language

Каждый элемент интерфейса доступен на русском и английском. Переключатель `RU / EN` стоит в шапке приложения и во вкладке «Аккаунт»; выбор сохраняется в DataStore и применяется ко всем экранам, подсказкам, сообщениям об ошибках и проверкам контракта. Русский остаётся языком по умолчанию.

Every label, hint, error and contract-validation message exists in Russian and English. The `RU / EN` switch sits in the app banner and on the Account tab; the choice is stored in DataStore and applies to the whole interface. Russian stays the default.

## Оформление

Тёмная тема в духе Path of Exile: чернёный камень, бронзовые рамки с косыми углами, гравированные заголовки с ромбовидным разделителем, рамки предметов в цветах редкости и сферы здоровья, маны и щита. Картинки игровых сущностей рисует сам клиент: набор векторных эмблем покрывает оружие, броню, украшения и расходники, а глифы приложения — навигацию и свойства. Растровых и сетевых изображений нет.

## Экраны

- **Персонажи / Каталог** — поиск и постраничный просмотр персонажей, экипировки и предметов. Фильтры по слоту, редкости, типу оружия, уровню и модификатору в пуле.
- **Герой** — сводка персонажа, класс, надетые слоты, характеристики сервера, инвентарь и сумка, применение валютных сфер, промокоды и рецепты.
- **Дерево** — дерево навыков: карта узлов, подсветка доступных, поиск по названию, взятие, возврат и полный сброс, баланс очков.
- **Аукцион** — витрина с серверным поиском, свои лоты и выставление.

Нижняя панель одинакова для всех: Каталог, Герой, Дерево, Аукцион, Аккаунт. «Редактор» и «Проверки» — инструменты администратора и открываются из вкладки «Аккаунт», чтобы не занимать место у игрока.
- **Редактор** — шаблоны экипировки и предметов, персонажи и их базовые характеристики.
- **Проверки** (администратор) — CRUD-сценарий и журнал запросов.
- **Аккаунт** — сервер, вход, смена пароля и язык.

## Получить предмет с рандомными роллами

Администратор на вкладке «Герой» выбирает редкость и категорию (слот) и жмёт **«Получить предмет с рандомными роллами»**. Клиент берёт случайный шаблон с такой редкостью и слотом и просит сервер создать его экземпляр. Всё остальное — дело сервера: он выбирает префиксы и суффиксы в количестве, которое задаёт редкость, добавляет постоянные источники (implicit, enchant, corruption, unique), роллит тир каждого модификатора и значение внутри его диапазона. Клиент не роллит ничего и не предсказывает результат.

Рядом остаются выдача конкретного шаблона по справочнику и изменение количества простых предметов в сумке.

## Сферы

Сервер 0.9.1 вернул валютные сферы POE, и клиент использует их все четырнадцать: превращения, улучшения, изменения, алхимии, царскую, хаоса, высшую, божественную, аннулирования, очищения, священную, ваал, удачи и Зеркало Каландры. Сфера — обычный предмет сумки из категории `CURRENCY`; клиент читает каталог из коллекции `items` и показывает рядом с каждой сферой то количество, которым персонаж владеет.

Применить сферу можно на вкладке «Герой»: откройте предмет в арсенале, выберите сферу и нажмите **«Применить сферу»**. Что сфера делает, решает только сервер — какая редкость ей нужна, сколько аффиксов она роллит и что оставляет нетронутым. Клиент отправляет пару «предмет + сфера» и печатает ответ сервера дословно, включая отказ. Сфера списывается из сумки в той же транзакции, поэтому неудачная проверка её не съедает.

Редкость с этой версии принадлежит копии предмета, а не шаблону: шаблон задаёт лишь то, с чем предмет падает. Порченый предмет (`Сфера ваал`, копия из Зеркала) помечен в карточке — сервер больше не примет на него ни одной сферы, и кнопка выключена.

Администратору доступна та же выдача в разделе **«Сферы на предметах»**: там предмет выбирается списком, а не открытием карточки, и рядом стоит кнопка «Выдать 10 таких сфер», чтобы проверить правило на живом сервере, не фармя валюту.

## Класс, уровни и требования

С 0.10.0 базу характеристик задаёт **класс персонажа**, а не сам персонаж: `stockSkills` с документа убраны, вместо них ссылка `classId`. Класс выбирается при создании персонажа и больше не меняется — сервер читает его базу при каждом расчёте, и перенос в другой класс переписал бы историю персонажа. Маршрута для такой правки у сервера нет.

Характеристики теперь приходят объектом: числа, уровень и, главное, вердикт сервера по каждому надетому предмету. Требования (`уровень`, `сила`, `ловкость`, `интеллект`) сервер проверяет дважды, и это два разных правила: надеть предмет, до которого персонаж не дорос, он не даст вовсе, а предмет, у которого требования перестали выполняться уже после надевания, **остаётся в слоте, но не работает** — в панели героя он помечен красным с причиной от сервера. Ни требования, ни характеристики клиент не пересчитывает и кнопку по ним сам не блокирует.

У предметов не осталось числовых полей под характеристики: броня, урон и скорость атаки задаются модификаторами в `baseParams`, с фиксированными значениями и без тира. Прочность оружия — единственное число, оставшееся полем предмета.

Опыт начисляет администратор во вкладке «Герой»; уровень и очки дерева пересчитает сервер по своей таблице.

## Дерево навыков

Дерево вернулось в 0.9.2 и реализовано целиком. Вкладка **«Дерево»** рисует граф по координатам, которые задаёт сервер: карту можно двигать и масштабировать, узел выбирается касанием. У выбранного узла видны вид, стоимость, бонусы и состояние, а дальше — «Взять узел», «Вернуть узел» и полный сброс.

Правила целиком на сервере: начинают со стартового узла своего класса, дальше берут только соседей уже взятых, вернуть узел можно лишь тогда, когда остальное дерево не повиснет, а стартовый — только полным сбросом. Клиент отправляет код узла и показывает отказ дословно. Бонусы узла записываются персонажу снимком, поэтому перебалансировка дерева не затрагивает уже прокачанных.

Очки дают уровни; баланс «всего / потрачено / доступно» считает сервер.

## Аукцион

Торговля между игроками живёт в отдельной вкладке с тремя разделами: **Витрина**, **Мои лоты**, **Выставить**. Вкладка «Герой» торговлю не ведёт — предмет продаётся отсюда.

**Витрина** — единственный список в приложении, который сужает сам сервер: у аукциона есть собственный поиск с фильтром и постраничной выдачей. Название, вид лота и потолок цены видны всегда; слот, редкость, диапазон уровня предмета и продавец — под «ещё фильтры». Все поля фильтра сравниваются со снимком, который лот несёт в себе, поэтому поиск укладывается в один запрос.

Свои лоты из витрины скрыты — купить их всё равно нельзя, — но переключатель «Показывать свои» возвращает их, чтобы сравнить свою цену с чужими.

**Цена назначается только в сферах**: это единственная валюта, в которой сервер торгует. В карточке лота цена читается как «4 × Сфера хаоса».

**Выставить** можно снятую экипировку и содержимое сумки, включая сами сферы. Надетый предмет сервер не примет — пока лот на витрине, товар лежит внутри него, и надеть или продать его второй раз нельзя. Поэтому в списке на продажу показывается только снятое.

Аукцион открывается с уровня, который задаёт сервер, и порог стоит даже на просмотре витрины. Клиент этого числа не хранит: он отправляет запрос и превращает отказ сервера в понятное объяснение — так порог не разойдётся с сервером при первой же правке константы.

## Connect

1. Start the server and its MongoDB replica set (the backend pins `mongodb://localhost:27017`, database `mongobase`).
2. In **Аккаунт**, save the server root URL (without `/api/v1`). Emulator: `http://10.0.2.2:8080/`; physical device: the computer's LAN address.
   `10.0.2.2` is the emulator's alias for the host loopback, so it reaches a server bound to `127.0.0.1`
   — unless a firewall drops the packets, which shows up as a ten-second timeout rather than a refusal.
   The way round that is an adb port forward, which needs no firewall rule because it rides the adb
   channel. Start the emulator first, then forward, then use `http://127.0.0.1:8080/` in the app:

   ```bash
   adb reverse tcp:8080 tcp:8080   # adb lives in <SDK>/platform-tools
   adb reverse --list              # confirms: (reverse) tcp:8080 tcp:8080
   ```

   **The forward does not survive a restart** of the emulator or the adb server — repeat it after each
   one. A vanished forward reads as "порт не принимает соединение" in the app, instantly rather than
   after a timeout: nothing is listening on the emulator's own loopback any more.
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
- **Lists are paged on the client, except the auction.** `/paged` was fixed in 0.13.1, but the generic route still offers no filter, and moving the catalogue onto it would cost every catalogue filter. The auction has a search route of its own, so its showcase is filtered and paged by the server.
- **The player auction (0.13.0).** Lots live in `auctionlot`; while a lot is listed the goods live inside it rather than with the seller. Prices are counted in currency orbs alone.
- **The tree moved into the character (0.12.1).** `CharacterSkillNode` is no longer a collection: taken nodes are a snapshot inside `Character.skillNodes`, and the routes live under `/api/v1/character/skilltree`.
- **The server owns every name (0.14.0).** No document in Mongo carries text any more: equipment, items, modifiers, tree nodes and classes store a **code**, and the strings live in static files — `GET /locale/index.json` lists the languages with a hash each, `GET /locale/{ru,en}.json` is the dictionary, keyed `<section>.<CODE>.<field>`. The client downloads the bundle once per hash, stores it per server and language, and looks every name up through it; without it a screen shows codes rather than blanks. A modifier's text is a **template** with `{0}`, `{1}` per effect, because a composite modifier cannot be assembled by bolting numbers onto a label in every language. An orb's result arrives as a message key plus arguments that are themselves keys. The client keeps its own tables for slots, rarities and stats: those take an explicit language and a dictionary holds one at a time.
- **Currency orbs are back (0.9.1).** They live in the `items` collection under the `CURRENCY` category and are applied through `POST /api/v1/characterequipment/applyOrb`. The rarity and the corruption flag now belong to the copy, not to the template.
- **The passive tree is back (0.9.2).** A shared seeded graph plus one document per node a character has taken, with allocate, refund and reset. Every rule is checked by the server.
- **Stats were reworked (0.10.0).** A character references a class instead of carrying base stats; `stats` answers an object naming the equipped items the server counted and the ones whose requirements are not met; an item's base is fixed modifiers rather than damage and defence fields; modifier effects can be attribute conversions.
- **Removed with the features the server no longer has:** combat and the battle arena, the crafting bench, the server icon set, JWT, cursor-paged inventory and three-way conflict review.
- **Upgrading the server needs a fresh database.** The schema migrator was removed in 0.10.1 and the player-data format has changed several times since, so a database that predates 0.14 is reseeded rather than migrated.

## Build and verification

JDK 17, Android SDK 37, Gradle wrapper:

```bash
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

GitHub Actions also runs Compose checks on an API 35 emulator. The `client-server` job builds the pinned backend, starts it against an isolated MongoDB replica set and exercises the real `GameApi`. APKs and UI reports are attached to each successful workflow run.
