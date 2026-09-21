# Контракт Exile Forge 2.0

Сервер: ветка `claude/tender-pasteur-a36kj2`, коммит `3a07a4f8d3e65a1365f088b4ab710e1c609ee257` (ktor-bestgame 0.14.0).
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

## Локализация: текста в базе больше нет

С 0.14.0 ни один документ Mongo не хранит текста. Сущность хранит **код**, а строки лежат в статических файлах сервера и отдаются без обёртки `{success,data}` и без учётной записи — язык должен читаться до входа.

| Операция | Запрос |
|---|---|
| Список языков | `GET /locale/index.json` → `{default, languages:[{code,label,hash}]}` |
| Словарь | `GET /locale/{code}.json` → плоская таблица `ключ → строка` |

Ключ строится одинаково на обеих сторонах: `<секция>.<КОД>.<поле>`. Секции: `equipment`, `item`, `modifier`, `skilltree`, `class`, `enum`, `error`, `currency`, `system`. Поля — `name` и `description`; у `enum`, `error`, `currency` и `system` ключ двухчастный (`error.AU_002`, `enum.EnumRarity.RARE`).

`hash` из манифеста — отпечаток словаря: клиент хранит скачанный файл вместе с ним (в разрезе сервера и языка) и перекачивает только при расхождении. Без словаря экран показывает коды, а не пустоту: отсутствующий ключ возвращает сам себя.

Текст модификатора — **шаблон** с `{0}`, `{1}` по числу эффектов: составной модификатор нельзя собрать, приклеив числа к названию, потому что порядок слов в языках разный. Аргументы сообщения о сфере (`messageArgs`) — **сами ключи локализации**, поэтому подстановка идёт через тот же словарь.

Ошибки тоже лежат в словаре (`error.<errorCode>`), но конверт ошибки несёт только готовое предложение и код — аргументов, которыми шаблон был заполнен, в нём нет. Поэтому клиент переводит отказ, только если у шаблона нет дырок (17 кодов из 114); в остальных случаях показывается предложение сервера.

Перечисления (`enum.*`) сервер тоже отдаёт, но клиент оставляет **свои** таблицы слотов, редкостей и статов: они принимают язык параметром, а словарь загружен ровно один, и чтение из него ответило бы на запрос английского по-русски. Сервер владеет названиями вещей, клиент — своими подписями.

## Коллекции

Каждая сущность обслуживается одинаковым набором маршрутов, имя сегмента — имя класса сервера в нижнем регистре: `user`, `character`, `characterequipment`, `items`, `equipment`, `recipe`, `redemptioncodes`, `modifierdefinition`, `modifiertier`, `characterclass`, `experiencelevel`, `skilltreenode`, `auctionlot`.

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

`/paged` починен в 0.13.1: `findPaged` считает страницы, а не документы. Но фильтра у общего маршрута по-прежнему нет, и перевод каталога на него стоил бы всех фильтров каталога — слота, редкости, уровня, модификатора в пуле. Поэтому каталог по-прежнему читает коллекцию целиком (`GET /api/v1/{collection}`) и режет её у себя; коллекции небольшие и сеются сервером.

Исключение — аукцион: у него есть собственный `GET /api/v1/auctionlot/search` с фильтром и страницей, и это единственный список в приложении, который сужает сам сервер.

## Экипировка

`Equipment` — sealed-класс с дискриминатором `type`, равным полному имени класса: `features.data.equipment.equipment_data.Weapon | Armor | Accessory`.

```json
{"type":"features.data.equipment.equipment_data.Weapon","code":"IRON_SKULLCAP","slot":"WEAPON_1H","rarity":"RARE",
 "itemLevel":30,"weaponType":"SWORD","damage_min":10.0,"damage_max":20.0,"attackSpeed":1.2,
 "durability":100,"modifierIds":["<ModifierDefinition._id>"]}
```

Редкости: `COMMON, UNCOMMON, RARE, EPIC, UNIQUE, MYTHICAL`. Слоты: `HELMET, BODY, GLOVES, RING, BOOTS, WINGS, BELT, WEAPON_1H, WEAPON_2H, QUIVER, SHIELD, AMULET`. Цену считает сервер в конструкторе — клиент её не пишет.

С 0.10.0 у предмета нет собственных числовых полей под характеристики: `damage_min`, `damage_max`, `attackSpeed` у оружия и `defense` у брони убраны. Броня, урон и скорость атаки задаются готовыми модификаторами в `baseParams` — с фиксированными значениями и без тира, потому что база базового типа не роллится. Единственное число, оставшееся полем предмета, — `durability` у оружия: это не характеристика персонажа.

Требования `requiredLevel`, `requiredStrength`, `requiredDexterity`, `requiredIntelligence` сервер проверяет дважды, и это два разных правила:

