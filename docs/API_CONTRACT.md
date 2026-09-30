# Контракт Exile Forge 3.0

Сервер: ветка `claude/tender-pasteur-a36kj2`, коммит `1d0ccd01206e1b32f86c2a7e0af1f8eafdceb755` (ktor-bestgame 1.53.1),
подключён подмодулем `backend/`; клиент собирается против его модуля `backend/rules` (`includeBuild`) и
закрепляет коммит и версию в `core/.../contract/Contract.kt`. Ревизия API — 35 (`API_REVISION`): клиент
требует от манифеста ровно её.

Конверт: успех `{"success":true,"data":...}`, ошибка `{"success":false,"error":{"message","errorCode","messageArgs"}}` (с 1.53.0 без имён классов и методов сервера).
Повтор команды по `Idempotency-Key` (заголовок `Idempotent-Replay: true`) отвечает `{"success":true,"data":null}` без снимка: клиент перечитывает героя сам.
Клиент показывает `error.<errorCode>` из словаря сервера, подставляя `messageArgs`; `message` — запасной текст.
Ответ команды героя несёт ещё `hero` — снимок (ниже).

## Одна истина — файлы контента

Справочников в Mongo нет. Всё, чем играют — статы, модификаторы, таблицы, основы и уникалки, предметы,
кампания, атлас, дерево, классы, умения, эссенции, силы, профессии, правила — лежит в
`backend/src/main/resources/content/*.json` (14 чанков, `ContentFiles.ALL`) и разворачивается модулем
`rules` одинаково на сервере и в клиенте: роллы, лист персонажа, строки модификаторов, заходы.

| Что | Запрос |
|---|---|
| Манифест старта (публичный) | `GET /static/index.json` → `{version, revision, routes:[{path,method}], locale, icons, portraits, content:{hash, chunks:{файл: отпечаток}}}` |
| Чанк контента (публичный) | `GET /content/{file}` → JSON файла; `ETag` = отпечаток, `If-None-Match` → 304, gzip по `Accept-Encoding` |
| Словари | `GET /locale/index.json` → `{default, languages:[{code,label,hash}]}`, `GET /locale/{code}.json` |
| Иконки, портреты | `GET /icons/index.json`, `GET /icons/{file}`, `GET /portraits/index.json`, `GET /portraits/{section}/{file}` |
| Здоровье, маршруты | `GET /system/health`, `GET /system/routes` |

Клиент хранит чанки на устройстве по отпечатку и после манифеста качает только изменившиеся;
`ContentLoader.load` собирает из них `ContentIndex`, чей `hash` обязан совпасть с `content.hash` манифеста.
Строки модификаторов сервер не разворачивает: клиент собирает их сам (`ModifierText`) из словаря
(`modifier.<код>.name`, `stat.template.*`, подписи статов `enum.EnumStat*.<код>`).

## Сессия

Вход отвечает `{user:{id,version,name,login,isActive,role,countCharacters}, token}`; токен — `Authorization: Bearer`.

| Операция | Запрос |
|---|---|
| Вход | `POST /api/v1/user/login`, тело `{login,password}` |
| Вход по устройству | `POST /api/v1/user/login/byDeviceId`, тело `{deviceId}`; `US_015` — устройство неизвестно |
| Регистрация по устройству | `POST /api/v1/user/byDeviceId`, тело `{deviceId}` |
| Текущий пользователь | `GET /api/v1/user/me` |
| Выход, смена пароля | `POST /api/v1/user/logout`, `POST /api/v1/user/changePassword` `{password,newPassword}` |

Без токена открыты только вход и регистрация, `/static`, `/content`, `/locale`, `/icons`, `/portraits`, `/system`.
Игрок видит и меняет только своих героев; выдача (`grant/*`) и промокоды (кроме `redeem`) — администратору.

## Герой — один документ

