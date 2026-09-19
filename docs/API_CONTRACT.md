# Контракт Exile Forge 2.0

Сервер: ветка `claude/tender-pasteur-a36kj2`, коммит `64b3577822e0f4b5d710ca8b9d4e250e65269359` (ktor-bestgame 0.9.1).
Успех: `{"success":true,"data":...}`. Ошибка: `{"success":false,"error":{"message","errorClass","errorMethod","errorCode"}}`; HTTP-статус сохраняется клиентом.

## Сессия

Токена нет. `GET /api/v1/user/login?login=…&password=…` отвечает документом учётной записи, и он же является всей сессией: клиент держит его в памяти, никуда не записывает и теряет при выходе. Пароль уходит параметром запроса — это форма маршрута сервера, поэтому в продакшене обязателен HTTPS. В журнале запросов строка параметров и тело входа и смены пароля заменены на `[скрыто]`.

| Операция | Запрос |
|---|---|
| Вход | `GET /api/v1/user/login?login=&password=` → `UserResponse` |
| Текущий пользователь | `GET /api/v1/user?id=` |
| Смена пароля | `GET /api/v1/user/changePassword?id=&password=&new_password=` |
| Возможности | `GET /system/routes` → `[{path,method}]`, метод печатается селектором Ktor как `(GET)` |
| Здоровье | `GET /system/health` |

Роль берётся из ответа (`role: USER | MODERATOR | ADMIN`), а не из клиентского состояния. `ApiCapabilities` строится из таблицы маршрутов сервера: перед входом клиент проверяет, что нужные ему пути существуют, и называет отсутствующие.

## Коллекции

Каждая сущность обслуживается одинаковым набором маршрутов, имя сегмента — имя класса сервера в нижнем регистре: `user`, `character`, `characterequipment`, `items`, `equipment`, `recipe`, `redemptioncodes`, `modifierdefinition`, `modifiertier`.

| Операция | Запрос |
|---|---|
| Вся коллекция | `GET /api/v1/{collection}` |
| Страница | `GET /api/v1/{collection}/paged?page=0&size=20` — **не используется, см. ниже** |
| По ID | `GET /api/v1/{collection}?id=<24 hex>` |
| Количество | `GET /api/v1/{collection}/count` |
| Создание | `POST /api/v1/{collection}`, тело — JSON-**массив** документов |
| Изменение | `PUT /api/v1/{collection}?id=…`, тело — объект только изменённых полей |
| Удаление | `DELETE /api/v1/{collection}?id=…`, без тела |

Версию определяет сервер: он читает документ, сравнивает его `version` в фильтре обновления и отклоняет гонку ошибкой. Клиент не присылает `expectedVersion` и **никогда** не повторяет запись автоматически. Служебные поля (`_id`, `id`, `version`, `deleted`, `createdAt`, `updatedAt`) сервер игнорирует в PUT, клиент их не отправляет. Дискриминатор `type` допустим только при создании экипировки.

`/paged` на этом коммите по-прежнему нельзя использовать. В 0.9.1 у `findLimited` поменяли местами параметры (`skip, limit`), но арифметику выше не тронули: `findPaged(page, pageSize)` передаёт номер страницы как смещение вместо `page * pageSize`. Первая страница теперь приходит правильно, а каждая следующая сдвигается на одну запись вместо целой страницы. Фильтров у маршрута тоже нет. Поэтому клиент читает коллекцию целиком (`GET /api/v1/{collection}`), фильтрует и режет на страницы у себя; коллекции небольшие и сеются сервером. Когда на сервере поправят смещение, страницу можно будет вернуть на `/paged`.

## Экипировка

`Equipment` — sealed-класс с дискриминатором `type`, равным полному имени класса: `features.data.equipment.equipment_data.Weapon | Armor | Accessory`.

```json
{"type":"features.data.equipment.equipment_data.Weapon","name":"…","slot":"WEAPON_1H","rarity":"RARE",
 "itemLevel":30,"weaponType":"SWORD","damage_min":10.0,"damage_max":20.0,"attackSpeed":1.2,
 "durability":100,"modifierIds":["<ModifierDefinition._id>"]}
```

Редкости: `COMMON, UNCOMMON, RARE, EPIC, UNIQUE, MYTHICAL`. Слоты: `HELMET, BODY, GLOVES, RING, BOOTS, WINGS, BELT, WEAPON_1H, WEAPON_2H, QUIVER, SHIELD, AMULET`. Цену считает сервер в конструкторе — клиент её не пишет.

`modifierIds` — это **пул ссылок**, а не выпавшие значения. Что попадёт на экземпляр, решает сервер: PREFIX и SUFFIX роллятся в количестве, которое задаёт редкость, а IMPLICIT, ENCHANTMENT, CORRUPTION и UNIQUE попадают на каждую копию.

## Инвентарь персонажа

Экземпляр предмета — отдельный документ коллекции `CharacterEquipment`: `{_id, characterId, equipmentId, params, rarity, corrupted, equippedSlot, version}`. `equippedSlot = null` означает «лежит в инвентаре»; слот задаёт шаблон, поэтому клиент его не выбирает.

`rarity` с 0.9.1 принадлежит копии, а не шаблону: шаблон задаёт лишь то, с чем предмет падает, дальше редкость двигают сферы. `corrupted = true` закрывает предмет навсегда — сервер отклонит на нём любую следующую сферу. В карточке инвентаря редкость экземпляра перекрывает редкость шаблона.