- `POST /api/v1/characterequipment/equip` **отказывает**, если требования не выполнены (`CH_013`, «Equipment requirements are not met»);
- уже надетый предмет при потере требований из слота не слетает — он просто перестаёт учитываться и попадает в `inactive` листа характеристик.

Клиент требования только печатает и никогда не блокирует по ним кнопку сам: команда отправляется, отказ сервера показывается.

`modifierIds` — это **пул ссылок**, а не выпавшие значения. Что попадёт на экземпляр, решает сервер: PREFIX и SUFFIX роллятся в количестве, которое задаёт редкость, а IMPLICIT, ENCHANTMENT, CORRUPTION и UNIQUE попадают на каждую копию.

## Инвентарь персонажа

Экземпляр предмета — отдельный документ коллекции `CharacterEquipment`: `{_id, characterId, equipmentId, params, rarity, corrupted, equippedSlot, version}`. `equippedSlot = null` означает «лежит в инвентаре»; слот задаёт шаблон, поэтому клиент его не выбирает.

`rarity` с 0.9.1 принадлежит копии, а не шаблону: шаблон задаёт лишь то, с чем предмет падает, дальше редкость двигают сферы. `corrupted = true` закрывает предмет навсегда — сервер отклонит на нём любую следующую сферу. В карточке инвентаря редкость экземпляра перекрывает редкость шаблона.

| Операция | Запрос |
|---|---|
| Весь инвентарь | `GET /api/v1/character/inventory/equipments?characterId=` |
| Только надетое | `GET /api/v1/character/inventory/equipped?characterId=` |
| Характеристики | `GET /api/v1/character/inventory/stats?characterId=` → `CharacterStats` (см. ниже) |
| Начислить опыт | `POST /api/v1/character/inventory/experience?characterId=&amount=` → документ персонажа |
| Простые предметы | `GET /api/v1/character/inventory/items?characterId=` → `[{itemId,amount}]` |
| Изменить сумку | `POST /api/v1/character/inventory/addItem?characterId=`, тело `[{itemId,amount}]` |
| **Выдать предмет с роллами** | `POST /api/v1/character/inventory/itemToInventory?characterId=&equipmentId=` |
| **Применить сферу** | `POST /api/v1/characterequipment/applyOrb?characterId=&inventoryId=&orbItemId=` |
| Надеть | `POST /api/v1/characterequipment/equip?characterId=&inventoryId=` |
| Снять | `POST /api/v1/characterequipment/unequip?characterId=&inventoryId=` |
| Рецепт | `POST /api/v1/recipe/useRecipe?characterId=&recipeId=`, тело `{ingridientsId,amount}` |
| Промокод | `POST /api/v1/redemptioncodes/useRedeptionCode?characterId=&redemptionCode=` |

Часть команд — POST с аргументами в строке запроса и без тела; клиент отправляет пустое тело, потому что этого требует HTTP-клиент, а не сервер.

## Класс, уровни и расчёт характеристик

`stockSkills` и `params` у персонажа убраны. Вместо них персонаж ссылается на класс полем `classId` — обязательным при создании и неизменяемым потом: базу класса читают при каждом расчёте, и перенос персонажа в другой класс молча переписал бы его историю. Маршрута для такой правки у сервера нет.

```json
// characterclass
{"_id":"…","code":"MARAUDER","name":"Marauder","startNodeCode":"STR_START",
 "baseStats":[{"stat":"STOCK_STRENGTH","value":32.0}],
 "perLevelStats":[{"stat":"STOCK_HEALTH","value":12.0}],
 "params":[{"modifierId":"…","values":[1.0]}]}
// experiencelevel
{"_id":"…","level":3,"experience":300.0,"skillPoints":2}
```

Ответ `stats`:

```json
{"characterId":"…","level":12,"stats":{"STOCK_HEALTH":188.4,…},
 "active":["<CharacterEquipment._id>"],
 "inactive":[{"inventoryId":"…","name":"Iron Skullcap","reasons":["strength: need 30, have 14"]}]}
```

Считается в два прохода: сначала база класса на уровне персонажа плюс дерево навыков, затем экипировка в порядке слотов — каждый следующий предмет проверяется по характеристикам, которые уже дали база, дерево и признанные рабочими предметы. Предмет, чьи требования не выполнены, остаётся в слоте, но не работает и попадает в `inactive` со своей причиной. Клиент печатает и числа, и причины; ни того, ни другого он не вычисляет.

У эффекта модификатора появились `perStat` и `perAmount`: это конверсия вида «+1 к здоровью за каждые 2 Силы». Неполный шаг не засчитывается. Порядок статов на сервере запрещает циклы конверсий — клиент на это не опирается. Флаг `isLocal` у описания говорит, что модификатор сворачивается внутри своего предмета, а наружу отдаёт результат.