`heroId` — параметр запроса каждой команды героя. Документ: `hero` (класс, уровень, опыт, золото,
умения, атлас, рецепты, слоты аукциона), `items` — копии `ItemInstance`
`{id, t: шаблон, r: редкость, m: роллы [{c: код, t: тир, p: доля, f: фрактурен, x: масштаб}], cor, mir, s: место, sk: гнездо, i: влияние, q: качество}`,
`bag` — `{код: количество}` (сферы, эссенции, книги, материалы), `tree` — `[{code, choice}]`,
`campaign`, `crafts`, `merchant`. Значения роллов не хранятся — их выводит `rules` из кода, тира и доли.

| Операция | Запрос |
|---|---|
| Герои аккаунта | `GET /api/v1/hero/byUser?userId` → `[{_id, userId, name, description, heroClass, level, experience, money}]` |
| Создать / удалить | `POST /api/v1/hero`, тело `[{userId,name,description,heroClass}]` → список; `DELETE /api/v1/hero?id=` |
| Снимок | `GET /api/v1/hero/view?heroId`, заголовок `X-Hero-Parts: часть=отпечаток,...` или `none`; `If-None-Match: "<version>"` → 304 |
| Надеть, снять | `POST /api/v1/hero/equip?heroId&itemId[&slot]`, `POST /api/v1/hero/unequip?heroId&itemId` → `ItemInstance` |
| Гнездо дерева | `POST /api/v1/hero/socket?heroId&itemId&nodeCode`, `POST /api/v1/hero/unsocket?heroId&itemId` |
| Продать | `POST /api/v1/hero/sell?heroId&itemId` → `{itemId, code, gold, money}` |
| Сфера, эссенция | `POST /api/v1/hero/orb?heroId&itemId&orb=<код>`, `POST /api/v1/hero/essence?heroId&itemId&essence=<код>` → `{messageKey, messageArgs, item, created}` |
| Верстак | `GET /api/v1/hero/bench?heroId` → `[BenchRecipe]`, `POST /api/v1/hero/craft?heroId&itemId&recipe`, `POST /api/v1/hero/uncraft?heroId&itemId` |
| Умения | `POST /api/v1/hero/skills/learn?heroId&skill`, `.../slot`, `.../flask`, `.../exchange` → `HeroSkills` |
| Дерево | `GET /api/v1/hero/skilltree/state?heroId`, `POST .../allocate?heroId&nodeCode[&choice]`, `.../refund`, `.../rechoose`, `.../reset` → `{total, spent, available, nodes, totals}` |
| Атлас | `GET /api/v1/hero/atlas/state?heroId`, `POST .../allocate?heroId&nodeCode`, `.../refund`, `.../reset` → `{allocated, earned, points, available}` |
| Ремёсла | `GET /api/v1/hero/crafts?heroId`, `POST .../start?heroId&job[&additives]`, `POST .../stop?heroId` |
| Торговец | `GET /api/v1/hero/merchant?heroId` → `{refreshAt, offers:[{id, item, price}]}`, `POST .../buy?heroId&offerId` → `{item, money}` |
| Выдача (админ) | `POST /api/v1/hero/grant/experience?heroId&amount`, `.../item?heroId&code&amount`, `.../equipment?heroId&template[&rarity]` |

Снимок: `{version, parts:{часть:{version, data}}}`, части `hero`, `items`, `bag`, `tree`, `campaign`,
`crafts`, `merchant`; отпечаток части — первые 16 знаков sha256 её JSON, `version` снимка — версия
документа. Каждая команда с `heroId` и `X-Hero-Parts` отвечает `{success, data, hero}` — только
частями, чьи отпечатки не совпали; так клиент почти не перечитывает героя. Лист персонажа, состояние
дерева, прогресс кампании и атласа клиент считает сам из снимка и контента.

Волшебная и редкая копия всегда несёт хотя бы один аффикс, редкая — не меньше дна своей редкости
(`rules.json → rarities`); это держат все пути: ролл, сферы, верстак, торговец, ремёсла.

## Заход по семени