| Операция | Запрос |
|---|---|
| Весь инвентарь | `GET /api/v1/character/inventory/equipments?characterId=` |
| Только надетое | `GET /api/v1/character/inventory/equipped?characterId=` |
| Характеристики | `GET /api/v1/character/inventory/stats?characterId=` → `{"STOCK_HEALTH":188.4,…}` |
| Простые предметы | `GET /api/v1/character/inventory/items?characterId=` → `[{itemId,amount}]` |
| Изменить сумку | `POST /api/v1/character/inventory/addItem?characterId=`, тело `[{itemId,amount}]` |
| **Выдать предмет с роллами** | `POST /api/v1/character/inventory/itemToInventory?characterId=&equipmentId=` |
| **Применить сферу** | `POST /api/v1/characterequipment/applyOrb?characterId=&inventoryId=&orbItemId=` |
| Надеть | `POST /api/v1/characterequipment/equip?characterId=&inventoryId=` |
| Снять | `POST /api/v1/characterequipment/unequip?characterId=&inventoryId=` |
| Рецепт | `POST /api/v1/recipe/useRecipe?characterId=&recipeId=`, тело `{ingridientsId,amount}` |
| Промокод | `POST /api/v1/redemptioncodes/useRedeptionCode?characterId=&redemptionCode=` |

Часть команд — POST с аргументами в строке запроса и без тела; клиент отправляет пустое тело, потому что этого требует HTTP-клиент, а не сервер.

Характеристики сводит сервер по формуле `(база + Σ ADD) · (1 + Σ INCREASED/100) · Π (1 + MORE/100)`, где базу дают `stockSkills` персонажа. Клиент печатает пришедшие числа и ничего не пересчитывает.

## Валютные сферы

Сфера — обычный предмет коллекции `items` с категорией `CURRENCY`; подкатегория равна имени элемента серверного `EnumCurrencyOrb` и связывает документ с его поведением. Отдельной коллекции и отдельного маршрута каталога у сфер нет: клиент читает `GET /api/v1/items` и отбирает эту категорию.

```json
{"_id":"…","name":"Chaos Orb","category":"CURRENCY","subCategory":"CHAOS_ORB",
 "description":"Перекатывает аффиксы редкого предмета","price":300}
```

Сервер реализует четырнадцать сфер: `ORB_OF_TRANSMUTATION, ORB_OF_AUGMENTATION, ORB_OF_ALTERATION, ORB_OF_ALCHEMY, REGAL_ORB, CHAOS_ORB, EXALTED_ORB, DIVINE_ORB, ORB_OF_ANNULMENT, ORB_OF_SCOURING, BLESSED_ORB, VAAL_ORB, ORB_OF_CHANCE, MIRROR_OF_KALANDRA`. Сфер POE, завязанных на сокеты, качество, карты и верстак, здесь нет.

`POST /api/v1/characterequipment/applyOrb?characterId=&inventoryId=&orbItemId=` отвечает:

```json
{"message":"Helm rerolled into 4 new affixes","item":{…CharacterEquipment…},"created":null}
```

Всё решает сервер: какую редкость сфера требует, сколько аффиксов роллит, какие оставляет. Сфера списывается из сумки персонажа в той же транзакции, поэтому отказ (`errorCode` семейства `CR_00…`) ничего не стоит. `created` заполняет только `MIRROR_OF_KALANDRA` — это созданная копия, и она сразу `corrupted`. Клиент печатает `message` как есть и не выводит результат сам.

Редкость задаёт вместимость аффиксов: `COMMON 0`, `UNCOMMON 1+1`, `RARE 3+3`, `EPIC 4+3`, `MYTHICAL 4+4`, `UNIQUE` не роллит аффиксов вовсе. Это правило сервера — клиент на него не опирается и ничего по нему не считает.

## Модификаторы

Описание и его диапазоны разнесены по двум коллекциям.

```json
// modifierdefinition
{"_id":"…","code":"life_and_mana","name":"Life and Mana","source":"PREFIX","tags":["life"],
 "effects":[{"stat":"STOCK_HEALTH","operation":"ADD"},{"stat":"STOCK_MANA","operation":"ADD"}]}
// modifiertier
{"_id":"…","modifierId":"…","tier":1,"minItemLevel":84,"weight":100,
 "values":[{"valueMin":46.0,"valueMax":48.0},{"valueMin":10.0,"valueMax":12.0}]}
```

Составной модификатор меняет несколько статов сразу: у него больше одного эффекта, и на экземпляре ему соответствует столько же значений в том же порядке. Тир 1 — лучший. Выпавший модификатор экземпляра: `{modifierId, tierId, tier, values:[Double]}`.

Дополнительно: `GET /api/v1/modifierdefinition/byCode?code=`, `GET /api/v1/modifiertier/byModifier?modifierId=`, `GET /api/v1/{collection}/cache/hash` для кэшируемых коллекций.

## Чего у этого сервера нет

Дерева навыков, боя, крафта на верстаке, серверного набора иконок, JWT, поиска и фильтров на сервере, курсорной постраничной выдачи инвентаря. Соответствующие экраны и модели из клиента удалены, а не заглушены. Сферы в 0.9.1 вернулись и реализованы целиком.