## Дерево навыков

Общее дерево — справочник `skilltreenode`. Прокачка персонажа с 0.12.1 лежит **внутри документа персонажа**, в поле `skillNodes`: отдельной коллекции у взятых узлов нет.

```json
// skilltreenode
{"_id":"…","code":"STR_LIFE_1","name":"Крепость","type":"NOTABLE","cost":1,
 "connections":["STR_START"],"positionX":-20,"positionY":10,
 "params":[{"modifierId":"…","values":[10.0]}]}
```

Виды узлов: `START, SMALL, NOTABLE, KEYSTONE`. Связи двусторонние и записаны в обоих концах. Координаты нужны только для отрисовки.

| Операция | Запрос |
|---|---|
| Дерево персонажа | `GET /api/v1/character/skilltree/state?characterId=` → `CharacterSkillTreeState` |
| Взять узел | `POST /api/v1/character/skilltree/allocate?characterId=&nodeCode=` |
| Вернуть узел | `POST /api/v1/character/skilltree/refund?characterId=&nodeCode=` |
| Сбросить дерево | `POST /api/v1/character/skilltree/reset?characterId=` |

Все четыре отвечают состоянием целиком: `{characterId, total, spent, available, nodes}`. Взятый узел — снимок `{code, params, name, type, cost, description}` без собственного `_id`. Очки даёт таблица уровней. Стартовый узел класса **выдаётся при создании персонажа** и стоит ноль очков, поэтому дерево нового персонажа не пустое. Сброс — это респек: он оставляет персонажа на стартовом узле, а не обнуляет дерево. Дальше берут только соседей уже взятых; вернуть узел можно, только если остальное дерево не повиснет, а стартовый — лишь полным сбросом. Форма дерева намеренно не копируется: связи и координаты берутся из актуального справочника. Все эти правила проверяет сервер; клиент называет код узла и показывает отказ.

## Аукцион

Коллекция `auctionlot`. Открывается с уровня, который задаёт константа сервера (`CONST_AUCTION_MIN_LEVEL`), и порог стоит на **всех** маршрутах, включая просмотр витрины: отказ `AU_002`. Клиент копии этого числа не держит и узнаёт порог только из отказа.

Пока лот на витрине, товар лежит **внутри него**, а не у продавца: экземпляр экипировки уходит из `CharacterEquipment` прямо в лот, стаки списываются со склада. Поэтому выставить предмет дважды или надеть выставленный нельзя, а надетый не выставить вовсе (`AU_010` — сначала снять).

```json
{"_id":"…","sellerId":"…","sellerName":"Изгнанник","kind":"EQUIPMENT|ITEM",
 "equipment":{…CharacterEquipment…},"itemId":"","amount":1,
 "priceOrbId":"<Items._id категории CURRENCY>","price":40,
 "itemCode":"IRON_SKULLCAP","slot":"HELMET","rarity":"RARE","itemLevel":30,
 "status":"ACTIVE|SOLD|CANCELLED","buyerId":null}
```

| Операция | Запрос |
|---|---|
| Витрина | `GET /api/v1/auctionlot/search?characterId=&page=&size=` + поля фильтра |
| Свои лоты | `GET /api/v1/auctionlot/my?characterId=` — и активные, и закрытые |
| Выставить экипировку | `POST /api/v1/auctionlot/sell/equipment?characterId=&inventoryId=&priceOrbId=&price=` |
| Выставить предметы | `POST /api/v1/auctionlot/sell/item?characterId=&itemId=&amount=&priceOrbId=&price=` |
| Купить | `POST /api/v1/auctionlot/buy?characterId=&lotId=` |
| Снять с продажи | `POST /api/v1/auctionlot/cancel?characterId=&lotId=` |

Поля фильтра: `kind`, `title`, `slot`, `rarity`, `minItemLevel`, `maxItemLevel`, `priceOrbId`, `maxPrice`, `sellerId`, `excludeSellerId`, `lang`. Названия в лоте нет — есть `itemCode`, поэтому поиск по тексту сервер превращает в поиск по кодам, и `lang` говорит, на каком языке игрок набрал запрос; клиент шлёт его только вместе с непустым `title`. Пропущенное поле — «не фильтровать», а **незнакомое значение перечисления сервер отклоняет**, поэтому клиент не отправляет пустые поля вовсе. Поиск по названию экранирует спецсимволы: игрок присылает текст, а не регулярное выражение. Закрытые лоты на витрине не показываются никогда.

Цена назначается **только в сферах**: `priceOrbId` обязан быть предметом категории `CURRENCY`, иначе лот не выставится (`AU_007`). Оплата, передача товара и закрытие лота идут одной транзакцией, поэтому нехватка сфер не оставляет ни списанных денег, ни потерянного предмета. Свой лот купить нельзя (`AU_006`). Снятие возвращает товар продавцу; закрытые лоты остаются историей торгов.