| Операция | Запрос |
|---|---|
| Прогресс | `GET /api/v1/hero/campaign/progress?heroId` → `{cleared, unlocked}` |
| Начать | `POST /api/v1/hero/campaign/start?heroId&mapCode[&itemId]` → `RunStart {id, seed, zone, level, context, count, startedAt}` |
| Журнал | `POST /api/v1/hero/campaign/events?heroId`, тело `[RunEvent]` → `RunReport {applied, rejected, reward, lost, level, experience, money, progress, open}` |

`start` проверяет, что зона открыта, тратит карту (`itemId`, если зона идёт по карте), обновляет окна
сундуков, кристаллов и трещин и замораживает `RunContext` героя (класс, уровень, бонусы по редкостям,
атлас, карта, Ваал-зона, связи, рецепты). Клиент строит `Run(index, zone, seed, context)` и катает
монстров и добычу сам — тем же кодом `rules`, что и сервер — а события пишет в журнал на диске:
`RunEvent {n, kind, i, m, index, depth, fallen, vaal}`, `kind` ∈ `KILL, CHEST, BOSS, CORRUPT, CRYSTAL,
CRYSTAL_VAAL, VAAL_OPEN, VAAL_LEAVE, ABYSS_OPEN, ABYSS_CLAIM, SUMMON, FALL, LEAVE`.

Журнал уходит на контрольных событиях (босс, ворота, кристаллы, Бездна, призыв, гибель, выход), пачками
и по таймеру; при следующем запуске недосланное досылается. Сервер проигрывает события по номеру:
`n < applied` пропускает (повтор безвреден), `n > applied` — `CP_019`, отклонённое правилом событие
попадает в `rejected`, но номер засчитывается; `FALL`/`LEAVE` закрывают заход. Каждый жетон убивается
один раз (`i*8+m`), `LEAVE` возможен только после босса, `SUMMON` возвращает босса за золото,
`FALL` списывает потерю по правилу смерти. Правда — ответ сервера: `reward`, `lost`, `progress`,
уровень и золото в отчёте и снимок героя.

## Аукцион и промокоды

| Операция | Запрос |
|---|---|
| Поиск | `GET /api/v1/auctionlot/search?heroId&page&size[&lang&title&kind&slot&rarity&minItemLevel&maxItemLevel&priceOrb&maxPrice&sellerId&excludeSellerId]` → `{items, page, totalItems, totalPages}` |
| Мои лоты, слоты | `GET /api/v1/auctionlot/my?heroId`; `GET`/`POST /api/v1/auctionlot/slots?heroId` → `{used, limit, max, price, money}` |
| Выставить | `POST /api/v1/auctionlot/sell/equipment?heroId&itemId&priceOrb&price`, `POST .../sell/item?heroId&code&amount&priceOrb&price` |
| Купить, снять | `POST /api/v1/auctionlot/buy?heroId&lotId`, `POST /api/v1/auctionlot/cancel?heroId&lotId` |
| Промокоды | `GET`/`POST`/`DELETE /api/v1/redemptioncodes` (админ); `POST /api/v1/redemptioncodes/redeem?heroId&code` → `"system.success"` |

Лот `{_id, sellerId, sellerName, kind: EQUIPMENT|ITEM, equipment: ItemInstance?, item: код, amount, priceOrb: код сферы,
price, itemCode, slot, rarity, itemLevel, status: ACTIVE|SOLD|CANCELLED, buyerId, createdAt, closedAt, version}`:
выставленная вещь выходит из документа героя и возвращается при снятии. Промокод
`{code, description, treasure:[{kind: ITEM|EQUIPMENT|EXPERIENCE|GOLD, item: код, amount}], used, expiredAt}`.

## Словари

Каждый модификатор всех пулов — предметов, монстров, карт, атласа — имеет имя на всех языках сервера
(`modifier.<код>.name`); каждый стат реестра — подпись (`enum.EnumStat*.<код>`); клиент держит свои
строки интерфейса в `core/src/main/resources/i18n/ui_*.json` и проверяет тестами, что языки совпадают
по ключам и что каждое перечисление `rules`, которое он показывает, названо.