Все поля витрины (`itemCode`, `slot`, `rarity`, `itemLevel`) — снимок предмета на момент выставления, поэтому поиск укладывается в один запрос. Редкость берётся с экземпляра, а не с шаблона: её могли изменить сферы.

## Валютные сферы

Сфера — обычный предмет коллекции `items` с категорией `CURRENCY`; подкатегория равна имени элемента серверного `EnumCurrencyOrb` и связывает документ с его поведением. Отдельной коллекции и отдельного маршрута каталога у сфер нет: клиент читает `GET /api/v1/items` и отбирает эту категорию.

```json
{"_id":"…","code":"CHAOS_ORB","category":"CURRENCY","subCategory":"CHAOS_ORB","price":300}
```

Название и описание — в словаре под `item.<CODE>.name` и `item.<CODE>.description`.

Сервер реализует четырнадцать сфер: `ORB_OF_TRANSMUTATION, ORB_OF_AUGMENTATION, ORB_OF_ALTERATION, ORB_OF_ALCHEMY, REGAL_ORB, CHAOS_ORB, EXALTED_ORB, DIVINE_ORB, ORB_OF_ANNULMENT, ORB_OF_SCOURING, BLESSED_ORB, VAAL_ORB, ORB_OF_CHANCE, MIRROR_OF_KALANDRA`. Сфер POE, завязанных на сокеты, качество, карты и верстак, здесь нет.

`POST /api/v1/characterequipment/applyOrb?characterId=&inventoryId=&orbItemId=` отвечает:

```json
{"messageKey":"currency.chaos","messageArgs":["equipment.IRON_SKULLCAP.name","4"],
 "item":{…CharacterEquipment…},"created":null}
```

Текста здесь нет: фразу собирает клиент по `messageKey` и `messageArgs`, причём **аргументы — тоже ключи локализации**, поэтому предмет называется через тот же словарь.

Всё решает сервер: какую редкость сфера требует, сколько аффиксов роллит, какие оставляет. Сфера списывается из сумки персонажа в той же транзакции, поэтому отказ (`errorCode` семейства `CR_00…`) ничего не стоит. `created` заполняет только `MIRROR_OF_KALANDRA` — это созданная копия, и она сразу `corrupted`. Клиент печатает собранную фразу как есть и не выводит результат сам.

Редкость задаёт вместимость аффиксов: `COMMON 0`, `UNCOMMON 1+1`, `RARE 3+3`, `EPIC 4+3`, `MYTHICAL 4+4`, `UNIQUE` не роллит аффиксов вовсе. Это правило сервера — клиент на него не опирается и ничего по нему не считает.

## Модификаторы

Описание и его диапазоны разнесены по двум коллекциям.

```json
// modifierdefinition
{"_id":"…","code":"LIFE_AND_MANA","source":"PREFIX","tags":["life"],
 "effects":[{"stat":"STOCK_HEALTH","operation":"ADD"},{"stat":"STOCK_MANA","operation":"ADD"}]}
// modifiertier
{"_id":"…","modifierId":"…","tier":1,"minItemLevel":84,"weight":100,
 "values":[{"valueMin":46.0,"valueMax":48.0},{"valueMin":10.0,"valueMax":12.0}]}
```

Текста у определения нет: строка лежит в словаре под `modifier.<CODE>.name` и является шаблоном с `{0}`, `{1}` по числу эффектов. Составной модификатор меняет несколько статов сразу: у него больше одного эффекта, и на экземпляре ему соответствует столько же значений в том же порядке. Тир 1 — лучший. Выпавший модификатор экземпляра: `{modifierId, tierId, tier, values:[Double]}`.

Дополнительно: `GET /api/v1/modifierdefinition/byCode?code=`, `GET /api/v1/modifiertier/byModifier?modifierId=`, `GET /api/v1/{collection}/cache/hash` для кэшируемых коллекций.

## Чего у этого сервера нет

Боя, крафта на верстаке, серверного набора иконок, JWT, поиска и фильтров на сервере, курсорной постраничной выдачи инвентаря. Соответствующие экраны и модели из клиента удалены, а не заглушены. Сферы вернулись в 0.9.1, дерево навыков — в 0.9.2; и то и другое реализовано целиком.

## Совместимость

`SchemaMigrator` убран в 0.10.1 вместе с коллекцией `SchemaVersion`. Формат данных игроков с тех пор менялся несколько раз (класс и дерево у персонажа, база предмета модификаторами), поэтому базу, пережившую 0.9.x, проще пересеять с нуля, чем чинить. В 0.14.0 из документов убраны `name` и `description` — база без кодов не читается вовсе.
